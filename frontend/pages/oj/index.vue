<template>
  <div class="problem-list">
    <el-table :data="problemList" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="title" label="标题"></el-table-column>
      <el-table-column prop="difficulty" label="难度" width="120">
        <template #default="scope">
          <el-tag :type="getDifficultyTagType(scope.row.difficulty)">
            {{ scope.row.difficulty }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="acceptance" label="通过率" width="120"></el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="handleView(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
export default {
  data() {
    return {
      problemList: []
    }
  },
  async created() {
    try {
      console.log('开始获取题目列表数据...')
      const res = await this.$axios.get('/api/problem/list')
      console.log('题目列表API响应:', res)
      console.log(res.data)
      if (res.code === 0) {
        // console.log(1)
        this.problemList = res.data.list
        console.log('成功获取题目列表:', this.problemList)
      } else {
        console.error('获取题目列表失败:', res.message || '未知错误')
        this.$message.error('获取题目列表失败: ' + (res.message || '未知错误'))
      }
    } catch (error) {
      console.error('获取题目列表异常:', error)
      this.$message.error('获取题目列表异常: ' + error.message)
    }
  },
  methods: {
    handleView(row) {
      console.log('查看题目:', row.id)
      this.$router.push({
        path: `/oj/problem/${row.id}`
      })
      console.log('路由跳转完成', row.id)
    },
    getDifficultyTagType(difficulty) {
      switch(difficulty) {
        case '简单': return 'success'
        case '中等': return 'warning'
        case '困难': return 'danger'
        default: return ''
      }
    }
  }
}
</script>

<style scoped>
.problem-list {
  padding: 20px;
}
</style>