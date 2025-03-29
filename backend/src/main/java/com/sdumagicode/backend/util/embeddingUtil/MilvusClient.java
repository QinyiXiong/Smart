package com.sdumagicode.backend.util.embeddingUtil;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
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
import org.springframework.stereotype.Component;
 
import java.util.*;
@Component
public class MilvusClient {
    private static final String COLLECTION_NAME = "FAQ";
    private static final int VECTOR_DIM = 1024;
    private static final String ID_FIELD = "id";
    private static final String VECTOR_FIELD = "title_vector";
    private static final String TITLE = "title";
    private static final String CONTENT = "content";
 
    private final MilvusServiceClient client;
 
    private final EmbeddingClient embeddingClient;
 
 
    public MilvusClient(MilvusServiceClient client, EmbeddingClient embeddingClient) {
        this.client = client;
        this.embeddingClient = embeddingClient;
    }
 
    public R<RpcStatus> createCollection() {
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
        FieldType title  = FieldType.newBuilder()
                .withName("title")
                .withDataType(DataType.VarChar)
                .withMaxLength(10000)
                .withDescription("title")
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
                .withCollectionName(COLLECTION_NAME)
                .addFieldType(id)
                .addFieldType(type_id)
                .addFieldType(title)
                .addFieldType(content)
                .addFieldType(title_vector)
                .build();
 
        R<RpcStatus> response = client.createCollection(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
        return response;
    }
 
    public Boolean isExitCollection(){
        R<Boolean> response = client.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(COLLECTION_NAME)
                        .build());
        return response.getData();
    }
 
 
    public R<RpcStatus> loadCollection() {
        LoadCollectionParam param = LoadCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
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
 
    public R<RpcStatus> createIndex() {
        CreateIndexParam param = CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
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
 
    public void insertMilvus(KnowledgeRecord record) {
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", Collections.singletonList(record.getRecordId())));
        fields.add(new InsertParam.Field("file_id", Collections.singletonList(record.getFileId())));
        fields.add(new InsertParam.Field("chunk_text", Collections.singletonList(record.getChunkText())));
        fields.add(new InsertParam.Field("chunk_index", Collections.singletonList(record.getChunkIndex())));
        fields.add(new InsertParam.Field(VECTOR_FIELD, embeddingClient.getEmbedding(record.getChunkText())));
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFields(fields)
                .build();
        R<MutationResult> response = client.insert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
    }
 
    public void updateMilvus(KnowledgeRecord record) {
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", Collections.singletonList(record.getRecordId())));
        fields.add(new InsertParam.Field("file_id", Collections.singletonList(record.getFileId())));
        fields.add(new InsertParam.Field("chunk_text", Collections.singletonList(record.getChunkText())));
        fields.add(new InsertParam.Field("chunk_index", Collections.singletonList(record.getChunkIndex())));
        fields.add(new InsertParam.Field(VECTOR_FIELD, embeddingClient.getEmbedding(record.getChunkText())));
        UpsertParam insertParam = UpsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFields(fields)
                .build();
        R<MutationResult> response = client.upsert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
    }
 
    public List list(){
        List<QueryResultsWrapper.RowRecord> rowRecords = new ArrayList<>();
        QuerySimpleParam querySimpleParam = QuerySimpleParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
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
 
    public void delete(Integer id) {
        List<Integer> ids = Lists.newArrayList(id);
        DeleteIdsParam param = DeleteIdsParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
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
 
 
    public List search(String keyword, Integer topK) {
        List<KnowledgeSearchVO> result = new ArrayList<>();
        List<List<Float>> targetVectors = embeddingClient.getEmbedding(keyword);
        SearchParam param = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withMetricType(MetricType.L2)
                .withTopK(topK)
                .withVectors(targetVectors)
                .withVectorFieldName(VECTOR_FIELD)
                .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                .withOutFields(Arrays.asList(ID_FIELD, TITLE, CONTENT ))
                .withParams("{\"nprobe\":10}")
                .build();
        R<SearchResults> response = client.search(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }
 
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        System.out.println("Search results:");
        for (int i = 0; i < targetVectors.size(); ++i) {
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(i);
            for (SearchResultsWrapper.IDScore score:scores) {
                System.out.println(score);
                Float scoreValue = score.getScore();
                Map<String, Object> fieldValues = score.getFieldValues();
                KnowledgeSearchVO vo = JSON.parseObject(JSON.toJSONString(fieldValues), KnowledgeSearchVO.class);
                vo.setRelevanceScore(scoreValue);
                result.add(vo);
            }
        }
        return result;
    }
}