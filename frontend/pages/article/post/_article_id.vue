<template>
  <el-row class="articles">
    <el-col v-if="hasPermissions">
      <!-- 标题输入区域 -->
      <el-col>
        <div class="input-section">
          <label class="section-label">
            <i class="el-icon-edit"></i>
            文章标题
          </label>
          <el-input
            v-model="articleTitle"
            class="article-title enhanced-input"
            placeholder="请输入一个吸引人的标题..."
            @change="setLocalstorage('title', articleTitle)"
            size="large">
          </el-input>
        </div>
      </el-col>

      <!-- 标签选择区域 -->
      <el-col style="margin-top: 2rem;">
        <div class="input-section">
          <label class="section-label">
            <i class="el-icon-price-tag"></i>
            文章标签
            <span class="label-desc">选择或创建相关标签，有助于内容分类</span>
          </label>
          <div class="tags-container">
            <el-select
              v-model="articleTags"
              multiple
              filterable
              allow-create
              default-first-option
              remote
              :remote-method="remoteMethod"
              placeholder="输入标签名或从推荐中选择..."
              :loading="loading"
              @change="setLocalstorage('tags', articleTags)"
              class="enhanced-select tags-select"
              size="large">
              <el-option
                v-for="item in options"
                :key="item.value"
                :label="item.label"
                :value="item.value"
                class="tag-option">
                <span class="tag-label">{{ item.label }}</span>
              </el-option>
            </el-select>
            <div class="selected-tags" v-if="articleTags.length > 0">
              <el-tag
                v-for="tag in articleTags"
                :key="tag"
                type="primary"
                size="medium"
                effect="light"
                closable
                @close="removeTag(tag)"
                class="selected-tag">
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 内容编辑器区域 -->
      <el-col>
        <div class="input-section">
          <label class="section-label">
            <i class="el-icon-document"></i>
            文章内容
          </label>
          <div id="contentEditor" class="content-editor-wrapper"></div>
        </div>
      </el-col>



      <!-- 面试相关配置区域 -->
      <el-col style="margin-top: 2rem;">
        <div class="interview-section">
          <div class="section-header">
            <h3 class="section-title">
              <i class="el-icon-user"></i>
              面试相关配置
            </h3>
            <p class="section-subtitle">关联面试官和面试记录，丰富文章内容</p>
          </div>

          <el-row :gutter="20">
            <!-- 面试官选择 -->
            <el-col :span="12">
              <div class="interview-card">
                <div class="card-header">
                  <i class="el-icon-s-custom card-icon"></i>
                  <span class="card-title">选择面试官</span>
                  <el-badge :value="selectedInterviewers.length" :hidden="selectedInterviewers.length === 0" class="item-badge">
                  </el-badge>
                </div>
                <div class="interviewer-list-container">
                  <div class="search-box">
                    <el-input
                      v-model="interviewerSearchQuery"
                      placeholder="搜索面试官..."
                      prefix-icon="el-icon-search"
                      clearable
                      size="small">
                    </el-input>
                  </div>
                  <el-scrollbar class="list-scrollbar">
                    <div class="list-container" v-loading="interviewersLoading">
                      <div
                        v-for="item in filteredInterviewers"
                        :key="item.interviewerId"
                        class="list-item"
                        :class="{ 'is-selected': selectedInterviewers.includes(item.interviewerId) }"
                        @click="toggleInterviewer(item.interviewerId)">
                        <div class="item-avatar">
                          <i class="el-icon-user-solid"></i>
                        </div>
                        <div class="item-info">
                          <span class="item-name">{{ item.name || "未命名" }}</span>
                        </div>
                        <div class="item-action" v-if="selectedInterviewers.includes(item.interviewerId)">
                          <i class="el-icon-check"></i>
                        </div>
                      </div>
                      <div v-if="filteredInterviewers.length === 0 && !interviewersLoading" class="empty-list">
                        <i class="el-icon-warning-outline"></i>
                        <span>{{ interviewerSearchQuery ? '未找到匹配的面试官' : '暂无面试官数据' }}</span>
                      </div>
                    </div>
                  </el-scrollbar>
                </div>
                <div class="selected-items" v-if="selectedInterviewers.length > 0">
                  <el-tag
                    v-for="interviewerId in selectedInterviewers"
                    :key="interviewerId"
                    type="success"
                    size="small"
                    effect="light"
                    closable
                    @close="removeInterviewer(interviewerId)"
                    class="selected-item-tag">
                    {{ getInterviewerName(interviewerId) }}
                  </el-tag>
                </div>
              </div>
            </el-col>

            <!-- 面试记录选择 -->
            <el-col :span="12">
              <div class="interview-card">
                <div class="card-header">
                  <i class="el-icon-chat-line-square card-icon"></i>
                  <span class="card-title">选择面试记录</span>
                  <el-badge :value="selectedInterviews.length" :hidden="selectedInterviews.length === 0" class="item-badge">
                  </el-badge>
                </div>
                <div class="interview-list-container">
                  <div class="search-box">
                    <el-input
                      v-model="interviewSearchQuery"
                      placeholder="搜索面试记录..."
                      prefix-icon="el-icon-search"
                      clearable
                      size="small">
                    </el-input>
                  </div>
                  <el-scrollbar class="list-scrollbar">
                    <div class="list-container" v-loading="interviewsLoading">
                      <div
                        v-for="item in filteredInterviews"
                        :key="item.chatId"
                        class="list-item"
                        :class="{ 'is-selected': selectedInterviews.includes(item.chatId) }"
                        @click="toggleInterview(item.chatId)">
                        <div class="item-avatar interview-avatar">
                          <i class="el-icon-chat-dot-round"></i>
                        </div>
                        <div class="item-info">
                          <span class="item-name">{{ item.topic || "未命名" }}</span>
                        </div>
                        <div class="item-action" v-if="selectedInterviews.includes(item.chatId)">
                          <i class="el-icon-check"></i>
                        </div>
                      </div>
                      <div v-if="filteredInterviews.length === 0 && !interviewsLoading" class="empty-list">
                        <i class="el-icon-warning-outline"></i>
                        <span>{{ interviewSearchQuery ? '未找到匹配的面试记录' : '暂无面试记录数据' }}</span>
                      </div>
                    </div>
                  </el-scrollbar>
                </div>
                <div class="selected-items" v-if="selectedInterviews.length > 0">
                  <el-tag
                    v-for="chatId in selectedInterviews"
                    :key="chatId"
                    type="warning"
                    size="small"
                    effect="light"
                    closable
                    @close="removeInterview(chatId)"
                    class="selected-item-tag">
                    {{ getInterviewTopic(chatId) }}
                  </el-tag>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-col>

      <!-- 操作按钮区域 -->
      <el-col v-if="!isEdit" style="margin-top: 2rem;">
        <div class="action-section">
          <div class="action-buttons">
            <!-- <el-button
              :loading="doLoading"
              @click="saveArticle"
              size="large"
              class="action-btn draft-btn">
              <i class="el-icon-document-copy"></i>
              保存草稿
            </el-button> -->
            <el-button
              type="primary"
              :loading="doLoading"
              @click="postArticle"
              size="large"
              class="action-btn publish-btn">
              <i class="el-icon-position"></i>
              立即发布
            </el-button>
          </div>
        </div>
      </el-col>

      <el-col v-else style="margin-top: 2rem;">
        <div class="action-section">
          <div class="action-buttons edit-buttons">
            <el-button
              type="danger"
              :loading="doLoading"
              @click="deleteArticle"
              size="large"
              class="action-btn delete-btn">
              <i class="el-icon-delete"></i>
              删除文章
            </el-button>
            <div class="right-buttons">
              <el-button
                v-if="articleStatus === '1'"
                :loading="doLoading"
                @click="saveArticle"
                size="large"
                class="action-btn draft-btn">
                <i class="el-icon-document-copy"></i>
                保存草稿
              </el-button>
              <el-button
                v-if="articleStatus === '0'"
                :loading="doLoading"
                type="primary"
                @click="postArticle"
                size="large"
                class="action-btn update-btn">
                <i class="el-icon-refresh"></i>
                更新发布
              </el-button>
              <el-button
                v-else
                type="primary"
                :loading="doLoading"
                @click="postArticle"
                size="large"
                class="action-btn publish-btn">
                <i class="el-icon-position"></i>
                发布文章
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-col>

    <el-col v-else class="text-center">
      <div class="permission-alert">
        <el-alert
          title="用户无权限"
          type="warning"
          center
          show-icon
          :closable="false"
          class="enhanced-alert">
          <template slot="title">
            <span class="alert-title">访问受限</span>
          </template>
          您暂时没有权限访问此页面，请联系管理员获取相应权限。
        </el-alert>
      </div>
    </el-col>
  </el-row>
</template>

<script>
import Vue from 'vue';
import { mapState } from 'vuex';
import apiConfig from '~/config/api.config';

export default {
  name: "PostArticle",
  middleware: 'auth',
  validate({ params, store }) {
    if (typeof params.article_id === 'undefined') {
      return true;
    }
    return params.article_id && !isNaN(Number(params.article_id))
  },
  asyncData({ store, params, error }) {
    return Promise.all([
      store.dispatch('article/fetchPostDetail', params)
        .catch(err => error({ statusCode: 404 }))
    ])
  },
  computed: {
    ...mapState({
      article: state => state.article.detail.data
    }),
    hasPermissions() {
      let account = this.$store.state.auth.user?.nickname;
      if (account) {
        if (this.$route.params.article_id) {
          if (account === this.article.articleAuthor.userNickname) {
            return true;
          }
        } else {
          return true;
        }
      }
      return this.$auth.hasScope('blog_admin') || this.$auth.hasScope('admin');
    },
    // 过滤面试官列表
    filteredInterviewers() {
      if (!this.interviewerSearchQuery) {
        return this.interviewersList;
      }
      const query = this.interviewerSearchQuery.toLowerCase();
      return this.interviewersList.filter(item =>
        item.name.toLowerCase().includes(query)
      );
    },
    // 过滤面试记录列表
    filteredInterviews() {
      if (!this.interviewSearchQuery) {
        return this.interviewsList;
      }
      const query = this.interviewSearchQuery.toLowerCase();
      return this.interviewsList.filter(item =>
        item.topic.toLowerCase().includes(query)
      );
    }
  },
  data() {
    return {
      contentEditor: null,
      tokenURL: {
        URL: '',
        linkToImageURL: '',
        token: ''
      },
      idArticle: 0,
      articleTitle: '',
      articleContent: '',
      articleType: 0,
      articleTags: [],
      articleStatus: '0',
      options: [],
      list: [],
      loading: false,
      doLoading: false,
      isEdit: false,
      notificationFlag: true,
      // 面试官相关数据
      selectedInterviewers: [],
      interviewersList: [],
      interviewersLoading: false,
      interviewerSearchQuery: '', // 面试官搜索关键词
      // 面试记录相关数据
      selectedInterviews: [],
      interviewsList: [],
      interviewsLoading: false,
      interviewSearchQuery: '' // 面试记录搜索关键词
    }
  },
  methods: {
    // 原有方法保持不变
    _initEditor(data) {
      let _ts = this;

      let toolbar = [
        'emoji',
        'headings',
        'bold',
        'italic',
        'strike',
        'link',
        '|',
        'list',
        'ordered-list',
        'check',
        'outdent',
        'indent',
        '|',
        'quote',
        'line',
        'code',
        'inline-code',
        'insert-before',
        'insert-after',
        '|',
        'upload',
        'table',
        '|',
        'undo',
        'redo',
        '|',
        'edit-mode',
        {
          name: 'more',
          toolbar: [
            'fullscreen',
            'both',
            'preview',
            'info'
          ],
        }]
      return new Vue.Vditor(data.id, {
        toolbar,
        mode: 'sv',
        tab: '\t',
        cdn: apiConfig.VDITOR,
        cache: {
          enable: this.$route.params.article_id ? false : true,
          id: this.$route.params.article_id ? this.$route.params.article_id : '',
        },
        after() {
          _ts.contentEditor.setValue(data.value ? data.value : '');
        },
        hint: {
          emoji: Vue.emoji
        },
        preview: {
          hljs: {
            enable: true,
            lineNumber: true,
            style: 'github'
          },
          markdown: {
            toc: true,
            autoSpace: true
          },
          math: {
            inlineDigit: true
          },
          delay: 500,
          mode: data.mode,
          parse: (element) => {
            if (element.style.display === 'none') {
              return
            }
          },
          theme: {
            path: apiConfig.VDITOR_CSS
          }
        },
        upload: {
          max: 10 * 1024 * 1024,
          url: this.tokenURL.URL,
          linkToImgUrl: this.tokenURL.linkToImageURL,
          token: this.$auth.strategy.token.get(),
          filename: name => name.replace(/[^(a-zA-Z0-9\u4e00-\u9fa5\.)]/g, '').
            replace(/[\?\\/:|<>\*\[\]\(\)\$%\{\}@~]/g, '').
            replace('/\\s/g', '')
        },
        height: data.height,
        counter: 102400,
        resize: {
          enable: data.resize,
        },
        lang: this.$store.state.locale,
        placeholder: data.placeholder,
      })
    },

    // 新增的辅助方法
    removeTag(tag) {
      const index = this.articleTags.indexOf(tag);
      if (index > -1) {
        this.articleTags.splice(index, 1);
        this.setLocalstorage('tags', this.articleTags);
      }
    },
    removeInterviewer(interviewerId) {
      const index = this.selectedInterviewers.indexOf(interviewerId);
      if (index > -1) {
        this.selectedInterviewers.splice(index, 1);
      }
    },
    removeInterview(chatId) {
      const index = this.selectedInterviews.indexOf(chatId);
      if (index > -1) {
        this.selectedInterviews.splice(index, 1);
      }
    },
    // 切换面试官选择状态
    toggleInterviewer(interviewerId) {
      const index = this.selectedInterviewers.indexOf(interviewerId);
      if (index > -1) {
        this.selectedInterviewers.splice(index, 1);
      } else {
        this.selectedInterviewers.push(interviewerId);
      }
    },
    // 切换面试记录选择状态
    toggleInterview(chatId) {
      const index = this.selectedInterviews.indexOf(chatId);
      if (index > -1) {
        this.selectedInterviews.splice(index, 1);
      } else {
        this.selectedInterviews.push(chatId);
      }
    },
    getInterviewerName(interviewerId) {
      const interviewer = this.interviewersList.find(item => item.interviewerId === interviewerId);
      return interviewer ? interviewer.name : `面试官-${interviewerId}`;
    },
    getInterviewTopic(chatId) {
      const interview = this.interviewsList.find(item => item.chatId === chatId);
      return interview ? interview.topic : `记录-${chatId}`;
    },

    setLocalstorage(type) {
      if (typeof arguments[0] === 'object') {
        localStorage.setItem('articleTags', arguments[1]);
        return
      }
      switch (type) {
        case 'title': {
          localStorage.setItem('article-title', arguments[1])
          break;
        }
        case 'tags': {
          localStorage.setItem('article-tags', arguments[1]);
          break;
        }
      }
    },
    remoteMethod(query) {
      if (query !== '') {
        this.loading = true;
        setTimeout(() => {
          this.loading = false;
          this.options = this.list.filter(item => {
            return item.label.toLowerCase()
              .indexOf(query.toLowerCase()) > -1;
          });
        }, 200);
      } else {
        this.options = [];
      }
    },
    deleteArticle() {
      let _ts = this;
      _ts.doLoading = true;
      this.$confirm('确定删除吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        let id = _ts.$route.params.article_id;
        _ts.$axios.$delete('/api/article/delete/' + id).then(function (res) {
          if (res && res.message) {
            _ts.$message(res.message);
            return false;
          }
          localStorage.removeItem('article-title');
          localStorage.removeItem('article-tags');
          _ts.contentEditor.setValue('');
          _ts.$set(_ts, 'notificationFlag', false);
          _ts.$router.push({
            name: 'index'
          })
        })
      }).catch(() => {
        _ts.doLoading = false;
      });
    },
    async postArticle() {
      let _ts = this;
      _ts.doLoading = true;
      let id = _ts.$route.params.article_id;
      let articleContent = _ts.contentEditor.getValue();
      let articleContentHtml = await _ts.contentEditor.getHTML();
      if (!(_ts.articleTitle && articleContent)) {
        _ts.$message("标题/正文不能为空！");
        _ts.doLoading = false;
        return false;
      }
      let article = {
        idArticle: _ts.idArticle,
        articleTitle: _ts.articleTitle,
        articleContent: articleContent,
        articleContentHtml: articleContentHtml,
        articleTags: _ts.articleTags.join(","),
        articleStatus: 0,
        interviewerList: _ts.selectedInterviewers.map(id => {
          const interviewer = _ts.interviewersList.find(item => item.interviewerId === id);
          return {
            interviewerId: id,
            name: interviewer ? interviewer.name : `面试官-${id}`,
            userId: null,
            knowledgeBaseId: null,
            customPrompt: null,
            settingsList: null
          };
        }),
        chatRecordsList: _ts.selectedInterviews.map(id => {
          const interview = _ts.interviewsList.find(item => item.chatId === id);
          return {
            interviewer: {
              interviewerId: interview ? interview.interviewerId : null,
              name: interview ? _ts.getInterviewerName(interview.interviewerId) : null,
              userId: null,
              knowledgeBaseId: null,
              customPrompt: null,
              settingsList: null
            },
            branch: null,
            chatId: Number(id),
            userId: null,
            interviewerId: interview ? interview.interviewerId : null,
            createdAt: null,
            updatedAt: null,
            topic: interview ? interview.topic : `记录-${id}`,
          };
        })
      };
      console.log("post article" + _ts.article);
      _ts.$axios[id ? '$put' : '$post']('/api/article/post', article).then(function (res) {
        if (res) {
          if (res.message) {
            _ts.$message(res.message);
            _ts.doLoading = false;
            return false;
          }
          localStorage.removeItem('article-title');
          localStorage.removeItem('article-tags');
          _ts.contentEditor.setValue('');
          _ts.$store.commit('article/clearDetailData')
          _ts.$set(_ts, 'notificationFlag', false);
          _ts.$router.push({
            path: `/article/${res}`
          })
        }
      })
    },
    async saveArticle() {
      let _ts = this;
      _ts.doLoading = true;
      let id = _ts.$route.params.article_id;
      let articleContent = _ts.contentEditor.getValue();
      let articleContentHtml = await _ts.contentEditor.getHTML();
      if (!(_ts.articleTitle && articleContent)) {
        _ts.$message("标题/正文不能为空！");
        _ts.doLoading = false;
        return false;
      }
      let article = {
        idArticle: _ts.idArticle,
        articleTitle: _ts.articleTitle,
        articleContent: articleContent,
        articleContentHtml: articleContentHtml,
        articleTags: _ts.articleTags.join(","),
        articleStatus: 0,
        interviewerList: _ts.selectedInterviewers.map(id => {
          const interviewer = _ts.interviewersList.find(item => item.interviewerId === id);
          return {
            interviewerId: id,
            name: interviewer ? interviewer.name : `面试官-${id}`,
            userId: null,
            knowledgeBaseId: null,
            customPrompt: null,
            settingsList: null
          };
        }),
        chatRecordsList: _ts.selectedInterviews.map(id => {
          const interview = _ts.interviewsList.find(item => item.chatId === id);
          return {
            interviewer: {
              interviewerId: interview ? interview.interviewerId : null,
              name: interview ? _ts.getInterviewerName(interview.interviewerId) : null,
              userId: null,
              knowledgeBaseId: null,
              customPrompt: null,
              settingsList: null
            },
            branch: null,
            chatId: Number(id),
            userId: null,
            interviewerId: interview ? interview.interviewerId : null,
            createdAt: null,
            updatedAt: null,
            topic: interview ? interview.topic : `记录-${id}`,
          };
        })
      };
      _ts.$axios[id ? '$put' : '$post']('/api/article/post', article).then(function (res) {
        if (res) {
          if (res.message) {
            _ts.$message(res.message);
            _ts.doLoading = false;
            return false;
          }
          localStorage.removeItem('article-title');
          localStorage.removeItem('article-tags');
          _ts.contentEditor.setValue('');
          _ts.$set(_ts, 'notificationFlag', false);
          _ts.$router.push({
            path: `/draft/${res}`
          })
        }
      })
    },
    getTags() {
      let _ts = this;
      _ts.$axios.$get('/api/tag/tags').then(function (res) {
        if (res) {
          _ts.$set(_ts, 'list', res);
        }
      })
    },
    async getInterviewers() {
      let _ts = this;
      _ts.interviewersLoading = true;
      try {
        const res = await _ts.$axios.$get('/api/share/getUserInterviewers');
        if (res) {
          _ts.$set(_ts, 'interviewersList', res);
          console.log(res);
        }
      } catch (err) {
        _ts.$message.error('获取面试官列表失败');
      } finally {
        _ts.interviewersLoading = false;
      }
    },
    async getInterviews() {
      let _ts = this;
      _ts.interviewsLoading = true;
      try {
        const res = await _ts.$axios.$get('/api/share/getUserChatRecords');
        if (res) {
          _ts.$set(_ts, 'interviewsList', res);
          console.log(res);
        }
      } catch (err) {
        _ts.$message.error('获取面试记录列表失败');
      } finally {
        _ts.interviewsLoading = false;
      }
    }
  },
  beforeRouteLeave(to, from, next) {
    let _ts = this;
    if (_ts.notificationFlag) {
      _ts.$confirm('系统可能不会保存您所做的更改。', '离开此网站?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        next();
      }).catch(() => {
        return false
      });
      _ts.$store.commit("setActiveMenu", "article-post");
    } else {
      next();
    }
  },
  beforeDestroy() {
    window.onbeforeunload = null;
  },
  async mounted() {
    if (!this.hasPermissions) {
      return
    }
    let _ts = this;
    _ts.$store.commit('setActiveMenu', 'article-post');
    const responseData = await _ts.$axios.$get('/api/upload/token');
    if (responseData) {
      _ts.$set(_ts, 'tokenURL', {
        token: responseData.uploadToken || '',
        URL: responseData.uploadURL || '',
        linkToImageURL: responseData.linkToImageURL || ''
      })
    }

    _ts.getTags();
    await _ts.getInterviewers();
    await _ts.getInterviews();
    Vue.nextTick(() => {
      let articleContent = '';
      if (_ts.$route.params.article_id) {
        _ts.$set(_ts, 'isEdit', true);
        let article = _ts.article;
        _ts.$set(_ts, 'idArticle', article.idArticle);
        _ts.$set(_ts, 'articleTitle', article.articleTitle);
        _ts.$set(_ts, 'articleContent', article.articleContent);
        _ts.$set(_ts, 'articleStatus', article.articleStatus);
        _ts.$set(_ts, 'articleTags', (article.articleTags).split(','));

        // 设置已选择的面试官和面试记录
        if (article.interviewers && article.interviewers.length > 0) {
          _ts.$set(_ts, 'selectedInterviewers', article.interviewers.map(interviewer => interviewer.interviewerId));
        }

        if (article.interviews && article.interviews.length > 0) {
          _ts.$set(_ts, 'selectedInterviews', article.interviews.map(interview => interview.chatId));
        }

        localStorage.setItem("article-title", article.articleTitle);
        localStorage.setItem("article-tags", (article.articleTags).split(','));
        articleContent = article.articleContent
      } else {
        _ts.$set(_ts, 'isEdit', false);
      }
      _ts.contentEditor = _ts._initEditor({
        id: 'contentEditor',
        mode: 'both',
        height: 600,
        placeholder: '',
        resize: false,
        value: articleContent
      });
    })
  }
}
</script>

<style lang="less">
@import "~vditor/src/assets/less/index.less";

// 主要变量定义
@primary-color: #409EFF;
@success-color: #67C23A;
@warning-color: #E6A23C;
@danger-color: #F56C6C;
@border-radius: 8px;
@box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
@transition: all 0.3s ease;

.articles {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  background: #fff;
  min-height: 100vh;

  // 输入区域样式
  .input-section {
    margin-bottom: 12px;

    .section-label {
      display: flex;
      align-items: center;
      margin-bottom: 12px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;

      i {
        margin-right: 8px;
        color: @primary-color;
        font-size: 18px;
      }

      .label-desc {
        margin-left: 8px;
        font-size: 12px;
        font-weight: 400;
        color: #909399;
      }
    }
  }

  // 增强输入框样式
  .enhanced-input {
    .el-input__inner {
      border-radius: @border-radius;
      border: 2px solid #DCDFE6;
      transition: @transition;

      &:focus {
        border-color: @primary-color;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
      }

      &:hover {
        border-color: #C0C4CC;
      }
    }
  }

  // 内容编辑器包装
  .content-editor-wrapper {
    border-radius: @border-radius;
    overflow: hidden;
    box-shadow: @box-shadow;
  }

  // 标签区域样式
  .tags-container {
    .tags-select {
      width: 100%;
      margin-bottom: 16px;
    }

    .selected-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .selected-tag {
        transition: @transition;

        &:hover {
          transform: translateY(-1px);
        }
      }
    }
  }

  // 增强选择器样式
  .enhanced-select {
    .el-select__tags {
      max-height: 80px;
      overflow-y: auto;
    }

    .el-input__inner {
      border-radius: @border-radius;
      border: 2px solid #DCDFE6;
      transition: @transition;

      &:focus {
        border-color: @primary-color;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
      }
    }
  }

  // 选项样式
  .tag-option,
  .interviewer-option,
  .interview-option {
    padding: 12px 16px;

    &:hover {
      background-color: #f5f7fa;
    }

    .tag-label {
      font-weight: 500;
    }

    .option-content {
      display: flex;
      align-items: center;

      .option-avatar {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: linear-gradient(135deg, @primary-color, #36cfc9);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;

        i {
          color: white;
          font-size: 14px;
        }

        &.interview-avatar {
          background: linear-gradient(135deg, @warning-color, #ffd666);
        }
      }

      .option-info {
        flex: 1;

        .option-name {
          display: block;
          font-weight: 500;
          color: #303133;
          margin-bottom: 2px;
        }

        .option-id {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }

  // 面试配置区域
  .interview-section {
    background: linear-gradient(135deg, #f8f9ff 0%, #f0f7ff 100%);
    border-radius: 12px;
    padding: 24px;
    border: 1px solid #e6f0ff;

    .section-header {
      text-align: center;
      margin-bottom: 24px;

      .section-title {
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 0 8px 0;
        font-size: 20px;
        font-weight: 600;
        color: #303133;

        i {
          margin-right: 8px;
          color: @primary-color;
          font-size: 24px;
        }
      }

      .section-subtitle {
        margin: 0;
        font-size: 14px;
        color: #606266;
      }
    }

    .interview-card {
      background: white;
      border-radius: @border-radius;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      transition: @transition;
      height: 100%;

      /* 移除卡片悬停效果 */
      /*
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
      }
      */

      .card-header {
        display: flex;
        align-items: center;
        margin-bottom: 16px;
        position: relative;

        .card-icon {
          color: @primary-color;
          font-size: 20px;
          margin-right: 8px;
        }

        .card-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          flex: 1;
        }

        .item-badge {
          .el-badge__content {
            background-color: @primary-color;
            border: none;
          }
        }
      }

      // 新增列表容器样式
      .interviewer-list-container,
      .interview-list-container {
        margin-bottom: 16px;
        border: 1px solid #e4e7ed;
        border-radius: @border-radius;
        overflow: hidden;

        .search-box {
          padding: 8px;
          background-color: #f5f7fa;
          border-bottom: 1px solid #e4e7ed;

          .el-input__inner {
            border-radius: 4px;
            height: 32px;
          }
        }

        .list-scrollbar {
          height: 240px;

          .el-scrollbar__wrap {
            overflow-x: hidden;
          }

          .list-container {
            padding: 8px 0;

            .list-item {
              display: flex;
              align-items: center;
              padding: 10px 16px;
              cursor: pointer;
              transition: @transition;

              /* 移除悬停效果 */
              /*
              &:hover {
                background-color: #f5f7fa;
              }
              */

              &.is-selected {
                background-color: #ecf5ff;
              }

              .item-avatar {
                width: 32px;
                height: 32px;
                border-radius: 50%;
                background: linear-gradient(135deg, @primary-color, #36cfc9);
                display: flex;
                align-items: center;
                justify-content: center;
                margin-right: 12px;

                i {
                  color: white;
                  font-size: 14px;
                }

                &.interview-avatar {
                  background: linear-gradient(135deg, @warning-color, #ffd666);
                }
              }

              .item-info {
                flex: 1;

                .item-name {
                  font-size: 14px;
                  font-weight: 500;
                  color: #303133;
                  white-space: nowrap;
                  overflow: hidden;
                  text-overflow: ellipsis;
                }
              }

              .item-action {
                color: @primary-color;
                font-size: 16px;
              }
            }

            .empty-list {
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              padding: 40px 0;
              color: #909399;

              i {
                font-size: 24px;
                margin-bottom: 8px;
              }

              span {
                font-size: 14px;
              }
            }
          }
        }
      }

      .selected-items {
        margin-top: 12px;
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        max-height: 100px;
        overflow-y: auto;

        .selected-item-tag {
          transition: @transition;
          font-size: 12px;

          &:hover {
            transform: translateY(-1px);
          }
        }
      }
    }
  }

  // 操作按钮区域
  .action-section {
    background: #fafbfc;
    border-radius: @border-radius;
    padding: 20px;
    border: 1px solid #e4e7ed;

    .action-buttons {
      display: flex;
      justify-content: center;
      gap: 16px;

      &.edit-buttons {
        justify-content: space-between;

        .right-buttons {
          display: flex;
          gap: 16px;
        }
      }

      .action-btn {
        padding: 12px 24px;
        border-radius: @border-radius;
        font-weight: 500;
        transition: @transition;
        min-width: 140px;

        i {
          margin-right: 6px;
        }

        &:hover {
          transform: translateY(-1px);
        }

        &.draft-btn {
          background: #f4f4f5;
          border-color: #d3d4d6;
          color: #606266;

          &:hover {
            background: #e9e9eb;
            border-color: #c0c4cc;
          }
        }

        &.publish-btn {
          background: linear-gradient(135deg, @primary-color, #36cfc9);
          border: none;

          &:hover {
            background: linear-gradient(135deg, #328FE6, #2db7b1);
          }
        }

        &.update-btn {
          background: linear-gradient(135deg, @success-color, #85ce61);
          border: none;

          &:hover {
            background: linear-gradient(135deg, #5daf34, #73c150);
          }
        }

        &.delete-btn {
          background: linear-gradient(135deg, @danger-color, #f78989);
          border: none;

          &:hover {
            background: linear-gradient(135deg, #f25555, #f56c6c);
          }
        }
      }
    }
  }

  // 权限提示区域
  .permission-alert {
    max-width: 600px;
    margin: 100px auto;

    .enhanced-alert {
      border-radius: @border-radius;
      border: none;
      box-shadow: @box-shadow;

      .alert-title {
        font-size: 18px;
        font-weight: 600;
      }
    }
  }

  // 响应式设计
  @media (max-width: 768px) {
    padding: 16px;

    .interview-section {
      .el-row {
        .el-col {
          margin-bottom: 16px;
        }
      }
    }

    .action-section {
      .action-buttons {
        flex-direction: column;

        &.edit-buttons {
          .right-buttons {
            flex-direction: column;
          }
        }

        .action-btn {
          min-width: auto;
        }
      }
    }
  }

  // 滚动条美化
  .selected-items::-webkit-scrollbar,
  .enhanced-select .el-select__tags::-webkit-scrollbar {
    width: 4px;
  }

  .selected-items::-webkit-scrollbar-track,
  .enhanced-select .el-select__tags::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 2px;
  }

  .selected-items::-webkit-scrollbar-thumb,
  .enhanced-select .el-select__tags::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 2px;

    &:hover {
      background: #a8a8a8;
    }
  }

  // 加载动画优化
  .el-loading-mask {
    border-radius: @border-radius;
  }

  // 选择器下拉面板美化
  .el-select-dropdown {
    border-radius: @border-radius;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    border: none;

    .el-select-dropdown__item {
      transition: @transition;

      &:hover {
        background-color: #f5f7fa;
      }

      &.selected {
        background-color: #e6f7ff;
        color: @primary-color;
        font-weight: 500;
      }
    }
  }
}
</style>