<template>
  <div class="right-chat-area">
    <div class="chat-header">
      <h2 v-if="currentInterviewer">{{ currentInterviewer.name }} 面试</h2>
      <h2 v-else>请选择AI面试官</h2>
    </div>

    <div class="chat-messages" ref="chatMessages">
      <div v-for="(message, index) in messageListForShow" :key="index"
        :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']"
        @mouseenter="message.showActions = true" @mouseleave="message.showActions = false">
        <div class="message-time">{{ formatTime(message.timestamp) }}</div>

        <div v-if="message.content.files.length > 0" class="processed-files">
          <el-card v-for="(file, index) in message.content.files" :key="index" class="file-card" shadow="hover">
            <div class="file-card-content">
              <i class="el-icon-document" style="margin-right: 8px;"></i>
              <span :title="file.fileName">{{ truncateFilename(file.fileName) }}</span>
            </div>
          </el-card>
        </div>
        <div class="message-content-wrapper">
          <div class="message-content">
            <div v-if="!message.editing" class="message-text"
              v-html="renderMarkdown(message.content.text, message.role)">
            </div>
            <el-input v-else type="textarea" :rows="3" v-model="message.editText" class="edit-input"></el-input>
          </div>
          <div v-if="message.role === 'user'" class="message-actions" :class="{'show': message.showActions || message.editing}">
            <el-button v-if="!message.editing" type="text" icon="el-icon-edit"
              @click="startEditMessage(message)"></el-button>
            <el-button v-if="!message.editing" type="text" icon="el-icon-refresh"
              @click="regenerateMessage(message)"></el-button>
            <div v-else class="edit-actions">
              <el-button size="mini" type="primary" @click="sendModifiedMessage(message)">发送</el-button>
              <el-button size="mini" @click="cancelEdit(message)">取消</el-button>
            </div>
          </div>
        </div>

        <!-- 添加分支切换按钮，放置在容器右侧 -->
        <div class="branch-switch-container">
          <div v-if="isFirstMessageInBranch(message) && checkSiblings(message) && !isLoading" class="branch-switch"
            :class="{ 'user-branch': message.role === 'user' }">
            <el-button type="text" @click="toggleShowBranch(message)">
              <i :class="message.showBranchTag ? 'el-icon-caret-top' : 'el-icon-caret-bottom'"></i>
              {{ message.showBranchTag ? '收起分支' : '显示分支' }}
            </el-button>
          </div>
        </div>

        <!-- 分支编辑面板 -->
        <div v-if="message.showBranchTag && !isLoading" class="branch-edit-panel"
          :class="{ 'user-branch-panel': message.role === 'user' }">
          <div class="branch-tag-list">
            <div v-for="(sibling, idx) in siblingNodes.find(node => node.branchId === message.branchId)?.siblings || []"
              :key="sibling.index" class="branch-tag-item"
              :class="{ 'active-branch': sibling.index === currentBranchIndex }">
              <!-- 显示模式 -->
              <template v-if="!sibling.editing">
                <span class="branch-tag-text" @click="switchBranch(sibling.index)">{{ sibling.tag }}</span>
                <el-button type="text" size="mini" @click="startEditBranchTag(sibling)" class="edit-branch-btn">
                  <i class="el-icon-edit"></i>
                </el-button>
              </template>

              <!-- 编辑模式 -->
              <template v-else>
                <el-input v-model="sibling.tag" size="mini" class="branch-tag-input"
                  @keyup.enter.native="saveBranchTag(sibling, message)"></el-input>
                <div class="branch-tag-actions">
                  <el-button type="text" size="mini" @click="cancelEditBranchTag(sibling)">取消</el-button>
                  <el-button type="text" size="mini" @click="saveBranchTag(sibling, message)">保存</el-button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <!-- 已处理文件显示区域 -->
      <div v-if="processedFiles.length > 0 && uploadOnWhichMessage === -1" class="processed-files">
        <el-card v-for="(file, index) in processedFiles" :key="index" class="file-card" shadow="hover">
          <div class="file-card-content">
            <i class="el-icon-document" style="margin-right: 8px;"></i>
            <span :title="file.name">{{ truncateFilename(file.name) }}</span>
            <el-button type="text" icon="el-icon-close" @click="removeProcessedFile(index)"
              class="file-remove-btn"></el-button>
          </div>
        </el-card>
      </div>


      <div class="input-tools">
        <el-button type="text" icon="el-icon-upload" @click="showUploadDialog" title="上传文件">
          <span class="button-text">上传文件</span>
        </el-button>
        <el-button type="text" icon="el-icon-microphone" @click="startVoiceInput" title="语音输入">
          <span class="button-text">语音输入</span>
        </el-button>
      </div>


      <div class="input-container">
        <el-input type="textarea" :rows="3" placeholder="请输入您的问题..." v-model="inputMessage"
          @keyup.enter.native="handleKeyEnter" class="message-input"></el-input>
        <el-button type="primary" @click="sendMessageWithPolling()" :disabled="!inputMessage.trim() || isLoading"
          class="send-button">
          <i class="el-icon-s-promotion"></i>
          <span v-if="!isLoading">发送</span>
          <span v-else>发送中...</span>
        </el-button>
      </div>
    </div>

  <el-dialog title="上传文件" :visible.sync="uploadDialogVisible" width="600px" @close="clearUploadFiles">
    <el-upload class="upload-area" drag action="#" ref="fileUpload" multiple :auto-upload="false"
      :on-change="handleFileChange" :show-file-list="false">
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <div class="el-upload__tip" slot="tip">
        支持上传pdf/docx/txt等格式文件，单个文件不超过50MB
      </div>
    </el-upload>

    <div class="selected-files" v-if="this.selectedFiles.length > 0">
      <h4>已选择文件：</h4>
      <ul>
        <li v-for="(file, index) in this.selectedFiles" :key="index">
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

import qs from 'qs';
import MarkdownIt from 'markdown-it';
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

      //文件上传功能相关
      uploadDialogVisible: false,  // 新增上传对话框状态
      selectedFiles: [],  // 新增选择的文件列表
      uploadOnWhichMessage: -1,//使用哪个消息框上传文件，用于显示（-1表示在下方消息框，不是-1表示在上方消息栏的编辑页面的messageId）
      processedFiles: [],//用于保存和展示上传的文件

      md: new MarkdownIt(),
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
    //渲染markdown
    renderMarkdown(text, role) {
      if (role === 'assistant') {
        const rendered = this.md.render(text || '');
        // 添加样式类名
        return `<div class="markdown-body">${rendered}</div>`;
      }
      return text;
    },
    checkSiblings(message) {

      const siblingNode = this.siblingNodes.find(b => b.branchId == message.branchId)

      if (siblingNode && siblingNode.siblings) {
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
    async fetchData(chatId) {
      try {

        // 获取所有分支
        const response = await this.$axios.post('/api/chat/getAllBranches', { chatId });

        if (response.data) {
          this.allBranches = response.data;

          // 找到根分支（parent_branchId的index为0）
          this.rootBranch = this.allBranches.find(b => b.index == 0);
          //刷新currentBranch的数据
          if (this.currentBranch) {
            this.currentBranch = this.allBranches.find(b => b.index == this.currentBranch.index)
          }

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
        if (currentBranch.children && currentBranch.children.length > 0) {
          childIndex = currentBranch.children[0].branchIndex
          // console.log(childIndex)
        } else {
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
        currentBranch = childIndex ? this.allBranches.find(b => b.index == childIndex) : null;

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
      this.processedFiles = [];
      let parentBranch = this.rootBranch;
      // 遍历父链中的每个分支
      for (const branch of parentChain) {

        if (branch) {
          if (branch.index != 0) {
            // 获取当前分支的所有消息
            const branchMessages = branch?.messageLocals || [];
            // 将消息添加到展示列表中
            this.messageListForShow.push(...branchMessages);



            this.siblingNodes.push({
              index: branch.index,
              branchId: branch.branchId,
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
        branchId: targetBranch.branchId,
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
              tag: `分支${parentBranch.children.length + 1}`
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
          this.modifiedBranch.push(newParentBranch, this.currentBranch, grandParentBranch);

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
          tag: '原分支'
        });
        this.currentBranch.parentBranchIndex = 0;
        this.modifiedBranch.push(this.rootBranch);
      }

      const userMessage = this.createMessage('user', messageText);
      // 关键修改：处理文件上传逻辑

      this.messageListForShow.push(userMessage);

      if (this.currentBranch) {
        if (!this.currentBranch.messageLocals) {
          this.currentBranch.messageLocals = [];
        }
        this.currentBranch.messageLocals.push(userMessage);
      }
      //先保存一下，给后面上传文件时用
      this.modifiedBranch.push(this.currentBranch);
      await this.saveBranchList(this.modifiedBranch)
      this.modifiedBranch = [];

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
        // 关键修改：处理文件上传逻辑

        const formData = new FormData();

        // 1. 添加聊天请求元数据
        formData.append('chatRequest', JSON.stringify({
          messageList: this.chatMessages,
          interviewer: this.currentInterviewer
        }));

        // 2. 添加文件数据（如果有）
        this.processedFiles.forEach(file => {
          formData.append('files', file.raw);
        });

        // 3. 添加消息ID
        formData.append('fileMessageId', userMessage.messageId);

        // 4. 发送请求
        const response = await this.$axios.post('/api/chat/sendMessageWithPoll', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });

        // 5. 清空已处理文件
        this.processedFiles = [];
        messageId = response.data; // 获取后端返回的消息ID

        await this.fetchData(this.chatRecordId);
        await this.buildPathForTargetBranch(this.currentBranch);
        // 创建AI消息占位对象
        const aiMessage = {
          messageId: messageId, // 添加前缀便于识别
          role: 'assistant',
          content: {
            text: '', // 初始为空

            files: []
          },
          timestamp: new Date().toISOString()
        };


        this.messageListForShow.push(aiMessage);
        this.currentAiMessageId = aiMessage.messageId;

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
              const aiMsg = this.messageListForShow.find(m => m.messageId === messageId);
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
              const aiMsg = this.messageListForShow.find(m => m.messageId === messageId);
              if (aiMsg) {

                // 将AI消息加入currentBranch
                this.currentBranch.messageLocals.push({
                  messageId: aiMsg.messageId,
                  role: 'assistant',
                  branchId: this.currentBranch.branchId,
                  content: {
                    text: aiMsg.content.text,

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

          files: [],
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
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0,
          v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    },



    //文件上传相关
    clearUploadFiles() {

      if (this.$refs.fileUpload) {
        this.$refs.fileUpload.clearFiles(); // 清除上传组件的内部文件列表
      }
    },
    formatFileSize(size) {
      if (size < 1024) {
        return size + 'B'
      } else if (size < 1024 * 1024) {
        return (size / 1024).toFixed(1) + 'KB'
      } else {
        return (size / (1024 * 1024)).toFixed(1) + 'MB'
      }
    },
    showUploadDialog() {
      this.selectedFiles = [];
      this.uploadDialogVisible = true;
      this.$nextTick(() => {
        this.clearUploadFiles(); // 清空组件内部文件列表
      });
    },
    clearUploadFiles() {
      if (this.$refs.fileUpload) {
        // 清除组件内部维护的文件列表
        this.$refs.fileUpload.uploadFiles = [];
      }
    },

    handleFileChange(file, fileList) {
      this.selectedFiles = fileList
    },

    removeSelectedFile(index) {
      this.selectedFiles.splice(index, 1)
    },

    async uploadFiles() {
      if (this.selectedFiles.length === 0) {
        this.$message.warning('请选择要上传的文件')
        return
      }
      this.processedFiles = [...this.processedFiles, ...this.selectedFiles.map(file => ({
        name: file.name,
        size: file.size,
        type: file.raw.type,
        raw: file.raw  // 保留原始File对象
      }))]
      console.log(this.processedFiles)
      this.uploadDialogVisible = false;
      this.selectedFiles = [];

    },
    removeProcessedFile(index) {
      this.processedFiles.splice(index, 1);
    },
    truncateFilename(filename) {
      const maxLength = 15; // 最大显示长度
      const extensionIndex = filename.lastIndexOf('.');

      if (extensionIndex === -1) {
        // 没有后缀名的情况
        return filename.length > maxLength
          ? `${filename.substring(0, maxLength)}...`
          : filename;
      }

      const name = filename.substring(0, extensionIndex);
      const extension = filename.substring(extensionIndex);

      if (name.length > maxLength) {
        return `${name.substring(0, maxLength)}...${extension}`;
      }

      return filename;
    },

    // 新增语音输入方法
    startVoiceInput() {
      this.$message.info('语音输入功能正在开发中')
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
  padding: 22px 20px;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
  font-weight: 500;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9fafc;
}

.message {
  margin-bottom: 10px;
  display: flex;
  padding-left: 40px;
  padding-right: 40px;
  flex-direction: column;
  align-items: flex-start;
  position: relative;
}

.message-content-wrapper {
  display: flex;
  align-items: flex-start;
  max-width: 60%;
}

.message-content {
  padding: 10px 16px;
  border-radius: 8px;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex: 1;
}

.user-message {
  align-items: flex-end;
}

.user-message .message-content-wrapper {
  flex-direction: row-reverse;
}

.user-message .message-content {
  background-color: #409eff;
  border: 1px solid #ebeef5;
  color: white;
  border-top-right-radius: 0;
}

.ai-message .message-content {
  background-color: #fff;
  color: #333;
  border: 1px solid #ebeef5;
  border-top-left-radius: 0;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin: 5px 0;
}

/* 分支切换容器 - 添加这个新的容器 */
.branch-switch-container {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: 5px;
}

/* 分支切换按钮 */
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
  width: 100%;
  max-width: 80%;
  transition: all 0.3s ease;
  align-self: flex-start;
}

.user-branch-panel {
  align-self: flex-end;
}

/* 分支标签列表 - 水平布局 */
.branch-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
}

.branch-tag-text:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

.edit-branch-btn {
  opacity: 0.5;
  transition: opacity 0.2s ease;
}

.branch-tag-item:hover .edit-branch-btn {
  opacity: 1;
}

.branch-tag-input {
  flex: 1;
  margin-right: 8px;
}

.branch-tag-actions {
  display: flex;
}

.chat-input {
  padding: 5px;
  border-top: 1px solid #ebeef5;
  background-color: #fff;
  display: flex;
  flex-direction: column;
}
.input-tools {
  display: flex;
  margin-bottom: 20px;
  margin-top: 10px;
  padding-left: 150px;
}
.input-tools .el-button {
  display: flex;
  align-items: center;
  margin-right: 15px;
  padding: 8px 12px;
  color: #606266;
  background-color: #f5f7fa;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.input-tools .el-button:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

.input-tools .button-text {
  margin-left: 5px;
  font-size: 13px;
}
.input-container {
  display: flex;
  align-items: center;
  height: 150px;
  padding-left: 150px;
  padding-bottom: 10px;
  position: relative;
}

.input-actions {
  display: flex;
  align-items: center;
  margin-top: 10px;
}
.message-input {
  flex: 1;
  border-radius: 8px;
  padding-bottom: 10px;
}

.message-input :deep(.el-textarea__inner) {
  resize: none;
  border-radius: 8px;
  transition: border-color 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  height: 150px;
  width: 70%;
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.send-button {
  position: absolute;
  right: 200px;
  height: 50px;
  width: 120px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
  align-self: center; /* 确保按钮在容器中垂直居中 */
}

.send-button i {
  font-size: 18px;
  margin-bottom: 5px;
}

/* 文件卡片样式 */
.file-card {
  width: 300px;
  height: 50px;
  margin-bottom: 12px;
  border-radius: 8px;
}

.file-card-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0px;
}

.file-remove-btn {
  padding: 0;
  margin-left: 18px;
  color: #f56c6c;
}

.processed-files {
  padding-left: 100px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 0px;
}
/* 消息操作按钮 */
.message-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
  margin: 0 8px;
}

.user-message .message-content-wrapper:hover .message-actions,
.message-actions.show {
  opacity: 1;
}

.edit-actions {
  display: flex;
  gap: 8px;
  opacity: 1;
}

.message-actions.show,
.message .message-actions:has(.edit-actions) {
  opacity: 1 !important;
  visibility: visible !important;
}

/* 打字机效果 */
.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

/* 上传对话框样式 */
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
/* 确保只有在悬停状态下才显示按钮，即使有.show类 */
.message:not(:hover) .message-actions:not(:has(.edit-actions)) {
  opacity: 0 !important;
}

/* 编辑状态下的按钮总是显示 */
.message .message-actions:has(.edit-actions) {
  opacity: 1 !important;
}
</style>