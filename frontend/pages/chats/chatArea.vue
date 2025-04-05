<template>
    <div class="right-chat-area">
      <div class="chat-header">
        <h2 v-if="currentInterviewer">{{ currentInterviewer.name }} 面试</h2>
        <h2 v-else>请选择AI面试官</h2>
      </div>
  
      <div class="chat-messages" ref="chatMessages">
        <div
          v-for="(message, index) in chatMessages"
          :key="index"
          :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']"
        >
          <div class="message-content">
            <div class="message-text">{{ message.content.text }}</div>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
        </div>
      </div>
  
      <div class="chat-input">
        <el-input
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题..."
          v-model="inputMessage"
          @keyup.enter.native="sendMessageWithFlux"
        ></el-input>
        <el-button
          type="primary"
          @click="sendMessageWithFlux"
          :disabled="!inputMessage.trim() || isLoading"
          class="send-button"
        >{{ isLoading ? '发送中...' : '发送' }}</el-button>
      </div>
    </div>
  </template>
  
  <script>
  import axios from 'axios';
  
  export default {
    name: 'ChatArea',
    props: {
      currentInterviewer: {
        type: Object,
        default: null
      },
      chatRecordId: {
        type: String,
        default: null
      },
      currentBranchId: {
        type: String,
        default: null
      }
    },
    data() {
      return {
        inputMessage: '',
        chatMessages: [],
        isLoading: false,
        currentBranch: null,
        messageReceived: '',
      }
    },
    watch: {
      chatRecordId: {
        handler(newVal) {
          if (newVal) {
            this.loadChatMessages(newVal);
          }
        },
        immediate: true
      },
      currentBranchId: {
        handler(newVal) {
          if (newVal && this.chatRecordId) {
            this.loadBranchMessages(newVal);
          }
        },
        immediate: true
      }
    },
    methods: {
      async loadChatMessages(chatId) {
        try {
          // 首先获取所有分支
          const branchRes = await this.$axios.post('/api/chat/getAllBranches', {
            chatId: chatId
          });
          
          if (branchRes.data && branchRes.data.code === 200 && branchRes.data.data) {
            // 找到根分支或第一个分支
            const branches = branchRes.data.data;
            // 默认加载第一个分支
            if (branches.length > 0) {
              this.loadBranchMessages(branches[0].branchId);
            }
          }
        } catch (error) {
          this.$message.error('获取聊天记录失败');
          console.error(error);
        }
      },
      async sendMessageWithFlux() {
        if (!this.inputMessage.trim() || this.isLoading) return;
        if (!this.currentInterviewer) {
          this.$message.warning('请先选择面试官');
          return;
        }
      
        const userMessage = this.createMessage('user', this.inputMessage);
      
        // 先将用户消息添加到界面
        this.chatMessages.push(userMessage);
        this.scrollToBottom();
      
        // 清空输入框
        const userInput = this.inputMessage;
        this.inputMessage = '';
        this.isLoading = true;
      
        try {
          // 准备要发送的消息列表
          
        
        
          // 创建AI消息对象
          const aiMessage = this.createMessage('assistant', '');
          this.chatMessages.push(aiMessage);
        
          // 使用responseType: 'text'来接收文本流
         // 第一步：发起请求获取流式连接
         console.log(this.messageList)
          const response = await this.$axios.post(
            '/api/chat/sendMessageWithFlux',
            {
              messageList: this.chatMessages,
              interviewer: this.currentInterviewer,
            },
            { responseType: 'stream' } // 关键配置：声明需要流式响应
          );
  
          const streamUrl = URL.createObjectURL(new Blob([response.data]));
          const eventSource = new EventSource(streamUrl);
          console.log(response)
          eventSource.onmessage = (event) => {
        try {
                const chunk = JSON.parse(event.data); // 解析单条数据块
                if (chunk.code === 200) { // 假设 GlobalResult 的 code 字段
                  // 实际流数据在 chunk.data 中（根据您的 GlobalResult 结构调整）
                  console.log('收到流数据:', chunk.data);
                  this.chatMessages.push(chunk.data); // 示例：存入数组
                } else {
                  console.error('服务端返回错误:', chunk.message);
                }
              } catch (e) {
                console.error('解析流数据失败:', e);
              }
            };
          // 流式响应处理完成后，将更新后的消息列表保存到分支
          console.log("SSE流处理完成");
        
          // 流式响应处理完成后，将更新后的消息列表保存到分支
          if (this.currentBranch) {
            // 更新分支中的消息列表
            this.currentBranch.messageLocals = this.chatMessages;
          
            // 保存分支数据
            await this.$axios.post('/api/chat/saveBranches', [this.currentBranch]);
          }
        } catch (error) {
          // 检查是否是由于请求中断等正常情况引起的错误
          if (error.response) {
            console.error('发送消息请求返回错误:', error.response.status, error.response.data);
            this.$message.error(`发送消息失败: ${error.response.status}`);
          } else if (error.request) {
            console.error('发送消息请求未收到响应:', error.request);
            this.$message.error('发送消息超时，请重试');
          } else {
            console.error('发送消息错误:', error.message);
            this.$message.error(`发送消息出错: ${error.message}`);
          }
        
          // 将最后一条消息(AI响应)从界面移除
          this.chatMessages.pop();
        } finally {
          this.isLoading = false;
          this.scrollToBottom();
        }
      },
      
      async sendMessageWithPolling() {
        if (!this.inputMessage.trim() || this.isLoading) return;
        if (!this.currentInterviewer) {
          this.$message.warning('请先选择面试官');
          return;
        }
        const userMessage = this.createMessage('user', this.inputMessage);
        this.chatMessages.push(userMessage);
        this.scrollToBottom();
        const userInput = this.inputMessage;
        this.inputMessage = '';
        this.isLoading = true;
  
        try {
          // 创建AI消息占位对象
          const aiMessage = {
            id: 1, // 添加前缀便于识别
            role: 'assistant',
            content: {
              text: '', // 初始为空
              voice: null,
              files: []
            },
            timestamp: new Date().toISOString()
          };
  
  
          this.scrollToBottom();
          // 发送消息到后端
          await this.$axios.post('/api/chat/sendMessageWithPoll', {
            messageList: this.chatMessages,
            interviewer: this.currentInterviewer
          });
          this.chatMessages.push(aiMessage);
          this.currentAiMessageId = aiMessage.id;
          // 开始轮询
          if (!this.isPolling) {
            this.isPolling = true;
            this.startPolling();
          }
        } catch (error) {
          this.$message.error('发送消息失败');
          this.isLoading = false;
          console.error(error);
        }
      },
  // 修改后的startPolling方法
    async startPolling() {
    if (this.pollingInterval) {
      clearTimeout(this.pollingInterval);
    }
    
    const poll = async () => {
      if (!this.isPolling) return;
      
      try {
        const response = await this.$axios.get('/api/chat/pollMessage');
        
        // 找到当前AI消息
        const aiMessageIndex = this.chatMessages.findIndex(
          msg => msg.id === 1
        );
        
        if (aiMessageIndex !== -1) {
          // 更新AI消息内容
          if (response.data && response.data.text) {
            // 确保content对象存在
            if (!this.chatMessages[aiMessageIndex].content) {
              this.chatMessages[aiMessageIndex].content = { text: '' };
            }
            
            // 追加新内容
            this.chatMessages[aiMessageIndex].content.text += response.data.text;
            
            // 触发视图更新
            this.$forceUpdate();
            this.scrollToBottom();
            
            console.log(response.data.finish)
            // 检查是否结束
            if (response.data.finish == "stop") {
              this.stopPolling();
              return;
            }
          }
        } else {
          this.stopPolling();
          return;
        }
        
        // 继续轮询
        this.pollingInterval = setTimeout(poll, 5);
      } catch (error) {
        console.error('轮询出错:', error);
        this.stopPolling();
      }
    };
    
    poll();
    },
    // 修改后的stopPolling方法
    stopPolling() {
      this.isPolling = false;
      this.isLoading = false;
      if (this.pollingInterval) {
        clearTimeout(this.pollingInterval);
        this.pollingInterval = null;
      }
    },
   
    
    beforeDestroy() {
      this.stopPolling()
    },
  
  
  
   
  
  
  
  
  
  
  async loadBranchMessages(branchId) {
    try {
      const response = await this.$axios.post('/api/chat/getBranchMessages', {
        branchId: branchId
      });
  
      if (response.data && response.data.code === 200) {
        this.chatMessages = response.data.data || [];
        // 设置当前分支
        this.currentBranch = {
          branchId: branchId
        };
        this.scrollToBottom();
      }
    } catch (error) {
      this.$message.error('获取分支消息失败');
      console.error(error);
    }
  },
  
  
  
  // 增加一个方法来保存聊天记录到后端
  async saveChatMessage(message) {
    if (!this.chatRecordId || !this.currentBranch) {
      return; // 没有聊天记录ID或分支ID时不保存
    }
    
    try {
      await this.$axios.post('/api/chat/saveMessage', {
        chatId: this.chatRecordId,
        branchId: this.currentBranch.branchId,
        message: message
      });
    } catch (error) {
      console.error('保存消息失败:', error);
    }
  },
  
  // 增加加载分支消息的方法
  async loadBranchMessages(branchId) {
    if (!this.chatRecordId) return;
    
    try {
      this.isLoading = true;
      const response = await this.$axios.post('/api/chat/getBranchMessages', {
        chatId: this.chatRecordId,
        branchId: branchId
      });
      
      if (response.data && response.data.code === 200) {
        this.chatMessages = response.data.data || [];
        this.currentBranch = { branchId: branchId };
        this.scrollToBottom();
      }
    } catch (error) {
      this.$message.error('获取分支消息失败');
      console.error(error);
    } finally {
      this.isLoading = false;
    }
  },
  
  
  
  
  
  
      
  
  
    
  
      
      createMessage(role, text) {
        return {
          messageId: this.generateUuid(),
          branchId: this.currentBranch ? this.currentBranch.branchId : null,
          role: role, 
          inputType: "text",
          content: {
            text: text,
            voice: null,
            files: []
          },
          timestamp: new Date()
        };
      },
      
      formatTime(timestamp) {
        if (!timestamp) return '';
        
        const date = new Date(timestamp);
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        return `${hours}:${minutes}`;
      },
      
      scrollToBottom() {
        this.$nextTick(() => {
          if (this.$refs.chatMessages) {
            this.$refs.chatMessages.scrollTop = this.$refs.chatMessages.scrollHeight;
          }
        });
      },
      
      generateUuid() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
          const r = Math.random() * 16 | 0, 
                v = c === 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
        });
      }
    }
  }
  </script>
  
  <style scoped>
  .right-chat-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    background-color: #fff;
    height: 100%;
  }
  
  .chat-header {
    padding: 20px;
    border-bottom: 1px solid #e6e6e6;
  }
  
  .chat-messages {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
  }
  
  .message {
    margin-bottom: 20px;
    display: flex;
  }
  
  .message-content {
    max-width: 70%;
    padding: 10px 15px;
    border-radius: 5px;
    position: relative;
  }
  
  .user-message {
    justify-content: flex-end;
  }
  
  .user-message .message-content {
    background-color: #409eff;
    color: white;
  }
  
  .ai-message {
    justify-content: flex-start;
  }
  
  .ai-message .message-content {
    background-color: #f2f6fc;
    color: #333;
  }
  
  .message-time {
    font-size: 12px;
    color: #999;
    margin-top: 5px;
  }
  
  .chat-input {
    padding: 20px;
    border-top: 1px solid #e6e6e6;
  }
  
  .send-button {
    margin-top: 10px;
    float: right;
  }
  </style>
  