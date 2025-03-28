<template>
  <div class="problem-add">
    <el-form ref="form" :model="form" label-width="80px">
      <el-form-item label="题目标题">
        <el-input v-model="form.title"></el-input>
      </el-form-item>
      <el-form-item label="难度">
        <el-select v-model="form.difficulty" placeholder="请选择难度">
          <el-option label="简单" value="简单"></el-option>
          <el-option label="中等" value="中等"></el-option>
          <el-option label="困难" value="困难"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-input v-model="form.category"></el-input>
      </el-form-item>
      <el-form-item label="测试用例">
        <el-input type="textarea" v-model="form.testCases"></el-input>
      </el-form-item>
      <el-form-item label="题目描述">
        <el-input type="textarea" v-model="form.description" :rows="5"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSubmit">提交</el-button>
        <el-button @click="onCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  data() {
    return {
      form: {
        title: '',
        difficulty: '',
        category: '',
        testCases: '',
        description: ''
      }
    }
  },
  methods: {
    onSubmit() {
      this.$axios.post('/api/problem/post', this.form)
        .then(res => {
          if (res.code === 0) {
            this.$message.success('添加成功')
            this.$router.push('/oj')
          } else {
            this.$message.error(res.message || '添加失败')
          }
        })
        .catch(error => {
          this.$message.error(error.message)
        })
    },
    onCancel() {
      this.$router.push('/oj')
    }
  }
}
</script>

<style scoped>
.problem-add {
  padding: 20px;
}
</style>