<template>
    <div class="ai-container">
      <!-- 主体内容 -->
      <div class="main-content">
        <!-- 左侧AI列表 -->
        <div class="left-panel">
          <div class="panel-header">
            <span>我的AI助手</span>
            <el-button 
              type="primary" 
              icon="el-icon-plus" 
              size="mini"
              @click="showAddDialog"
            ></el-button>
          </div>
          <div class="ai-list">
            <el-scrollbar style="height:100%">
              <el-menu
                :default-active="activeAi"
                @select="handleSelect"
              >
                <el-menu-item 
                  v-for="item in aiList" 
                  :key="item.interviewerId" 
                  :index="item.interviewerId"
                >
                  <span>{{ item.name || '未命名AI' }}</span>
                  <el-button 
                    type="text" 
                    icon="el-icon-delete" 
                    class="delete-btn"
                    @click.stop="deleteAi(item.interviewerId)"
                  ></el-button>
                </el-menu-item>
              </el-menu>
            </el-scrollbar>
          </div>
        </div>
  
        <!-- 分割线 -->
        <div class="divider"></div>
  
        <!-- 右侧AI设置详情 -->
        <div class="right-panel">
          <div v-if="activeAi" class="detail-container">
            <div class="detail-header">
              <h2>{{ currentAi.name || '未命名AI' }}</h2>
            </div>
            
            <el-form :model="currentAi" label-width="120px" class="ai-form">
              <!-- 知识库选择 -->
              <el-form-item label="知识库">
                <el-select 
                  v-model="currentAi.knowledgeBaseId" 
                  placeholder="请选择知识库"
                  style="width: 100%"
                >
                  <el-option
                    v-for="db in databases"
                    :key="db.knowledgeBaseId"
                    :label="db.databaseName"
                    :value="db.knowledgeBaseId"
                  ></el-option>
                </el-select>
              </el-form-item>
  
              <!-- 提示词设置 -->
              <el-form-item label="提示词模板">
                <el-input
                  type="textarea"
                  :rows="5"
                  v-model="currentAi.customPrompt"
                  placeholder="请输入自定义提示词"
                ></el-input>
              </el-form-item>
  
              <!-- AI设置项 -->
              <!-- 修改设置项的绑定方式 -->
              <el-form-item 
                v-for="setting in currentAi.settingsList" 
                :key="setting.id"
                :label="setting.settingName"  
              >
                <div class="setting-item">
                  <el-tooltip 
                    :content="setting.description"  
                    placement="top"
                  >
                    <i class="el-icon-info"></i>
                  </el-tooltip>

                  <el-slider
                    v-model="setting.extent"       
                    :min="1"
                    :max="10"
                    show-input
                    style="width: 80%"
                  ></el-slider>
                </div>
              </el-form-item>

  
              <el-form-item>
                <el-button 
                  type="primary" 
                  @click="saveAi"
                >
                  保存设置
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <div v-else class="empty-state">
            <el-empty description="请从左侧选择一个AI或创建新的AI"></el-empty>
          </div>
        </div>
      </div>
  
      <!-- 添加AI对话框 -->
      <el-dialog title="新建AI助手" :visible.sync="addDialogVisible" width="30%">
        <el-form :model="newAi" label-width="80px">
          <el-form-item label="名称" required>
            <el-input 
              v-model="newAi.name" 
              placeholder="请输入AI名称"
            ></el-input>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="addDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="addAi">确 定</el-button>
        </span>
      </el-dialog>
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        activeAi: null,
        aiList: [],
        currentAi: {
          interviewerId: '',
          name: '',
          userId: null,
          knowledgeBaseId: '',
          customPrompt: '',
          settingsList: [],
        },
        newAi: {
          interviewerId: '',
          name: '',
          userId: null,
          knowledgeBaseId: '',
          customPrompt: '',
          settingsList: [],
        },
        databases: [],
        allSettings: [],
        addDialogVisible: false
      }
    },
    async mounted() {
      await this.fetchAiList();
      await this.fetchDatabases();
      await this.fetchAiSettings();
    },
    methods: {
      async fetchAiList() {
        try {
          const res = await this.$axios.get('/api/Interviewer/list');
          console.log(res.data)
          this.aiList = res.data || [];
        } catch (error) {
          this.$message.error('获取AI列表失败');
          console.error(error);
        }
      },
      async fetchDatabases() {
        try {
          const res = await this.$axios.get('/api/MilvusDatabase');
          this.databases = res.data || [];
        } catch (error) {
          this.$message.error('获取知识库列表失败');
          console.error(error);
        }
      },
      async fetchAiSettings() {
        try {
          const res = await this.$axios.get('/api/Interviewer/getAiSettings');
          this.allSettings = res.data || [];
          console.log(this.allSettings)
          
        } catch (error) {
          this.$message.error('获取AI设置项失败');
          console.error(error);
        }
      },
      handleSelect(index) {
  this.activeAi = index;
  const selectedAi = this.aiList.find(ai => ai.interviewerId === index);
  if (selectedAi) {
    this.currentAi = JSON.parse(JSON.stringify(selectedAi));
    
    // 创建现有设置的映射表（以id为key）
    const existingSettingsMap = new Map();
    if (this.currentAi.settingsList) {
      this.currentAi.settingsList.forEach(item => {
        existingSettingsMap.set(item.id, item.extent); // 只保存extent值
      });
    }
    
    // 重新构建settingsList：合并allSettings的属性和保存的extent值
    this.currentAi.settingsList = this.allSettings.map(template => {
      return {
        ...template,                // 展开模板属性（id, settingName, description）
        extent: existingSettingsMap.has(template.id) 
               ? existingSettingsMap.get(template.id) 
               : 5                  // 默认值
      };
    });
  }
},
      showAddDialog() {
        this.newAi = {
          name: '',
          knowledgeBaseId: this.databases[0]?.knowledgeBaseId || '',
          customPrompt: '',
          settingsList: this.allSettings.map(template => ({
            ...template,    // 继承模板属性
            extent: 5       // 设置默认值
          }))
        };
        this.addDialogVisible = true;
      },
      async addAi() {
        if (!this.newAi.name) {
          this.$message.warning('请输入AI名称');
          return;
        }
        
        try {
          await this.$axios.post('/api/Interviewer/saveOrUpdate', this.newAi);
          this.$message.success('添加成功');
          this.addDialogVisible = false;
          await this.fetchAiList();
        } catch (error) {
          this.$message.error('添加失败');
          console.error(error);
        }
      },
      async saveAi() {
        try {
          await this.$axios.post('/api/Interviewer/saveOrUpdate', this.currentAi);
          this.$message.success('保存成功');
          await this.fetchAiList();
        } catch (error) {
          this.$message.error('保存失败');
          console.error(error);
        }
      },
      deleteAi(id) {
        this.$confirm('确定要删除这个AI吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          try {
            await this.$axios.delete(`/api/Interviewer/delete/${id}`);
            this.$message.success('删除成功');
            await this.fetchAiList();
            if (this.activeAi === id) {
              this.activeAi = null;
            }
          } catch (error) {
            this.$message.error('删除失败');
            console.error(error);
          }
        }).catch(() => {});
      }
    }
  }
  </script>
  
  <style scoped>
  .ai-container {
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  
  .main-content {
    display: flex;
    height: calc(100vh - 60px);
  }
  
  .left-panel {
    width: 250px;
    height: 100%;
    border-right: 1px solid #e6e6e6;
    display: flex;
    flex-direction: column;
  }
  
  .panel-header {
    padding: 15px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #e6e6e6;
  }
  
  .ai-list {
    flex: 1;
    overflow: hidden;
  }
  
  .divider {
    width: 1px;
    background-color: #e6e6e6;
  }
  
  .right-panel {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
  }
  
  .detail-container {
    max-width: 800px;
    margin: 0 auto;
  }
  
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  
  .ai-form {
    margin-top: 20px;
  }
  
  .setting-item {
    display: flex;
    align-items: center;
    width: 100%;
  }
  
  .setting-item .el-icon-info {
    margin-right: 10px;
    color: #909399;
    cursor: pointer;
  }
  
  .setting-desc {
    margin-right: 20px;
    color: #909399;
    font-size: 12px;
    flex-shrink: 0;
  }
  
  .delete-btn {
    float: right;
    padding: 0;
    margin-left: 10px;
  }
  
  .empty-state {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  </style>
  