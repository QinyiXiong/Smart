<template>
  <div class="interview-modal">
    <div class="interview-header">
      <h3>{{ interview ? interview.topic || '面试对话' : '加载中...' }}</h3>
      <el-button
        v-if="isDevMode"
        size="mini"
        type="text"
        @click="showDebugInfo = !showDebugInfo">
        {{ showDebugInfo ? '隐藏调试' : '显示调试' }}
      </el-button>
    </div>

    <div v-if="showDebugInfo" class="debug-info">
      <p><strong>面试ID:</strong> {{ interviewId }}</p>
      <p><strong>话题:</strong> {{ interview ? interview.topic : 'null' }}</p>
      <p><strong>消息数量:</strong> {{ messages ? messages.length : 0 }}</p>
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
      <template v-else-if="messages && messages.length > 0">
        <div v-for="(message, index) in messages" :key="index"
          :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']">
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>

          <div class="message-content-wrapper">
            <div class="message-content">
              <div class="message-text" v-html="renderMarkdown(message.content.text, message.role)"></div>
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
            messages: [],
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

            try {
                console.log('正在获取面试ID为', this.interviewId, '的记录');

                // 获取聊天记录
                const response = await this.$axios.post('/api/v1/chat/getAllBranches', {
                    chatId: Number(this.interviewId)  // 确保ID是数字类型
                });

                console.log('API响应:', response.data);
                this.apiResponse = response.data;

                if (response.data && response.data.data && response.data.data.length > 0) {
                    // 使用response.data.data访问实际数据（后端GlobalResult包装）
                    const branches = response.data.data;

                    // 找到根分支
                    const rootBranch = branches.find(b => b.index == 0);
                    if (rootBranch && rootBranch.messageLocals) {
                        this.messages = rootBranch.messageLocals;
                        this.interview = {
                            topic: rootBranch.tag || '面试对话',
                            chatId: this.interviewId
                        };
                    } else {
                        this.$message.warning('未找到面试记录内容');
                        console.log('根分支数据:', rootBranch);
                        this.apiError = '根分支中未找到消息数据';
                    }
                } else {
                    // 尝试使用直接获取聊天记录的API
                    try {
                        const chatResponse = await this.$axios.post('/api/v1/chat/getChatRecords', {
                            chatId: Number(this.interviewId)  // 确保ID是数字类型
                        });

                        console.log('聊天记录API响应:', chatResponse.data);
                        this.apiResponse = chatResponse.data;

                        if (chatResponse.data && chatResponse.data.data && chatResponse.data.data.length > 0) {
                            const chatRecord = chatResponse.data.data[0];
                            this.interview = {
                                topic: chatRecord.topic || '面试对话',
                                chatId: this.interviewId
                            };

                            // 如果有消息数据
                            if (chatRecord.branches && chatRecord.branches.length > 0) {
                                const mainBranch = chatRecord.branches[0];
                                if (mainBranch.messageLocals) {
                                    this.messages = mainBranch.messageLocals;
                                }
                            } else {
                                this.$message.warning('面试记录中无对话内容');
                                this.apiError = '面试记录中无分支数据';
                            }
                        } else {
                            this.$message.warning('未能加载面试记录');
                            console.log('API响应:', chatResponse.data);
                            this.apiError = '未找到匹配的聊天记录';
                        }
                    } catch (chatError) {
                        console.error('获取聊天记录详情失败:', chatError);
                        this.apiError = '获取聊天记录详情失败: ' + chatError.message;
                    }
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
    height: 60vh;
    width: 100%;
}

.interview-header {
    padding: 10px 20px;
    border-bottom: 1px solid #ebeef5;
    background-color: #f5f7fa;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.interview-header h3 {
    margin: 0;
    color: #303133;
}

.debug-info {
    padding: 10px;
    background-color: #f8f8f8;
    border: 1px dashed #dcdfe6;
    font-family: monospace;
    font-size: 12px;
    max-height: 200px;
    overflow: auto;
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background-color: #f5f7fa;
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
}

.user-message {
    align-self: flex-end;
    margin-left: auto;
}

.ai-message {
    align-self: flex-start;
}

.message-time {
    font-size: 12px;
    color: #909399;
    margin-bottom: 5px;
}

.message-content-wrapper {
    display: flex;
}

.message-content {
    padding: 10px 15px;
    border-radius: 8px;
    word-break: break-word;
}

.user-message .message-content {
    background-color: #ecf5ff;
    color: #303133;
}

.ai-message .message-content {
    background-color: #f4f4f5;
    color: #303133;
}

.message-text {
    line-height: 1.5;
}

/* 为markdown内容添加样式 */
:deep(.markdown-body) {
    font-size: 14px;
}

:deep(.markdown-body pre) {
    background-color: #282c34;
    padding: 10px;
    border-radius: 5px;
    overflow-x: auto;
}

:deep(.markdown-body code) {
    color: #476582;
    padding: 0.25rem 0.5rem;
    margin: 0;
    font-size: 0.85em;
    background-color: rgba(27, 31, 35, 0.05);
    border-radius: 3px;
}
</style>
