import { createNamespacedHelpers } from 'vuex';

const { mapActions, mapState } = createNamespacedHelpers('problem');

const getDefaultListData = () => ({
  data: [],
  pagination: {},
  statistics: {
    total: 0,
    byDifficulty: {}
  }
});

export const state = () => ({
  list: {
    fetching: false,
    data: getDefaultListData()
  },
  currentProblem: {
    fetching: false,
    data: null
  },
  filters: {
    difficulties: ['简单', '中等', '困难'],
    tags: []
  }
});

export const mutations = {
  // 问题列表
  updateListFetching(state, action) {
    state.list.fetching = action;
  },
  updateListData(state, action) {
    state.list.data = action;
  },
  // 当前问题
  updateCurrentProblemFetching(state, action) {
    state.currentProblem.fetching = action;
  },
  updateCurrentProblemData(state, action) {
    state.currentProblem.data = action;
  },
  // 更新统计信息
  updateStatistics(state, action) {
    state.list.data.statistics = action;
  },
  // 更新标签列表
  updateTags(state, action) {
    state.filters.tags = action;
  }
};

export const actions = {
  // 获取问题列表
  async fetchList({ commit }, params = {}) {
    commit('updateListFetching', true);
    commit('updateListData', getDefaultListData());
    
    try {
      const { difficulty, tags, page = 1, pageSize = 10 } = params;
      const response = await this.$axios.get('/api/v1/problems', {
        params: { difficulty, tags, page, pageSize }
      });
      
      commit('updateListData', {
        data: response.data.list,
        pagination: {
          current: page,
          pageSize,
          total: response.data.total
        }
      });
      
      return response.data;
    } catch (error) {
      console.error('获取题目列表失败:', error);
      throw error;
    } finally {
      commit('updateListFetching', false);
    }
  },

  // 获取问题详情
  async fetchProblemById({ commit }, id) {
    commit('updateCurrentProblemFetching', true);
    
    try {
      const response = await this.$axios.get(`/api/v1/problems/${id}`);
      commit('updateCurrentProblemData', response.data);
      return response.data;
    } catch (error) {
      console.error('获取题目详情失败:', error);
      throw error;
    } finally {
      commit('updateCurrentProblemFetching', false);
    }
  },

  // 获取题目统计信息
  async fetchStatistics({ commit }) {
    try {
      const response = await this.$axios.get('/api/v1/problems/statistics');
      commit('updateStatistics', response.data);
      return response.data;
    } catch (error) {
      console.error('获取统计信息失败:', error);
      throw error;
    }
  },

  // 获取所有标签
  async fetchTags({ commit }) {
    try {
      const response = await this.$axios.get('/api/v1/problems/tags');
      commit('updateTags', response.data);
      return response.data;
    } catch (error) {
      console.error('获取标签列表失败:', error);
      throw error;
    }
  },

  // 提交题目
  async submitProblem({ commit }, data) {
    try {
      const response = await this.$axios.post('/api/v1/problems', data);
      return response.data;
    } catch (error) {
      console.error('提交题目失败:', error);
      throw error;
    }
  },

  // 更新题目
  async updateProblem({ commit }, { id, data }) {
    try {
      const response = await this.$axios.put(`/api/v1/problems/${id}`, data);
      return response.data;
    } catch (error) {
      console.error('更新题目失败:', error);
      throw error;
    }
  },

  // 删除题目
  async deleteProblem({ commit }, id) {
    try {
      const response = await this.$axios.delete(`/api/v1/problems/${id}`);
      return response.data;
    } catch (error) {
      console.error('删除题目失败:', error);
      throw error;
    }
  }
};

export const getters = {
  list: state => state.list,
  currentProblem: state => state.currentProblem,
  difficulties: state => state.filters.difficulties,
  tags: state => state.filters.tags,
  statistics: state => state.list.data.statistics
};