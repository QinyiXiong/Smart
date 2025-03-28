import { createNamespacedHelpers } from 'vuex';

const { mapActions, mapState } = createNamespacedHelpers('problem');

const getDefaultListData = () => ({
  data: [],
  pagination: {}
});

export const state = () => ({
  list: {
    fetching: false,
    data: getDefaultListData()
  },
  currentProblem: {
    fetching: false,
    data: null
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
  }
};

export const actions = {
  // 获取问题列表
  fetchList({ commit }, params = {}) {
    commit('updateListFetching', true);
    // 清空已有数据
    commit('updateListData', getDefaultListData());
    const { difficulty, category, page = 1, rows = 10 } = params;
    return this.$axios.get('/api/v1/problem/list', {
      params: { difficulty, category, page, rows }
    })
    .then(response => {
      commit('updateListFetching', false);
      commit('updateListData', response.data);
    })
    .catch(error => {
      console.error(error);
      commit('updateListFetching', false);
    });
  },
  // 获取问题详情
  fetchProblemById({ commit }, id) {
    commit('updateCurrentProblemFetching', true);
    return this.$axios.get(`/api/v1/problem/${id}`)
    .then(response => {
      commit('updateCurrentProblemFetching', false);
      commit('updateCurrentProblemData', response.data);
    })
    .catch(error => {
      console.error(error);
      commit('updateCurrentProblemFetching', false);
    });
  },
  // 按分类获取问题
  fetchProblemsByCategory({ commit }, { category, page = 1, rows = 10 }) {
    commit('updateListFetching', true);
    commit('updateListData', getDefaultListData());
    return this.$axios.get(`/api/v1/problem/category/${category}`, {
      params: { page, rows }
    })
    .then(response => {
      commit('updateListFetching', false);
      commit('updateListData', response.data);
    })
    .catch(error => {
      console.error(error);
      commit('updateListFetching', false);
    });
  },
  // 按难度获取问题
  fetchProblemsByDifficulty({ commit }, { difficulty, page = 1, rows = 10 }) {
    commit('updateListFetching', true);
    commit('updateListData', getDefaultListData());
    return this.$axios.get(`/api/v1/problem/difficulty/${difficulty}`, {
      params: { page, rows }
    })
    .then(response => {
      commit('updateListFetching', false);
      commit('updateListData', response.data);
    })
    .catch(error => {
      console.error(error);
      commit('updateListFetching', false);
    });
  }
};

export const getters = {
  list: state => state.list,
  currentProblem: state => state.currentProblem
};