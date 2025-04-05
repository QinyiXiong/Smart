<template>
  <el-container style="height: 100vh">
    <!-- 左侧历史记录 -->
    <el-aside width="300px" style="border-right: 1px solid #eee">
      <div class="history-header">聊天历史</div>
      <el-menu :default-active="currentSessionId.toString()">
        <el-menu-item 
          v-for="session in historySessions"
          :key="session.id"
          :index="session.id.toString()"
          @click="switchSession(session.id)"
        >
          <span>{{ session.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主聊天区域 -->
    <el-main>
      <div class="chat-container">
        <!-- 消息区域 -->
        <div class="message-area" ref="messageContainer">
          
          <div 
            v-for="(message, index) in currentMessages"
            :key="index"
            class="message-wrapper"
            :class="{ 'user-message': message.isUser }"
          >
            <div class="message-bubble">
              <div class="message-content" v-html="message.contentHtml"></div>
              <div class="message-time">{{ message.time }}</div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area-fixed">
          <div class="input-wrapper">
            <el-input
            type="textarea"
            :rows="1"
            placeholder="输入你的消息..."
            v-model="inputText"
            @keyup.enter.native="sendMessage"
          ></el-input>
          <el-button 
          type="primary" 
          class="send-btn"
          @click="sendMessage"
        >发送</el-button>
        </div>
      </div>
    </div>
      <!-- 隐藏的Markdown解析器 -->
      <div id="md-converter" style="display: none"></div>
    </el-main>
  </el-container>
</template>

<script>
import Vditor from 'vditor'
import 'vditor/dist/index.css'

export default {
  data() {
    return {
      mdConverter: null,
      currentSessionId: 1,
      inputText: '',
      historySessions: [
        { id: 1, title: '如何学习Vue', time: '2023-07-20 14:30' },
        { id: 2, title: 'Element UI使用问题', time: '2023-07-19 09:15' }
      ],
      currentMessages: [
        {
          content: '你好！我是AI助手，有什么可以帮您的？',
          contentHtml: '你好！我是AI助手，有什么可以帮您的？',
          time: '14:30:45',
          isUser: false
        },
        {
          content: '我想学习Vue框架，应该从哪里开始？',
          contentHtml: '我想学习Vue框架，应该从哪里开始？',
          time: '14:31:20',
          isUser: true
        },
        {
          content: '建议从官方文档开始，先了解基础概念，然后通过实战项目加深理解。',
          contentHtml: '建议从官方文档开始，先了解基础概念，然后通过实战项目加深理解。',
          time: '14:31:50',
          isUser: false
        }
      ]
    }
  },
  mounted() {
    this.initMarkdownConverter()
  },
  methods: {
    initMarkdownConverter() {
      this.mdConverter = new Vditor('md-converter', {
        mode: 'sv',
        preview: {
          hljs: { enable: true, lineNumber: true, style: 'github' },
          markdown: { toc: true, autoSpace: true },
          math: { inlineDigit: true },
          theme: { path: 'https://unpkg.com/vditor@3.8.17/dist/css/content-theme' }
        },
        after: () => {
          document.getElementById('md-converter').style.display = 'none'
        }
      })
    },
    async sendMessage() {
      if (!this.inputText.trim()) return

      try {
        const mdContent = this.inputText
        this.mdConverter.setValue(mdContent)
        
        // 添加用户消息
        this.currentMessages.push({
          content: mdContent,
          contentHtml: await this.mdConverter.getHTML(),
          time: new Date().toLocaleTimeString(),
          isUser: true
        })

        // 模拟AI回复
        setTimeout(async () => {
          const reply = '这是模拟的AI回复内容'
          this.mdConverter.setValue(reply)
          
          this.currentMessages.push({
            content: reply,
            contentHtml: await this.mdConverter.getHTML(),
            time: new Date().toLocaleTimeString(),
            isUser: false
          })

          this.scrollToBottom()
        }, 1000)

        this.inputText = ''
        this.scrollToBottom()
      } catch (e) {
        console.error('消息发送失败:', e)
      }
    },
    switchSession(sessionId) {
      this.currentSessionId = sessionId
      // 这里可以添加加载对应会话消息的逻辑
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messageContainer
        container.scrollTop = container.scrollHeight
      })
    }
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.message-area {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 80px; /* 给输入区域留出空间 */
  min-height: 0;
}

.input-area-fixed {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: 20px;
  border-top: 1px solid #eee;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.message-area::after {
  content: '';
  display: table;
  clear: both;
}

.message-wrapper {
  margin: 15px 0;
  clear: both;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 8px;
  position: relative;
  background-color: #fff;
  float: left;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-message .message-bubble {
  background-color: #409EFF;
  color: white;
  float: right;
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
}
.input-wrapper {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.message-content >>> pre {
  padding: 15px;
  background: #f6f8fa;
  border-radius: 6px;
  margin: 10px 0;
  overflow-x: auto;
}

.user-message .message-content >>> pre {
  background: rgba(255,255,255,0.15);
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  text-align: right;
}

.user-message .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.input-area {
  padding: 25px;
  border-top: 1px solid #eee;
  background-color: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}
.send-btn {
  height: 30px; /* 保持与输入框高度一致 */
  margin-left: 10px;
}
:deep(.el-textarea__inner) {
  resize: none;
  flex: 1;
}
.history-header {
  padding: 20px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #eee;
  background-color: #fafafa;
}

.el-container {
  height: calc(100vh - 导航栏高度) !important; /* 根据实际导航栏高度调整 */
}

.el-main {
  padding: 0 !important;
  display: flex;
  flex-direction: column;
}
</style>