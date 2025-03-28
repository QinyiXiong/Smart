<template>
  <div class="problem-detail">
    <h1>{{ problem.title }}</h1>
    <div class="meta">
      <el-tag :type="getDifficultyTagType(problem.difficulty)">
        {{ problem.difficulty }}
      </el-tag>
      <span>通过率: {{ problem.acceptance }}</span>
    </div>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="content">
          <h2>题目描述</h2>
          <div v-html="problem.description"></div>
          
          <h2>输入描述</h2>
          <div v-html="problem.input_description"></div>
          
          <h2>输出描述</h2>
          <div v-html="problem.output_description"></div>
          
          <h2>示例</h2>
          <div v-if="problem.testCases">
            <h4>测试用例</h4>
            <pre>{{ problem.testCases }}</pre>
          </div>
        </div>
      </el-col>
      
      <el-col :span="12">
        <div class="ide-container">
          <h2>在线IDE</h2>
          <el-select v-model="language" placeholder="选择编程语言" style="margin-bottom: 15px">
            <el-option label="Java" value="java"></el-option>
            <el-option label="C++" value="cpp"></el-option>
            <el-option label="Python" value="python"></el-option>
          </el-select>
          <monaco-editor
            v-model="code"
            :language="language"
            :options="editorOptions"
            height="500"
            theme="vs"
          ></monaco-editor>
          <div class="test-case">
            <h3>测试用例</h3>
            <el-input
              type="textarea"
              :rows="5"
              placeholder="输入测试用例"
              v-model="testCase"
            ></el-input>
          </div>
          <div class="action-buttons">
            <el-button type="primary" @click="runCode">运行代码</el-button>
            <el-button @click="resetCode">重置</el-button>
          </div>
          <div class="result-container" v-if="runResult">
            <div class="result-meta">
              <span :class="runResult.status">状态: {{ runResult.status === 'success' ? '通过' : '错误' }}</span>
              <span>执行时间: {{ runResult.time }}ms</span>
              <span>内存消耗: {{ runResult.memory }}KB</span>
            </div>
            <div class="result-output">
              <pre>{{ runResult.output }}</pre>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import MonacoEditor from 'vue-monaco-editor';

export default {
  components: {
    MonacoEditor,
  },
  data() {
    return {
      problem: {
        title: '',
        description: '',
        difficulty: '',
        acceptance: '',
        input_description: '',
        output_description: '',
        testCases: ''
      },
      code: '',
      language: 'java',
      testCase: '',
      runResult: null,
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
        suggestOnTriggerCharacters: true,
        wordBasedSuggestions: true
      }
    }
  },
  async created() {
    try {
      console.log('获取题目ID:', this.$route.params.id)
      const id = this.$route.params.id
      if (!id) {
        this.$message.error('题目ID参数缺失')
        return this.$router.push('/oj')
      }
      
      const res = await this.$axios.get(`/api/problem/${id}`)
      if (res.code === 0) {
        this.problem = res.data
        // 处理示例数据
        if (!this.problem.samples) {
          this.problem.samples = []
        }
      } else {
        this.$message.error('获取题目详情失败: ' + (res.message || '未知错误'))
        this.$router.push('/oj')
      }
    } catch (error) {
      this.$message.error('获取题目详情异常: ' + error.message)
      this.$router.push('/oj')
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
    async runCode() {
      if (!this.code) {
        this.$message.warning('请输入代码');
        return;
      }
      
      try {
        const res = await this.$axios.post('/api/v1/problem/run', {
          code: this.code,
          language: this.language,
          testCase: this.testCase
        });
        
        if (res.code === 0) {
          this.$message.success('运行成功');
          this.runResult = {
            time: res.data.time || 0,
            memory: res.data.memory || 0,
            status: res.data.status || 'success',
            output: res.data.output || ''
          };
          console.log('运行结果:', this.runResult);
        } else {
          this.$message.error(res.message || '运行失败');
          this.runResult = {
            time: 0,
            memory: 0,
            status: 'error',
            output: res.message || '运行失败'
          };
        }
      } catch (error) {
        this.$message.error('运行异常: ' + error.message);
        this.runResult = {
          time: 0,
          memory: 0,
          status: 'error',
          output: error.message || '运行异常'
        };
      }
    },
    resetCode() {
      this.code = ''
    }
  }
}
</script>

<style scoped>
.problem-detail {
  padding: 30px;
  max-width: 1200px;
  margin: 0 auto;
}
.meta {
  margin: 20px 0;
  display: flex;
  align-items: center;
  gap: 20px;
}
.content {
  margin-top: 30px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
.content h2 {
  color: #303133;
  margin: 20px 0 15px;
  font-weight: 500;
}
.ide-container {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 25px;
  background: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
.ide-container h2 {
  color: #303133;
  margin-bottom: 20px;
  font-weight: 500;
}
.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 15px;
}
.test-case {
  margin-top: 20px;
}
.test-case h3 {
  color: #606266;
  margin-bottom: 10px;
  font-size: 16px;
}
.result-container {
  margin-top: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 4px;
}
.result-meta {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
  color: #666;
}
.result-meta .success {
  color: #67c23a;
}
.result-meta .error {
  color: #f56c6c;
}
.result-output {
  background: #fff;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.result-output pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

@media (max-width: 768px) {
  .el-row {
    flex-direction: column;
  }
  .el-col {
    width: 100%;
  }
}
</style>