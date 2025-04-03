<template>
  <div class="problem-detail">
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
      testType: 'custom',
      testCase: '',
      selectedExample: 0,
      examples: [],
      isRunning: false,
      isSubmitting: false,
      runResult: null,
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
    }
  },
  async created() {
    await this.fetchProblemDetail()
  },
  mounted() {
    this.loadMonaco()
  },
  beforeDestroy() {
    if (this.monacoInstance) {
      this.monacoInstance.dispose()
    }
  },
  methods: {
    getDifficultyTagType(difficulty) {
      switch(difficulty) {
        case '简单': return 'success'
        case '中等': return 'warning'
        case '困难': return 'danger'
        default: return ''
      }
    },
    getResultTagType(status) {
      switch(status) {
        case 'success': return 'success'
        case 'error': return 'danger'
        case 'timeout': return 'warning'
        default: return 'info'
      }
    },
    getResultText(status) {
      switch(status) {
        case 'success': return '通过'
        case 'error': return '错误'
        case 'timeout': return '超时'
        default: return '未知'
      }
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
        this.$watch('language', (newLang) => {
          if (this.monacoInstance) {
            const model = this.monacoInstance.getModel()
            if (model) {
              window.monaco.editor.setModelLanguage(model, newLang)
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
            status: res.data.status,
            time: res.data.time || 0,
            memory: res.data.memory || 0,
            output: res.data.output || ''
          }
        } else {
          this.$message.error(res.message || '运行失败')
          this.runResult = {
            status: 'error',
            time: 0,
            memory: 0,
            output: res.message || '运行失败'
          }
        }
      } catch (error) {
        this.$message.error('运行异常: ' + error.message)
        this.runResult = {
          status: 'error',
          time: 0,
          memory: 0,
          output: error.message || '运行异常'
        }
      } finally {
        this.isRunning = false
      }
    },
    async submitCode() {
      if (!this.code) {
        this.$message.warning('请输入代码')
        return
      }

      this.isSubmitting = true
      try {
        const res = await this.$axios.post('/api/problems/submit', {
          problemId: this.$route.params.id,
          code: this.code,
          language: this.language
        })

        if (res.code === 0) {
          this.$message.success('提交成功')
          // 更新题目统计信息
          this.problem.submitCount++
          if (res.data.status === 'success') {
            this.problem.acceptCount++
            this.problem.acceptanceRate = this.problem.acceptCount / this.problem.submitCount
          }
        } else {
          this.$message.error(res.message || '提交失败')
        }
      } catch (error) {
        this.$message.error('提交异常: ' + error.message)
      } finally {
        this.isSubmitting = false
      }
    },
    resetCode() {
      this.code = ''
      this.runResult = null
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
