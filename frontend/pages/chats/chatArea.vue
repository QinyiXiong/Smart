<template>
  <div class="right-chat-area">
    <div class="chat-header">
      <h2 v-if="currentInterviewer">{{ currentInterviewer.name }}</h2>
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
              <i :class="getFileIcon(file.fileName)" style="margin-right: 8px;"></i>
              <span :title="file.fileName">{{ truncateFilename(file.fileName) }}</span>
            </div>
          </el-card>
        </div>
        
        <div class="message-content-wrapper">
          <div class="message-content">
            <div v-if="!message.editing">
              <!-- AI消息加载状态 -->
              <div v-if="message.role === 'assistant' && isAiThinking && message.content.text === ''" 
                   class="thinking-indicator">
                <div class="blue-spinner"><div class="spinner"></div></div>
                <span>思考中...</span>
              </div>
              <div v-else class="message-text" v-html="renderMarkdown(message.content.text, message.role)"></div>
            </div>
            <div v-else class="edit-input-container">
              <el-input type="textarea" :rows="3" v-model="message.editText" class="edit-input"></el-input>
              <div class="edit-actions">
                <el-button size="mini" @click="cancelEdit(message)">取消</el-button>
                <el-button size="mini" type="primary" @click="sendModifiedMessage(message)">确定</el-button>
              </div>
            </div>
          </div>
          <div v-if="message.role === 'user'" class="message-actions"
            :class="{ 'show': message.showActions || message.editing }">
            <el-button v-if="!message.editing" type="text" icon="el-icon-edit"
              @click="startEditMessage(message)"></el-button>
            <el-button v-if="!message.editing" type="text" icon="el-icon-refresh"
              @click="regenerateMessage(message)"></el-button>
          </div>
        </div>

        <!-- 分支切换按钮 -->
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
              <template v-if="!sibling.editing">
                <span class="branch-tag-text" @click="switchBranch(sibling.index)">{{ sibling.tag }}</span>
                <el-button type="text" size="mini" @click="startEditBranchTag(sibling)" class="edit-branch-btn">
                  <i class="el-icon-edit"></i>
                </el-button>
              </template>
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
      <div v-if="processedFiles.length > 0 && uploadOnWhichMessage === -1" class="processed-files">
        <el-card v-for="(file, index) in processedFiles" :key="index" class="file-card" shadow="hover">
          <div class="file-card-content">
            <i :class="getFileIcon(file.name)" style="margin-right: 8px;"></i>
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
        <el-button type="primary" @click="sendMessageWithPolling()"
          :disabled="(!inputMessage.trim() && processedFiles.length === 0) || isLoading" class="send-button">
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

      // 语音录音相关
      mediaRecorder: null, // 媒体录音器实例
      recordingTimer: null, // 录音计时器
      recordingMessageBox: null, // 录音消息框

      // ai思考跟踪
      isAiThinking: false,

      md: new MarkdownIt(),
    }
  },
  watch: {
    chatRecordId: {
      handler(newVal) {
        if (newVal) {
          this.loadChatMessages(newVal);
        } else {
          // 当chatRecordId为null时清空聊天区域
          this.messageListForShow = [];
          this.allBranches = [];
          this.currentBranch = null;
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
    getFileIcon(filename) {
      const extension = filename.split('.').pop().toLowerCase();
      switch (extension) {
        // 音频文件
        case 'mp3':
        case 'wav':
        case 'ogg':
        case 'flac':
        case 'aac':
        case 'm4a':
          return 'el-icon-headset';
        
        // 视频文件
        case 'mp4':
        case 'avi':
        case 'mov':
        case 'wmv':
        case 'mkv':
        case 'flv':
        case 'webm':
          return 'el-icon-video-camera';
        
        // 文档文件
        case 'pdf':
          return 'el-icon-document';
        case 'doc':
        case 'docx':
          return 'el-icon-document-checked';
        case 'xls':
        case 'xlsx':
        case 'csv':
          return 'el-icon-document-excel';
        case 'ppt':
        case 'pptx':
          return 'el-icon-document-ppt';
        case 'txt':
        case 'md':
        case 'rtf':
          return 'el-icon-document-text';
        
        // 图片文件
        case 'jpg':
        case 'jpeg':
        case 'png':
        case 'gif':
        case 'bmp':
        case 'svg':
        case 'webp':
        case 'tiff':
        case 'ico':
          return 'el-icon-picture';
        
        // 压缩文件
        case 'zip':
        case 'rar':
        case '7z':
        case 'tar':
        case 'gz':
          return 'el-icon-folder-checked';
        
        // 代码文件
        case 'js':
        case 'ts':
        case 'html':
        case 'css':
        case 'scss':
        case 'less':
        case 'vue':
        case 'jsx':
        case 'tsx':
        case 'json':
        case 'xml':
        case 'py':
        case 'java':
        case 'c':
        case 'cpp':
        case 'cs':
        case 'php':
        case 'rb':
        case 'go':
        case 'sql':
          return 'el-icon-tickets';
        
        // 默认图标
        default:
          return 'el-icon-document';
      }
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
      // 如果是Shift+Enter组合键，允许换行
      if (e.shiftKey) {
        return;
      }
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

        if (parentBranch) {
          // 更新标签
          const siblings = this.siblingNodes.find(node => node.branchId == message.branchId)?.siblings || [];
          parentBranch.children.forEach(child => {
            const sibling = siblings.find(s => s.index == child.branchIndex);
            if (sibling) {
              child.tag = sibling.tag;
            }
          });

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
      let messageText = messageContent !== null ? String(messageContent) : String(this.inputMessage);
      if ((!messageText || !messageText.trim()) && this.processedFiles.length === 0 || this.isLoading) return;

      // 如果输入框为空但有上传文件，添加默认文本
      if ((!messageText || !messageText.trim()) && this.processedFiles.length > 0) {
        messageText = '用户上传文件';
      }
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
      //处理文件上传逻辑

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
      this.isAiThinking = true;
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
        this.isAiThinking = false;
        console.error(error);
      }
    },

    async startPolling(messageId) {
      const POLLING_TIMEOUT = 30000; // 5秒超时
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
          console.log(response.data)
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

                // 确保msg.text不为null或undefined再处理
                if (msg.text !== null && msg.text !== undefined) {
                  await this.typeText(aiMsg, msg.text);
                  if (aiMsg.content.text.length > oldLength) {
                    hasNewContent = true;  // 只有内容变化时才标记
                  }
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
              this.isAiThinking = false;
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
                this.$emit('polling-completed');
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

    // 处理action171_push类型数据的方法
    handleActionPush(problemId) {
      if (problemId) {
        // 跳转到题目页面，并带上currentBranchId和chatId参数
        this.$router.push(`/oj/problem/${problemId}?branchId=${this.currentBranch.branchId}&chatId=${this.chatRecordId}`);
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
      this.stopPolling();

      // 清理录音相关资源
      if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
        this.mediaRecorder.stop();
      }

      if (this.recordingTimer) {
        clearInterval(this.recordingTimer);
        this.recordingTimer = null;
      }

      // 关闭录音消息框
      if (this.recordingMessageBox) {
        this.recordingMessageBox.close();
      }
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

    // 语音输入方法
    startVoiceInput() {
      // 检查浏览器是否支持录音API
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        this.$message.error('您的浏览器不支持录音功能');
        return;
      }

      // 创建录音对话框
      this.$confirm('准备开始录音，点击确定开始录音，取消则退出', '语音输入', {
        confirmButtonText: '开始录音',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        // 开始录音
        this.startRecording();
      }).catch(() => {
        this.$message.info('已取消录音');
      });
    },

    // 开始录音
    async startRecording() {
      try {
        // 请求麦克风权限
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

        // 创建MediaRecorder实例
        this.mediaRecorder = new MediaRecorder(stream);

        // 存储录音数据
        const audioChunks = [];

        // 监听数据可用事件
        this.mediaRecorder.ondataavailable = (event) => {
          if (event.data.size > 0) {
            audioChunks.push(event.data);
          }
        };

        // 监听录音停止事件
        this.mediaRecorder.onstop = () => {
          // 将录音数据合并为一个Blob
          const audioBlob = new Blob(audioChunks, { type: 'audio/mp3' });

          // 创建一个唯一的文件名
          const fileName = `voice_${new Date().getTime()}.mp3`;

          // 创建File对象
          const audioFile = new File([audioBlob], fileName, { type: 'audio/mp3' });

          // 将录音文件添加到文件列表
          this.processedFiles.push({
            name: fileName,
            size: audioBlob.size,
            type: 'audio/mp3',
            raw: audioFile
          });

          // 关闭所有轨道
          stream.getTracks().forEach(track => track.stop());

          this.$message.success('录音已完成并添加到文件列表');
        };

        // 开始录音
        this.mediaRecorder.start();

        // 显示正在录音的提示
        this.$message({
          message: '正在录音中，点击停止按钮结束录音',
          type: 'warning',
          duration: 0,
          showClose: true,
          center: true,
          customClass: 'recording-message'
        });

        // 创建音频分析器用于波形图显示
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const analyser = audioContext.createAnalyser();
        analyser.fftSize = 2048;
        const bufferLength = analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);
        
        // 创建音频源
        const source = audioContext.createMediaStreamSource(stream);
        source.connect(analyser);
        
        // 创建停止录音按钮
        const h = this.$createElement;
        this.recordingMessageBox = this.$msgbox({
          title: '录音中',
          message: h('div', null, [
            h('p', { style: 'text-align: center' }, '正在录音，请对着麦克风说话'),
            h('div', { style: 'text-align: center; margin-top: 20px' }, [
              h('span', { style: 'color: #f56c6c; margin-right: 10px' }, '录音时长: '),
              h('span', { class: 'recording-timer', ref: 'timer' }, '00:00')
            ]),
            // 添加波形图canvas
            h('canvas', { 
              style: 'width: 100%; height: 80px; background-color: #f5f5f5; margin-top: 10px;',
              class: 'waveform-canvas',
              ref: 'waveformCanvas'
            })
          ]),
          showCancelButton: false,
          confirmButtonText: '停止录音',
          beforeClose: (action, instance, done) => {
            // 无论如何关闭对话框（点击确认按钮或叉号），都停止录音并清理资源
            // 停止录音
            if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
              this.mediaRecorder.stop();
            }
            // 清除计时器和动画
            if (this.recordingTimer) {
              clearInterval(this.recordingTimer);
              this.recordingTimer = null;
            }
            if (this.animationFrame) {
              cancelAnimationFrame(this.animationFrame);
              this.animationFrame = null;
            }
            // 关闭音频上下文
            audioContext.close();
            // 关闭所有消息
            this.$message.closeAll();
            done();
          }
        });
        
        // 在下一个事件循环中获取canvas元素并开始绘制波形
        this.$nextTick(() => {
          const canvas = document.querySelector('.waveform-canvas');
          if (canvas) {
            const canvasCtx = canvas.getContext('2d');
            const width = canvas.width;
            const height = canvas.height;
            
            // 创建波形数据缓冲区，用于存储历史波形数据
            const waveformHistory = [];
            // 设置要显示的数据点数量
            const displayPoints = 100;
            // 每次从分析器获取的数据中抽样的点数
            const samplePoints = 10;
            
            // 绘制波形函数 - 从右向左推进（使用垂直线条）
            const drawWaveform = () => {
              this.animationFrame = requestAnimationFrame(drawWaveform);
              
              // 获取音频数据
              analyser.getByteTimeDomainData(dataArray);
              
              // 计算当前音频数据的平均振幅
              let sum = 0;
              for (let i = 0; i < bufferLength; i++) {
                // 将值转换为 -1 到 1 的范围
                const amplitude = (dataArray[i] / 128.0) - 1.0;
                sum += Math.abs(amplitude);
              }
              const averageAmplitude = sum / bufferLength;
              
              // 将平均振幅添加到历史记录的开头（新数据在左侧）
               waveformHistory.unshift(averageAmplitude);
               
               // 如果历史记录过长，则移除最早的数据点（右侧）
               while (waveformHistory.length > displayPoints) {
                 waveformHistory.pop();
               }
              
              // 清除画布
              canvasCtx.fillStyle = '#f5f5f5';
              canvasCtx.fillRect(0, 0, width, height);
              
              // 设置线条样式
              canvasCtx.lineWidth = 2;
              canvasCtx.strokeStyle = '#409EFF';
              
              // 计算每个线条的宽度
              const barWidth = width / displayPoints;
              const barSpacing = 2; // 线条之间的间距
              const actualBarWidth = barWidth - barSpacing;
              
              // 绘制垂直线条（从左向右）
               for (let i = 0; i < waveformHistory.length; i++) {
                 // 计算x坐标，使最新的数据在最左侧
                 const x = i * barWidth;
                
                // 计算线条高度（基于振幅）
                const amplitude = waveformHistory[i];
                const barHeight = Math.max(2, amplitude * height * 0.8); // 最小高度为2像素
                
                // 计算线条的起点和终点（垂直居中）
                const startY = (height - barHeight) / 2;
                const endY = startY + barHeight;
                
                // 绘制垂直线条
                canvasCtx.beginPath();
                canvasCtx.moveTo(x, startY);
                canvasCtx.lineTo(x, endY);
                canvasCtx.stroke();
              }
            };
            
            // 开始绘制波形
            drawWaveform();
          }
        });

        // 开始计时
        let seconds = 0;
        this.recordingTimer = setInterval(() => {
          seconds++;
          const minutes = Math.floor(seconds / 60);
          const remainingSeconds = seconds % 60;
          const timeString = `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;

          // 更新计时器显示
          const timerElements = document.getElementsByClassName('recording-timer');
          if (timerElements && timerElements.length > 0) {
            timerElements[0].textContent = timeString;
          }

          // 如果录音超过3分钟，自动停止
          if (seconds >= 180) {
            if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
              this.mediaRecorder.stop();
            }
            clearInterval(this.recordingTimer);
            this.recordingTimer = null;
            this.recordingMessageBox.close();
            this.$message.info('录音已达到最大时长(3分钟)，已自动停止');
          }
        }, 1000);
      } catch (error) {
        console.error('录音失败:', error);
        this.$message.error('无法访问麦克风，请确保已授予麦克风权限');
      }
    }
  }
}
</script>

<style scoped>
@import url("//unpkg.com/element-ui@2.15.13/lib/theme-chalk/index.css");

.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.el-icon-loading {
  font-size: 16px;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.el-icon-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
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
  background-color: #EFF6FF;
  border: 1px solid #ebeef5;
  color: #262626;
  border-top-right-radius: 0;
}

.ai-message .message-content {
  background-color: #f5f5f5;
  color: #262626;
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
  /* 移除固定宽度，使用自适应宽度 */
  display: inline-block;
  min-width: 200px;
  max-width: 90%;
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
  /* 确保长文本能够适应空间 */
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
  /* 确保长文本能够适应空间 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
  /* 限制最大宽度，避免太长的标签文本 */
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
  border-top: none;
  background-color: #f9fafc;
  display: flex;
  flex-direction: column;
}

.input-tools {
  display: flex;
  margin-bottom: 20px;
  margin-top: 10px;
  padding-left: 150px;
  background-color: #f9fafc;

}

.input-tools .el-button {
  display: flex;
  align-items: center;
  margin-right: 15px;
  padding: 8px 12px;
  color: #474747;
  background-color: #ededee;
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
  align-items: flex-start;
  height: 150px;
  padding-left: 150px;
  padding-right: 150px;
  padding-bottom: 10px;
  background-color: #f9fafc;
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
  width: 100%;
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.send-button {
  height: 50px;
  width: 120px;
  margin-left: 15px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
  align-self: center;
  /* 确保按钮在容器中垂直居中 */
}

.send-button i {
  font-size: 18px;
  margin-bottom: 5px;
}

/* 文件卡片样式 */
.file-card {
  width: 300px;
  height: 50px;
  padding-bottom: 10px;
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
  padding-left: 150px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 0px;
}

.processed-files .file-card {}

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

.edit-input-container {
  position: relative;
  width: 100%;
  min-width: 450px;
  /* 设置最小宽度 */
  max-width: 100%;
  padding-bottom: 40px;
  /* 为按钮留出空间 */
}

/* 编辑输入框 */
.edit-input {
  width: 100%;
}

/* 编辑操作按钮 */
.edit-actions {
  position: absolute;
  bottom: 0px;
  right: 8px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* 确保消息内容在编辑状态下有足够空间 */
.message-content {
  min-width: 0px;
  transition: all 0.3s ease;
}

/* 调整用户消息的编辑样式 */
.user-message .edit-input-container .el-textarea__inner {
  background-color: rgba(64, 158, 255, 0.05);
  border-color: #409eff;
}

.user-message .edit-input :deep(.el-textarea__inner) {
  background-color: rgba(64, 158, 255, 0.03);
  /* 半透明蓝色背景 */
  border: none;
  /* 蓝色边框 */
}

/* 录音相关样式 */
.recording-message {
  background-color: #fef0f0;
  border: 1px solid #fbc4c4;
}

.recording-timer {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}
/* 蓝色转圈加载器 */
.blue-spinner {
  display: inline-block;
  position: relative;
  width: 20px;
  height: 20px;
  margin-right: 8px;
}

.blue-spinner .spinner {
  box-sizing: border-box;
  display: block;
  position: absolute;
  width: 20px;
  height: 20px;
  border: 2px solid #409EFF;
  border-radius: 50%;
  border-color: #409EFF transparent transparent transparent;
  animation: spinner 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

@keyframes spinner {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 思考中指示器 */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  padding: 10px 16px;
}

.thinking-indicator .el-icon {
  animation: rotating 2s linear infinite;
  font-size: 16px;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 确保AI消息气泡有最小高度 */
.ai-message .message-content {
  min-height: 50px;
  display: flex;
  align-items: center;
}
</style>