package com.sdumagicode.backend.util.embeddingUtil;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.sdumagicode.backend.entity.milvus.KnowledgeRecord;
import com.sdumagicode.backend.entity.milvus.KnowledgeSearchVO;

import io.grpc.StatusRuntimeException;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.GetVersionResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.UpsertParam;
import io.milvus.param.highlevel.dml.DeleteIdsParam;
import io.milvus.param.highlevel.dml.QuerySimpleParam;
import io.milvus.param.highlevel.dml.response.DeleteResponse;
import io.milvus.param.highlevel.dml.response.QueryResponse;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class MilvusClient {

    private static final int VECTOR_DIM = 1024;

    private static final int MAX_CHUNK_SIZE = 400; // 最大分块大小
    private static final int OVERLAP_SIZE = 100;    // 分块重叠大小
    private static final String ID_FIELD = "id";
    private static final String VECTOR_FIELD = "title_vector";
    private static final String TITLE = "title";
    private static final String CONTENT = "content";

    private static final String TYPE_ID = "type_id";
    private final MilvusServiceClient client;
 
    private final EmbeddingClient embeddingClient;

    private final ExecutorService embeddingExecutor;

    private static final Logger logger = LoggerFactory.getLogger(MilvusClient.class);


    public MilvusClient(MilvusServiceClient client, EmbeddingClient embeddingClient) {
        this.client = client;
        this.embeddingClient = embeddingClient;

        // 初始化线程池
        int threadCount = Runtime.getRuntime().availableProcessors();
        this.embeddingExecutor = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * 优化后的文档分割方法 - 适合RAG数据库的分段存储
     * @param content 原始文本内容
     * @return 分割后的文本块列表
     */
    public List<String> splitDocument(String content) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }


        int length = content.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + MAX_CHUNK_SIZE, length);

            // 优先在句子边界分割
            if (true && end < length) {
                int sentenceEnd = findSentenceEnd(content, start, end);
                if (sentenceEnd > start) {
                    end = sentenceEnd;
                } else {
                    // 找不到句子边界则尝试在段落或空格分割
                    int paragraphEnd = findParagraphEnd(content, start, end);
                    if (paragraphEnd > start) {
                        end = paragraphEnd;
                    } else {
                        int lastSpace = findLastSpace(content, start, end);
                        end = (lastSpace > start) ? lastSpace : end;
                    }
                }
            }

            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 计算下一个起始位置（考虑重叠）
            start = (end - OVERLAP_SIZE > start) ? end - OVERLAP_SIZE : end;

            // 强制推进至少1个字符，防止死循环
            if (start >= end) {
                start = end;
            }
        }

        return chunks;
    }

    // 查找句子结束位置（简单实现）
    private int findSentenceEnd(String content, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = content.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                // 确保不是缩写或数字中的点
                if (i + 1 < content.length() && Character.isWhitespace(content.charAt(i + 1))) {
                    return i + 1;
                }
            }
        }
        return -1; // 没找到句子边界
    }

    // 查找段落结束位置
    private int findParagraphEnd(String content, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            if (content.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return -1; // 没找到段落边界
    }

    // 查找最后一个空格位置
    private int findLastSpace(String content, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            if (Character.isWhitespace(content.charAt(i))) {
                return i + 1; // 包含空格后的位置
            }
        }
        return end; // 没找到空格
    }

    /**
     * 生成集合名称
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
     * @return 格式为"user_{userId}_kb_{knowledgeBaseId}"
     */
    private String generateCollectionName(Long userId, String knowledgeBaseId) {
        return String.format("user_%d_kb_%s", userId, knowledgeBaseId);
    }

    public R<RpcStatus> createCollection(Long userId, String knowledgeBaseId) {
        String collectionName = generateCollectionName(userId, knowledgeBaseId);
        FieldType id = FieldType.newBuilder()
                .withName(ID_FIELD)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .withDescription(ID_FIELD)
                .build();
        FieldType type_id  = FieldType.newBuilder()
                .withName("type_id")
                .withDataType(DataType.Int64)
                .withDescription("type_id")
                .build();
        FieldType chunk_index  = FieldType.newBuilder()
                .withName("chunk_index")
                .withDataType(DataType.Int64)
                .withMaxLength(10000)
                .withDescription("chunk_index")
                .build();
        FieldType content  = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(10000)
                .withDescription("content")
                .build();
        FieldType title_vector = FieldType.newBuilder()
                .withName(VECTOR_FIELD)
                .withDescription(VECTOR_FIELD)
                .withDataType(DataType.FloatVector)
                .withDimension(VECTOR_DIM)
                .build();
 
        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .addFieldType(id)
                .addFieldType(type_id)
                .addFieldType(chunk_index)
                .addFieldType(content)
                .addFieldType(title_vector)
                .build();
 
        R<RpcStatus> response = client.createCollection(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
        return response;
    }
 
    public Boolean isExitCollection(Long userId, String knowledgeBaseId){
        String collectionName = generateCollectionName(userId, knowledgeBaseId);
        R<Boolean> response = client.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build());
        return response.getData();
    }
 
 
    public R<RpcStatus> loadCollection(Long userId, String knowledgeBaseId) {
        String collectionName = generateCollectionName(userId, knowledgeBaseId);
        LoadCollectionParam param = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withReplicaNumber(1)
                .withSyncLoad(Boolean.TRUE)
                .withSyncLoadWaitingInterval(500L)
                .withSyncLoadWaitingTimeout(30L)
                .build();
        R<RpcStatus> response = client.loadCollection(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
        return response;
    }
 
    public R<RpcStatus> createIndex(Long userId, String knowledgeBaseId) {
        String collectionName = generateCollectionName(userId, knowledgeBaseId);
        CreateIndexParam param = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(VECTOR_FIELD)
                .withIndexType(IndexType.GPU_IVF_FLAT)
                .withMetricType(MetricType.L2)
                .withExtraParam("{\"nlist\":2048}")
                .build();
        R<RpcStatus> response = client.createIndex(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
        return response;
    }
 
    public void insertMilvus(KnowledgeRecord record,Long userId, String knowledgeBaseId) {
        String collectionName = generateCollectionName(userId, knowledgeBaseId);

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", Collections.singletonList(record.getRecordId())));
        fields.add(new InsertParam.Field("file_id", Collections.singletonList(record.getFileId())));
        fields.add(new InsertParam.Field("type_id", Collections.singletonList(1L)));
        fields.add(new InsertParam.Field("content", Collections.singletonList(record.getChunkText())));
        fields.add(new InsertParam.Field("chunk_index", Collections.singletonList(record.getChunkIndex())));
        fields.add(new InsertParam.Field(VECTOR_FIELD, embeddingClient.getEmbedding(record.getChunkText())));
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();
        R<MutationResult> response = new R<>();
        try {
            response = client.insert(insertParam);
        } catch (StatusRuntimeException e) {
            logger.error("gRPC error: ", e);
            logger.error("Status: {}, Description: {}", e.getStatus(), e.getTrailers());
        }
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
    }

//无多线程版本
//    public void batchInsertMilvus(List<KnowledgeRecord> records, Long userId, String knowledgeBaseId) {
//        if (records == null || records.isEmpty()) {
//            throw new IllegalArgumentException("Records list cannot be null or empty");
//        }
//
//        String collectionName = generateCollectionName(userId, knowledgeBaseId);
//
//        List<InsertParam.Field> fields = new ArrayList<>();
//        for (KnowledgeRecord record : records) {
//            fields.add(new InsertParam.Field("id", Collections.singletonList(record.getRecordId())));
//            fields.add(new InsertParam.Field("file_id", Collections.singletonList(record.getFileId())));
//            fields.add(new InsertParam.Field("type_id", Collections.singletonList(1L)));
//            fields.add(new InsertParam.Field("content", Collections.singletonList(record.getChunkText())));
//            fields.add(new InsertParam.Field("chunk_index", Collections.singletonList(record.getChunkIndex())));
//            fields.add(new InsertParam.Field(VECTOR_FIELD, embeddingClient.getEmbedding(record.getChunkText())));
//        }
//
//        // 构建批量插入参数
//        InsertParam insertParam = InsertParam.newBuilder()
//                .withCollectionName(collectionName)
//                .withFields(fields)
//                .build();
//
//        R<MutationResult> response = new R<>();
//        try {
//            response = client.insert(insertParam);
//        } catch (StatusRuntimeException e) {
//            logger.error("gRPC error: ", e);
//            logger.error("Status: {}, Description: {}", e.getStatus(), e.getTrailers());
//        }
//        if (response.getStatus() != R.Status.Success.getCode()) {
//            System.out.println(response.getMessage());
//        }
//    }


public void batchInsertMilvus(List<KnowledgeRecord> records, Long userId, String knowledgeBaseId) {
    if (records == null || records.isEmpty()) {
        throw new IllegalArgumentException("Records list cannot be null or empty");
    }
    String collectionName = generateCollectionName(userId, knowledgeBaseId);

    // 使用并行流处理embedding获取
    List<CompletableFuture<Void>> futures = records.stream()
            .map(record -> CompletableFuture.runAsync(() -> {
                if(record.getChunkText() == null || record.getChunkText().isEmpty()){
                    return;
                }
                List<List<Float>> embeddings = embeddingClient.getEmbedding(record.getChunkText());
                // 将多个embedding拼接成一个长向量
                List<Float> concatenated = embeddings.stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
                record.setEmbedding(concatenated);
            }, embeddingExecutor))
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // 准备各字段的数据集合
    List<Long> idList = new ArrayList<>();
    List<String> fileIdList = new ArrayList<>();
    List<Long> typeIdList = new ArrayList<>();
    List<String> contentList = new ArrayList<>();
    List<Long> chunkIndexList = new ArrayList<>();
    List<List<Float>> vectorList = new ArrayList<>();

    // 只保留一个循环来收集数据
    for (KnowledgeRecord record : records) {
        idList.add(record.getRecordId());
        fileIdList.add(record.getFileId());
        typeIdList.add(1L); // 固定值
        contentList.add(record.getChunkText());
        chunkIndexList.add(Long.valueOf(record.getChunkIndex()));

        if (record.getEmbedding() != null && !record.getEmbedding().isEmpty()) {
            vectorList.add(record.getEmbedding());
        } else {
            logger.warn("Empty embedding for record: {}", record.getRecordId());
            vectorList.add(new ArrayList<>());
        }
    }

    // 构建插入参数
    List<InsertParam.Field> fields = new ArrayList<>();
    fields.add(new InsertParam.Field("id", idList));
    fields.add(new InsertParam.Field("file_id", fileIdList));
    fields.add(new InsertParam.Field("type_id", typeIdList));
    fields.add(new InsertParam.Field("content", contentList));
    fields.add(new InsertParam.Field("chunk_index", chunkIndexList));
    fields.add(new InsertParam.Field(VECTOR_FIELD, vectorList));

    // 构建批量插入参数
    InsertParam insertParam = InsertParam.newBuilder()
            .withCollectionName(collectionName)
            .withFields(fields)
            .build();

    R<MutationResult> response = new R<>();
    try {
        response = client.insert(insertParam);
    } catch (StatusRuntimeException e) {
        logger.error("gRPC error: ", e);
        logger.error("Status: {}, Description: {}", e.getStatus(), e.getTrailers());
        e.printStackTrace();
        throw new RuntimeException("Failed to insert data into Milvus", e);
    }

    if (response.getStatus() != R.Status.Success.getCode()) {
        logger.error("Milvus insert failed: {}", response.getMessage());
        throw new RuntimeException("Failed to insert data into Milvus: " + response.getMessage());
    }
}



//    public void batchInsertMilvus(List<KnowledgeRecord> records, Long userId, String knowledgeBaseId) {
//        if (records == null || records.isEmpty()) {
//            throw new IllegalArgumentException("Records list cannot be null or empty");
//        }
//
//        String collectionName = generateCollectionName(userId, knowledgeBaseId);
//
//        // 使用并行流处理embedding获取
//        List<CompletableFuture<Void>> futures = records.stream()
//                .map(record -> CompletableFuture.runAsync(() -> {
//                    List<List<Float>> embedding = embeddingClient.getEmbedding(record.getChunkText());
//                    record.setEmbedding(embedding);
//                }, embeddingExecutor)) // 使用自定义线程池
//                .collect(Collectors.toList());
//
//        // 等待所有异步任务完成
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//        // 构建插入参数
//        List<InsertParam.Field> fields = new ArrayList<>();
//        for (KnowledgeRecord record : records) {
//            fields.add(new InsertParam.Field("id", Collections.singletonList(record.getRecordId())));
//            fields.add(new InsertParam.Field("file_id", Collections.singletonList(record.getFileId())));
//            fields.add(new InsertParam.Field("type_id", Collections.singletonList(1L)));
//            fields.add(new InsertParam.Field("content", Collections.singletonList(record.getChunkText())));
//            fields.add(new InsertParam.Field("chunk_index", Collections.singletonList(record.getChunkIndex())));
//            fields.add(new InsertParam.Field(VECTOR_FIELD, record.getEmbedding())); // 使用预先计算好的embedding
//            InsertParam insertParam = InsertParam.newBuilder()
//                    .withCollectionName(collectionName)
//                    .withFields(fields)
//                    .build();
//            R<MutationResult> response = new R<>();
//            try {
//                response = client.insert(insertParam);
//            } catch (StatusRuntimeException e) {
//                logger.error("gRPC error: ", e);
//                logger.error("Status: {}, Description: {}", e.getStatus(), e.getTrailers());
//            }
//            if (response.getStatus() != R.Status.Success.getCode()) {
//                System.out.println(response.getMessage());
//            }
//        }
//
//        // 构建批量插入参数
//
//
//
//    }



    public void updateMilvus(KnowledgeRecord record,Long userId, String knowledgeBaseId) {
        String collectionName = generateCollectionName(userId, knowledgeBaseId);
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", Collections.singletonList(record.getRecordId())));
        fields.add(new InsertParam.Field("file_id", Collections.singletonList(record.getFileId())));
        fields.add(new InsertParam.Field("type_id", Collections.singletonList(1L)));
        fields.add(new InsertParam.Field("content", Collections.singletonList(record.getChunkText())));
        fields.add(new InsertParam.Field("chunk_index", Collections.singletonList(record.getChunkIndex())));
        fields.add(new InsertParam.Field(VECTOR_FIELD, embeddingClient.getEmbedding(record.getChunkText())));
        UpsertParam insertParam = UpsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();
        R<MutationResult> response = client.upsert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
    }
 
    public List list(Long userId, String knowledgeBaseId){
        String collectionName = generateCollectionName(userId, knowledgeBaseId);
        List<QueryResultsWrapper.RowRecord> rowRecords = new ArrayList<>();
        QuerySimpleParam querySimpleParam = QuerySimpleParam.newBuilder()
                .withCollectionName(collectionName)
                .withOutputFields(Lists.newArrayList("*"))
                .withFilter("id > 0")
                .withLimit(100L)
                .withOffset(0L)
                .build();
        R<QueryResponse> response = client.query(querySimpleParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
 
        for (QueryResultsWrapper.RowRecord rowRecord : response.getData().getRowRecords()) {
            System.out.println(rowRecord);
            rowRecords.add(rowRecord);
        }
        return rowRecords;
    }
 
    public void delete(String id,Long userId, String knowledgeBaseId) {
        String collectionName = generateCollectionName(userId, knowledgeBaseId);
        List<String> ids = Lists.newArrayList(id);
        DeleteIdsParam param = DeleteIdsParam.newBuilder()
                .withCollectionName(collectionName)
                .withPrimaryIds(ids)
                .build();
 
        R<DeleteResponse> response = client.delete(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
 
        for (Object deleteId : response.getData().getDeleteIds()) {
            System.out.println(deleteId);
        }
    }

    public void deleteBatch(List<Long> ids,Long userId, String knowledgeBaseId) {
        String collectionName = generateCollectionName(userId, knowledgeBaseId);

        DeleteIdsParam param = DeleteIdsParam.newBuilder()
                .withCollectionName(collectionName)
                .withPrimaryIds(ids)
                .build();

        R<DeleteResponse> response = client.delete(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }

        for (Object deleteId : response.getData().getDeleteIds()) {
            System.out.println(deleteId);
        }
    }
 
 
    public List search(String keyword, Integer topK,Long userId, String knowledgeBaseId) {
        List<KnowledgeSearchVO> result = new ArrayList<>();

        if(knowledgeBaseId == null || knowledgeBaseId.isEmpty()){
            return result;
        }
        String collectionName = generateCollectionName(userId, knowledgeBaseId);

        List<List<Float>> targetVectors = embeddingClient.getEmbedding(keyword);
        SearchParam param = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withTopK(topK)
                .withVectors(targetVectors)
                .withVectorFieldName(VECTOR_FIELD)
                .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                .withOutFields(Arrays.asList(ID_FIELD,TYPE_ID,CONTENT ))
                .withParams("{\"nprobe\":10}")
                .build();
        R<SearchResults> response = client.search(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            //System.out.println(response.getMessage());
        }
        if(response == null || response.getData() == null){
            return result;
        }
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        //System.out.println("Search results:");
        for (int i = 0; i < targetVectors.size(); ++i) {
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(i);
            for (SearchResultsWrapper.IDScore score:scores) {
                //System.out.println(score);
                Float scoreValue = score.getScore();
                Map<String, Object> fieldValues = score.getFieldValues();
                KnowledgeSearchVO vo = JSON.parseObject(JSON.toJSONString(fieldValues), KnowledgeSearchVO.class);
                vo.setRelevanceScore(scoreValue);
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * 删除指定用户和知识库的集合
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
     * @return RpcStatus响应结果
     */
    public R<RpcStatus> dropCollection(Long userId, String knowledgeBaseId) {
        String collectionName = generateCollectionName(userId, knowledgeBaseId);

        // 先检查集合是否存在
        R<Boolean> hasCollection = client.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build());

        if (!hasCollection.getData()) {
            // 集合不存在，返回失败响应
            return R.failed(R.Status.CollectionNotExists,
                    String.format("Collection %s does not exist", collectionName));
        }

        // 执行删除集合操作
        DropCollectionParam param = DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<RpcStatus> response = client.dropCollection(param);

        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }

        return response;
    }

    /**
     * 根据用户ID、知识库ID和关键词搜索相关内容，并整合成<RAG>标签包裹的字符串
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
     * @param keyword 搜索关键词
     * @param topK 返回结果数量
     * @return 用<RAG></RAG>标签包围的整合后的搜索内容
     */
    public String buildRAGContent(Long userId, String knowledgeBaseId, String keyword, Integer topK) {
        if(keyword == null || keyword.isEmpty()){
            return "";
        }

        // 1. 执行搜索获取结果列表
        List<KnowledgeSearchVO> searchResults = search(keyword, topK, userId, knowledgeBaseId);

        // 2. 构建StringBuilder来拼接结果
        StringBuilder ragContent = new StringBuilder();
        ragContent.append("<RAG>用户的知识库：\n");

        // 3. 遍历搜索结果并拼接内容
        for (KnowledgeSearchVO result : searchResults) {
            ragContent.append("【相关度得分: ").append(result.getRelevanceScore()).append("】\n");
            ragContent.append(result.getChunkText()).append("\n\n");
        }

        // 4. 添加结束标签
        ragContent.append("</RAG>");

        return ragContent.toString();
    }
}