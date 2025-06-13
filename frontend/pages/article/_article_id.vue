<template>
  <div class="article-container" :class="{ 'with-detail': selectedInterviewId, 'resizing': isResizing }">
    <!-- 左侧内容区域 -->
    <div class="left-panel" :style="getLeftPanelStyle()">
      <el-row class="article__wrapper">
        <el-col>
          <el-card>
            <div class="card-body d-flex flex-column article">
              <div class="article__item">
                <h1 class="list__title">
                  <span v-if="isPerfect" style="color: gold;" title="优选">
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"
                         style="fill: #FFB71B;"><path
                      d="M12 22c3.859 0 7-3.141 7-7s-3.141-7-7-7c-3.86 0-7 3.141-7 7s3.14 7 7 7zm0-12c2.757 0 5 2.243 5 5s-2.243 5-5 5-5-2.243-5-5 2.243-5 5-5zm-1-8H7v5.518a8.957 8.957 0 0 1 4-1.459V2zm6 0h-4v4.059a8.957 8.957 0 0 1 4 1.459V2z"></path><path
                      d="m10.019 15.811-.468 2.726L12 17.25l2.449 1.287-.468-2.726 1.982-1.932-2.738-.398L12 11l-1.225 2.481-2.738.398z"></path></svg>
                  </span>
                  {{ article.articleTitle }}
                </h1>
                <!-- 面试官和面试记录卡片 -->
                <el-row :gutter="20" style="margin: 20px 0;">
                  <!-- 面试官卡片列表 -->
                  <el-col :span="24" v-for="(interviewer, index) in article.interviewerList" :key="'interviewer-' + index">
                    <el-card shadow="hover" :style="{ marginBottom: '20px' }">
                      <el-row type="flex" align="middle">
                        <el-col :span="4">
                          <el-avatar :size="64" :src="interviewer.avatarUrl || 'https://static.rymcu.com/article/1578475481946.png'"></el-avatar>
                        </el-col>
                        <el-col :span="16">
                          <h3>{{ interviewer.name || '暂无面试官名称' }}</h3>
                          <p>{{ interviewer.customPrompt || '暂无面试官信息' }}</p>
                        </el-col>
                        <el-col :span="4" style="text-align: right;">
                          <el-button 
                            type="primary" 
                            size="small" 
                            @click="getInterviewer(interviewer.interviewerId)"
                            class="modern-btn interviewer-btn">
                            <i class="el-icon-upload2"></i>
                            获取面试官
                          </el-button>
                        </el-col>
                      </el-row>
                    </el-card>
                  </el-col>
                  <!-- 面试记录卡片列表 -->
                  <el-col :span="24" v-for="(interview, index) in article.chatRecordsList" :key="'interview-' + index">
                    <el-card shadow="hover" :style="{ marginBottom: '20px', backgroundColor: selectedInterviewId === interview.chatId ? '#f0f9ff' : 'white' }">
                      <el-row>
                        <el-col :span="24">
                          <h4>面试记录 #{{ index + 1 }}</h4>
                          <p class="interview-desc">{{ interview.topic || '暂无面试记录描述' }}</p>
                          <p class="interviewer-name">面试官：{{ interview.interviewer.name || '暂无面试官' }}</p>
                        </el-col>
                        <el-col :span="24" style="text-align: right; margin-top: 10px;">
                          <el-button 
                            size="small" 
                            :type="selectedInterviewId === interview.chatId ? 'primary' : 'default'"
                            @click="viewInterviewDetail(interview.chatId)"
                            class="modern-btn view-btn"
                            :class="{'selected-btn': selectedInterviewId === interview.chatId}">
                            <i class="el-icon-view"></i>
                            {{ selectedInterviewId === interview.chatId ? '已选中' : '查看详情' }}
                          </el-button>
                          <el-button 
                            type="success" 
                            size="small" 
                            @click="getInterview(interview.chatId)"
                            class="modern-btn get-btn">
                            <i class="el-icon-download"></i>
                            获取面试记录
                          </el-button>
                        </el-col>
                      </el-row>
                    </el-card>
                  </el-col>
                </el-row>
                <el-row class="pt-5">
                  <el-col :xs="3" :sm="1" :xl="1">
                    <el-avatar v-if="article.articleAuthorAvatarUrl" :src="article.articleAuthorAvatarUrl"></el-avatar>
                    <el-avatar v-else src="https://static.rymcu.com/article/1578475481946.png"></el-avatar>
                  </el-col>
                  <el-col :xs="9" :sm="11" :xl="11">
                    <div style="margin-left: 1rem;">
                      <el-link :href="'/user/' + article.articleAuthor?.userAccount" class="text-default">
                        {{ article.articleAuthorName }}
                      </el-link>
                      <small class="d-block text-muted">{{ article.timeAgo }}</small>
                    </div>
                  </el-col>
                  <el-col :xs="12" :sm="12" :xl="12" v-if="user" class="text-right">
                    <el-link rel="nofollow" :underline="false" title="总浏览数"><i class="el-icon-s-data"></i><span
                      style="color: red;">{{ article.articleViewCount }}</span>
                    </el-link>
                  </el-col>
                  <el-col style="margin: 1rem 0;">
                    <el-col :span="12">
                      <el-tag
                        style="margin-right: 0.5rem;"
                        v-for="tag in article.tags"
                        :key="tag.idTag"
                        size="small"
                        effect="plain">
                        # {{ tag.tagTitle }}
                      </el-tag>
                    </el-col>
                    <client-only>
                      <el-col v-if="user" :span="12" style="text-align: right;">
                        <template v-if="user.idUser !== article.articleAuthorId">
                          <el-button size="mini" v-if="isFollower(article.articleAuthorId)"
                                     @click="cancelFollowUser(article.articleAuthorId)" plain>
                            取消关注
                          </el-button>
                          <el-button size="mini" v-else @click="followUser(article.articleAuthorId)" plain>关注</el-button>
                        </template>
                        <el-button size="mini" v-if="hasPermissions" @click="handleCommand('edit')" plain>编辑文章</el-button>
                        <template v-if="isAdmin">
                          <el-button size="mini" @click="handleCommand('editTag')" plain>编辑标签</el-button>
                          <el-button v-if="isPerfect" size="mini" @click="cancelPreference" plain>取消优选</el-button>
                          <el-button v-else size="mini" @click="setPreference" plain>设为优选</el-button>
                        </template>
                        <template v-else-if="hasPermissions">
                          <el-button size="mini" @click="handleCommand('editTag')" plain>编辑标签</el-button>
                        </template>
                        <el-button size="mini" @click="handleCommand('share')" plain>分享</el-button>
                      </el-col>
                      <el-col v-else :span="12" style="text-align: right;">
                        <el-button size="mini" @click="gotoLogin" plain>关注</el-button>
                        <el-button size="mini" @click="handleCommand('share')" plain>分享</el-button>
                      </el-col>
                    </client-only>
                  </el-col>
                  <el-col v-if="isShare" style="margin-bottom: 1rem;">
                    <share-box :url="shareUrl"></share-box>
                  </el-col>
                  <el-col v-if="article.portfolios && article.portfolios.length > 0">
                    <portfolios-widget :portfolios="article.portfolios"></portfolios-widget>
                  </el-col>
                </el-row>
                <div class="pt-7 pipe-content__reset vditor-reset" id="articleContent" v-html="article.articleContent"
                     style="overflow: hidden;"></div>
              </div>
              <el-row>
                <el-col v-if="article.portfolios && article.portfolios.length > 0">
                  <portfolios-widget :portfolios="article.portfolios"></portfolios-widget>
                </el-col>
                <el-col v-if="loggedIn">
                  <el-tooltip class="item" effect="dark" content="酷" placement="top-start">
                    <el-button type="text" style="font-size: 1.2rem;" @click="thumbsUp">
                      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                           style="fill: rgba(0, 0, 0, 1);">
                        <path
                          d="M14.683 14.828a4.055 4.055 0 0 1-1.272.858 4.002 4.002 0 0 1-4.875-1.45l-1.658 1.119a6.063 6.063 0 0 0 1.621 1.62 5.963 5.963 0 0 0 2.148.903 6.035 6.035 0 0 0 3.542-.35 6.048 6.048 0 0 0 1.907-1.284c.272-.271.52-.571.734-.889l-1.658-1.119a4.147 4.147 0 0 1-.489.592z"></path>
                        <path
                          d="M12 2C6.486 2 2 6.486 2 12s4.486 10 10 10 10-4.486 10-10S17.514 2 12 2zm0 2c2.953 0 5.531 1.613 6.918 4H5.082C6.469 5.613 9.047 4 12 4zm0 16c-4.411 0-8-3.589-8-8 0-.691.098-1.359.264-2H5v1a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2h2a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2v-1h.736c.166.641.264 1.309.264 2 0 4.411-3.589 8-8 8z"></path>
                      </svg>
                      {{ article.articleThumbsUpCount }}
                    </el-button>
                  </el-tooltip>
                  <el-tooltip v-if="article.canSponsor" class="item" effect="dark" content="赞赏" placement="top-start" :disabled="!$auth.user.bankAccount">
                    <el-button v-if="user.idUser === article.articleAuthorId" type="text"
                               style="font-size: 1.2rem;">
                      <svg width="24" height="24">
                        <path :d="buymeacoffee.path"></path>
                      </svg>
                      {{ article.articleSponsorCount }}
                    </el-button>
                    <el-popconfirm
                      v-else
                      title="赞赏本文作者 20 巴旦木？"
                      @confirm="sponsor"
                    >
                      <el-button slot="reference" type="text" style="font-size: 1.2rem;">
                        <svg width="24" height="24">
                          <path :d="buymeacoffee.path"></path>
                        </svg>
                        {{ article.articleSponsorCount }}
                      </el-button>
                    </el-popconfirm>
                  </el-tooltip>
                </el-col>
                <el-col v-else>
                  <el-tooltip class="item" effect="dark" content="酷" placement="top-start">
                    <el-button type="text" style="font-size: 1.2rem;">
                      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                           style="fill: rgba(0, 0, 0, 1);">
                        <path
                          d="M14.683 14.828a4.055 4.055 0 0 1-1.272.858 4.002 4.002 0 0 1-4.875-1.45l-1.658 1.119a6.063 6.063 0 0 0 1.621 1.62 5.963 5.963 0 0 0 2.148.903 6.035 6.035 0 0 0 3.542-.35 6.048 6.048 0 0 0 1.907-1.284c.272-.271.52-.571.734-.889l-1.658-1.119a4.147 4.147 0 0 1-.489.592z"></path>
                        <path
                          d="M12 2C6.486 2 2 6.486 2 12s4.486 10 10 10 10-4.486 10-10S17.514 2 12 2zm0 2c2.953 0 5.531 1.613 6.918 4H5.082C6.469 5.613 9.047 4 12 4zm0 16c-4.411 0-8-3.589-8-8 0-.691.098-1.359.264-2H5v1a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2h2a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2v-1h.736c.166.641.264 1.309.264 2 0 4.411-3.589 8-8 8z"></path>
                      </svg>
                      {{ article.articleThumbsUpCount }}
                    </el-button>
                  </el-tooltip>
                  <el-tooltip class="item" effect="dark" content="赞赏" placement="top-start">
                    <el-button type="text" style="font-size: 1.2rem;">
                      <svg width="24" height="24">
                        <path :d="buymeacoffee.path"></path>
                      </svg>
                      {{ article.articleSponsorCount }}
                    </el-button>
                  </el-tooltip>
                </el-col>
              </el-row>
            </div>
          </el-card>
        </el-col>
        <el-col>
          <comment-box :fetching="isFetching" :title="article.articleTitle"
                       :authorId="article.articleAuthorId" @gotoLogin="gotoLogin"></comment-box>
        </el-col>
      </el-row>
    </div>

    <!-- 可拖拽的分割线 -->
    <div 
      v-if="selectedInterviewId"
      class="resize-bar"
      @mousedown="startResize"
      :style="{ left: leftPanelWidth + '%' }">
      <div class="resize-handle">
        <i class="el-icon-more"></i>
      </div>
    </div>

    <!-- 右侧面试详情面板 -->
    <div 
      v-if="selectedInterviewId" 
      class="right-panel" 
      :style="{ width: rightPanelWidth + '%' }">
      <div class="interview-detail-header">
        <span class="interview-detail-title">面试记录详情</span>
        <el-button 
          type="text" 
          icon="el-icon-close" 
          @click="closeInterviewDetail"
          class="close-btn">
        </el-button>
      </div>
      <interview-modal :interviewId="selectedInterviewId"></interview-modal>
    </div>

    <!-- 编辑标签对话框 -->
    <el-dialog :visible.sync="dialogVisible">
      <edit-tags
        :idArticle="article.idArticle"
        :tags="article.articleTags"
        @closeDialog="closeTagsDialog">
      </edit-tags>
    </el-dialog>
  </div>
</template>

<script>
import Vue from 'vue';
import { mapState } from 'vuex';
import ShareBox from '~/components/widget/share';
import PortfoliosWidget from '~/components/widget/portfolios';
import EditTags from '~/components/widget/tags';
import InterviewModal from '~/components/interview-modal';
import 'vditor/dist/css/content-theme/light.css';
import { buymeacoffee } from "simple-icons"
import apiConfig from '~/config/api.config';

export default {
  name: "ArticleDetail",
  components: {
    ShareBox,
    PortfoliosWidget,
    EditTags,
    InterviewModal
  },
  validate({ params, store }) {
    return params.article_id && !isNaN(Number(params.article_id))
  },
  fetch() {
    let { store, params, error } = this.$nuxt.context
    return Promise.all([
      store
        .dispatch('article/fetchDetail', params)
        .catch(err => error({ statusCode: 404 }))
    ])
  },
  computed: {
    ...mapState({
      article: state => state.article.detail.data,
      isFetching: state => state.article.detail.fetching,
      loggedIn: state => state.auth.loggedIn,
      user: state => state.auth.user,
      avatar: state => state.auth.user?.avatarUrl
    }),
    hasPermissions() {
      let account = this.$store.state.auth.user?.nickname;
      if (account) {
        if (account === this.article.articleAuthor?.userNickname) {
          return true;
        }
      }
      return false;
    },
    isAdmin() {
      return this.$auth.hasScope('admin') || this.$auth.hasScope('blog_admin');
    },
    rightPanelWidth() {
      return 100 - this.leftPanelWidth;
    }
  },
  head() {
    return {
      title: this.article.articleTitle || 'SDU_MagiCode 模拟面试平台',
      meta: [
        {
          name: 'keywords',
          content: this.article.articleTags || 'RYMCU'
        },
        {
          name: 'description',
          content: this.article.articlePreviewContent
        },
        {
          name: 'site_name',
          content: 'RYMCU'
        },
        {
          name: 'url',
          content: this.article.articlePermalink
        },
        {
          name: 'og:title',
          content: this.article.articleTitle + ' - RYMCU'
        },
        {
          name: 'og:description',
          content: this.article.articlePreviewContent
        },
        {
          name: 'og:site_name',
          content: 'RYMCU'
        },
        {
          name: 'og:url',
          content: this.article.articlePermalink
        }
      ]
    }
  },
  data() {
    return {
      buymeacoffee,
      loading: false,
      isShare: false,
      dialogVisible: false,
      isPerfect: false,
      shareUrl: '',
      selectedInterviewId: null,
      leftPanelWidth: 60, // 默认宽度改为60%
      isResizing: false,
      startX: 0,
      startLeftWidth: 0
    }
  },
  
  methods: {
    // 缓动函数 - 使用更自然的弹性动画
    easeInOutCubic(t) {
      // 更平滑的缓动
      return t < 0.5
        ? 4 * t * t * t
        : 1 - Math.pow(-2 * t + 2, 3) / 2;
    },
    
    // 弹性缓动函数，用于弹性效果
    easeOutElastic(t) {
      const c4 = (2 * Math.PI) / 3;
      return t === 0
        ? 0
        : t === 1
        ? 1
        : Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
    },
    onRouter(name, data) {
      this.$router.push(
        {
          path: `/${name}/${data}`
        }
      )
    },
    handleCommand(item) {
      let _ts = this;
      if (item === 'edit') {
        _ts.$router.push({
          path: `/article/post/${_ts.article.idArticle}`
        })
      } else if (item === 'editTag') {
        _ts.$set(_ts, 'dialogVisible', true);
      } else {
        if (_ts.isShare) {
          _ts.$set(_ts, 'isShare', false);
        } else {
          if (_ts.user) {
            _ts.$axios.$get('/api/article/' + _ts.article.idArticle + '/share').then(function (res) {
              if (res) {
                _ts.$set(_ts, 'shareUrl', res);
                _ts.$set(_ts, 'isShare', true);
              }
            });
          } else {
            _ts.$set(_ts, 'shareUrl', _ts.article.articlePermalink);
            _ts.$set(_ts, 'isShare', true);
          }
        }
      }
    },
    gotoLogin() {
      this.$router.push({
        name: 'login',
        query: {
          historyUrl: window.location.href
        }
      })
    },
    closeTagsDialog() {
      this.$set(this, 'dialogVisible', false);
      this.$store.dispatch('article/fetchDetail', this.$route.params);
    },
    followUser(idUser) {
      let _ts = this;
      if (_ts.user) {
        _ts.$axios.$post('/api/follow', {
          followingId: idUser,
          followingType: 0
        }).then(function (res) {
          _ts.$set(_ts, 'isFollow', res);
          _ts.$store.dispatch('follow/fetchUserFollowingList');
        })
      } else {
        _ts.gotoLogin();
      }
    },
    cancelFollowUser(idUser) {
      let _ts = this;
      if (_ts.user) {
        _ts.$axios.$post('/api/follow/cancel-follow', {
          followingId: idUser,
          followingType: 0
        }).then(function (res) {
          _ts.$set(_ts, 'isFollow', res);
          _ts.$store.dispatch('follow/fetchUserFollowingList');
        })
      } else {
        _ts.gotoLogin();
      }
    },
    setPreference() {
      let _ts = this;
      _ts.$axios.$patch("/api/admin/article/update-perfect", {
        idArticle: _ts.article.idArticle,
        articlePerfect: '1',
      }).then(function (res) {
        if (res) {
          _ts.$set(_ts, 'isPerfect', true);
          _ts.$message.success("设置成功!");
        } else {
          _ts.$message.error("设置失败!");
        }
      })
    },
    cancelPreference() {
      let _ts = this;
      _ts.$axios.$patch("/api/admin/article/update-perfect", {
        idArticle: _ts.article.idArticle,
        articlePerfect: '0',
      }).then(function (res) {
        if (res) {
          _ts.$set(_ts, 'isPerfect', false);
          _ts.$message.success("取消成功!");
        } else {
          _ts.$message.error("设置失败!");
        }
      })
    },
    thumbsUp() {
      let _ts = this;
      _ts.$axios.$post('/api/article/thumbs-up', {
        idArticle: _ts.article.idArticle
      }).then(function (res) {
        if (res) {
          _ts.$message.success("点赞成功");
          _ts.$store.dispatch('article/updateThumbsUpCount', { thumbsUpNumber: res })
        }
      })
    },
    sponsor() {
      let _ts = this;
      _ts.$axios.$post('/api/article/sponsor', {
        dataType: '0',
        dataId: _ts.article.idArticle
      }).then(function (res) {
        if (res) {
          _ts.$message.success('赞赏成功');
          _ts.$store.dispatch('article/updateSponsorCount', { sponsorNumber: 1 })
        }
      })
    },
    // 获取分享的面试官信息
    async getInterviewer(interviewerId) {
      try {
        console.log(interviewerId)
        await this.$axios.$post(`/api/share/interviewShareToUser`, { interviewerId: interviewerId })
        this.$message.success('获取面试官成功')
        // 刷新文章数据
        await this.getArticle()
        this.$router.push({
          path: `/interviewer/interviewer`
        })
      } catch (err) {
        this.$message.error(err.response?.data?.message || '获取面试官失败')
      }
    },

    // 获取面试记录
    async getInterview(interviewId) {
      try {
        console.log(interviewId)
        await this.$axios.$post(`/api/share/chatShareToUser`, { chatId: interviewId })
        this.$message.success('获取面试记录成功')
        // 刷新文章数据
        await this.getArticle()
        this.$router.push({
          path: `/chats/interviewContainer`
        })
      } catch (err) {
        this.$message.error(err.response?.data?.message || '获取面试记录失败')
      }
    },

    // 获取左侧面板样式
    getLeftPanelStyle() {
      if (this.selectedInterviewId) {
        // 有详情时的左侧面板样式
        return {
          width: this.leftPanelWidth + '%',
        }
      } else {
        // 无详情时使用默认样式，CSS会处理居中
        return {}
      }
    },

    // 查看面试详情
    viewInterviewDetail(interviewId) {
      if (this.selectedInterviewId === interviewId) {
        this.closeInterviewDetail();
      } else {
        // 获取当前左侧面板宽度，用于动画过渡
        const currentLeftWidth = this.leftPanelWidth;
        const targetLeftWidth = window.innerWidth <= 768 ? 100 : 50;
        
        // 先设置ID，让CSS类生效
        this.selectedInterviewId = interviewId;
        
        // 执行平滑的宽度过渡动画
        const startTime = Date.now();
        const duration = 300; // 动画持续300毫秒
        
        const animateWidth = () => {
          const elapsed = Date.now() - startTime;
          const progress = Math.min(elapsed / duration, 1);
          // 使用更平滑的动画曲线
          const easeProgress = this.easeOutElastic(progress);
          
          // 根据进度计算当前宽度
          this.leftPanelWidth = currentLeftWidth + (targetLeftWidth - currentLeftWidth) * easeProgress;
          
          if (progress < 1) {
            requestAnimationFrame(animateWidth);
          } else {
            // 动画完成后，确保内容不超出
            this.adjustContentOverflow();
          }
        };
        
        // 启动动画
        requestAnimationFrame(animateWidth);
        
        // 在移动设备上自动滚动到详情页
        if (window.innerWidth <= 768) {
          this.$nextTick(() => {
            const rightPanel = document.querySelector('.right-panel');
            if (rightPanel) {
              rightPanel.scrollIntoView({ behavior: 'smooth' });
            }
          });
        }
      }
    },

    // 关闭面试详情面板
    closeInterviewDetail() {
      // 获取当前左侧面板宽度，用于动画过渡
      const currentLeftWidth = this.leftPanelWidth;
      const targetLeftWidth = 60; // 恢复默认宽度
      
      // 为面板添加关闭动画的类
      const rightPanel = document.querySelector('.right-panel');
      if (rightPanel) {
        rightPanel.classList.add('closing');
      }
      
      // 执行平滑的宽度过渡动画
      const startTime = Date.now();
      const duration = 300; // 动画持续300毫秒
      
      const animateWidth = () => {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / duration, 1);
        // 使用更平滑的动画曲线
        const easeProgress = this.easeOutElastic(progress);
        
        // 根据进度计算当前宽度
        this.leftPanelWidth = currentLeftWidth + (targetLeftWidth - currentLeftWidth) * easeProgress;
        
        if (progress < 1) {
          requestAnimationFrame(animateWidth);
        } else {
          // 动画完成后，清除面板ID
          this.selectedInterviewId = null;
          
          // 移除关闭动画类
          if (rightPanel) {
            rightPanel.classList.remove('closing');
          }
        }
      };
      
      // 启动动画
      requestAnimationFrame(animateWidth);
    },

    // 开始拖拽调整
    startResize(e) {
      this.isResizing = true;
      this.startX = e.clientX;
      this.startLeftWidth = this.leftPanelWidth;
      
      // 添加全局鼠标事件监听
      document.addEventListener('mousemove', this.handleResize);
      document.addEventListener('mouseup', this.stopResize);
      
      // 防止文本选择
      e.preventDefault();
    },

    // 处理拖拽调整
    handleResize(e) {
      if (!this.isResizing) return;
      
      const deltaX = e.clientX - this.startX;
      const containerWidth = window.innerWidth;
      const deltaPercent = (deltaX / containerWidth) * 100;
      
      let newLeftWidth = this.startLeftWidth + deltaPercent;
      
      // 限制宽度范围在 20% 到 80% 之间
      newLeftWidth = Math.max(20, Math.min(80, newLeftWidth));
      
      // 设置左侧面板宽度
      this.leftPanelWidth = newLeftWidth;
      
      // 添加一个CSS类来表示正在调整大小
      document.body.classList.add('resizing-layout');
    },

    // 停止拖拽调整
    stopResize() {
      this.isResizing = false;
      document.removeEventListener('mousemove', this.handleResize);
      document.removeEventListener('mouseup', this.stopResize);
      
      // 移除调整大小的CSS类
      document.body.classList.remove('resizing-layout');
      
      // 拖拽结束后检查是否需要调整以防止内容溢出
      this.$nextTick(() => {
        this.adjustContentOverflow();
      });
    },

    // 刷新文章数据
    async getArticle() {
      await this.$store.dispatch('article/fetchDetail', this.$route.params)
    },

    isFollower(idUser) {
      return this.$store.getters["follow/isFollower"](idUser)
    },
    
    // 处理窗口大小变化
    handleWindowResize() {
      // 如果是小屏幕且详情页已打开，调整布局
      if (window.innerWidth <= 768 && this.selectedInterviewId) {
        // 在移动设备上不需要左右分栏
        this.leftPanelWidth = 100;
      } else if (window.innerWidth > 768 && this.selectedInterviewId) {
        // 在大屏幕上恢复左右分栏
        this.leftPanelWidth = 50;
      }
      
      // 调整内容溢出
      this.$nextTick(() => {
        this.adjustContentOverflow();
      });
    },
    
    // 调整内容溢出问题
    adjustContentOverflow() {
      if (!this.selectedInterviewId) return;
      
      // 检查左侧面板内容是否溢出
      const leftPanel = document.querySelector('.left-panel');
      if (leftPanel) {
        const articleContent = leftPanel.querySelector('.article__item');
        if (articleContent && articleContent.scrollWidth > leftPanel.clientWidth) {
          // 如果内容溢出，增加左侧宽度并减少右侧宽度
          this.leftPanelWidth = Math.min(this.leftPanelWidth + 5, 60);
        }
      }
    }
  },
  beforeDestroy() {
    // 组件销毁时清理事件监听
    document.removeEventListener('mousemove', this.handleResize);
    document.removeEventListener('mouseup', this.stopResize);
    window.removeEventListener('resize', this.handleWindowResize);
  },
  mounted() {
  let _ts = this;
  _ts.$store.commit('setActiveMenu', 'articleDetail');
  
  // 监听窗口大小变化
  window.addEventListener('resize', this.handleWindowResize);
  
  // 初始化布局
  this.handleWindowResize();
  
  Vue.nextTick(() => {
    const previewElement = document.getElementById("articleContent");
    Vue.VditorPreview.codeRender(previewElement, 'zh_CN');
    Vue.VditorPreview.highlightRender({
      "enable": true,
      "lineNumber": true,
      "style": "github"
    }, previewElement, apiConfig.VDITOR);
    Vue.VditorPreview.mathRender(previewElement, {
      math: { "engine": "KaTeX", "inlineDigit": false, "macros": {} }, cdn: apiConfig.VDITOR
    });
    Vue.VditorPreview.mermaidRender(previewElement, apiConfig.VDITOR);
    Vue.VditorPreview.graphvizRender(previewElement, apiConfig.VDITOR);
    Vue.VditorPreview.chartRender(previewElement, apiConfig.VDITOR);
    Vue.VditorPreview.mindmapRender(previewElement, apiConfig.VDITOR);
    Vue.VditorPreview.abcRender(previewElement, apiConfig.VDITOR);
    Vue.VditorPreview.mediaRender(previewElement);
    Vue.VditorPreview.lazyLoadImageRender(previewElement);
    
    previewElement.addEventListener("click", (event) => {
      if (event.target.tagName === "IMG") {
        Vue.VditorPreview.previewImage(event.target);
      }
    });
    _ts.$set(_ts, 'isPerfect', _ts.article.articlePerfect === '1')
  })
  console.log(_ts.article);
}
}
</script>

<style scoped>
.article-container {
  display: flex;
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden; /* 改回hidden以控制布局 */
  justify-content: center; /* 默认居中 */
}

.article-container.with-detail {
  justify-content: flex-start; /* 有详情时靠左对齐 */
}

.left-panel {
  flex-shrink: 0;
  overflow-y: auto;
  padding: 0 10px;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); /* 匹配右面板的过渡效果 */
  margin: 0 auto;
  width: 60%;
  will-change: transform, width; /* 提示浏览器优化动画 */
  backface-visibility: hidden; /* 减少闪烁 */
}

.right-panel {
  position: absolute;
  right: 0;
  top: 0;
  height: 100vh;
  flex-shrink: 0;
  border-left: 1px solid #e8e8e8;
  background: white;
  overflow-y: hidden;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); /* 使用更平滑的贝塞尔曲线 */
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.05);
  z-index: 10; /* 确保右侧面板在左侧面板之上 */
  overflow-x: hidden; /* 防止内容超出宽度 */
  transform: translateX(0); /* 默认位置 */
  will-change: transform, opacity, width; /* 提示浏览器优化动画 */
  backface-visibility: hidden; /* 减少闪烁 */
  -webkit-font-smoothing: subpixel-antialiased;
}

/* 添加进入和离开动画 */
.article-container:not(.with-detail) .right-panel {
  transform: translateX(110%); /* 隐藏时向右移动更远一点 */
  opacity: 0;
  pointer-events: none; /* 隐藏时不接受鼠标事件 */
  will-change: transform, opacity; /* 提示浏览器优化动画 */
}

/* 关闭动画 */
.right-panel.closing {
  transform: translateX(100%);
  opacity: 0;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.5s ease;
}

.resize-bar {
  position: absolute;
  top: 0;
  width: 6px;
  height: 100%;
  cursor: col-resize;
  z-index: 1000;
  user-select: none;
  transform: translateX(-3px);
  transition: opacity 0.3s cubic-bezier(0.16, 1, 0.3, 1), 
              background-color 0.3s cubic-bezier(0.16, 1, 0.3, 1),
              width 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); /* 更加平滑的过渡效果 */
  background-color: transparent;
  /* 必须确保调整栏准确地显示在左侧面板的右边缘 */
  pointer-events: auto;
}

.resize-bar:hover {
  background-color: rgba(64, 158, 255, 0.1);
  width: 8px;
}

.resize-handle {
  width: 100%;
  height: 40px;
  background: #e8e8e8;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0.6;
}

.resize-bar:hover .resize-handle {
  opacity: 1;
}

.resize-handle:hover {
  background: #409eff;
}

.resize-handle i {
  color: #666;
  font-size: 12px;
  transform: rotate(90deg);
}

.resize-handle:hover i {
  color: white;
}

.interview-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #f8f9fa;
  position: sticky;
  top: 0;
  z-index: 100;
  margin-bottom: 0;
}

.interview-detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.close-btn {
  font-size: 16px;
  padding: 0;
}

/* 移除了interview-detail-content样式，现在直接使用InterviewModal组件的滚动 */

/* 面试记录卡片样式优化 */
.interview-desc {
  color: #606266;
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.4;
}

.interviewer-name {
  color: #909399;
  font-size: 12px;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .article-container {
    flex-direction: column;
    height: auto;
  }
  
  .left-panel {
    width: 100% !important;
    max-width: 100% !important;
    padding: 0 10px;
    margin: 0 !important;
    float: none !important;
  }
  
  .right-panel {
    position: static; /* 在移动设备上使用静态定位 */
    width: 100% !important;
    height: auto;
    min-height: 500px;
    border-left: none;
    border-top: 1px solid #e8e8e8;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  }
  
  .resize-bar {
    display: none;
  }
  
  .article-container.with-detail {
    justify-content: flex-start;
  }
  
  .interview-detail-header {
    position: sticky;
    top: 0;
    z-index: 100;
  }
}

/* 防止在拖拽时选中文本 */
.article-container.resizing {
  user-select: none;
}

.article-container.resizing * {
  pointer-events: none;
}

.article-container.resizing .resize-bar {
  pointer-events: auto;
}

/* 添加左侧面板在拖拽时的样式 */
.article-container.with-detail .left-panel {
  margin: 0;
  float: left;
  height: 100vh;
  overflow-y: auto;
  word-break: break-word; /* 确保文字换行而不是溢出 */
  overflow-x: hidden; /* 防止水平溢出 */
  max-width: 100%; /* 确保在移动设备上适应宽度 */
}

/* 过渡动画 */
.left-panel, .right-panel {
  transition: width 0.4s cubic-bezier(0.25, 1, 0.5, 1), 
              margin 0.4s cubic-bezier(0.25, 1, 0.5, 1), 
              float 0.4s cubic-bezier(0.25, 1, 0.5, 1), 
              transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); /* 使用弹性贝塞尔曲线 */
}

/* 拖拽时的特殊样式 */
body.resizing-layout {
  cursor: col-resize !important;
}

body.resizing-layout * {
  user-select: none !important;
}

/* 确保文章内容可以适应宽度 */
.article__wrapper {
  width: 100%;
  overflow-x: hidden;
}

.article__item {
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
}

/* 滚动条样式优化 */
.left-panel::-webkit-scrollbar,
.right-panel::-webkit-scrollbar,
.interview-detail-content::-webkit-scrollbar {
  width: 6px;
}

.left-panel::-webkit-scrollbar-track,
.right-panel::-webkit-scrollbar-track,
.interview-detail-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.left-panel::-webkit-scrollbar-thumb,
.right-panel::-webkit-scrollbar-thumb,
.interview-detail-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.left-panel::-webkit-scrollbar-thumb:hover,
.right-panel::-webkit-scrollbar-thumb:hover,
.interview-detail-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 现代化按钮样式 */
.modern-btn {
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-weight: 500;
  letter-spacing: 0.3px;
  position: relative;
  overflow: hidden;
  padding-left: 15px;
  padding-right: 15px;
  border: none;
}

.modern-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.modern-btn:active {
  transform: translateY(0);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.modern-btn i {
  margin-right: 4px;
  font-size: 14px;
}

.view-btn {
  background-color: #f0f9ff;
  color: #409eff;
  border: 1px solid #d9ecff;
}

.view-btn:hover {
  background-color: #ecf5ff;
}

.view-btn.selected-btn {
  background-color: #409eff;
  color: white;
}

.get-btn {
  background: linear-gradient(135deg, #67C23A, #95D475);
  color: white;
}

.get-btn:hover {
  background: linear-gradient(135deg, #85CE61, #67C23A);
}

/* 为按钮添加点击涟漪效果 */
.modern-btn::after {
  content: '';
  display: block;
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
  background-image: radial-gradient(circle, #fff 10%, transparent 10.01%);
  background-repeat: no-repeat;
  background-position: 50%;
  transform: scale(10, 10);
  opacity: 0;
  transition: transform .5s, opacity 1s;
}

.modern-btn:active::after {
  transform: scale(0, 0);
  opacity: 0.3;
  transition: 0s;
}

/* 面试官按钮样式 */
.interviewer-btn {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: white;
}

.interviewer-btn:hover {
  background: linear-gradient(135deg, #66b1ff, #409eff);
}
</style>