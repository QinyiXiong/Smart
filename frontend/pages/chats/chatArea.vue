<template>
    <div class="right-chat-area">
      <div class="chat-header">
        <h2 v-if="currentInterviewer">{{ currentInterviewer.name }} 面试</h2>
        <h2 v-else>请选择AI面试官</h2>
      </div>
  
      <div class="chat-messages" ref="chatMessages">
        <div
          v-for="(message, index) in messageListForShow"
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
          @keyup.enter.native="sendMessageWithPolling"
        ></el-input>
        <el-button
          type="primary"
          @click="sendMessageWithPolling"
          :disabled="!inputMessage.trim() || isLoading"
          class="send-button"
        >{{ isLoading ? '发送中...' : '发送' }}</el-button>
      </div>
    </div>
  </template>
  
  <script>

  import qs from 'qs';
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
        chatMessages: [],//用于发送信息的messagelist
        messageListForShow: [],//处理分支信息后展示用的messagelist
        allBranches: [], // 存储所有分支数据
        branchPath: [], // 当前分支路径 [{branchIndex, childIndex}]
        branchSelectionMap: new Map(), // 记录每个分支的选择 {branchIndex: childIndex}
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
          this.branchPath = [];
          this.messageListForShow = [];
          // 获取所有分支
          const response = await this.$axios.post('/api/chat/getAllBranches', { chatId });
          
          if (response.data) {
            this.allBranches = response.data;

            // 找到根分支（parent_branchId的index为0）
            const rootBranch = this.allBranches.find(b => b.index == 0);

            if (rootBranch) {
              // 构建初始分支路径
              await this.buildBranchPath(rootBranch);
            }
          }
        } catch (error) {
          this.$message.error('获取聊天记录失败');
          console.error(error);
        }
      },

      async buildBranchPath(startBranch) {
        
        
        let currentBranch = startBranch;

        while (currentBranch) {
          console.log(currentBranch)
          // 添加当前分支的消息
          if (currentBranch.messageLocals?.length) {
            
            this.messageListForShow.push(...currentBranch.messageLocals);
          }

          // 获取当前分支的选择
          let childIndex = this.branchPath.find(p => p.branchIndex === currentBranch.index)?.childIndex
          // 如果没有选择，则选择第一个分支
          if(!childIndex){
            if(currentBranch.children && currentBranch.children.length > 0){
              childIndex = currentBranch.children[0].branchIndex
            }else{
                childIndex = null; // 没有子分支时设为null
                break; // 退出循环
            }
          }


          this.branchPath.push({
            branchIndex: currentBranch.index,
            childIndex
          });

          // 移动到下一个分支
          
          currentBranch = childIndex ? this.allBranches.find(b => b.branchIndex === childIndex): null;
        }

        // 更新当前分支
        if (this.branchPath.length > 0) {
          const lastBranchIndex = this.branchPath[this.branchPath.length - 1].branchIndex;
          this.currentBranch = this.allBranches.find(b => b.index === lastBranchIndex);
        }
      },

   

      async createNewBranch() {
        try {

          const newIndex = this.allBranches.length;

          // 本地创建分支对象
          const newBranch = {
            branchId: this.generateUuid(),
            chatId: this.chatRecordId,
            index: newIndex,
            parent_branchId: null, // 根分支
            children: [],
            messageLocals: []
          };

          // 添加到分支列表
          this.allBranches.push(newBranch);

          // 更新当前分支
          this.currentBranch = newBranch;
          this.branchPath = [{
            branchId: newBranch.branchId,
            childIndex: 0
          }];

         

        } catch (error) {
          console.error('创建分支失败:', error);
          throw error;
        }
      },



     
      
      async sendMessageWithPolling() {
        if (!this.inputMessage.trim() || this.isLoading) return;
        if (!this.currentInterviewer) {
          this.$message.warning('请先选择面试官');
          return;
        }
        if (!this.currentBranch) {
          await this.createNewBranch();
          
        }
        const userMessage = this.createMessage('user', this.inputMessage);
        this.messageListForShow.push(userMessage);
        // 检查并创建初始分支
        
        if (this.currentBranch ) {
          if (!this.currentBranch.messageLocals) {
            this.currentBranch.messageLocals = [];
          }
          this.currentBranch.messageLocals.push(userMessage);
          
        }
        this.buildContextMessages();

        this.inputMessage = '';
        this.isLoading = true;
        let messageId = null; // 用于存储消息ID

        try {
          
  
          
          this.scrollToBottom();
          // 发送消息到后端
          const response = await this.$axios.post('/api/chat/sendMessageWithPoll', {
            messageList: this.chatMessages,
            interviewer: this.currentInterviewer
          });
         
          messageId = response.data; // 获取后端返回的消息ID
          
          
          // 创建AI消息占位对象
          const aiMessage = {
            id: messageId, // 添加前缀便于识别
            role: 'assistant',
            content: {
              text: '', // 初始为空
              voice: null,
              files: []
            },
            timestamp: new Date().toISOString()
          };
         

          this.messageListForShow.push(aiMessage);
          this.currentAiMessageId = aiMessage.id;
          this.scrollToBottom();
          // 开始轮询
          if (!this.isPolling) {
            this.isPolling = true;
            this.startPolling(messageId);
            console.log(this.chatMessages)
          }
        } catch (error) {
          this.$message.error('发送消息失败');
          this.isLoading = false;
          console.error(error);
        }
      },

      async startPolling(messageId) {
        const POLLING_TIMEOUT = 5000; // 5秒超时
        let pollingStartTime = Date.now();

          const processBatch = async () => {
              if (!this.isPolling) return;

              try {
                  // 检查是否超时
                  if (Date.now() - pollingStartTime > POLLING_TIMEOUT) {
                      throw new Error('轮询超时，未收到有效响应');
                  }

                  const params = new URLSearchParams();
                  params.append('messageId', messageId);  // 确保参数名完全匹配
                  params.append('batchSize', '10');  // 字符串形式

                  const response = await this.$axios.get('/api/chat/pollMessages', {
                      params: params,
                      paramsSerializer: params => params.toString()  // 使用默认序列化
                  });

                  if (response.data && response.data.length) {
                      let shouldStop = false;
                      // 重置超时计时器（每次收到有效数据就重置）
                      pollingStartTime = Date.now();

                      // 批量处理消息
                      for (const msg of response.data) {
                          

                          // 检查是否收到停止信号
                          if (msg.finish === "stop") {
                              shouldStop = true;
                          }

                          // 更新AI消息内容
                          const aiMsg = this.messageListForShow.find(m => m.id === messageId);
                          if (aiMsg) {
                              aiMsg.content = aiMsg.content || { text: '' };
                              await this.typeText(aiMsg, msg.text);
                          }
                      }

                      this.$forceUpdate();
                      this.scrollToBottom();

                      // 如果收到停止信号，则中止轮询
                      if (shouldStop) {
                          this.stopPolling();
                          const aiMsg = this.messageListForShow.find(m => m.id === messageId);
                          if (aiMsg) {
                              // 将AI消息加入currentBranch
                              this.currentBranch.messageLocals.push({
                                  messageId:aiMsg.id,
                                  role: 'assistant',
                                  branchId: this.currentBranch.branchId,
                                  content: {
                                      text: aiMsg.content.text,
                                      voice: null,
                                      files: []
                                  },
                                  timestamp: new Date()
                              });
                              console.log(this.currentBranch)
                              // 调用封装的方法保存currentBranch，并手动传入branchList
                              const newBranchList = [this.currentBranch];
                              console.log(newBranchList)
                              await this.saveBranchList(newBranchList);
                          }
                          this.isLoading = false;
                          return;
                      }
                  }

                  // 继续轮询
                   this.pollingAnimationFrame = requestAnimationFrame(processBatch); // 200ms轮询间隔

              } catch (error) {
                  console.error('轮询出错:', error);
                  this.stopPolling();
                  this.isLoading = false;
                  this.$message.error('获取消息失败: ' + error.message);
              }
          };

          this.isPolling = true;
          processBatch();
      },
      stopPolling() {
          this.isPolling = false;
          if (this.pollingTimer) {
              clearTimeout(this.pollingTimer);
              this.pollingTimer = null;
          }
      },

   // 打字机效果方法
    async typeText(messageObj, newText) {
        const typingSpeed = 30; // 控制打字速度(ms/字)

        for (let i = 0; i < newText.length; i++) {
            await new Promise(resolve => setTimeout(resolve, typingSpeed));
            messageObj.content.text += newText[i];
            this.$forceUpdate();
            this.scrollToBottom();
        }
    },
    
    beforeDestroy() {
      this.stopPolling()
    },
  
  
  
   
    buildContextMessages() {
    
    
      const maxContextLength = 40; // 保留最近20轮对话(用户和AI各20条)
      const startIndex = Math.max(0, this.chatMessages.length - maxContextLength);
      this.chatMessages = this.messageListForShow.slice(startIndex);

    },
  
  
  
  
      async saveBranchList() {
        try {
          const response = await this.$axios.post('/api/chat/saveBranches', this.allBranches);
          
            //this.$message.success('分支保存成功');
          
        } catch (error) {
          console.error('保存分支列表失败:', error);
          this.$message.error('保存分支列表失败: ' + error.message);
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
  
