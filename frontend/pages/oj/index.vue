<template>
  <div class="problem-list">
    <el-card class="filter-card">
      <div class="filter-section">
        <el-input
          v-model="searchQuery"
          placeholder="搜索题目..."
          prefix-icon="el-icon-search"
          clearable
          class="search-input"
        />
        <el-select v-model="selectedDifficulty" placeholder="难度" clearable class="filter-select">
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
        <el-select
          v-model="selectedTags"
          multiple
          collapse-tags
          placeholder="标签"
          clearable
          class="filter-select"
        >
          <el-option
            v-for="tag in availableTags"
            :key="tag"
            :label="tag"
            :value="tag"
          />
        </el-select>
      </div>

      <div class="statistics-section">
        <el-row :gutter="20">
          <el-col :span="8" v-for="(stat, type) in statistics" :key="type">
            <el-card shadow="hover" :class="['stat-card', `stat-${type}`]">
              <div class="stat-content">
                <div class="stat-icon">
                  <i :class="getStatIcon(type)"></i>
                </div>
                <div class="stat-info">
                  <div class="stat-title">{{ stat.title }}</div>
                  <div class="stat-value">{{ stat.value }}</div>
                  <el-progress 
                    :percentage="stat.rate" 
                    :color="getStatColor(type)" 
                    :show-text="false"
                    :stroke-width="4"
                    class="stat-progress"
                  ></el-progress>
                  <div class="stat-rate">{{ getRateLabel(type) }}: {{ stat.rate }}%</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-table
      :data="filteredProblems"
      style="width: 100%"
      :header-cell-style="{ background: '#f5f7fa' }"
      @row-click="handleProblemClick"
    >
      <el-table-column prop="problemCode" label="题号" width="100" align="center">
        <template #default="scope">
          <span class="problem-code">{{ scope.row.problemCode }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="title" label="标题">
        <template #default="scope">
          <div class="problem-title-cell">
            <span class="problem-title">{{ scope.row.title }}</span>
            <div class="problem-tags">
              <el-tag
                v-for="tag in scope.row.tags"
                :key="tag"
                size="small"
                effect="plain"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="difficulty" label="难度" width="100" align="center">
        <template #default="scope">
          <el-tag :type="getDifficultyTagType(scope.row.difficulty)" size="small">
            {{ scope.row.difficulty }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="通过率" width="200" align="center">
        <template #default="scope">
          <div class="acceptance-cell">
            <el-progress
              :percentage="Number((scope.row.acceptanceRate * 100).toFixed(1))"
              :color="getAcceptanceColor(scope.row.acceptanceRate)"
            />
            <span class="acceptance-text">
              {{ scope.row.acceptCount }}/{{ scope.row.submitCount }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120" align="center">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            @click.stop="handleView(scope.row)"
            icon="el-icon-view"
          >
            开始做题
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        background
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      searchQuery: '',
      selectedDifficulty: '',
      selectedTags: [],
      currentPage: 1,
      pageSize: 20,
      total: 0,
      problems: [], // 当前页面显示的题目
      loading: false, // 加载状态
      difficulties: [
        { label: '简单', value: '简单' },
        { label: '中等', value: '中等' },
        { label: '困难', value: '困难' }
      ],
      availableTags: [],
      statistics: {
        total: { title: '总题数', value: 0, rate: 0 },
        solved: { title: '已解决', value: 0, rate: 0 },
        attempted: { title: '尝试过', value: 0, rate: 0 }
      }
    }
  },
  computed: {
    // 根据标签筛选题目
    filteredProblems() {
      if (this.selectedTags.length === 0) {
        return this.problems;
      }
      
      return this.problems.filter(problem => {
        return this.selectedTags.every(tag => problem.tags.includes(tag));
      });
    }
  },
  watch: {
    // 监听筛选条件变化，重新加载数据
    selectedDifficulty() {
      this.currentPage = 1;
      this.fetchProblems();
    },
    searchQuery: {
      handler: function(val) {
        if (val === '') {
          // 搜索条件为空时重新加载数据
          this.fetchProblems();
        }
      }
    }
  },
  async created() {
    await this.fetchProblems();
    await this.fetchTags();
    await this.fetchStatistics();
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
    getAcceptanceColor(rate) {
      rate = rate * 100;
      if (rate >= 70) return '#67C23A';
      if (rate >= 40) return '#E6A23C';
      return '#F56C6C';
    },
    getStatIcon(type) {
      switch(type) {
        case 'total': return 'el-icon-s-grid';
        case 'solved': return 'el-icon-check';
        case 'attempted': return 'el-icon-s-promotion';
        default: return 'el-icon-data-line';
      }
    },
    getStatColor(type) {
      switch(type) {
        case 'total': return '#409EFF';
        case 'solved': return '#67C23A';
        case 'attempted': return '#E6A23C';
        default: return '#909399';
      }
    },
    getRateLabel(type) {
      switch(type) {
        case 'total': return '通过率';
        case 'solved': return '完成率';
        case 'attempted': return '尝试率';
        default: return '比率';
      }
    },
    async fetchProblems() {
      this.loading = true;
      try {
        // 构建API请求参数
        const params = {
          page: this.currentPage,
          rows: this.pageSize
        };
        
        if (this.selectedDifficulty) {
          params.difficulty = this.selectedDifficulty;
        }
        
        if (this.searchQuery) {
          // 如果有搜索关键词，使用全量API进行前端筛选
          const res = await this.$axios.get('/api/problems/all', { params: { difficulty: this.selectedDifficulty } });
          if (res.code === 0) {
            const allProblems = res.data || [];
            // 前端筛选搜索结果
            const filteredProblems = allProblems.filter(problem => {
              return problem.title.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
                problem.problemCode.toLowerCase().includes(this.searchQuery.toLowerCase());
            });
            this.total = filteredProblems.length;
            
            // 前端分页
            const start = (this.currentPage - 1) * this.pageSize;
            const end = start + this.pageSize;
            this.problems = filteredProblems.slice(start, end);
          }
        } else {
          // 使用分页API获取数据
          const res = await this.$axios.get('/api/problems', { params });
          if (res.code === 0) {
            this.problems = res.data.list || [];
            this.total = res.data.total;
          }
        }
      } catch (error) {
        console.error('获取题目列表失败:', error);
        this.$message.error('获取题目列表失败');
      } finally {
        this.loading = false;
      }
    },
    async fetchTags() {
      try {
        const res = await this.$axios.get('/api/problems/tags');
        if (res.code === 0) {
          this.availableTags = res.data;
        }
      } catch (error) {
        console.error('获取标签列表失败:', error);
        this.$message.error('获取标签列表失败');
      }
    },
    async fetchStatistics() {
      try {
        const res = await this.$axios.get('/api/problems/statistics');
        console.log('统计信息响应:', res);
        if (res && res.data) {
          this.statistics = res.data;
        }
      } catch (error) {
        console.error('获取统计信息失败:', error);
        this.$message.error('获取统计信息失败');
      }
    },
    getRateLabel(type) {
      switch(type) {
        case 'solved':
          return '通过率';
        case 'attempted':
          return '尝试率';
        default:
          return '完成率';
      }
    },
    handleView(row) {
      this.$router.push(`/oj/problem/${row.id}`);
    },
    handleProblemClick(row) {
      this.handleView(row);
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1; // 重置为第一页
      this.fetchProblems(); // 重新请求数据
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.fetchProblems(); // 重新请求数据
    }
  }
}
</script>

<style scoped>
.problem-list {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.filter-card {
  margin-bottom: 24px;
}

.filter-section {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.search-input {
  width: 300px;
}

.filter-select {
  width: 200px;
}

.statistics-section {
  margin-top: 24px;
}

.stat-card {
  transition: transform 0.3s, box-shadow 0.3s;
  border-radius: 8px;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.stat-content {
  padding: 16px;
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
}

.stat-total .stat-icon {
  background-color: rgba(64, 158, 255, 0.1);
  color: #409EFF;
}

.stat-solved .stat-icon {
  background-color: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.stat-attempted .stat-icon {
  background-color: rgba(230, 162, 60, 0.1);
  color: #E6A23C;
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-total .stat-value {
  color: #409EFF;
}

.stat-solved .stat-value {
  color: #67C23A;
}

.stat-attempted .stat-value {
  color: #E6A23C;
}

.stat-progress {
  margin-bottom: 4px;
}

.stat-rate {
  font-size: 12px;
  color: #909399;
}

.problem-code {
  font-family: 'Fira Code', monospace;
  color: #409EFF;
  font-weight: 500;
}

.problem-title-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.problem-title {
  color: #303133;
  font-weight: 500;
}

.problem-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-item {
  margin: 0;
}

.acceptance-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.acceptance-text {
  font-size: 12px;
  color: #909399;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media screen and (max-width: 768px) {
  .problem-list {
    padding: 12px;
  }

  .filter-section {
    flex-direction: column;
  }

  .search-input,
  .filter-select {
    width: 100%;
  }
}
</style>
