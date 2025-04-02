<template>
    <div class="knowledge-container">
  
      
  
      <!-- 主体内容 -->
      <div class="main-content">
        <!-- 左侧知识库列表 -->
        <div class="left-panel">
          <div class="panel-header">
            <span>我的知识库</span>
            <el-button 
              type="primary" 
              icon="el-icon-plus" 
              size="mini"
              @click="showAddDialog"
            ></el-button>
          </div>
          <div class="library-list">
            <el-scrollbar style="height:100%">
              <el-menu
                :default-active="activeLibrary"
                @select="handleSelect"
              >
                <el-menu-item 
                  v-for="item in libraries" 
                  :key="item.knowledgeBaseId" 
                  :index="item.knowledgeBaseId"
                >
                  <span>{{ item.databaseName }}</span>
                  <el-button 
                    type="text" 
                    icon="el-icon-delete" 
                    class="delete-btn"
                    @click.stop="deleteLibrary(item.knowledgeBaseId)"
                  ></el-button>
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
              <h2>{{ currentLibrary.name }}</h2>
              <el-button 
                type="primary" 
                icon="el-icon-upload" 
                @click="showUploadDialog"
              >
                上传文件
              </el-button>
            </div>
            
            <div class="file-list">
              <el-table :data="files" style="width: 100%">
                <el-table-column prop="name" label="文件名" width="180">
                  <template #default="{row}">
                    <div class="file-name">
                      <i :class="getFileIcon(row.type)"></i>
                      <span>{{ row.name }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="size" label="大小" width="120">
                  <template #default="{row}">
                    {{ formatFileSize(row.size) }}
                  </template>
                </el-table-column>
                <el-table-column prop="updateTime" label="更新时间" width="180"></el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="{row}">
                    <el-button 
                      type="text" 
                      icon="el-icon-download"
                      @click="downloadFile(row.id)"
                    ></el-button>
                    <el-button 
                      type="text" 
                      icon="el-icon-delete"
                      @click="deleteFile(row.id)"
                    ></el-button>
                  </template>
                </el-table-column>
              </el-table>
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
            <el-input 
              type="textarea" 
              v-model="newLibrary.description" 
              placeholder="请输入知识库描述"
            ></el-input>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="addDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="addLibrary">确 定</el-button>
        </span>
      </el-dialog>
  
      <!-- 上传文件对话框 -->
      <el-dialog title="上传文件" :visible.sync="uploadDialogVisible" width="50%">
        <el-upload
          class="upload-area"
          drag
          action="#"
          multiple
          :auto-upload="false"
          :on-change="handleFileChange"
        >
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
              <el-button 
                type="text" 
                icon="el-icon-close" 
                @click="removeSelectedFile(index)"
              ></el-button>
            </li>
          </ul>
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button @click="uploadDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="uploadFiles">开始上传</el-button>
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
        //   console.log(this.libraries)
        //   if (this.libraries.length > 0 && !this.activeLibrary) {
        //     this.activeLibrary = this.libraries[0].knowledgeBaseId
        //     console.log(this.activeLibrary)
        //     this.fetchFiles()
        //   }
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
    //   console.log(index)
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
      }).catch(() => {})
    },
    
    showUploadDialog() {
      this.selectedFiles = []
      this.uploadDialogVisible = true
    },
    
    handleFileChange(file, fileList) {
      this.selectedFiles = fileList
    },
    
    removeSelectedFile(index) {
      this.selectedFiles.splice(index, 1)
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
      `/api/MilvusDatabase/${this.activeLibrary}/files`,  // 路径变量knowledgeBaseId对应activeLibrary
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    )
    
    // 根据后端GlobalResult结构调整判断逻辑
    if (res.success) {  // 假设GlobalResult中有success字段表示成功
      this.$message.success(res.message || '文件上传成功')
      this.uploadDialogVisible = false
      this.fetchFiles()
    } else {
      this.$message.error(res.message || '文件上传失败')
    }
  } catch (error) {
    console.error('文件上传异常:', error)
    // 适配后端返回的错误信息结构
    const errorMsg = error.response?.data?.message || error.message
    this.$message.error('文件上传异常: ' + errorMsg)
  }
},
    
    // 下载文件
    downloadFile(id) {
      const file = this.files.find(item => item.id === id)
      if (!file || !file.url) {
        this.$message.warning('文件下载链接无效')
        return
      }
      
      // 创建隐藏的下载链接
      const link = document.createElement('a')
      link.href = file.url
      link.download = file.name
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      this.$message.success(`开始下载: ${file.name}`)
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
      }).catch(() => {})
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
    height: 100vh;
    display: flex;
    flex-direction: column;
    background-color: #f5f7fa;
  }
  
  .header {
    padding: 15px 20px;
    background-color: #409EFF;
    color: white;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  
  .user-info {
    color: white;
    font-size: 16px;
  }
  
  .main-content {
    display: flex;
    flex: 1;
    overflow: hidden;
  }
  
  .left-panel {
    width: 250px;
    background-color: white;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #e6e6e6;
  }
  
  .panel-header {
    padding: 15px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e6e6e6;
    font-weight: bold;
  }
  
  .library-list {
    flex: 1;
    overflow: hidden;
  }
  
  .el-menu {
    border-right: none;
  }
  
  .el-menu-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .delete-btn {
    padding: 0;
    color: #F56C6C;
  }
  
  .divider {
    width: 1px;
    background-color: #e6e6e6;
    margin: 0 5px;
  }
  
  .right-panel {
    flex: 1;
    background-color: white;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  
  .detail-container {
    padding: 20px;
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  
  .file-list {
    flex: 1;
    overflow: auto;
  }
  
  .file-name {
    display: flex;
    align-items: center;
  }
  
  .file-name i {
    margin-right: 8px;
    font-size: 18px;
  }
  
  .empty-state {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
  }
  
  .upload-area {
    margin-bottom: 20px;
  }
  
  .selected-files {
    margin-top: 20px;
    padding: 10px;
    background-color: #f5f7fa;
    border-radius: 4px;
  }
  
  .selected-files ul {
    list-style: none;
    padding: 0;
    margin: 10px 0 0;
  }
  
  .selected-files li {
    padding: 5px 0;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  </style>
  