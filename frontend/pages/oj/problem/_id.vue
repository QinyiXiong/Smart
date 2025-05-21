<template>
  <div class="problem-detail">
    <!-- 返回按钮 -->
    <div class="return-button-container" v-if="chatId && branchId">
      <el-button type="primary" icon="el-icon-back" @click="returnToChat">返回聊天</el-button>
    </div>
    <el-card class="problem-header">
      <div class="title-section">
        <div class="title-row">
          <span class="problem-code">{{ problem.problemCode }}</span>
          <h1>{{ problem.title }}</h1>
        </div>
        <div class="meta">
          <el-tag :type="getDifficultyTagType(problem.difficulty)" effect="dark" size="medium">
            {{ problem.difficulty }}
          </el-tag>
          <el-tag v-for="tag in problem.tags" :key="tag" type="info" effect="plain" class="tag-item">
            {{ tag }}
          </el-tag>
          <div class="problem-stats">
            <el-tooltip content="时间限制" placement="top">
              <span class="stat-item">
                <i class="el-icon-timer"></i>
                {{ problem.timeLimit }}ms
              </span>
            </el-tooltip>
            <el-tooltip content="内存限制" placement="top">
              <span class="stat-item">
                <i class="el-icon-coin"></i>
                {{ problem.memoryLimit }}MB
              </span>
            </el-tooltip>
            <el-tooltip content="通过率" placement="top">
              <span class="stat-item">
                <i class="el-icon-data-line"></i>
                {{ (problem.acceptanceRate * 100).toFixed(1) }}%
                ({{ problem.acceptCount }}/{{ problem.submitCount }})
              </span>
            </el-tooltip>
          </div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="24" class="main-content">
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="description-card">
          <div class="content">
            <div class="section">
              <h2><i class="el-icon-document"></i> 题目描述</h2>
              <div class="markdown-content" v-html="renderedDescription"></div>
            </div>

            <div class="section">
              <h2><i class="el-icon-upload2"></i> 输入描述</h2>
              <div class="markdown-content" v-html="renderedInputDescription"></div>
            </div>

            <div class="section">
              <h2><i class="el-icon-download"></i> 输出描述</h2>
              <div class="markdown-content" v-html="renderedOutputDescription"></div>
            </div>

            <div class="section">
              <h2><i class="el-icon-notebook-2"></i> 示例</h2>
              <el-card v-for="(example, index) in examples" :key="index" class="example-card" shadow="never">
                <div class="example-header">示例 {{ index + 1 }}</div>
                <div class="example-content">
                  <div class="example-input">
                    <div class="example-label">输入：</div>
                    <pre>{{ example.input }}</pre>
                  </div>
                  <div class="example-output">
                    <div class="example-label">输出：</div>
                    <pre>{{ example.output }}</pre>
                  </div>
                </div>
              </el-card>
            </div>

            <div v-if="problem.hints" class="section">
              <h2><i class="el-icon-info"></i> 提示</h2>
              <div class="markdown-content" v-html="renderedHints"></div>
            </div>

            <div v-if="problem.source" class="section">
              <h2><i class="el-icon-collection-tag"></i> 来源</h2>
              <div class="source-content">{{ problem.source }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="ide-card">
          <div class="ide-header">
            <div class="ide-title">
              <h2><i class="el-icon-edit"></i> 代码编辑器</h2>
              <el-select
                v-model="language"
                placeholder="选择编程语言"
                size="small"
                class="language-select"
              >
                <el-option
                  v-for="lang in languages"
                  :key="lang.value"
                  :label="lang.label"
                  :value="lang.value"
                >
                  <span class="language-option">
                    <i :class="lang.icon"></i>
                    {{ lang.label }}
                  </span>
                </el-option>
              </el-select>
            </div>
            <div class="ide-actions">
              <el-tooltip content="自动补全" placement="top">
                <el-switch v-model="editorOptions.quickSuggestions.other" />
              </el-tooltip>
              <el-tooltip content="代码格式化" placement="top">
                <el-button
                  size="small"
                  icon="el-icon-magic-stick"
                  @click="formatCode"
                >
                  格式化
                </el-button>
              </el-tooltip>
            </div>
          </div>

          <div class="code-editor-container">
            <div ref="monacoContainer" class="code-editor"></div>
            <div v-if="monacoLoading" class="editor-loading">
              <i class="el-icon-loading"></i>
              <span>加载中...</span>
            </div>
          </div>

          <div class="test-section">
            <div class="test-header">
              <h3><i class="el-icon-tickets"></i> 测试用例</h3>
              <el-radio-group v-model="testType" size="small">
                <el-radio-button label="custom">自定义测试</el-radio-button>
                <el-radio-button label="example">示例测试</el-radio-button>
              </el-radio-group>
            </div>

            <div v-if="testType === 'custom'" class="custom-test">
              <el-input
                type="textarea"
                :rows="4"
                placeholder="输入测试用例"
                v-model="testCase"
                class="test-input"
              ></el-input>
            </div>

            <div v-else class="example-test">
              <el-select v-model="selectedExample" placeholder="选择示例" size="small">
                <el-option
                  v-for="(example, index) in examples"
                  :key="index"
                  :label="`示例 ${index + 1}`"
                  :value="index"
                ></el-option>
              </el-select>
            </div>
          </div>

          <div class="action-buttons">
            <el-button
              type="primary"
              @click="runCode"
              icon="el-icon-video-play"
              :loading="isRunning"
            >
              运行代码
            </el-button>
            <el-button
              type="success"
              @click="submitCode"
              icon="el-icon-upload"
              :loading="isSubmitting"
            >
              提交代码
            </el-button>
            <el-button
              @click="resetCode"
              icon="el-icon-refresh-right"
              plain
            >
              重置
            </el-button>
          </div>

          <transition name="fade">
            <div v-if="runResult" class="result-container">
              <div class="result-header">
                <el-tag
                  :type="getResultTagType(runResult.status)"
                  effect="dark"
                >
                  {{ getResultText(runResult.status) }}
                </el-tag>
                <div class="result-stats">
                  <span class="stat-item">
                    <i class="el-icon-timer"></i>
                    执行时间: {{ runResult.time }}ms
                  </span>
                  <span class="stat-item">
                    <i class="el-icon-coin"></i>
                    内存消耗: {{ runResult.memory }}MB
                  </span>
                </div>
              </div>
              <el-card class="output-card" shadow="never">
                <pre class="result-output" :class="{ 'error': runResult.status === 'error' }">{{ runResult.output }}</pre>
              </el-card>
            </div>
          </transition>
          
          <!-- AI代码评价结果 -->
          <transition name="fade">
            <div v-if="aiReviewResult" class="ai-review-container">
              <div class="ai-review-header">
                <h3><i class="el-icon-chat-line-round"></i> AI代码评价</h3>
                <el-tag v-if="isPollingAiReview" type="info">正在生成评价...</el-tag>
              </div>
              <el-card class="ai-review-card" shadow="hover">
                <div class="ai-review-content markdown-body" v-html="renderedAiReview"></div>
              </el-card>
            </div>
          </transition>

          <div v-if="judgeResult" class="judge-result">
            <el-card class="result-card">
              <div class="result-header">
                <h3>评测结果</h3>
                <el-tag :type="getJudgeStatusType(judgeResult.status)" effect="dark">
                  {{ getJudgeStatusText(judgeResult.status) }}
                </el-tag>
              </div>
              <div class="result-info">
                <div class="info-item">
                  <span class="label">执行时间:</span>
                  <span class="value">{{ judgeResult.time }}ms</span>
                </div>
                <div class="info-item">
                  <span class="label">内存消耗:</span>
                  <span class="value">{{ judgeResult.memory }}MB</span>
                </div>
              </div>
              <div v-if="judgeResult.testcases" class="testcase-results">
                <div v-for="(testcase, index) in judgeResult.testcases" :key="index" class="testcase">
                  <div class="testcase-header">
                    <span>测试用例 #{{ index + 1 }}</span>
                    <el-tag :type="getJudgeStatusType(testcase.status)" size="small">
                      {{ getJudgeStatusText(testcase.status) }}
                    </el-tag>
                  </div>
                  <div v-if="testcase.status !== 'success'" class="testcase-detail">
                    <div class="detail-item">
                      <div class="detail-label">输入:</div>
                      <pre>{{ testcase.input }}</pre>
                    </div>
                    <div class="detail-item">
                      <div class="detail-label">预期输出:</div>
                      <pre>{{ testcase.expectedOutput }}</pre>
                    </div>
                    <div class="detail-item">
                      <div class="detail-label">实际输出:</div>
                      <pre>{{ testcase.actualOutput }}</pre>
                    </div>
                    <div v-if="testcase.error" class="detail-item error">
                      <div class="detail-label">错误信息:</div>
                      <pre>{{ testcase.error }}</pre>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { marked } from 'marked'
import hljs from 'highlight.js'

marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  }
})

export default {
  name: 'ProblemDetail',
  components: {
  },
  head() {
    return {
      link: [
        { rel: 'stylesheet', href: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.34.0/min/vs/editor/editor.main.css' }
      ],
      script: [
        { src: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.34.0/min/vs/loader.js' }
      ]
    }
  },
  data() {
    return {
      // 聊天相关参数
      chatId: null,
      branchId: null,
      problem: {
        problemCode: '',
        title: '',
        description: '',
        difficulty: '',
        acceptanceRate: 0,
        inputDescription: '',
        outputDescription: '',
        timeLimit: 1000,
        memoryLimit: 256,
        tags: [],
        hints: '',
        source: '',
        submitCount: 0,
        acceptCount: 0
      },
      code: '',
      language: 'java',
      hasLoadedTemplate: false,
      testType: 'custom',
      testCase: '',
      selectedExample: 0,
      examples: [],
      isRunning: false,
      isSubmitting: false,
      isGettingAiReview: false,
      isPollingAiReview: false,
      aiReviewResult: null,
      pollingAnimationFrame: null,
      runResult: null,
      judgeResult: null,
      languages: [
        { label: 'Java', value: 'java', icon: 'el-icon-platform-eleme' },
        { label: 'C++', value: 'cpp', icon: 'el-icon-cpu' },
        { label: 'Python', value: 'python', icon: 'el-icon-magic-stick' }
      ],
      editorOptions: {
        automaticLayout: true,
        fontSize: 14,
        lineNumbers: 'on',
        minimap: { enabled: true },
        scrollBeyondLastLine: false,
        roundedSelection: true,
        autoClosingBrackets: 'always',
        autoClosingQuotes: 'always',
        folding: true,
        quickSuggestions: {
          other: true,
          comments: true,
          strings: true
        },
        suggestOnTriggerCharacters: true,
        wordBasedSuggestions: true,
        renderLineHighlight: 'all',
        scrollbar: {
          vertical: 'visible',
          horizontal: 'visible'
        },
        fontFamily: "'Fira Code', 'Source Code Pro', monospace",
        fontLigatures: true
      },
      monacoLoading: true,
      monacoInstance: null
    }
  },
  computed: {
    renderedDescription() {
      return marked(this.problem.description || '')
    },
    renderedInputDescription() {
      return marked(this.problem.inputDescription || '')
    },
    renderedOutputDescription() {
      return marked(this.problem.outputDescription || '')
    },
    renderedHints() {
      return marked(this.problem.hints || '')
    },
    renderedAiReview() {
      return marked(this.aiReviewResult || '')
    }
  },
  async created() {
    await this.fetchProblemDetail()
    // 加载代码模板
    await this.$store.dispatch('code-template/fetchAllTemplates')
  },
  mounted() {
    // 获取URL参数
    const query = this.$route.query
    if (query.chatId) this.chatId = query.chatId
    if (query.branchId) this.branchId = query.branchId
    
    this.loadMonaco()
  },
  beforeDestroy() {
    if (this.monacoInstance) {
      this.monacoInstance.dispose()
    }
  },
  methods: {
    // 返回聊天界面
    async returnToChat() {
      try {
        // 构建基本返回参数
        const params = {
          chatId: this.chatId,
          branchId: this.branchId
        }
        
        // 如果有评测结果或AI评测结果，存入Redis
        if ((this.judgeResult || (this.aiReviewResult && this.aiReviewResult !== '正在分析代码，请稍候...')) && 
            this.chatId && this.branchId) {
          
          // 构建要存储的数据
          const redisData = {}
          
          // 添加评测结果
          if (this.judgeResult) {
            redisData.judgeResult = {
              status: this.judgeResult.status,
              time: this.judgeResult.time,
              memory: this.judgeResult.memory,
              passedTestCases: this.judgeResult.passedTestCases,
              totalTestCases: this.judgeResult.totalTestCases
            }
          }
          
          // 添加AI评测结果
          if (this.aiReviewResult && this.aiReviewResult !== '正在分析代码，请稍候...') {
            redisData.aiReviewResult = this.aiReviewResult
          }
          
          // 调用API将数据存入Redis
          const res = await this.$axios.post('/api/problems/save-results', {
            chatId: this.chatId,
            branchId: this.branchId,
            data: redisData
          })
          console.log('保存结果到Redis成功:', res)
          if (res.code !== 0) {
            console.error('保存结果到Redis失败:', res.message)
          }
        }
        
        // 跳转回聊天界面，只传递必要的参数
        this.$router.push({
          path: '/chats/interviewContainer',
          query: params
        })
      } catch (error) {
        console.error('保存结果到Redis异常:', error)
        // 发生异常时仍然返回聊天界面
        this.$router.push({
          path: '/chats/interviewContainer',
          query: {
            chatId: this.chatId,
            branchId: this.branchId
          }
        })
      }
    },
    /**
     * 加载指定语言的代码模板
     */
    async loadTemplateForLanguage(language) {
      try {
        // 从store获取模板
        const template = this.$store.getters['code-template/getTemplate'](language)
        if (template) {
          this.code = template
          if (this.monacoInstance) {
            this.monacoInstance.setValue(template)
          }
        }
      } catch (error) {
        console.error('加载代码模板失败:', error)
      }
    },
    
    getDifficultyTagType(difficulty) {
      switch(difficulty) {
        case '简单': return 'success'
        case '中等': return 'warning'
        case '困难': return 'danger'
        default: return ''
      }
    },
    getResultTagType(status) {
      const statusMap = {
        'ACCEPTED': 'success',
        'WRONG_ANSWER': 'danger',
        'TIME_LIMIT_EXCEEDED': 'warning',
        'MEMORY_LIMIT_EXCEEDED': 'warning',
        'RUNTIME_ERROR': 'danger',
        'COMPILATION_ERROR': 'info',
        'SYSTEM_ERROR': 'danger',
        'PENDING': 'info',
        'JUDGING': 'warning'
      }
      return statusMap[status] || 'info'
    },
    getResultText(status) {
      const statusMap = {
        'ACCEPTED': '通过',
        'WRONG_ANSWER': '答案错误',
        'TIME_LIMIT_EXCEEDED': '超时',
        'MEMORY_LIMIT_EXCEEDED': '内存超限',
        'RUNTIME_ERROR': '运行时错误',
        'COMPILATION_ERROR': '编译错误',
        'SYSTEM_ERROR': '系统错误',
        'PENDING': '等待评测',
        'JUDGING': '评测中'
      }
      return statusMap[status] || '未知状态'
    },
    getJudgeStatusType(status) {
      return this.getResultTagType(status)
    },
    getJudgeStatusText(status) {
      return this.getResultText(status)
    },
    loadMonaco() {
      const loadScript = (src) => {
        return new Promise((resolve, reject) => {
          const script = document.createElement('script')
          script.src = src
          script.onload = resolve
          script.onerror = reject
          document.head.appendChild(script)
        })
      }

      const initMonaco = () => {
        // 配置 require
        window.require.config({
          paths: { vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.34.0/min/vs' }
        })

        // 加载编辑器
        window.require(['vs/editor/editor.main'], () => {
          this.monacoLoading = false
          this.initEditor()
        })
      }

      // 如果已经加载了 loader.js
      if (window.require) {
        initMonaco()
      } else {
        loadScript('https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.34.0/min/vs/loader.js')
          .then(() => {
            initMonaco()
          })
          .catch((error) => {
            console.error('Failed to load Monaco Editor:', error)
            this.$message.error('代码编辑器加载失败，请刷新页面重试')
          })
      }
    },
    initEditor() {
      try {
        const container = this.$refs.monacoContainer
        if (!container) {
          throw new Error('Editor container not found')
        }

        // 创建编辑器实例
        this.monacoInstance = window.monaco.editor.create(container, {
          value: this.code || '',
          language: this.language || 'java',
          theme: 'vs-dark',
          fontSize: 14,
          lineNumbers: 'on',
          roundedSelection: false,
          scrollBeyondLastLine: false,
          readOnly: false,
          minimap: {
            enabled: true
          },
          automaticLayout: true,
          wordWrap: 'on'
        })

        // 监听内容变化
        this.monacoInstance.onDidChangeModelContent(() => {
          this.code = this.monacoInstance.getValue()
        })

        // 监听语言变化
        this.$watch('language', async (newLang) => {
          if (this.monacoInstance) {
            const model = this.monacoInstance.getModel()
            if (model) {
              window.monaco.editor.setModelLanguage(model, newLang)
              
              // 如果代码为空或者用户确认要替换代码，则加载新语言的模板
              const currentCode = this.monacoInstance.getValue().trim()
              if (!currentCode || confirm('切换语言将替换当前代码，是否继续？')) {
                await this.loadTemplateForLanguage(newLang)
              }
            }
          }
        })

        // 监听代码变化
        this.$watch('code', (newCode) => {
          if (this.monacoInstance && newCode !== this.monacoInstance.getValue()) {
            this.monacoInstance.setValue(newCode || '')
          }
        })

        // 触发一次重新布局
        setTimeout(() => {
          if (this.monacoInstance) {
            this.monacoInstance.layout()
          }
        }, 100)
        
        // 如果还没有加载过模板，则加载当前语言的模板
        if (!this.hasLoadedTemplate) {
          this.loadTemplateForLanguage(this.language)
          this.hasLoadedTemplate = true
        }
      } catch (error) {
        console.error('Failed to initialize Monaco Editor:', error)
        this.$message.error('代码编辑器初始化失败')
        this.monacoLoading = true
      }
    },
    formatCode() {
      if (this.monacoInstance) {
        const action = this.monacoInstance.getAction('editor.action.formatDocument')
        if (action) {
          action.run()
        }
      }
    },
    async fetchProblemDetail() {
      try {
        const id = this.$route.params.id
        if (!id) {
          this.$message.error('题目ID参数缺失')
          return this.$router.push('/oj')
        }

        const res = await this.$axios.get(`/api/problems/${id}`)
        if (res.code === 0) {
          this.problem = res.data
          this.examples = this.parseExamples()
        } else {
          this.$message.error(res.message || '获取题目详情失败')
          this.$router.push('/oj')
        }
      } catch (error) {
        this.$message.error('获取题目详情异常: ' + error.message)
        this.$router.push('/oj')
      }
    },
    parseExamples() {
      const examples = []
      if (this.problem.sampleInput && this.problem.sampleOutput) {
        const inputs = this.problem.sampleInput.split('\n\n')
        const outputs = this.problem.sampleOutput.split('\n\n')
        for (let i = 0; i < Math.min(inputs.length, outputs.length); i++) {
          examples.push({
            input: inputs[i].trim(),
            output: outputs[i].trim()
          })
        }
      }
      return examples
    },
    async runCode() {
      if (!this.code) {
        this.$message.warning('请输入代码')
        return
      }

      const testInput = this.testType === 'custom'
        ? this.testCase
        : this.examples[this.selectedExample]?.input

      if (!testInput) {
        this.$message.warning('请输入测试用例')
        return
      }

      this.isRunning = true
      try {
        const res = await this.$axios.post('/api/problems/run', {
          problemId: this.$route.params.id,
          code: this.code,
          language: this.language,
          input: testInput
        })

        if (res.code === 0) {
          this.$message.success('运行成功')
          this.runResult = {
            status: res.data.status || 'SYSTEM_ERROR',  // 确保有状态值
            time: res.data.time || 0,
            memory: res.data.memory || 0,
            output: res.data.output || ''
          }
        } else {
          this.$message.error(res.message || '运行失败')
          this.runResult = {
            status: 'SYSTEM_ERROR',
            time: 0,
            memory: 0,
            output: res.message || '运行失败'
          }
        }
      } catch (error) {
        this.$message.error('运行异常: ' + error.message)
        this.runResult = {
          status: 'SYSTEM_ERROR',
          time: 0,
          memory: 0,
          output: error.message || '运行异常'
        }
      } finally {
        this.isRunning = false
      }
    },
    
    /**
     * 获取提交结果
     */
    async fetchSubmissionResult(submissionId) {
      try {
        const res = await this.$axios.get(`/api/problems/submissions/${submissionId}`)
        if (res.code === 0) {
          const result = res.data
          console.log('轮询获取的评测结果:', result)
          
          // 如果状态还是running或judging，继续轮询
          if (result.status === 'running' || result.status === 'PENDING' || result.status === 'JUDGING') {
            await new Promise(resolve => setTimeout(resolve, 1000)); // 等待1秒
            this.fetchSubmissionResult(submissionId);
            return;
          }
          
          // 更新评测结果
          this.judgeResult = {
            status: result.status,
            time: result.time || 0,
            memory: result.memory || 0,
            errorMessage: result.errorMessage,
            passedTestCases: result.passedTestCases || 0,
            totalTestCases: result.totalTestCases || 0,
            testcases: result.testcases || [],
            submissionId: submissionId,
            aiReviewRequested: result.aiReviewRequested
          }
          
          // 如果评测完成且后端已请求AI评价，但前端还没有开始轮询，则自动开始轮询AI评价结果
          if (result.aiReviewRequested && !this.isGettingAiReview && !this.aiReviewResult) {
            this.aiReviewResult = '正在分析代码，请稍候...'
            this.isGettingAiReview = true
            this.startPollingAiReview(submissionId.toString())
          }
          
          // 更新题目统计信息
          if (result.status === 'ACCEPTED') {
            this.problem.acceptCount++
            this.problem.acceptanceRate = this.problem.acceptCount / this.problem.submitCount
          }
        }
      } catch (error) {
        console.error('获取评测结果失败:', error)
      }
    },
    
    async submitCode() {
      if (!this.code) {
        this.$message.warning('请输入代码')
        return
      }

      this.isSubmitting = true
      this.judgeResult = null

      try {
        const res = await this.$axios.post('/api/problems/submit', {
          problemId: this.$route.params.id,
          code: this.code,
          language: this.language
        })

        console.log('提交响应数据:', res)

        if (res.code === 0) {
          this.$message.success('提交成功')
          
          // 更新题目提交计数
          this.problem.submitCount++
          
          // 开始轮询获取评测结果
          if (res.data.submissionId) {
            this.fetchSubmissionResult(res.data.submissionId)
            
            // 如果后端已请求AI评价，自动开始轮询AI评价结果
            if (res.data.aiReviewRequested) {
              this.aiReviewResult = '正在分析代码，请稍候...'
              this.isGettingAiReview = true
              this.startPollingAiReview(res.data.submissionId.toString())
            }
          }
          
          // 设置初始评测状态
          this.judgeResult = {
            status: 'JUDGING',
            time: 0,
            memory: 0,
            passedTestCases: 0,
            totalTestCases: res.data.totalTestCases || 0,
            testcases: [],
            submissionId: res.data.submissionId,
            aiReviewRequested: res.data.aiReviewRequested
          }
        } else {
          console.error('提交失败:', res.message)
          this.$message.error(res.message || '提交失败')
        }
      } catch (error) {
        console.error('提交异常:', error)
        this.$message.error('提交异常: ' + error.message)
      } finally {
        this.isSubmitting = false
      }
    },
    resetCode() {
      this.code = ''
      this.runResult = null
      this.aiReviewResult = null
    },
    
    // 获取AI代码评价
    async getAiReview() {
      if (!this.code) {
        this.$message.warning('请先输入代码')
        return
      }
      
      // 如果没有提交过代码，先提交代码
      if (!this.judgeResult || !this.judgeResult.submissionId) {
        this.$message.info('需要先提交代码才能获取AI评价')
        await this.submitCode()
        if (!this.judgeResult || !this.judgeResult.submissionId) {
          return
        }
      }
      
      this.isGettingAiReview = true
      this.aiReviewResult = ''
      
      try {
        // 发送请求获取AI评价
        const res = await this.$axios.post(`/api/problems/submissions/${this.judgeResult.submissionId}/ai-review`)
        
        if (res.code === 0 && res.data) {
          // 开始轮询获取AI评价结果
          this.aiReviewResult = '正在分析代码，请稍候...'
          this.startPollingAiReview(res.data)
        } else {
          this.$message.error(res.message || '获取AI评价失败')
        }
      } catch (error) {
        console.error('获取AI评价异常:', error)
        this.$message.error('获取AI评价异常: ' + error.message)
        this.isGettingAiReview = false
      }
    },
    
    // 开始轮询获取AI评价结果
    startPollingAiReview(messageId) {
      const POLLING_TIMEOUT = 60000 // 60秒超时
      let pollingStartTime = Date.now()
      this.isPollingAiReview = true
      
      const processBatch = async () => {
        if (!this.isPollingAiReview) return
        
        try {
          // 检查是否超时
          if (Date.now() - pollingStartTime > POLLING_TIMEOUT) {
            throw new Error('轮询超时，未收到有效响应')
          }
          
          const params = new URLSearchParams()
          params.append('messageId', messageId)
          params.append('batchSize', '5')
          
          const response = await this.$axios.get('/api/problems/poll-ai-review', {
            params: params
          })
          
          if (response.code === 0 && response.data && response.data.length) {
            let shouldStop = false
            // 重置超时计时器（每次收到有效数据就重置）
            pollingStartTime = Date.now()
            
            // 批量处理消息
            for (const msg of response.data) {
              // 检查是否收到停止信号
              if (msg.finish === 'stop') {
                shouldStop = true
              }
              
              // 使用打字机效果添加文本
              await this.typeText(msg.text)
            }
            
            // 如果收到停止信号，则中止轮询
            if (shouldStop) {
              this.stopPollingAiReview()
              return
            }
          }
          
          // 继续轮询
          this.pollingAnimationFrame = requestAnimationFrame(processBatch)
          
        } catch (error) {
          this.stopPollingAiReview()
          this.$message.error('获取AI评价失败: ' + error.message)
        }
      }
      
      processBatch()
    },
    
    // 停止轮询
    stopPollingAiReview() {
      this.isPollingAiReview = false
      this.isGettingAiReview = false
      if (this.pollingAnimationFrame) {
        cancelAnimationFrame(this.pollingAnimationFrame)
        this.pollingAnimationFrame = null
      }
    },
    
    // 打字机效果
    async typeText(newText) {
      if (!newText) return
      
      const typingSpeed = 30 // 控制打字速度(ms/字)
      
      for (let i = 0; i < newText.length; i++) {
        await new Promise(resolve => setTimeout(resolve, typingSpeed))
        this.aiReviewResult += newText[i]
        this.$forceUpdate()
      }
    }
  }
}
</script>

<style scoped>
.problem-detail {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  position: relative;
}

.return-button-container {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 10;
}

.problem-header {
  margin-bottom: 24px;
}

.title-section {
  padding: 16px 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.problem-code {
  font-family: 'Fira Code', monospace;
  font-size: 20px;
  color: #409EFF;
  font-weight: 500;
}

.title-row h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
  font-weight: 600;
}

.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tag-item {
  margin: 0;
}

.problem-stats {
  margin-left: auto;
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 14px;
}

.main-content {
  margin-bottom: 24px;
}

.description-card,
.ide-card {
  height: 100%;
  margin-bottom: 24px;
}

.section {
  margin-bottom: 32px;
}

.section h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  color: #303133;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #ebeef5;
}

.section h2 i {
  color: #409EFF;
}

.markdown-content {
  line-height: 1.6;
  color: #606266;
}

.example-card {
  background: #f8f9fa;
  margin-bottom: 16px;
}

.example-header {
  font-weight: 500;
  color: #409EFF;
  margin-bottom: 12px;
}

.example-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.example-input,
.example-output {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.example-label {
  font-weight: 500;
  color: #606266;
}

.example-content pre {
  margin: 0;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  line-height: 1.5;
  color: #2c3e50;
}

.source-content {
  color: #606266;
  font-style: italic;
}

.ide-header {
  margin-bottom: 16px;
}

.ide-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.ide-title h2 {
  margin: 0;
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ide-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.language-select {
  width: 120px;
}

.language-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.code-editor-container {
  position: relative;
  height: 400px;
  margin-bottom: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.code-editor {
  width: 100%;
  height: 100%;
}

.editor-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.editor-loading i {
  font-size: 24px;
  color: #409EFF;
  margin-bottom: 8px;
}

.editor-loading span {
  color: #606266;
  font-size: 14px;
}

.test-section {
  margin: 24px 0;
}

.test-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.test-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #303133;
  margin: 0;
}

.custom-test,
.example-test {
  margin-bottom: 16px;
}

.test-input {
  font-family: 'Fira Code', monospace;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.result-container {
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-stats {
  display: flex;
  gap: 16px;
}

.output-card {
  background: #f8f9fa;
}

.result-output {
  margin: 0;
  padding: 16px;
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  line-height: 1.5;
  color: #2c3e50;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.result-output.error {
  color: #f56c6c;
}

.ai-review-container {
  margin-top: 24px;
  margin-bottom: 24px;
}

.ai-review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ai-review-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  color: #E6A23C;
}

.ai-review-header h3 i {
  font-size: 20px;
}

.ai-review-card {
  background: #fffbf2;
  border-left: 4px solid #E6A23C;
}

.ai-review-content {
  margin: 0;
  padding: 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
}

.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;
}

.markdown-body pre {
  background-color: #f6f8fa;
  border-radius: 3px;
  padding: 16px;
  overflow: auto;
  font-family: 'Source Code Pro', monospace;
}

.markdown-body code {
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  font-family: 'Source Code Pro', monospace;
  padding: 0.2em 0.4em;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 16px;
}

.markdown-body table th,
.markdown-body table td {
  padding: 6px 13px;
  border: 1px solid #dfe2e5;
}

.markdown-body table tr {
  background-color: #fff;
  border-top: 1px solid #c6cbd1;
}

.markdown-body table tr:nth-child(2n) {
  background-color: #f6f8fa;
}

.judge-result {
  margin-top: 24px;
}

.result-card {
  background: #fff;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-info {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  color: #606266;
  font-weight: 500;
}

.info-item .value {
  color: #409EFF;
  font-family: 'Fira Code', monospace;
}

.testcase-results {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.testcase {
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  padding: 16px;
}

.testcase-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.testcase-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  background: #F8F9FA;
  padding: 12px;
  border-radius: 4px;
}

.detail-label {
  color: #606266;
  font-weight: 500;
  margin-bottom: 8px;
}

.detail-item pre {
  margin: 0;
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.detail-item.error {
  background: #FEF0F0;
}

.detail-item.error pre {
  color: #F56C6C;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .problem-detail {
    padding: 12px;
  }

  .title-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .problem-stats {
    margin-left: 0;
    margin-top: 12px;
    flex-direction: column;
    gap: 8px;
  }

  .ide-title {
    flex-direction: column;
    gap: 12px;
  }

  .ide-actions {
    width: 100%;
    justify-content: space-between;
  }

  .action-buttons {
    flex-direction: column;
  }

  .result-header {
    flex-direction: column;
    gap: 12px;
  }

  .result-stats {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
