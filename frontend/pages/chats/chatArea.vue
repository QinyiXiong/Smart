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
            <div v-if="!message.editing" class="message-text">{{ message.content.text }}</div>
            <el-input
              v-else
              type="textarea"
              :rows="3"
              v-model="message.editText"
              class="edit-input"
            ></el-input>
           
          </div>
          <!-- 添加分支切换按钮 -->
          <div 
            v-if="isFirstMessageInBranch(message) && 
                  checkSiblings(message) &&
                  !isLoading""
            class="branch-switch"
          >
            <el-button
              type="text"
              @click="toggleShowBranch(message)"
            >{{ message.showBranchTag ? '关闭' : '显示分支' }}</el-button>
          </div>
        
          
          <!-- 分支编辑面板 -->
          <div 
            v-if="message.showBranchTag && !isLoading"
            class="branch-edit-panel"
          >
          <div class="branch-tag-list">
            <div 
              v-for="(sibling, idx) in siblingNodes.find(node => node.branchId === message.branchId)?.siblings || []"
              :key="sibling.index"
              class="branch-tag-item"
            >
              <!-- 显示模式 -->
              <template v-if="!sibling.editing">
                <span 
                  class="branch-tag-text"
                  @click="switchBranch(sibling.index)"
                >{{ sibling.tag }}</span>
                <el-button
                  type="text"
                  size="mini"
                  @click="startEditBranchTag(sibling)"
                >修改</el-button>
              </template>
              
              <!-- 编辑模式 -->
              <template v-else>
                <el-input
                  v-model="sibling.tag"
                  size="mini"
                  class="branch-tag-input"
                  @keyup.enter.native="saveBranchTag(sibling, message)"
                ></el-input>
                <div class="branch-tag-actions">
                  <el-button
                    type="text"
                    size="mini"
                    @click="cancelEditBranchTag(sibling)"
                  >取消</el-button>
                  <el-button
                    type="text"
                    size="mini"
                    @click="saveBranchTag(sibling, message)"
                  >保存</el-button>
                </div>
              </template>
            </div>
          </div>
          
          </div>
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          <div v-if="message.role === 'user'" class="message-actions">
            <el-button
              v-if="!message.editing"
              type="text"
              icon="el-icon-edit"
              @click="startEditMessage(message)"
            ></el-button>
            <el-button
              v-if="!message.editing"
              type="text"
              icon="el-icon-refresh"
              @click="regenerateMessage(message)"
            ></el-button>
            <div v-else class="edit-actions">
              <el-button size="mini" @click="cancelEdit(message)">取消</el-button>
              <el-button
                size="mini"
                type="primary"
                @click="sendModifiedMessage(message)"
              >发送</el-button>
            </div>
          </div>
        </div>
      </div>
  
      <div class="chat-input">
        <el-input
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题..."
          v-model="inputMessage"
          @keyup.enter.native="handleKeyEnter"
        ></el-input>
        <el-button
          type="primary"
          @click="sendMessageWithPolling()"
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
        rootBranch: null,
        modifiedBranch: [],
        siblingNodes: [], // 存储当前路径上每个节点的兄弟节点信息
        showBranchSwitch: false, // 控制分支切换面板的显示
        currentSwitchMessageId: null, // 当前显示切换面板的消息ID
        branchTagBackup: null,
        
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
      
    },
    methods: {
      checkSiblings(message){
       
        const siblingNode = this.siblingNodes.find(b => b.branchId == message.branchId)
        
        if(siblingNode && siblingNode.siblings){
          return siblingNode.siblings.length > 1
        }
        return false;
      },
      isFirstMessageInBranch(message) {
        
        
        // console.log(message)
        // 获取当前消息在branch中的索引
        const branch = this.allBranches.find(b => b.branchId == message.branchId);
        if (!branch || !branch.messageLocals) return false;
      
        const branchMsgIndex = branch.messageLocals.findIndex(

          msg => msg.messageId == message.messageId
        );
        
        // console.log(branchMsgIndex)
        // 如果是branch中的第一条消息，返回true
        return branchMsgIndex == 0;
      },
      async fetchData(chatId){
        try {
          
          // 获取所有分支
          const response = await this.$axios.post('/api/chat/getAllBranches', { chatId });
          
          if (response.data) {
            this.allBranches = response.data;

            // 找到根分支（parent_branchId的index为0）
            this.rootBranch = this.allBranches.find(b => b.index == 0);

          
          }
        } catch (error) {
          this.$message.error('同步聊天记录失败');
          console.error(error);
        }
      },

      async loadChatMessages(chatId) {
        try {
          // this.branchPath = [];
          this.messageListForShow = [];
          // 获取所有分支
          const response = await this.$axios.post('/api/chat/getAllBranches', { chatId });
          
          if (response.data) {
            this.allBranches = response.data;

            // 找到根分支（parent_branchId的index为0）
            this.rootBranch = this.allBranches.find(b => b.index == 0);

            if (!this.rootBranch) {
              await this.createNewBranch();
              this.rootBranch = this.currentBranch
            }
            await this.buildPathForTargetBranch(this.rootBranch);
          }
        } catch (error) {
          this.$message.error('获取聊天记录失败');
          console.error(error);
        }
      },


      async regenerateMessage(message) {
     
        this.$set(message, 'editing', true);
        this.$set(message, 'editText', message.content.text);
       
        // 等待一个事件循环让UI更新
        await this.$nextTick();

        await this.sendModifiedMessage(message);
      },

      handleKeyEnter(e) {
          // 阻止默认行为（避免在textarea中换行）
          e.preventDefault();
          // 只有当输入框有内容时才发送
          if (this.inputMessage.trim()) {
            this.sendMessageWithPolling();
          }
        },
      toggleShowBranch(message) {
        if (message.showBranchTag) {
            // 退出状态时，将所有sibling.editing状态改为false，恢复原样
            const siblings = this.siblingNodes.find(node => node.branchId === message.branchId)?.siblings || [];
            siblings.forEach(sibling => {
              if (sibling.editing) {
                this.cancelEditBranchTag(sibling);
              }
            });
            this.$set(message, 'showBranchTag', false);
          } else {
            // 进入编辑状态时，关闭其他message正在打开的分支页面
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
          this.showBranchSwitch = false;
        }
      },

      startEditBranchTag(sibling) {
        // 备份原始标签以便取消时可以恢复
        this.branchTagBackup = sibling.tag;
        this.$set(sibling, 'editing', true);
      },
      cancelEditBranchTag(sibling) {
        // 恢复备份的标签
        sibling.tag = this.branchTagBackup;
        this.$set(sibling, 'editing', false);
      },
      async saveBranchTag(sibling, message) {
        this.$set(sibling, 'editing', false);
        try {
          // 找到对应的父分支
          const branch = this.allBranches.find(b => b.branchId == message.branchId);
          
          if (!branch) return;
          
          const parentBranch = this.allBranches.find(b => 
            b.children?.some(child => child.branchIndex == branch.index)
          );
          console.log(parentBranch)
          if (parentBranch) {
            // 更新标签
            const siblings = this.siblingNodes.find(node => node.branchId == message.branchId)?.siblings || [];
            parentBranch.children.forEach(child => {
              const sibling = siblings.find(s => s.index == child.branchIndex);
              if (sibling) {
                child.tag = sibling.tag;
              }
            });
            console.log(parentBranch)
            // 保存到服务器
            await this.saveBranchList([parentBranch]);
          }
        } catch (error) {
          console.error('保存分支标签失败:', error);
        }
      },
      
      //按照默认方式构建分支路径（没有指定需要经过哪个节点）
      async buildBranchPath(startBranch) {
     
        this.currentBranch = startBranch;
        let currentBranch = startBranch;
       
        while (currentBranch) {
          
          // 添加当前分支的消息
          if (currentBranch.messageLocals?.length) {
            
            this.messageListForShow.push(...currentBranch.messageLocals);
            
          }

          

          // 获取当前分支的选择
          // let childIndex = this.branchPath.find(p => p.index === currentBranch.index)?.index
          
          // 如果没有选择，则选择第一个分支
          let childIndex = -1
          if(currentBranch.children && currentBranch.children.length > 0){
            childIndex = currentBranch.children[0].branchIndex
            // console.log(childIndex)
          }else{
              //到达叶子节
              childIndex = -1; // 没有子分支时设为-
              this.branchPath.push({
                index: currentBranch.index,
                childIndex
              })
              
              break; // 退出循环
          }
          
          let child = this.allBranches.find(p => p.index == childIndex);
          
          //以当前节点添加其子节点的兄弟信息
          this.siblingNodes.push({
            index: childIndex,
            branchId: child?.branchId,
            siblings: currentBranch.children.map(child => ({
              index: child.branchIndex,
              tag: child.tag
            }))
          })
       

     
         
          
          // 移动到下一个分支
          currentBranch = childIndex ? this.allBranches.find(b => b.index == childIndex): null;
          
        }

        // 更新当前分支
        
        this.currentBranch = currentBranch;
      },
      async buildPathForTargetBranch(targetBranch) {
        //如果不存在（如新建分支时接受信息，则使用兄弟分支）
        if (!targetBranch) return;
          
        
       
        // 1. 向上查找父分支链
        const parentChain = [];
        let current = targetBranch;
        
        while (current && current.parentBranchIndex !== -1) {
          const parent = this.allBranches.find(b => b.index == current.parentBranchIndex);
          if (parent) {
            parentChain.unshift(parent); // 添加到数组开头
            current = parent;
          } else {
            break;
          }
        }

        // 2. 从根分支开始向下构建路径
        // this.branchPath = []; // 重置分支路径
        this.siblingNodes = [];
        this.messageListForShow = []; // 清空消息展示列表
        let parentBranch = this.rootBranch;
        // 遍历父链中的每个分支
        for (const branch of parentChain) {

          if (branch) {
            if(branch.index != 0){
                // 获取当前分支的所有消息
                const branchMessages = branch?.messageLocals || [];
                // 将消息添加到展示列表中
                this.messageListForShow.push(...branchMessages);

               

                this.siblingNodes.push({
                  index: branch.index,
                  branchId:branch.branchId,
                  siblings: parentBranch.children.map(child => ({
                    index: child.branchIndex,
                    tag: child.tag
                  }))
                })
            
            }

            parentBranch = branch;
          }  
        }
        
     
        this.siblingNodes.push({
          index: targetBranch.index,
          branchId:targetBranch.branchId,
          siblings: parentBranch.children.map(child => ({
            index: child.branchIndex,
            tag: child.tag
          }))
        })
        
        // 3. 添加目标以及之后的分支到路径
        await this.buildBranchPath(targetBranch);
       
      
      },

   

      async createNewBranch() {
        try {

          const newIndex = this.allBranches.length;

          // 本地创建分支对象
          const newBranch = {
            branchId: this.generateUuid(),
            chatId: this.chatRecordId,
            index: newIndex,
            parentBranchIndex: -1, // 根分支
            children: [],
            messageLocals: [],
           
          };

          // 添加到分支列表
          this.allBranches.push(newBranch);

          // 更新当前分支
          this.currentBranch = newBranch;
          // this.branchPath = [{
          //   branchId: 0,
          //   childIndex: -1,
          // }];

         

        } catch (error) {
          console.error('创建分支失败:', error);
          throw error;
        }
      },
      //message为产生分支操作的原message（不是新message）
      async newChatBranch(index) {
        
        try {
          // 情况1：是分支的第一个消息（index为0）
          if (index == 0) {
           
            const newBranch = {
              branchId: this.generateUuid(),
              chatId: this.chatRecordId,
              index: this.allBranches.length,
              parentBranchIndex: this.currentBranch.parentBranchIndex,
              children: [],
              messageLocals: []
            };
            
            // 添加到分支列表
            this.allBranches.push(newBranch);
          
            // 在父分支的children中添加新分支
            const parentBranch = this.allBranches.find(
              b => b.index == this.currentBranch.parentBranchIndex
            );
            
            if (parentBranch) {
              parentBranch.children.push({
                branchIndex: newBranch.index,
                tag:  `分支${parentBranch.children.length + 1}`
              });
            }
          
           
            
            // 切换当前分支
            this.currentBranch = newBranch;
            this.modifiedBranch.push(parentBranch);//先不加入新增的branch，到后面消息接受完毕后再更新
          } 
          // 情况2：不是第一个消息
          else {
            
            // 创建新父分支（包含index之前的消息）
            const newParentBranch = {
              branchId: this.generateUuid(),
              chatId: this.chatRecordId,
              index: this.allBranches.length,
              parentBranchIndex: this.currentBranch.parentBranchIndex,
              children: [
              {
                branchIndex: this.currentBranch.index,  // 新index分配给当前分支
                tag: '原分支'
              }
            ],
              messageLocals: []
            };
            newParentBranch.messageLocals = this.currentBranch.messageLocals.slice(0, index).map(msg => ({
                ...msg,
                branchId: newParentBranch.branchId // 更新branchId
              }))
            // 找到newParentBranch的父分支
            const grandParentBranch = this.allBranches.find(
              b => b.index == newParentBranch.parentBranchIndex
            );
            if (grandParentBranch) {
              // 遍历父分支的children数组
              grandParentBranch.children.forEach(child => {
                if (child.branchIndex == this.currentBranch.index) {
                  // 将当前分支的引用改为新父分支
                  child.branchIndex = newParentBranch.index;
                }
              });
            }
          
            // 将当前分支从index开始的message分配给新分支
            this.currentBranch.messageLocals = this.currentBranch.messageLocals.slice(index).map(msg => ({
              ...msg,
              branchId: this.currentBranch.branchId // 保持当前branchId
            }));
            this.currentBranch.parentBranchIndex = newParentBranch.index;
          
            // 创建新子分支（包含index及之后的消息）
            const newChildBranch = {
              branchId: this.generateUuid(),
              chatId: this.chatRecordId,
              index: this.allBranches.length + 1,
              parentBranchIndex: newParentBranch.index,
              children: [],
              messageLocals: []
            };
            
            newParentBranch.children.push({
                branchIndex: newChildBranch.index,
                tag: `分支${newParentBranch.children.length + 1}`
              });
            // 添加到分支列表
            this.allBranches.push(newParentBranch, newChildBranch);
            //新建的分支在正式接受到信息之后再保存
            this.modifiedBranch.push(newParentBranch, this.currentBranch,grandParentBranch);
          
            // 切换当前分支到新创建的子分支
            this.currentBranch = newChildBranch;
          }
        
          
          //console.log(this.currentBranch)
          // 重新构建经过currentBranch的路径
          // console.log(this.branchPath)
          await this.buildPathForTargetBranch(this.currentBranch);
        
          
        } catch (error) {
          console.error('创建分支失败:', error);
          this.$message.error('创建分支失败');
        }
      },

      startEditMessage(message) {
        this.$set(message, 'editing', true);
        this.$set(message, 'editText', message.content.text);
      },

      cancelEdit(message) {
        this.$set(message, 'editing', false);
        this.$set(message, 'editText', '');
      },

      async sendModifiedMessage(message) {
        console.log(message)
        const modifiedText = message.editText;
        if (!modifiedText.trim()) {
          this.$message.warning('修改内容不能为空');
          return;
        }

      try {
        this.currentBranch = this.allBranches.find(b => b.branchId == message.branchId)
        // 在当前分支中查找要修改的消息索引
        const messageIndex = this.currentBranch.messageLocals.findIndex(
          msg => msg.messageId == message.messageId
        );
        
        if (messageIndex !== -1) {
          await this.newChatBranch(messageIndex);
          this.currentBranch.messageLocals = [];
          this.messageListForShow = [];
          
          await this.buildPathForTargetBranch(this.currentBranch)
          await this.sendMessageWithPolling(modifiedText);

        }
      } catch (error) {
        console.error('修改消息失败:', error);
        this.$message.error('修改消息失败');
      }

      },
      

     
      
      async sendMessageWithPolling(messageContent = null) {
         // 参数messageContent为null时表示来自聊天框的消息，否则表示修改后的消息

         // 参数messageContent为null时表示来自聊天框的消息，否则表示修改后的消息
         const messageText = messageContent !== null ? String(messageContent) : String(this.inputMessage);
         if (!messageText || !messageText.trim() || this.isLoading) return;
         if (!messageText.trim() || this.isLoading) return;
         if (!this.currentInterviewer) {
           this.$message.warning('请先选择面试官');
           return;
         }
         
         // 如果当前分支是0号根节点，则创建新分支
         if (!this.currentBranch || this.currentBranch.index == 0) {
           await this.createNewBranch();
           this.rootBranch.children.push({
             branchIndex: this.currentBranch.index,
             tag: 'new_branch'
           });
           this.currentBranch.parentBranchIndex = 0;
           this.modifiedBranch.push(this.rootBranch);
         }
         
         const userMessage = this.createMessage('user', messageText);
         this.messageListForShow.push(userMessage);
         
         if (this.currentBranch) {
           if (!this.currentBranch.messageLocals) {
             this.currentBranch.messageLocals = [];
           }
           this.currentBranch.messageLocals.push(userMessage);
         }
         
         this.buildContextMessages();
         
         // 如果是来自聊天框的消息，清空输入框
         if (!messageContent) {
           this.inputMessage = '';
         }
         
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

            
          }
        } catch (error) {
          this.$message.error('发送消息失败');
          this.isLoading = false;
          console.error(error);
        }
      },

      async startPolling(messageId) {
        const POLLING_TIMEOUT = 20000; // 5秒超时
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
                  params.append('batchSize', '5');  // 字符串形式

                  const response = await this.$axios.get('/api/chat/pollMessages', {
                      params: params,
                      paramsSerializer: params => params.toString()  // 使用默认序列化
                  });
                
                  if (response.data && response.data.length) {
                      let shouldStop = false;
                      // 重置超时计时器（每次收到有效数据就重置）
                      pollingStartTime = Date.now();
                      let hasNewContent = false;  
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
                              const oldLength = aiMsg.content.text.length;
                              await this.typeText(aiMsg, msg.text);
                              if (aiMsg.content.text.length > oldLength) {
                                  hasNewContent = true;  // 只有内容变化时才标记
                              }
                          }
                      }

                      // 只有内容变化时才强制更新和滚动
                      if (hasNewContent) {
                          this.$forceUpdate();
                          this.scrollToBottom();
                      }

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
                              this.modifiedBranch.push(this.currentBranch);
                              // 调用封装的方法保存currentBranch，并手动传入branchList
                              console.log(this.modifiedBranch)
                              await this.saveBranchList(this.modifiedBranch);
                              this.modifiedBranch = [];
                              await this.fetchData(this.chatRecordId)
                              this.scrollToBottom();
                          }
                          this.isLoading = false;
                          return;
                      }
                  }

                  // 继续轮询
                   this.pollingAnimationFrame = requestAnimationFrame(processBatch); 

              } catch (error) {
                 
                  this.modifiedBranch = [];
                  this.stopPolling();
                  this.isLoading = false;
                  await this.loadChatMessages(this.chatRecordId)
                  await this.buildPathForTargetBranch(this.rootBranch);
                  this.$message.error('获取消息失败: ' + error.message);
                  this.scrollToBottom();
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
  
  
  
  
      async saveBranchList(branchList) {
        try {
          const response = await this.$axios.post('/api/chat/saveBranches', branchList);
          
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
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}`;
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
    flex-direction: column;
    align-items: flex-start;
  }
  
  
  .message-content {
    max-width: 70%;
    padding: 10px 15px;
    border-radius: 5px;
    position: relative;
    margin-top: 5px;
  }
  
  .user-message {
    align-items: flex-end;
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
    margin-bottom: 5px;
  }
  
  .chat-input {
    padding: 20px;
    border-top: 1px solid #e6e6e6;
  }
  
  .send-button {
    margin-top: 10px;
    float: right;
  }
  .message-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 5px;
  }

  .edit-actions {
    display: flex;
    gap: 8px;
  }

  .branch-switch {
    margin-top: 5px;
    text-align: center;
  }
  
  .branch-switch-panel {
    margin-top: 10px;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  
  .branch-option {
    padding: 8px;
    cursor: pointer;
    &:hover {
      background: #e6e6e6;
    }
  }
  .branch-tag-item {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
  }
  
  .branch-tag-text {
    padding: 4px 8px;
    cursor: pointer;
    border-radius: 4px;
    background-color: #f5f7fa;
    margin-right: 8px;
    &:hover {
      background-color: #e6e9ed;
    }
  }
  
  .branch-tag-input {
    flex: 1;
    margin-right: 8px;
  }
  
  .branch-tag-actions {
    display: flex;
  }
  
  </style>
  
