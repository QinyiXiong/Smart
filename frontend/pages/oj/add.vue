<template>
  <div class="problem-add">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <h2>添加题目</h2>
        </div>
      </template>

      <el-form
        ref="problemForm"
        :model="problemForm"
        :rules="rules"
        label-width="120px"
        class="problem-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="题目编号" prop="problemCode">
              <el-input v-model="problemForm.problemCode" placeholder="例如：P1001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题目标题" prop="title">
              <el-input v-model="problemForm.title" placeholder="请输入题目标题" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="problemForm.difficulty" placeholder="请选择难度">
                <el-option
                  v-for="item in difficulties"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                >
                  <el-tag :type="getDifficultyTagType(item.value)" size="small">
                    {{ item.label }}
                  </el-tag>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时间限制" prop="timeLimit">
              <el-input-number
                v-model="problemForm.timeLimit"
                :min="100"
                :max="10000"
                :step="100"
                controls-position="right"
              >
                <template #suffix>ms</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="内存限制" prop="memoryLimit">
              <el-input-number
                v-model="problemForm.memoryLimit"
                :min="16"
                :max="1024"
                :step="16"
                controls-position="right"
              >
                <template #suffix>MB</template>
              </el-input-number>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题目标签" prop="tags">
          <el-select
            v-model="problemForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入标签"
          >
            <el-option
              v-for="tag in availableTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="题目描述" prop="description">
          <v-md-editor
            v-model="problemForm.description"
            height="300px"
            @change="handleDescriptionChange"
          />
        </el-form-item>

        <el-form-item label="输入描述" prop="inputDescription">
          <v-md-editor
            v-model="problemForm.inputDescription"
            height="200px"
          />
        </el-form-item>

        <el-form-item label="输出描述" prop="outputDescription">
          <v-md-editor
            v-model="problemForm.outputDescription"
            height="200px"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="示例输入" prop="sampleInput">
              <el-input
                v-model="problemForm.sampleInput"
                type="textarea"
                :rows="4"
                placeholder="请输入示例输入"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="示例输出" prop="sampleOutput">
              <el-input
                v-model="problemForm.sampleOutput"
                type="textarea"
                :rows="4"
                placeholder="请输入示例输出"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="测试用例" prop="testCases">
          <el-table :data="testCases" border style="width: 100%">
            <el-table-column label="输入" prop="input">
              <template #default="scope">
                <el-input
                  v-model="scope.row.input"
                  type="textarea"
                  :rows="2"
                />
              </template>
            </el-table-column>
            <el-table-column label="输出" prop="output">
              <template #default="scope">
                <el-input
                  v-model="scope.row.output"
                  type="textarea"
                  :rows="2"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button
                  type="danger"
                  size="small"
                  icon="el-icon-delete"
                  @click="removeTestCase(scope.$index)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="add-test-case">
            <el-button type="primary" @click="addTestCase">
              添加测试用例
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="提示" prop="hints">
          <v-md-editor
            v-model="problemForm.hints"
            height="200px"
          />
        </el-form-item>

        <el-form-item label="来源" prop="source">
          <el-input v-model="problemForm.source" placeholder="请输入题目来源" />
        </el-form-item>

        <el-form-item label="是否可见" prop="visible">
          <el-switch
            v-model="problemForm.visible"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm">提交</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
export default {
  data() {
    return {
      problemForm: {
        problemCode: '',
        title: '',
        description: '',
        inputDescription: '',
        outputDescription: '',
        difficulty: '',
        timeLimit: 1000,
        memoryLimit: 256,
        tags: [],
        sampleInput: '',
        sampleOutput: '',
        hints: '',
        source: '',
        visible: 1
      },
      testCases: [],
      difficulties: [
        { label: '简单', value: '简单' },
        { label: '中等', value: '中等' },
        { label: '困难', value: '困难' }
      ],
      availableTags: [],
      rules: {
        problemCode: [
          { required: true, message: '请输入题目编号', trigger: 'blur' },
          { pattern: /^P\d{4}$/, message: '题目编号格式为P+4位数字', trigger: 'blur' }
        ],
        title: [
          { required: true, message: '请输入题目标题', trigger: 'blur' },
          { min: 2, max: 100, message: '标题长度在2-100个字符之间', trigger: 'blur' }
        ],
        difficulty: [
          { required: true, message: '请选择题目难度', trigger: 'change' }
        ],
        description: [
          { required: true, message: '请输入题目描述', trigger: 'blur' }
        ],
        inputDescription: [
          { required: true, message: '请输入输入描述', trigger: 'blur' }
        ],
        outputDescription: [
          { required: true, message: '请输入输出描述', trigger: 'blur' }
        ],
        timeLimit: [
          { required: true, message: '请设置时间限制', trigger: 'blur' },
          { type: 'number', message: '时间限制必须为数字', trigger: 'blur' }
        ],
        memoryLimit: [
          { required: true, message: '请设置内存限制', trigger: 'blur' },
          { type: 'number', message: '内存限制必须为数字', trigger: 'blur' }
        ],
        tags: [
          { required: true, message: '请至少选择一个标签', trigger: 'change' },
          { type: 'array', min: 1, message: '请至少选择一个标签', trigger: 'change' }
        ]
      }
    }
  },
  async created() {
    await this.fetchTags();
  },
  methods: {
    getDifficultyTagType(difficulty) {
      switch(difficulty) {
        case '简单': return 'success';
        case '中等': return 'warning';
        case '困难': return 'danger';
        default: return '';
      }
    },
    async fetchTags() {
      try {
        const res = await this.$axios.get('/api/problems/tags');
        if (res.code === 0) {
          this.availableTags = res.data;
        }
      } catch (error) {
        this.$message.error('获取标签列表失败');
      }
    },
    handleDescriptionChange(text, html) {
      this.problemForm.description = text;
    },
    addTestCase() {
      this.testCases.push({
        input: '',
        output: ''
      });
    },
    removeTestCase(index) {
      this.testCases.splice(index, 1);
    },
    async submitForm() {
      try {
        await this.$refs.problemForm.validate();

        // 处理测试用例中的换行符
        const processedTestCases = this.testCases.map(testCase => ({
          ...testCase,
          input: testCase.input.replace(/\n/g, '\\n'),
          output: testCase.output.replace(/\n/g, '\\n')
        }));

        const formData = {
          ...this.problemForm,
          testCases: JSON.stringify(processedTestCases)
        };

        const res = await this.$axios.post('/api/problems', formData);

        if (res.code === 0) {
          this.$message.success('添加题目成功');
          this.$router.push('/oj');
        } else {
          this.$message.error(res.message || '添加题目失败');
        }
      } catch (error) {
        if (error.message) {
          this.$message.error(error.message);
        }
      }
    },
    resetForm() {
      this.$refs.problemForm.resetFields();
      this.testCases = [];
    }
  }
}
</script>

<style scoped>
.problem-add {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.form-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.problem-form {
  margin-top: 24px;
}

.add-test-case {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

:deep(.v-md-editor) {
  border-radius: 4px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

@media screen and (max-width: 768px) {
  .problem-add {
    padding: 12px;
  }

  .el-form-item {
    margin-bottom: 22px;
  }
}
</style>
