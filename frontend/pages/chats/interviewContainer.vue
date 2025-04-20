<template>
    <div class="interview-container">
      <!-- 左侧导航栏 -->
      <div class="left-sidebar">
        <!-- 切换标签 -->
        <div class="tab-switcher">
          <el-radio-group v-model="activeTab" @change="handleTabChange">
            <el-radio-button label="interviewer">面试官</el-radio-button>
            <el-radio-button label="record">面试记录</el-radio-button>
          </el-radio-group>
        </div>
        <!-- AI面试官列表 -->
        <div class="ai-interviewer-section" v-show="activeTab === 'interviewer'">
          <h3 class="section-title">AI面试官</h3>
          <el-menu
            :default-active="activeInterviewer"
            class="interviewer-menu"
            @select="handleInterviewerSelect"
          >
            <el-menu-item
              v-for="interviewer in aiList"
              :key="interviewer.interviewerId"
              :index="interviewer.interviewerId"
            >
              <i class="el-icon-user-solid"></i>
              <span slot="title">{{ interviewer.name }}</span>
            </el-menu-item>
          </el-menu>
        </div>
        <!-- 聊天记录列表 -->
        <div class="chat-records-section" v-show="activeTab === 'record' && showChatRecords">
          <div class="section-header">
            <h3 class="section-title">话题</h3>
            <el-button
              type="primary"
              size="mini"
              @click="handleNewChat"
              v-if="currentInterviewer"
            >新增对话</el-button>
          </div>
          <el-menu
            :default-active="activeChatRecord"
            class="chat-records-menu"
            @select="handleChatRecordSelect"
          >
          <el-menu-item
          v-for="record in chatRecords"
          :key="record.chatId"
          :index="record.chatId.toString()"
          >
            <i class="el-icon-chat-dot-round"></i>
            <span slot="title">{{ record.topic || '未命名对话' }}</span>
            <span class="record-date">{{ formatDate(record.createdAt) }}</span>
            <!-- 添加删除按钮 -->
            <el-button
              type="text"
              size="mini"
              icon="el-icon-delete"
              class="delete-btn"
              @click.stop="handleDeleteRecord(record.chatId)"
            ></el-button>
          </el-menu-item>

          </el-menu>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <chat-area
        :current-interviewer="currentInterviewer"
        :chat-record-id="activeChatRecord"

      />
    </div>
  </template>

  <script>
  import ChatArea from './chatArea.vue'
  import axios from '../../store/utils/interceptor'
  export default {
    components: {
      ChatArea
    },
    data() {
      return {
        aiList: [],
        chatRecords: [],
        activeInterviewer: null,
        activeTab: 'interviewer',
        activeChatRecord: null,
        currentInterviewer: null,
        showChatRecords: false
      }
    },
    async mounted() {
      await this.fetchAiList()
    },
    methods: {
      async fetchAiList() {
        try {
          const res = await axios.get('/api/Interviewer/list')
          this.aiList = res.data || []
        } catch (error) {
          this.$message.error(error)
          console.error(error)
        }
      },
      async loadChatRecords() {
        try {
          const res = await axios.post('/api/chat/getChatRecords', {
            interviewerId: this.activeInterviewer
          })
          this.chatRecords = res.data || []
        } catch (error) {
          this.$message.error('获取聊天记录失败')
          console.error(error)
        }
      },
      async handleNewChat() {
        try {
          const res = await axios.post('/api/chat/saveChatRecords', {
            interviewerId: this.activeInterviewer,

            userId: this.$store.state.user.userId
          })

          this.chatRecords.unshift(res.data)
          this.$message.success('新建对话成功')
          this.activeChatRecord = res.data.chatId.toString()
        } catch (error) {
          this.$message.error('新建对话失败')
          console.error(error)
        }
      },
      async handleInterviewerSelect(interviewerId) {
        this.activeInterviewer = interviewerId
        this.currentInterviewer = this.aiList.find(item => item.interviewerId === interviewerId)
        this.showChatRecords = true

        if (this.activeTab === 'record') {
          await this.loadChatRecords()
        }
      },
      async handleDeleteRecord(chatId) {
        try {
          await this.$confirm('确认删除该对话记录吗?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          })

          const res = await axios.post('/api/chat/deleteChatRecords', {
            chatId: chatId
          })


            this.$message.success('删除成功')
            // 删除后重新加载记录
            await this.loadChatRecords()
            // 如果删除的是当前激活的记录，则清空activeChatRecord
            if (this.activeChatRecord === chatId.toString()) {
              this.activeChatRecord = null
            }

        } catch (error) {
          if (error !== 'cancel') {
            this.$message.error('删除失败')
            console.error(error)
          }
        }
      },
      async loadChatRecords() {
        try {
          const res = await axios.post('/api/chat/getChatRecords', {
            interviewerId: this.activeInterviewer
          })
          // 添加排序逻辑，按创建时间倒序
          this.chatRecords = (res.data || []).sort((a, b) => {
            return new Date(b.createdAt) - new Date(a.createdAt)
          })
        } catch (error) {
          this.$message.error('获取聊天记录失败')
          console.error(error)
        }
      },


      handleChatRecordSelect(chatId) {
        this.activeChatRecord = chatId
      },
      formatDate(dateString) {
        if (!dateString) return ''
        const date = new Date(dateString)
        return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
      },
      handleTabChange(tab) {
        this.activeTab = tab
        if (tab === 'record' && this.currentInterviewer) {
          this.loadChatRecords()
        }
      }
    }
  }
  </script>

  <style scoped>
  .tab-switcher {
    padding: 15px;
    border-bottom: 1px solid #e6e6e6;
  }
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    margin: 10px 0;
  }
  .section-header .section-title {
    margin: 0;
  }
  .delete-btn {
  margin-left: 10px;
  color: #f56c6c;
  font-size: 14px;
  padding: 0;
}

.delete-btn:hover {
  color: #ff0000;
}

  .interview-container {
    display: flex;
    height: 100vh;
    background-color: #f5f7fa;
  }

  .left-sidebar {
    width: 300px;
    background-color: #fff;
    border-right: 1px solid #e6e6e6;
    overflow-y: auto;
    padding: 20px 0;
  }

  .section-title {
    padding: 0 20px;
    margin: 10px 0;
    font-size: 16px;
    color: #666;
  }

  .interviewer-menu, .chat-records-menu {
    border-right: none;
  }

  .el-menu-item {
    height: 60px;
    line-height: 60px;
    display: flex;
    align-items: center;
  }

  .record-date {
    margin-left: auto;
    font-size: 12px;
    color: #999;
  }
  </style>
