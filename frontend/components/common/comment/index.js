
import Comment from './main'

export const comment = {
  install(Vue) {
    Vue.component('comment-box', Comment)
  }
}

export default comment
