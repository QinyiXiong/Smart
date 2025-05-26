<template>
  <div class="knowledge-container">
    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 左侧知识库列表 -->
      <div class="left-panel no-horizontal-scroll">
        <div class="panel-header">
          <span>我的知识库</span>
          <el-button type="primary" icon="el-icon-plus" size="mini" style="border-radius: 30px;"
            @click="showAddDialog"></el-button>
        </div>
        <div class="library-list">
          <el-scrollbar style="height:100%">
            <el-menu :default-active="activeLibrary" @select="handleSelect">
              <el-menu-item v-for="item in libraries" :key="item.knowledgeBaseId" :index="item.knowledgeBaseId"
                style="display: flex; justify-content: space-between; align-items: center">
                <span>{{ item.databaseName.length > 10 ? item.databaseName.substring(0, 10) + '...' : item.databaseName }}</span>
                <el-popover
                  placement="right"
                  width="110"
                  trigger="hover"
                  :popper-class="'library-popover'"
                >
                  <div class="action-menu">
                    <div class="action-item" @click.stop="renameLibrary(item.knowledgeBaseId)">
                      <i class="el-icon-edit"></i>
                      <span>重命名</span>
                    </div>
                    <div class="action-item delete" @click.stop="deleteLibrary(item.knowledgeBaseId)">
                      <i class="el-icon-delete"></i>
                      <span>删除</span>
                    </div>
                  </div>
                  <el-button slot="reference" type="text" icon="el-icon-more" class="more-btn"></el-button>
                </el-popover>
              </el-menu-item>
            </el-menu>
          </el-scrollbar>
        </div>
      </div>

      <!-- 分割线 -->
      <div class="divider"></div>

      <!-- 右侧知识库详情 -->
      <div class="right-panel">
        <div v-if="activeLibrary" class="detail-container">
          <div class="detail-header">
            <h2>{{ currentLibrary.databaseName>10 ?currentLibrary.databaseName.substring(0.,10)+'...':currentLibrary.databaseName || '未命名知识库' }}</h2>
            <el-button type="primary" icon="el-icon-upload" @click="showUploadDialog">
              上传文件
            </el-button>
          </div>

          <div class="file-list">
            <el-scrollbar style="height:100%" class="vertical-only-scrollbar">
              <el-table :data="files" style="width: 100%">
                <el-table-column prop="name" label="文件名" width="380">
                  <template #default="{ row }">
                    <div class="file-name">
                      <i :class="getFileIcon(row.type)"></i>
                      <span>{{ row.name }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="size" label="大小" width="220">
                  <template #default="{ row }">
                    {{ formatFileSize(row.size) }}
                  </template>
                </el-table-column>
                <el-table-column prop="updateTime" label="更新时间" width="280"></el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button type="text" icon="el-icon-delete" @click="deleteFile(row.id)"></el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-scrollbar>
          </div>
        </div>

        <div v-else class="empty-state">
          <el-empty description="请从左侧选择一个知识库或创建新的知识库"></el-empty>
        </div>
      </div>
    </div>

    <!-- 添加知识库对话框 -->
    <el-dialog title="新建知识库" :visible.sync="addDialogVisible" width="30%">
      <el-form :model="newLibrary" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="newLibrary.name" placeholder="请输入知识库名称"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="newLibrary.description" placeholder="请输入知识库描述"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="addDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="addLibrary">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 上传文件对话框 - 已修改为与a.txt中一致 -->
    <el-dialog title="上传文件" :visible.sync="uploadDialogVisible" width="600px" @close="clearUploadFiles"
      class="upload-dialog">
      <el-upload class="upload-area" drag action="#" ref="fileUpload" multiple :auto-upload="false"
        :on-change="handleFileChange" :show-file-list="false">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">
          支持上传pdf/docx/txt等格式文件，单个文件不超过50MB
        </div>
      </el-upload>

      <div class="selected-files" v-if="selectedFiles.length > 0">
        <h4>已选择文件：</h4>
        <ul>
          <li v-for="(file, index) in selectedFiles" :key="index">
            {{ file.name }} ({{ formatFileSize(file.size) }})
            <el-button type="text" icon="el-icon-close" @click="removeSelectedFile(index)"
              class="file-remove-btn"></el-button>
          </li>
        </ul>
      </div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="uploadDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="uploadFiles">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      activeLibrary: null,
      libraries: [],
      files: [],
      addDialogVisible: false,
      uploadDialogVisible: false,
      newLibrary: {
        name: '',
        description: ''
      },
      selectedFiles: []
    }
  },
  computed: {
    currentLibrary() {
      return this.libraries.find(item => item.knowledgeBaseId === this.activeLibrary) || {}
    }
  },
  created() {
    this.fetchLibraries()
  },
  methods: {
    // 获取知识库列表
    async fetchLibraries() {
      try {
        const res = await this.$axios.get('/api/MilvusDatabase')
        if (res.code === 0) {
          this.libraries = res.data
        } else {
          this.$message.error('获取知识库列表失败: ' + (res.message || '未知错误'))
        }
      } catch (error) {
        console.error('获取知识库列表异常:', error)
        this.$message.error('获取知识库列表异常: ' + error.message)
      }
    },

    // 获取指定知识库的文件列表
    async fetchFiles() {
      if (!this.activeLibrary) return

      try {
        const res = await this.$axios.get(`/api/MilvusDatabase/${this.activeLibrary}/files`)
        if (res.code === 0) {
          this.files = res.data.map(file => ({
            id: file.milvusFileId,
            name: file.fileInfo.fileName,
            type: file.fileInfo.type,
            size: file.fileInfo.size,
            updateTime: new Date().toLocaleString(), // 这里应该使用实际的上传时间
            url: file.fileInfo.url
          }))
        } else {
          this.$message.error('获取文件列表失败: ' + (res.message || '未知错误'))
        }
      } catch (error) {
        console.error('获取文件列表异常:', error)
        this.$message.error('获取文件列表异常: ' + error.message)
      }
    },

    handleSelect(index) {
      this.activeLibrary = index
      this.fetchFiles()
    },

    showAddDialog() {
      this.addDialogVisible = true
    },

    // 创建知识库
    async addLibrary() {
      if (!this.newLibrary.name) {
        this.$message.error('请输入知识库名称')
        return
      }

      try {
        const res = await this.$axios.post('/api/MilvusDatabase', {
          databaseName: this.newLibrary.name,
          description: this.newLibrary.description
        })

        if (res.code === 0) {
          this.$message.success('知识库创建成功')
          this.addDialogVisible = false
          this.newLibrary = { name: '', description: '' }
          this.fetchLibraries()
        } else {
          this.$message.error('知识库创建失败: ' + (res.message || '未知错误'))
        }
      } catch (error) {
        console.error('创建知识库异常:', error)
        this.$message.error('创建知识库异常: ' + error.message)
      }
    },

    // 重命名知识库
    renameLibrary(id) {
      this.$prompt('请输入新的知识库名称', '重命名', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '名称不能为空'
      }).then(async ({ value }) => {
        try {
          // 找到对应的知识库
          const library = this.libraries.find(item => item.knowledgeBaseId === id);
          if (library) {
            const res = await this.$axios.put(`/api/MilvusDatabase/${id}`, {
              ...library,
              databaseName: value
            });
            
            if (res.code === 0) {
              this.$message.success('重命名成功');
              this.fetchLibraries();
            } else {
              this.$message.error('重命名失败: ' + (res.message || '未知错误'));
            }
          }
        } catch (error) {
          console.error('重命名知识库异常:', error);
          this.$message.error('重命名失败: ' + error.message);
        }
      }).catch(() => {});
    },

    // 删除知识库
    async deleteLibrary(id) {
      this.$confirm('确定要删除该知识库吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$axios.delete(`/api/MilvusDatabase/${id}`)
          if (res.code === 0) {
            this.$message.success('知识库删除成功')
            if (this.activeLibrary === id) {
              this.activeLibrary = null
              this.files = []
            }
            this.fetchLibraries()
          } else {
            this.$message.error('知识库删除失败: ' + (res.message || '未知错误'))
          }
        } catch (error) {
          console.error('删除知识库异常:', error)
          this.$message.error('删除知识库异常: ' + error.message)
        }
      }).catch(() => { })
    },

    // 显示上传对话框
    showUploadDialog() {
      this.selectedFiles = []
      this.uploadDialogVisible = true
    },

    // 清空上传文件列表
    clearUploadFiles() {
      if (this.$refs.fileUpload) {
        this.$refs.fileUpload.clearFiles();
      }
    },

    // 文件选择变化处理
    handleFileChange(file, fileList) {
      this.selectedFiles = fileList;
    },

    // 移除选择的文件
    removeSelectedFile(index) {
      this.selectedFiles.splice(index, 1);
    },

    // 上传文件
    async uploadFiles() {
      if (this.selectedFiles.length === 0) {
        this.$message.warning('请选择要上传的文件')
        return
      }

      const formData = new FormData()
      this.selectedFiles.forEach(file => {
        formData.append('files', file.raw)  // 注意这里参数名要与后端@RequestParam("files")一致
      })

      try {
        const res = await this.$axios.post(
          `/api/MilvusDatabase/${this.activeLibrary}/files`,
          formData,
          {
            headers: {
              'Content-Type': 'multipart/form-data'
            }
          }
        )

        if (res.code === 0) {  // 修改为与其他API调用一致的判断方式
          this.$message.success('文件上传成功')
          this.uploadDialogVisible = false
          this.fetchFiles()
        } else {
          this.$message.error(res.message || '文件上传失败')
        }
      } catch (error) {
        console.error('文件上传异常:', error)
        const errorMsg = error.response?.data?.message || error.message
        this.$message.error('文件上传异常: ' + errorMsg)
      }
    },

    // 删除文件
    async deleteFile(id) {
      this.$confirm('确定要删除该文件吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$axios.delete(
            `/api/MilvusDatabase/${this.activeLibrary}/files/${id}`
          )

          if (res.code === 0) {
            this.$message.success('文件删除成功')
            this.fetchFiles()
          } else {
            this.$message.error('文件删除失败: ' + (res.message || '未知错误'))
          }
        } catch (error) {
          console.error('删除文件异常:', error)
          this.$message.error('删除文件异常: ' + error.message)
        }
      }).catch(() => { })
    },

    getFileIcon(type) {
      const iconMap = {
        pdf: 'el-icon-document',
        docx: 'el-icon-document',
        word: 'el-icon-document',
        ppt: 'el-icon-picture-outline',
        xls: 'el-icon-document',
        txt: 'el-icon-document',
        mp3: 'el-icon-headset',
        wav: 'el-icon-headset',
        mp4: 'el-icon-video-camera',
        default: 'el-icon-document'
      }
      return iconMap[type.toLowerCase()] || iconMap.default
    },

    formatFileSize(size) {
      if (size < 1024) {
        return size + 'B'
      } else if (size < 1024 * 1024) {
        return (size / 1024).toFixed(1) + 'KB'
      } else {
        return (size / (1024 * 1024)).toFixed(1) + 'MB'
      }
    }
  }
}
</script>

<style scoped>
.knowledge-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.main-content {
  display: flex;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-left: 40px;
  margin-top: 30px;
  margin-bottom: 30px;
  margin-right: 40px;
  overflow: hidden;
}

.left-panel {
  width: 350px;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
}

.panel-header {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
}

.panel-header span {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.library-list {
  flex: 1;
  overflow: hidden;
  background: #fff;
}

.el-menu {
  border-right: none;
}

.el-menu-item {
  height: 50px;
  line-height: 50px;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
  position: relative;
}

.el-menu-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.el-menu-item.is-active {
  color: #409eff;
  background-color: #ecf5ff;
}

.more-btn {
  color: #909399;
  font-size: 16px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.el-menu-item:hover .more-btn {
  opacity: 1;
}

.divider {
  width: 1px;
  background-color: #ebeef5;
}

.right-panel {
  padding-top: 24px;
  padding-left: 100px;
  width: 1040px;
  background: #fff;
}

.detail-container {
  margin: 0 auto;
  width: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
}

.detail-header h2 {
  color: #303133;
  font-size: 20px;
  font-weight: 500;
  margin: 0;
}

.file-list {
  background: #f5f7fa;
  width: 1000px;
  height: 600px;
  border-radius: 8px;
  padding: 20px;
  overflow: hidden;
}

.file-list .el-scrollbar__wrap {
  overflow-x: hidden;
}

.file-name {
  display: flex;
  align-items: center;
}

.file-name i {
  margin-right: 10px;
  font-size: 18px;
  color: #606266;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.el-button--primary {
  background-color: #409eff;
  border-color: #409eff;
}

.el-button--primary:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

.el-dialog {
  border-radius: 8px;
}

.el-dialog__header {
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
}

.el-dialog__title {
  font-size: 16px;
  color: #303133;
}

.el-dialog__body {
  padding: 20px;
}

.el-dialog__footer {
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
}

.el-table {
  background: transparent;
}

.el-table::before {
  height: 0;
}

.el-table th {
  background-color: #f5f7fa;
}

/* 隐藏水平滚动条 */
.no-horizontal-scroll ::v-deep .el-scrollbar__wrap {
  overflow-x: hidden !important;
}

/* 使用深度选择器隐藏水平滚动条 */
:deep(.vertical-only-scrollbar .el-scrollbar__wrap) {
  overflow-x: hidden !important;
  padding-bottom: 0 !important;
}

/* 上传对话框样式 - 已修改为与a.txt中一致 */
.upload-dialog {
  border-radius: 8px;
}

.upload-dialog .el-dialog__header {
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
}

.upload-dialog .el-dialog__body {
  padding: 20px;
}

/* 上传区域样式 */
.upload-area {
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 20px;
  text-align: center;
  background-color: #f5f7fa;
  margin-bottom: 20px;
}

.upload-area:hover {
  border-color: #c0c4cc;
}

.el-upload__text {
  font-size: 14px;
  color: #606266;
  margin: 10px 0;
}

.el-upload__text em {
  color: #409eff;
  font-style: normal;
}

.el-upload__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 10px;
}

/* 已选择文件列表样式 */
.selected-files {
  margin-top: 20px;
}

.selected-files h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #606266;
}

.selected-files ul {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 200px;
  overflow-y: auto;
}

.selected-files li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.file-remove-btn {
  padding: 0;
  color: #f56c6c;
}

/* 对话框底部按钮样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

/* 操作菜单样式 - 从interviewer.vue复制 */
.action-menu {
  padding: 4px 0;
}

.action-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
  margin: 0 4px;
}

.action-item:hover {
  background-color: #f5f7fa;
}

.action-item.delete:hover {
  color: #f56c6c;
}

.action-item i {
  margin-right: 8px;
  font-size: 14px;
}

/* 自定义弹窗样式 */
.library-popover {
  padding: 0;
  min-width: 110px;
  border-radius: 22px !important;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}
</style>