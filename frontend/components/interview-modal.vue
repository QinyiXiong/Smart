<template>
  <div class="interview-modal">
    <div v-if="isDevMode" class="debug-toggle">
      <el-button
        size="mini"
        type="text"
        @click="showDebugInfo = !showDebugInfo">
        {{ showDebugInfo ? '隐藏调试' : '显示调试' }}
      </el-button>
    </div>

    <div v-if="showDebugInfo" class="debug-info">
      <p><strong>面试ID:</strong> {{ interviewId }}</p>
      <p><strong>话题:</strong> {{ interview ? interview.topic : 'null' }}</p>
      <p><strong>消息数量:</strong> {{ messageListForShow ? messageListForShow.length : 0 }}</p>
      <p><strong>分支数量:</strong> {{ allBranches ? allBranches.length : 0 }}</p>
      <p><strong>当前分支:</strong> {{ currentBranch ? currentBranch.index : 'null' }}</p>
      <p v-if="apiError"><strong>错误:</strong> {{ apiError }}</p>
      <p><strong>API响应:</strong></p>
      <pre v-if="apiResponse">{{ JSON.stringify(apiResponse, null, 2) }}</pre>
      <pre v-else>暂无响应数据</pre>
    </div>

    <div class="chat-messages" ref="chatMessages">
      <div v-if="loading" class="loading-container">
        <i class="el-icon-loading"></i>
        <span>加载中...</span>
      </div>
      <template v-else-if="messageListForShow && messageListForShow.length > 0">
        <div v-for="(message, index) in messageListForShow" :key="index"
          :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']"
          @mouseenter="message.showActions = true" @mouseleave="message.showActions = false">
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>

          <div class="message-content-wrapper">
            <div class="message-content">
              <div class="message-text" v-html="renderMarkdown(message.content.text, message.role)"></div>
            </div>
          </div>

          <!-- 分支切换按钮 -->
          <div class="branch-switch-container">
            <div v-if="isFirstMessageInBranch(message) && checkSiblings(message)" class="branch-switch"
              :class="{ 'user-branch': message.role === 'user' }">
              <el-button type="text" @click="toggleShowBranch(message)">
                <i :class="message.showBranchTag ? 'el-icon-caret-top' : 'el-icon-caret-bottom'"></i>
                {{ message.showBranchTag ? '收起分支' : '显示分支' }}
              </el-button>
            </div>
          </div>

          <!-- 分支编辑面板 -->
          <div v-if="message.showBranchTag" class="branch-edit-panel"
            :class="{ 'user-branch-panel': message.role === 'user', 'ai-branch-panel': message.role === 'assistant' }">
            <div class="branch-tag-list">
              <div v-for="(sibling, idx) in siblingNodes.find(node => node.branchId === message.branchId)?.siblings || []"
                :key="sibling.index" class="branch-tag-item"
                :class="{ 'active-branch': sibling.index === currentBranchIndex }">
                <span class="branch-tag-text" @click="switchBranch(sibling.index)">{{ sibling.tag || `分支${idx + 1}` }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="empty-message-container">
        <el-empty description="暂无面试对话记录"></el-empty>
      </div>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it';

export default {
  name: 'InterviewModal',

  props: {
    interviewId: {
      type: [String, Number],
      required: true
    }
  },

  data() {
    return {
      interview: null,
      messages: [],  // 原始消息列表，现在仅用于存储
      messageListForShow: [], // 用于显示的消息列表，处理分支逻辑后的结果
      allBranches: [], // 存储所有分支数据
      siblingNodes: [], // 存储当前路径上每个节点的兄弟节点信息
      currentBranch: null, // 当前正在查看的分支
      currentBranchIndex: 0,
      rootBranch: null,  // 根分支
      loading: true,
      md: new MarkdownIt(),
      showDebugInfo: false,
      isDevMode: process.env.NODE_ENV === 'development',
      apiResponse: null,
      apiError: null
    };
  },

  watch: {
    interviewId: {
      immediate: true,
      handler() {
        if (this.interviewId) {
          this.fetchInterviewData();
        }
      }
    }
  },

  methods: {
    async fetchInterviewData() {
      this.loading = true;
      this.apiResponse = null;
      this.apiError = null;
      this.messageListForShow = [];
      this.allBranches = [];
      this.siblingNodes = [];

      try {
        console.log('正在获取面试ID为', this.interviewId, '的记录');

        // 获取所有分支数据
        const response = await this.$axios.post('/api/chat/getAllBranches', {
          chatId: Number(this.interviewId)  // 确保ID是数字类型
        });

        console.log('API响应:', response.data);
        this.apiResponse = response.data;

        if (response.data?.data) {
          this.allBranches = response.data.data;

          // 找到根分支（parentBranchIndex为-1，index为0的通常是根分支）
          this.rootBranch = this.allBranches.find(b => b.index === 0);

          if (this.rootBranch) {
            this.interview = {
              topic: this.rootBranch.topic || '面试对话',
              chatId: this.interviewId
            };

            // 查找第一个有消息的分支作为默认显示分支
            const firstBranchWithMessages = this.allBranches.find(
              b => b.messageLocals && b.messageLocals.length > 0
            );

            // 构建分支路径并显示消息
            if (firstBranchWithMessages) {
              await this.buildPathForTargetBranch(firstBranchWithMessages);
            } else {
              await this.buildPathForTargetBranch(this.rootBranch);
            }
          } else {
            this.$message.warning('未找到面试记录内容');
            console.log('根分支数据不存在');
            this.apiError = '根分支不存在';
          }
        } else if (response.data) { // 处理可能没有data字段的情况
          this.allBranches = response.data;

          // 找到根分支（parentBranchIndex为-1，index为0的通常是根分支）
          this.rootBranch = this.allBranches.find(b => b.index === 0);

          if (this.rootBranch) {
            this.interview = {
              topic: this.rootBranch.topic || '面试对话',
              chatId: this.interviewId
            };

            // 查找第一个有消息的分支作为默认显示分支
            const firstBranchWithMessages = this.allBranches.find(
              b => b.messageLocals && b.messageLocals.length > 0
            );

            // 构建分支路径并显示消息
            if (firstBranchWithMessages) {
              await this.buildPathForTargetBranch(firstBranchWithMessages);
            } else {
              await this.buildPathForTargetBranch(this.rootBranch);
            }
          } else {
            this.$message.warning('未找到面试记录内容');
            console.log('根分支数据不存在');
            this.apiError = '根分支不存在';
          }
        } else {
          this.$message.warning('未找到面试记录内容');
          this.apiError = '返回数据格式不正确';
        }
      } catch (error) {
        console.error('获取面试记录失败:', error);
        this.$message.error('获取面试记录失败: ' + (error.response?.data?.message || error.message));
        this.apiError = '获取面试记录失败: ' + (error.response?.data?.message || error.message);
      } finally {
        this.loading = false;
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }
    },

    // 构建到目标分支的路径
    async buildPathForTargetBranch(targetBranch) {
      if (!targetBranch) return;

      // 1. 向上查找父分支链
      const parentChain = [];
      let current = targetBranch;

      while (current && current.parentBranchIndex !== -1 && current.parentBranchIndex !== "-1") {
        const parent = this.allBranches.find(b => b.index == current.parentBranchIndex);
        if (parent) {
          parentChain.unshift(parent); // 添加到数组开头
          current = parent;
        } else {
          break;
        }
      }

      // 2. 清空消息和兄弟节点列表，重新构建
      this.siblingNodes = [];
      this.messageListForShow = [];

      // 3. 从根分支开始向下构建路径
      let parentBranch = this.rootBranch;

      // 遍历父链中的每个分支
      for (const branch of parentChain) {
        if (branch) {
          if (branch.index != 0) {
            // 获取当前分支的所有消息
            const branchMessages = branch?.messageLocals || [];
            // 将消息添加到展示列表中
            this.messageListForShow.push(...branchMessages);

            // 添加兄弟节点信息
            if (parentBranch && parentBranch.children) {
              this.siblingNodes.push({
                index: branch.index,
                branchId: branch.branchId,
                siblings: parentBranch.children.map(child => ({
                  index: child.branchIndex,
                  tag: child.tag
                }))
              });
            }
          }

          parentBranch = branch;
        }
      }

      // 4. 添加目标分支的兄弟节点信息
      if (parentBranch && parentBranch.children) {
        this.siblingNodes.push({
          index: targetBranch.index,
          branchId: targetBranch.branchId,
          siblings: parentBranch.children.map(child => ({
            index: child.branchIndex,
            tag: child.tag
          }))
        });
      }

      // 5. 显示目标分支的消息
      if (targetBranch.messageLocals && targetBranch.messageLocals.length > 0) {
        this.messageListForShow.push(...targetBranch.messageLocals);
      }

      // 6. 更新当前分支
      this.currentBranch = targetBranch;
      this.currentBranchIndex = targetBranch.index;
    },

    // 检查消息是否有兄弟分支
    checkSiblings(message) {
      const siblingNode = this.siblingNodes.find(b => b.branchId == message.branchId);

      if (siblingNode && siblingNode.siblings) {
        return siblingNode.siblings.length > 1;
      }
      return false;
    },

    // 检查消息是否是分支中的第一条
    isFirstMessageInBranch(message) {
      const branch = this.allBranches.find(b => b.branchId == message.branchId);
      if (!branch || !branch.messageLocals) return false;

      const branchMsgIndex = branch.messageLocals.findIndex(
        msg => msg.messageId == message.messageId
      );

      return branchMsgIndex == 0;
    },

    // 切换显示分支切换面板
    toggleShowBranch(message) {
      if (message.showBranchTag) {
        this.$set(message, 'showBranchTag', false);
      } else {
        // 关闭其他打开的分支面板
        this.messageListForShow.forEach(msg => {
          if (msg.showBranchTag && msg !== message) {
            this.$set(msg, 'showBranchTag', false);
          }
        });
        this.$set(message, 'showBranchTag', true);
      }
    },

    // 切换分支
    async switchBranch(targetIndex) {
      const targetBranch = this.allBranches.find(b => b.index == targetIndex);

      if (targetBranch) {
        await this.buildPathForTargetBranch(targetBranch);
      }
    },

    // 生成UUID
    generateUuid() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0,
          v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    },

    renderMarkdown(text, role) {
      if (role === 'assistant') {
        const rendered = this.md.render(text || '');
        // 添加内联样式
        const styledContent = rendered
          .replace(/<ul>/g, '<ul style="margin: 0px 0; padding-left: 20px;padding-top:0px;padding-bottom:0px;">')
          .replace(/<ol>/g, '<ol style="margin: 0px 0; padding-left: 20px;padding-top:0px;padding-bottom:0px;">')
          .replace(/<li>/g, '<li style="margin: 0px 0; padding: 0; line-height: 1.0;">')
          .replace(/<p>/g, '<p style="margin: 0px 0;padding-top:0px;padding-bottom:0px;">');
        return `<div class="markdown-body">${styledContent}</div>`;
      }
      return text;
    },

    formatTime(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      return date.toLocaleString();
    },

    scrollToBottom() {
      const container = this.$refs.chatMessages;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }
  }
}
</script>

<style scoped>
.interview-modal {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 61px); /* 减去头部高度 */
  width: 100%;
  overflow: hidden;
  max-width: 100%; /* 确保不超出父容器宽度 */
}

.interview-header {
  display: none; /* 隐藏原有的标题，因为已经在父组件中显示标题 */
}

.debug-toggle {
  position: absolute;
  top: 10px;
  right: 20px;
  z-index: 10;
}

.debug-info {
  position: absolute;
  top: 40px;
  right: 20px;
  padding: 10px;
  background-color: #f8f8f8;
  border: 1px dashed #dcdfe6;
  font-family: monospace;
  font-size: 12px;
  max-height: 300px;
  max-width: 400px;
  width: 90%;
  overflow: auto;
  z-index: 100;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-message-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.message {
  margin-bottom: 20px;
  max-width: 80%;
  position: relative;
  word-break: break-word; /* 确保长文本会自动换行 */
}

.user-message {
  margin-left: auto;
  padding-right: 10px;
}

.ai-message {
  margin-right: auto;
  padding-left: 10px;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.user-message .message-time {
  text-align: right;
}

.message-content-wrapper {
  display: flex;
}

.user-message .message-content-wrapper {
  justify-content: flex-end;
}

.ai-message .message-content-wrapper {
  justify-content: flex-start;
}

.message-content {
  padding: 10px 15px;
  border-radius: 8px;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 60px;
  max-width: 100%;
  position: relative;
}

.user-message .message-content {
  background-color: #ecf5ff;
  color: #303133;
  border-top-right-radius: 0;
}

.ai-message .message-content {
  background-color: #f4f4f5;
  color: #303133;
  border-top-left-radius: 0;
}

/* 添加气泡箭头 */
.user-message .message-content:after {
  content: '';
  position: absolute;
  top: 0;
  right: -8px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-left-color: #ecf5ff;
  border-right: 0;
  border-top: 0;
  margin-right: 0;
}

.ai-message .message-content:after {
  content: '';
  position: absolute;
  top: 0;
  left: -8px;
  width: 0;
  height: 0;
  border: 8px solid transparent;
  border-right-color: #f4f4f5;
  border-left: 0;
  border-top: 0;
  margin-left: 0;
}

.message-text {
  line-height: 1.5;
}

/* 分支切换容器和按钮 */
.branch-switch-container {
  width: 100%;
  display: flex;
  margin-top: 5px;
}

.user-message .branch-switch-container {
  justify-content: flex-end;
}

.ai-message .branch-switch-container {
  justify-content: flex-start;
}

.branch-switch {
  transition: all 0.3s ease;
}

.branch-switch .el-button {
  font-size: 12px;
  color: #909399;
  padding: 5px 10px;
}

.branch-switch .el-button:hover {
  color: #409eff;
}

/* 分支编辑面板 */
.branch-edit-panel {
  margin-top: 10px;
  background-color: #f9fafc;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: inline-block;
  min-width: 200px;
  max-width: 90%;
  transition: all 0.3s ease;
}

.user-branch-panel {
  margin-left: auto;
}

.ai-branch-panel {
  margin-right: auto;
}

/* 分支标签列表 - 水平布局 */
.branch-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-start;
}

/* 分支标签项 */
.branch-tag-item {
  display: inline-flex;
  align-items: center;
  background-color: #f5f7fa;
  border-radius: 20px;
  padding: 4px 12px 4px 4px;
  border: 1px solid #ebeef5;
  transition: all 0.2s ease;
  max-width: 100%;
  overflow: hidden;
}

.branch-tag-item:hover {
  border-color: #c0c4cc;
}

.branch-tag-item.active-branch {
  background-color: #ecf5ff;
  border-color: #b3d8ff;
}

.branch-tag-text {
  padding: 4px 8px;
  cursor: pointer;
  border-radius: 16px;
  background-color: #fff;
  margin-right: 8px;
  border: 1px solid #ebeef5;
  font-size: 12px;
  transition: all 0.2s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}

.branch-tag-text:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

/* 为markdown内容添加样式 */
:deep(.markdown-body) {
  font-size: 15px;
  line-height: 1.6;
}

/* 专门为AI消息增加样式 */
.ai-message :deep(.markdown-body) {
  font-size: 16px;
}

.ai-message :deep(.markdown-body p) {
  margin: 10px 0;
}

.ai-message :deep(.markdown-body li) {
  margin: 6px 0;
}

:deep(.markdown-body pre) {
  background-color: #282c34;
  padding: 10px;
  border-radius: 5px;
  overflow-x: auto;
  margin: 8px 0;
}

:deep(.markdown-body code) {
  color: #476582;
  padding: 0.25rem 0.5rem;
  margin: 0;
  font-size: 0.85em;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
}

:deep(.markdown-body p) {
  margin: 8px 0;
}

:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3),
:deep(.markdown-body h4),
:deep(.markdown-body h5),
:deep(.markdown-body h6) {
  margin-top: 12px;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.25;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 20px;
  margin: 8px 0;
}

:deep(.markdown-body li) {
  margin: 4px 0;
}
</style>
