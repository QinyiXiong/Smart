<template>
  <div class="problem-list">
    <!-- 上部分：统计数据 -->
    <div class="statistics-dashboard">
      <!-- 上左侧：难度统计 -->
      <el-card class="stats-card difficulty-stats">
        <div slot="header" class="card-header">
          <span>题目难度统计</span>
        </div>
        <div class="difficulty-items">
          <div v-for="(data, difficulty) in statistics.byDifficulty" :key="difficulty" class="difficulty-item">
            <div class="difficulty-label">
              <el-tag :type="getDifficultyTagType(difficulty)" size="medium">{{ difficulty }}</el-tag>
            </div>
            <div class="difficulty-progress">
              <el-progress 
                :percentage="data.rate"
                :stroke-width="15"
                :color="getDifficultyColor(difficulty)"
              ></el-progress>
              <div class="difficulty-numbers">
                已完成 {{ data.solved }} 题 / 题库总量 {{ data.total }} 题
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 上右侧：扇形图 -->
      <el-card class="stats-card pie-chart">
        <div slot="header" class="card-header">
          <span>完成情况统计</span>
        </div>
        <div class="chart-container" ref="pieChart"></div>
      </el-card>
    </div>
    
    <!-- 下部分：提交热力图 -->
    <el-card class="heatmap-card">
      <div slot="header" class="card-header">
        <span>过去一年总提交数</span>
      </div>
      <div class="heatmap-container" ref="heatmap"></div>
    </el-card>

    <!-- 筛选区 -->
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

      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="scope">
          <i v-if="scope.row.status === 'solved'" class="el-icon-check" style="color: #67C23A; font-size: 18px;"></i>
          <i v-else-if="scope.row.status === 'attempted'" class="el-icon-time" style="color: #E6A23C; font-size: 18px;"></i>
          <i v-else class="el-icon-minus" style="color: #909399; font-size: 18px;"></i>
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
import * as echarts from 'echarts';

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
        attempted: { title: '尝试过', value: 0, rate: 0 },
        byDifficulty: {
          '简单': { solved: 0, total: 0, rate: 0 },
          '中等': { solved: 0, total: 0, rate: 0 },
          '困难': { solved: 0, total: 0, rate: 0 }
        },
        heatmapData: {},
        monthLabels: []
      },
      pieChart: null,
      heatmap: null
    }
  },
  computed: {
    // 根据标签筛选题目
    filteredProblems() {
      let problems = this.problems;
      if (this.selectedDifficulty) {
        problems = problems.filter(problem => problem.difficulty === this.selectedDifficulty);
      }
      if (this.selectedTags.length > 0) {
        problems = problems.filter(problem =>
          this.selectedTags.every(tag => problem.tags.includes(tag))
        );
      }
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase();
        problems = problems.filter(problem =>
          problem.title.toLowerCase().includes(query) ||
          problem.problemCode.toLowerCase().includes(query)
        );
      }
      return problems;
    },
    sortedByDifficulty() {
      const order = ['简单', '中等', '困难'];
      const sorted = {};
      order.forEach(key => {
        if (this.statistics.byDifficulty[key]) {
          sorted[key] = this.statistics.byDifficulty[key];
        }
      });
      return sorted;
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
  mounted() {
    // 组件挂载后初始化图表，但需要等待统计数据加载完成
    this.$nextTick(() => {
      this.initPieChart();
      this.initHeatmap();
    });
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
    getDifficultyColor(difficulty) {
      switch(difficulty) {
        case '简单': return '#67C23A';
        case '中等': return '#E6A23C';
        case '困难': return '#F56C6C';
        default: return '#909399';
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
            
            // 为每个问题添加状态标识
            this.addProblemStatus(this.problems);
          }
        } else {
          // 使用分页API获取数据
          const res = await this.$axios.get('/api/problems', { params });
          if (res.code === 0) {
            this.problems = res.data.list || [];
            this.total = res.data.total;
            
            // 为每个问题添加状态标识
            this.addProblemStatus(this.problems);
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
          // 确保 byDifficulty 按照预设顺序更新
          const order = ['简单', '中等', '困难'];
          const newByDifficulty = {};
          order.forEach(key => {
            newByDifficulty[key] = res.data.byDifficulty[key] || { solved: 0, total: 0, rate: 0 };
          });
          this.statistics = { ...res.data, byDifficulty: newByDifficulty };
          this.addProblemStatus(this.problems); // 确保在统计数据加载后更新问题状态
          
          // 更新后重新初始化图表
          this.$nextTick(() => {
            this.initPieChart();
            this.initHeatmap();
          });
        }
      } catch (error) {
        console.error('获取统计信息失败:', error);
        this.$message.error('获取统计信息失败');
      }
    },
    initPieChart() {
      if (!this.$refs.pieChart) return;
      
      if (this.pieChart) {
        this.pieChart.dispose();
      }
      
      this.pieChart = echarts.init(this.$refs.pieChart);
      
      // 转换数据为饼图所需格式
      const pieData = [
        { 
          name: '简单',
          value: this.statistics.byDifficulty['简单']?.solved || 0,
          itemStyle: { color: '#67C23A' }
        },
        { 
          name: '中等',
          value: this.statistics.byDifficulty['中等']?.solved || 0,
          itemStyle: { color: '#E6A23C' }
        },
        { 
          name: '困难',
          value: this.statistics.byDifficulty['困难']?.solved || 0,
          itemStyle: { color: '#F56C6C' }
        }
      ];
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          data: ['简单', '中等', '困难']
        },
        series: [
          {
            name: '完成题目',
            type: 'pie',
            radius: ['50%', '70%'],
            avoidLabelOverlap: false,
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '16',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: pieData
          }
        ]
      };
      
      this.pieChart.setOption(option);
      
      // 窗口大小变化时重新渲染图表
      window.addEventListener('resize', () => {
        this.pieChart.resize();
      });
    },
    initHeatmap() {
      if (!this.$refs.heatmap) return;
      
      if (this.heatmapChart) {
        this.heatmapChart.dispose();
      }
      
      this.heatmapChart = echarts.init(this.$refs.heatmap);
      
      // 获取热力图数据和月份标签
      const heatmapData = this.statistics.heatmapData || {};
      const monthLabels = this.statistics.monthLabels || [];
      
      // 计算过去一年的总提交数
      const totalSubmissions = Object.values(heatmapData).reduce((sum, count) => sum + count, 0);
      
      // 生成日历数据
      const calendarData = [];
      for (const [date, count] of Object.entries(heatmapData)) {
        calendarData.push([date, count]);
      }
      
      // 获取当前日期和12个月前的日期
      const endDate = new Date();
      const startDate = new Date();
      startDate.setMonth(startDate.getMonth() - 12);
      
      // 格式化日期
      const formatDate = (date) => {
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        return `${year}-${month}-${day}`;
      };
      
      const option = {
        title: {
          text: `过去一年总提交数: ${totalSubmissions}`,
          left: 'center',
          top: 0
        },
        tooltip: {
          formatter: function (params) {
            return `${params.value[0]}: ${params.value[1]} 次提交`;
          }
        },
        visualMap: {
          min: 0,
          max: Math.max(...Object.values(heatmapData), 1),
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: 20,
          inRange: {
            color: ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127']
          }
        },
        calendar: {
          top: 50,
          left: 30,
          right: 30,
          cellSize: ['auto', 15],
          range: [formatDate(startDate), formatDate(endDate)],
          itemStyle: {
            borderWidth: 0.5
          },
          yearLabel: { show: false },
          monthLabel: {
            nameMap: 'cn',
            formatter: function (params) {
              console.log(params);
              return params.M + '月';
            }
          },
          dayLabel: {
            firstDay: 1, // 从周一开始
            nameMap: 'cn'
          }
        },
        series: {
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data: calendarData
        }
      };
      
      this.heatmapChart.setOption(option);
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
    },
    // 为每个问题添加状态标识
    addProblemStatus(problems) {
      if (!problems || !this.statistics) return;
      
      // 获取已解决和尝试过的题目ID集合
      const solvedProblemIds = new Set();
      const attemptedProblemIds = new Set();
      
      // 从统计数据中提取已解决和尝试过的题目ID
      if (this.statistics.solvedProblems) {
        this.statistics.solvedProblems.forEach(id => solvedProblemIds.add(id));
      }
      
      if (this.statistics.attemptedProblems) {
        this.statistics.attemptedProblems.forEach(id => {
          // 只添加未解决但尝试过的题目
          if (!solvedProblemIds.has(id)) {
            attemptedProblemIds.add(id);
          }
        });
      }
      
      // 为每个问题设置状态
      problems.forEach(problem => {
        let newStatus = 'untried';
        if (solvedProblemIds.has(problem.id)) {
          newStatus = 'solved';
        } else if (attemptedProblemIds.has(problem.id)) {
          newStatus = 'attempted';
        }
        // 使用 this.$set 确保响应式更新
        this.$set(problem, 'status', newStatus);
      });
    },
  }
}
</script>

<style scoped>
.problem-list {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 统计面板样式 */
.statistics-dashboard {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.stats-card {
  flex: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

/* 难度统计样式 */
.difficulty-stats {
  flex: 3;
}

.difficulty-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.difficulty-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.difficulty-label {
  width: 60px;
  text-align: center;
}

.difficulty-progress {
  flex: 1;
}

.difficulty-numbers {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
}

/* 饼图样式 */
.pie-chart {
  flex: 2;
}

.chart-container {
  height: 220px;
}

/* 热力图样式 */
.heatmap-card {
  margin-bottom: 24px;
}

.heatmap-container {
  height: 250px;
  margin-top: 10px;
}

/* 筛选区域样式 */
.filter-card {
  margin-bottom: 24px;
}

.filter-section {
  display: flex;
  gap: 16px;
}

.search-input {
  width: 300px;
}

.filter-select {
  width: 200px;
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

  .statistics-dashboard {
    flex-direction: column;
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
