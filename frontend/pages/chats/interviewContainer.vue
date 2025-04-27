<template>
  <div class="interview-container">
    <!-- 左侧导航栏 -->
    <div class="left-sidebar">
      <div class="panel-header">
        <span>AI面试官</span>
      </div>

      <!-- 切换标签 -->
      <div class="tab-switcher">
        <div class="custom-tabs">
          <div
            class="tab-button"
            :class="{ active: activeTab === 'interviewer' }"
            @click="handleTabSwitch('interviewer')"
          >
            <i class="el-icon-user-solid"></i>
            <span>面试官</span>
          </div>
          <div
            class="tab-button"
            :class="{ active: activeTab === 'record' }"
            @click="handleTabSwitch('record')"
          >
            <i class="el-icon-chat-dot-round"></i>
            <span>面试记录</span>
          </div>
          <div class="slider" :class="activeTab"></div>
        </div>
      </div>

      <!-- AI面试官列表 -->
      <div class="ai-interviewer-section" v-show="activeTab === 'interviewer'">
        <el-scrollbar class="no-horizontal-scroll" style="height:100%">
          <div class="interviewer-list">
            <div
              class="interviewer-list"
              v-for="interviewer in aiList"
              :key="interviewer.interviewerId"
              :class="['interviewer-item', { 'active': activeInterviewer === interviewer.interviewerId }]"
              @click="handleInterviewerSelect(interviewer.interviewerId)"
            >
              <div class="interviewer-avatar">
                <i class="el-icon-user-solid"></i>
              </div>
              <div class="interviewer-name">{{ interviewer.name }}</div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <!-- 聊天记录列表 -->
      <div class="chat-records-section" v-show="activeTab === 'record' && showChatRecords">
        <div class="section-header">
          <h3 class="section-title">话题</h3>
          <!-- <el-button
            type="primary"
            size="mini"
            @click="handleNewChat"
            v-if="currentInterviewer"
            icon="el-icon-plus"
            circle
          ></el-button> -->

        </div>
        <el-scrollbar style="height:calc(100% - 110px)" class="no-horizontal-scroll-1">
          <div class="chat-records-list">
            <div
              v-for="record in chatRecords"
              :key="record.chatId"
              :class="['chat-record-item', { 'active': activeChatRecord === record.chatId.toString() }]"
              @click="handleChatRecordSelect(record.chatId.toString())"
            >
              <div class="record-content">
                <i class="el-icon-chat-dot-round"></i>
                <span class="record-title">{{ record.topic || '未命名对话' }}</span>
              </div>

              <div class="record-actions">
                <el-popover
                  placement="right"
                  width="110"
                  trigger="hover"
                  :popper-class="'record-popover'"
                >
                  <div class="action-menu">
                    <div class="action-item" @click.stop="handleRenameRecord(record)">
                      <i class="el-icon-edit"></i>
                      <span>重命名</span>
                    </div>
                    <div class="action-item delete" @click.stop="handleDeleteRecord(record.chatId)">
                      <i class="el-icon-delete"></i>
                      <span>删除</span>
                    </div>
                  </div>
                  <i slot="reference" class="el-icon-more record-more"></i>
                </el-popover>
              </div>
            </div>
          </div>
        </el-scrollbar>
        <div class="new-chat-button-container" v-if="currentInterviewer">
          <button class="new-chat-button" @click="handleNewChat">
            <i class="el-icon-plus"></i>
            <span>新建面试对话</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <chat-area
      :current-interviewer="currentInterviewer"
      :chat-record-id="activeChatRecord"
    />

    <!-- 重命名对话框 -->
    <el-dialog
      title="重命名对话"
      :visible.sync="renameDialogVisible"
      width="30%"
      :close-on-click-modal="false"
    >
      <el-input v-model="newTopicName" placeholder="请输入新的话题名称"></el-input>
      <span slot="footer" class="dialog-footer">
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import ChatArea from './chatArea.vue'
import axios from '../../store/utils/interceptor'

export default {
  props: {
    value: {
      type: String,
      default: 'interviewer'
    }
  },
  components: {
    ChatArea
  },
  data() {
    return {
      aiList: [],
      chatRecords: [],
      activeInterviewer: null,
      activeChatRecord: null,
      currentInterviewer: null,
      showChatRecords: false,
      activeTab: this.value,
      renameDialogVisible: false,
      newTopicName: '',
      currentRecordToRename: null
    }
  },
  watch: {
    value(newVal) {
      this.activeTab = newVal;
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
        this.$message.error('获取面试官列表失败')
        console.error(error)
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
    handleChatRecordSelect(chatId) {
      this.activeChatRecord = chatId
    },
    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
    },
    handleTabSwitch(tab) {
      this.activeTab = tab;
      this.$emit('input', tab);
      this.$emit('change', tab);
      if (tab === 'record' && this.activeInterviewer) {
        this.loadChatRecords()
      }
    },
    handleRenameRecord(record) {
      this.currentRecordToRename = record;
      this.newTopicName = record.topic || '';
      this.renameDialogVisible = true;
    },
    async confirmRename() {
      if (!this.currentRecordToRename) return;

      try {
        const res = await axios.post('/api/chat/updateChatTopic', {
          chatId: this.currentRecordToRename.chatId,
          topic: this.newTopicName
        });

        this.$message.success('重命名成功');
        await this.loadChatRecords();
        this.renameDialogVisible = false;
      } catch (error) {
        this.$message.error('重命名失败');
        console.error(error);
      }
    }
  }
}
</script>

<style scoped>
.interview-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #f5f7fa;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin: 30px 40px;
  overflow: hidden;
  overflow-x: hidden;
}

.left-sidebar {
  width: 350px;
  height: 100%;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 21px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
}

.panel-header span {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.tab-switcher {
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
}

.custom-tabs {
  position: relative;
  display: flex;
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.tab-button {
  flex: 1;
  padding: 10px 0;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tab-button i {
  font-size: 16px;
}

.tab-button.active {
  color: #409eff;
}

.slider {
  position: absolute;
  height: calc(100% - 8px);
  width: calc(50% - 8px);
  background-color: white;
  border-radius: 6px;
  top: 4px;
  left: 4px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 1;
}

.slider.record {
  transform: translateX(calc(100% + 8px));
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
}

.section-header .section-title {
  margin: 0;
  font-size: 14px;
  color: #606266;
  font-weight: 600;
}

.ai-interviewer-section,
.chat-records-section {
  flex: 1;
  overflow: hidden;
  overflow-x: hidden;
}

/* 面试官列表样式 */
.interviewer-list {
  padding: 10px;
}

.interviewer-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.interviewer-item:hover {
  background-color: #f5f7fa;
}

.interviewer-item.active {
  background-color: #ecf5ff;
}

.interviewer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #e6f1fc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.interviewer-avatar i {
  font-size: 20px;
  color: #409eff;
}

.interviewer-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

/* 聊天记录列表样式 */
.chat-records-list {
  padding: 10px;
  overflow-x: hidden;
}

.chat-record-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  margin-bottom: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-sizing: border-box;
  width: 100%;
}

.chat-record-item:hover {
  background-color: #f5f7fa;
}

.chat-record-item.active {
  background-color: #ecf5ff;
}

.record-content {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.record-content i {
  font-size: 16px;
  color: #909399;
  margin-right: 10px;
}

.record-title {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.record-more {
  font-size: 16px;
  color: #909399;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.chat-record-item:hover .record-more {
  opacity: 1;
}

/* 操作菜单样式 */
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
  /* 添加圆角 */
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
.record-popover {
  padding: 0;
  min-width: 110px;
}

/* 对话框样式 */
.el-dialog__body {
  padding: 20px 20px 10px !important;
}

.record-popover {
  padding: 0;
  min-width: 110px;
  border-radius: 22px !important;
  /* 增加圆角半径 */
  overflow: hidden;
  /* 确保内容不会溢出圆角 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  /* 可选：添加更柔和的阴影 */
}

.new-chat-button-container {
  padding: 16px;
  margin-top: auto;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: center;
}

.new-chat-button {
  width: auto;
  min-width: 180px;
  height: 40px;
  /* 固定高度代替min-height */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  /* 移除顶部padding，保持左右padding */
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  background: linear-gradient(to right, #409eff, #1890ff);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
  line-height: 1;
  /* 确保文字行高正常 */
}

.new-chat-button:hover {
  background: linear-gradient(to right, #66b1ff, #40a9ff);
  transform: translateY(-1px);
  box-shadow: 0 6px 12px rgba(64, 158, 255, 0.4);
}

.new-chat-button:active {
  transform: translateY(1px);
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.4);
}

.new-chat-button i {
  margin-right: 8px;
  font-size: 16px;
}

/* 隐藏水平滚动条 */
.no-horizontal-scroll ::v-deep .el-scrollbar__wrap {
  overflow-x: hidden !important;
}

.no-horizontal-scroll-1 ::v-deep .el-scrollbar__wrap {
  overflow-x: hidden !important;
}

/* 确保内容不会溢出触发水平滚动 */
.interviewer-list {
  white-space: nowrap;
  /* 如果不需要横向排列则移除 */
  width: 100%;
  box-sizing: border-box;
}
</style>


