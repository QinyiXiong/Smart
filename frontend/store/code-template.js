/**
 * 代码模板状态管理
 */
export const state = () => ({
  templates: {},
  loading: false,
  error: null
})

export const mutations = {
  SET_TEMPLATES(state, templates) {
    // 将模板列表转换为以语言为键的对象
    const templatesMap = {}
    templates.forEach(template => {
      templatesMap[template.language] = template
    })
    state.templates = templatesMap
  },
  SET_LOADING(state, loading) {
    state.loading = loading
  },
  SET_ERROR(state, error) {
    state.error = error
  }
}

export const actions = {
  /**
   * 获取所有代码模板
   */
  async fetchAllTemplates({ commit }) {
    commit('SET_LOADING', true)
    commit('SET_ERROR', null)
    
    try {
      const response = await this.$axios.get('/api/code-templates')
      if (response.code === 0) {
        commit('SET_TEMPLATES', response.data)
      } else {
        commit('SET_ERROR', response.message || '获取代码模板失败')
      }
    } catch (error) {
      commit('SET_ERROR', error.message || '获取代码模板异常')
    } finally {
      commit('SET_LOADING', false)
    }
  },
  
  /**
   * 根据语言获取代码模板
   */
  async getTemplateByLanguage({ state, dispatch }, language) {
    // 如果模板缓存为空，先获取所有模板
    if (Object.keys(state.templates).length === 0) {
      await dispatch('fetchAllTemplates')
    }
    
    return state.templates[language] || null
  }
}

export const getters = {
  /**
   * 获取所有支持的语言列表
   */
  supportedLanguages: (state) => {
    return Object.values(state.templates).map(template => ({
      value: template.language,
      label: template.displayName,
      icon: template.icon
    }))
  },
  
  /**
   * 根据语言获取模板
   */
  getTemplate: (state) => (language) => {
    return state.templates[language]?.template || ''
  }
}