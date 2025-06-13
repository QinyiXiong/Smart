package com.sdumagicode.backend.controller.chat;

import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.entity.milvus.MilvusDatabase;
import com.sdumagicode.backend.entity.milvus.MilvusFile;
import com.sdumagicode.backend.service.MilvusService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/MilvusDatabase")
@RequiresPermissions(value = "user")
public class MilvusDatabaseController {

    @Autowired
    private MilvusService milvusService;

    /**
     * 创建Milvus数据库
     * @return 创建结果
     */
    @PostMapping
    public GlobalResult<Boolean> createDatabase(@RequestBody MilvusDatabase milvusDatabase) {
        boolean result = milvusService.createMilvusDatabase(milvusDatabase);
        return result ?
                GlobalResultGenerator.genSuccessResult("知识库创建成功") :
                GlobalResultGenerator.genErrorResult("知识库创建失败");
    }

    /**
     * 获取所有Milvus数据库列表
     * @return 数据库列表
     */
    @GetMapping
    public GlobalResult<List<MilvusDatabase>> getAllDatabases() {
        List<MilvusDatabase> databases = milvusService.getMilvusBases();
        return GlobalResultGenerator.genSuccessResult(databases);
    }

    /**
     * 更新知识库信息
     * @param knowledgeBaseId 知识库ID
     * @param milvusDatabase 知识库更新信息
     * @return 更新结果
     */
    @PutMapping("/{knowledgeBaseId}")
    public GlobalResult<Boolean> updateDatabase(
            @PathVariable String knowledgeBaseId,
            @RequestBody MilvusDatabase milvusDatabase) {
        milvusDatabase.setKnowledgeBaseId(knowledgeBaseId);
        boolean result = milvusService.updateMilvusDatabase(milvusDatabase);
        return result ?
                GlobalResultGenerator.genSuccessResult("知识库更新成功") :
                GlobalResultGenerator.genErrorResult("知识库更新失败");
    }

    /**
     * 获取指定知识库的文件列表
     * @param knowledgeBaseId 知识库ID
     * @return 文件列表
     */
    @GetMapping("/{knowledgeBaseId}/files")
    public GlobalResult<List<MilvusFile>> getFilesByDatabase(
            @PathVariable String knowledgeBaseId) {
        List<MilvusFile> files = milvusService.getMilvusFileList(knowledgeBaseId);
        return GlobalResultGenerator.genSuccessResult(files);
    }

    /**
     * 删除指定知识库
     * @param knowledgeBaseId 知识库ID
     * @return 删除结果
     */
    @DeleteMapping("/{knowledgeBaseId}")
    public GlobalResult<Boolean> deleteDatabase(
            @PathVariable String knowledgeBaseId) {
        boolean result = milvusService.deleteMilvusDatabase(knowledgeBaseId);
        return result ?
                GlobalResultGenerator.genSuccessResult("知识库删除成功") :
                GlobalResultGenerator.genErrorResult("知识库删除失败");
    }

    /**
     * 上传文件到指定知识库
     * @param knowledgeBaseId 知识库ID
     * @param files 文件列表
     * @return 上传结果
     */
    @PostMapping("/{knowledgeBaseId}/files")
    public GlobalResult<Boolean> uploadFiles(
            @PathVariable String knowledgeBaseId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            boolean result = milvusService.uploadFile(knowledgeBaseId, files);
            return result ?
                    GlobalResultGenerator.genSuccessResult("文件上传成功") :
                    GlobalResultGenerator.genErrorResult("文件上传失败");
        } catch (Exception e) {
            e.printStackTrace();
            return GlobalResultGenerator.genErrorResult("文件上传异常: " + e.getMessage());
        }
    }

    /**
     * 删除知识库中的指定文件
     * @param knowledgeBaseId 知识库ID
     * @param milvusFileId 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{knowledgeBaseId}/files/{milvusFileId}")
    public GlobalResult<Boolean> deleteFile(
            @PathVariable String knowledgeBaseId,
            @PathVariable String milvusFileId) throws IOException {
        boolean result = milvusService.deleteMilvusFilse(knowledgeBaseId, milvusFileId);
        return result ?
                GlobalResultGenerator.genSuccessResult("文件删除成功") :
                GlobalResultGenerator.genErrorResult("文件删除失败");
    }
}
