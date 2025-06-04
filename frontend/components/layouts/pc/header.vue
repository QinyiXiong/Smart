<template>
  <el-row :gutter="20" justify="space-between" type="flex">
    <el-col :md="4" :span="4" :xs="8">
      <a class="navbar-brand" href="/" rel="nofollow">
        <img alt="logo" class="navbar-brand-img" src="@/assets/little-logo.svg">
      </a>
    </el-col>
    <el-col :md="14" :span="10" :xs="0" style="max-height: 58px;overflow: hidden">
      <el-menu :default-active="activeMenu" @select="handleSelectMenu" mode="horizontal"
               style="margin-top: -2px;border: 0;">

       <!-- 模拟面试官主菜单 -->
       <el-submenu index="interview" class="modern-submenu">
        <template slot="title">
          <i class="el-icon-user" style="margin-right: 6px;"></i>
          模拟面试官
        </template>
        <el-menu-item index="interview-simulate" class="modern-menu-item">
          <i class="el-icon-video-camera" style="margin-right: 8px;"></i>
          模拟面试
        </el-menu-item>
        <el-menu-item index="interview-officer" class="modern-menu-item">
          <i class="el-icon-user-solid" style="margin-right: 8px;"></i>
          我的面试官
        </el-menu-item>
        <el-menu-item index="milvus-database" class="modern-menu-item">
          <i class="el-icon-data-board" style="margin-right: 8px;"></i>
          知识库管理
        </el-menu-item>
        <el-menu-item index="interview-record" class="modern-menu-item">
          <i class="el-icon-document" style="margin-right: 8px;"></i>
          面试记录
        </el-menu-item>
      </el-submenu>
      
      <!-- OJ主菜单 -->
      <el-submenu index="oj" class="modern-submenu">
        <template slot="title">
          <i class="el-icon-cpu" style="margin-right: 6px;"></i>
          OJ题库
        </template>
        <el-menu-item index="oj" class="modern-menu-item">
          <i class="el-icon-tickets" style="margin-right: 8px;"></i>
          题目列表
        </el-menu-item>
        <el-menu-item index="oj-add" class="modern-menu-item">
          <i class="el-icon-circle-plus" style="margin-right: 8px;"></i>
          添加题目
        </el-menu-item>
      </el-submenu>
      
      <!-- 社区主菜单 -->
      <el-submenu index="community" class="modern-submenu">
        <template slot="title">
          <i class="el-icon-s-comment" style="margin-right: 6px;"></i>
          社区
        </template>
        <el-menu-item index="hot-posts" class="modern-menu-item">
          <i class="el-icon-hot-water" style="margin-right: 8px;"></i>
          热门
        </el-menu-item>
        <el-menu-item index="my-follow" class="modern-menu-item">
          <i class="el-icon-star-on" style="margin-right: 8px;"></i>
          我的关注
        </el-menu-item>
        <el-menu-item index="portfolios" class="modern-menu-item">
          <i class="el-icon-collection" style="margin-right: 8px;"></i>
          作品集
        </el-menu-item>
      </el-submenu>

      </el-menu>
    </el-col>

    <el-col :md="10" :span="10" :xs="16" style="margin-top: 12px; position: relative;">
      <client-only>
        <el-col style="text-align: right;" v-if="loggedIn">
          <el-popover
            @show="handleShowPopover"
            placement="bottom"
            trigger="click"
            v-model="showPopover"
            width="420"
            popper-class="modern-search-popover">
            <div class="modern-search-container">
              <div class="search-wrapper">
                <el-input 
                  @keyup.enter.native="querySearchAsync" 
                  name="searchInput" 
                  placeholder="搜索文章, 作品集, 用户..."
                  v-model="queryString"
                  class="modern-search-input"
                  prefix-icon="el-icon-search">
                </el-input>
                <el-button @click="querySearchAsync" class="modern-search-btn" type="primary">
                  <i class="el-icon-search"></i>
                  搜索
                </el-button>
              </div>
            </div>
            <el-button circle size="small" slot="reference" class="modern-icon-btn">
              <i class="el-icon-search"></i>
            </el-button>

          </el-popover>

          <el-link :underline="false" href="/portfolio/post" rel="nofollow"
                   class="modern-nav-link">
            <i class="el-icon-edit-outline" style="margin-right: 4px;"></i>
            创建专栏
          </el-link>
          
          <el-link :underline="false" href="/article/post" rel="nofollow"
                   class="modern-nav-link">
            <i class="el-icon-document-add" style="margin-right: 4px;"></i>
            发帖
          </el-link>
          
          <el-link :underline="false" rel="nofollow" class="modern-nav-link">
            <el-dropdown @command="handleCommand" trigger="click" class="modern-dropdown">
              <el-badge :value="notificationNumbers" class="item">
                <el-button circle size="small" class="modern-icon-btn">
                  <i class="el-icon-bell"></i>
                </el-button>
              </el-badge>
              <el-dropdown-menu slot="dropdown" class="modern-notification-dropdown">
                <div class="notification-header">
                  <i class="el-icon-bell" style="margin-right: 8px;"></i>
                  <span>通知消息</span>
                </div>
                <div class="notification-list">
                  <el-dropdown-item 
                    :key="notification.idNotification" 
                    command="notification"
                    v-for="notification in notifications"
                    class="modern-notification-item">
                    <div class="notification-content">
                      <i class="el-icon-message" style="margin-right: 8px; color: #409EFF;"></i>
                      <span>{{ notification.dataSummary }}</span>
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item command="notification" class="view-all-item">
                    <i class="el-icon-more" style="margin-right: 8px;"></i>
                    查看所有消息
                  </el-dropdown-item>
                </div>
              </el-dropdown-menu>
            </el-dropdown>
          </el-link>
          <el-link :underline="false" rel="nofollow" style="margin-left: 10px;">
            <el-dropdown @command="handleCommand" trigger="click" class="modern-dropdown">
              <div class="user-avatar-container">
                <el-avatar :src="user.avatarUrl" size="small" v-if="user.avatarUrl"></el-avatar>
                <el-avatar size="small" src="https://static.rymcu.com/article/1578475481946.png" v-else></el-avatar>
              </div>
              <el-dropdown-menu slot="dropdown" class="modern-user-dropdown">
                <div class="user-info-header">
                  <el-avatar :src="user.avatarUrl" size="medium" v-if="user.avatarUrl"></el-avatar>
                  <el-avatar size="medium" src="https://static.rymcu.com/article/1578475481946.png" v-else></el-avatar>
                  <div class="user-details">
                    <span class="username">{{ user.nickname }}</span>
                    <span class="user-status">在线</span>
                  </div>
                </div>
                <div class="user-menu-list">
                  <el-dropdown-item command="user" class="modern-dropdown-item">
                    <i class="el-icon-user" style="margin-right: 8px;"></i>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="drafts" class="modern-dropdown-item">
                    <i class="el-icon-document" style="margin-right: 8px;"></i>
                    我的草稿
                  </el-dropdown-item>
                  <el-dropdown-item command="user-info" class="modern-dropdown-item">
                    <i class="el-icon-setting" style="margin-right: 8px;"></i>
                    设置
                  </el-dropdown-item>
                  <el-dropdown-item command="admin-dashboard" v-if="hasPermissions" class="modern-dropdown-item">
                    <i class="el-icon-s-tools" style="margin-right: 8px;"></i>
                    系统管理
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" class="modern-dropdown-item logout-item">
                    <i class="el-icon-switch-button" style="margin-right: 8px;"></i>
                    退出登录
                  </el-dropdown-item>
                </div>
              </el-dropdown-menu>
            </el-dropdown>
          </el-link>
        </el-col>
        
        <el-col style="text-align: right;" v-else>
          <el-popover
            @show="handleShowPopover"
            placement="bottom"
            trigger="click"
            v-model="showPopover"
            width="420"
            popper-class="modern-search-popover">
            <div class="modern-search-container">
              <el-input 
                @keyup.enter.native="querySearchAsync" 
                name="searchInput" 
                placeholder="搜索文章, 作品集, 用户..."
                v-model="queryString"
                class="modern-search-input"
                prefix-icon="el-icon-search">
                <el-button @click="querySearchAsync" slot="append" class="modern-search-btn">
                  搜索
                </el-button>
              </el-input>
            </div>
            <el-button circle size="small" slot="reference" class="modern-icon-btn">
              <i class="el-icon-search"></i>
            </el-button>
          </el-popover>
          
          <el-link :underline="false" @click="login" rel="nofollow" class="modern-auth-link">
            <i class="el-icon-user" style="margin-right: 4px;"></i>
            登录
          </el-link>
          
          <el-link :underline="false" href="/register" rel="nofollow" class="modern-auth-link">
            <i class="el-icon-user-solid" style="margin-right: 4px;"></i>
            注册
          </el-link>
        </el-col>
      </client-only>
    </el-col>
  </el-row>
</template>

<script>
import { mapState } from 'vuex';
// import sockClient from '~/plugins/sockjs';

export default {
  name: "PcHeader",
  computed: {
    ...mapState({
      activeMenu: state => state.activeMenu,
      user: state => state.auth.user,
      loggedIn: state => state.auth.loggedIn
    }),
    hasPermissions() {
      return this.$auth.hasScope('admin') || this.$auth.hasScope('blog_admin');
    }
  },
  data() {
    return {
      queryString: '',
      timeout: null,
      show: false,
      notifications: [],
      notificationNumbers: "",
      showPopover: false,
      autofocus: false
    };
  },
  watch: {
    user: function () {
      this.getUnreadNotifications();
    }
  },

  methods: {
    querySearchAsync() {
      this.$router.push({
        path: `/search?q=${this.queryString}`
      })
      this.$set(this, 'showPopover', false);
      this.$set(this, 'queryString', '');
    },
    handleShowPopover() {
      setTimeout(function () {
        document.getElementsByName("searchInput")[0].focus()
      }, 500);
    },
    handleSelectMenu(item) {
      let _ts = this;
      switch (item) {
        case 'topic':
          _ts.$router.push({
            path: '/topic/news?page=1'
          })
          break;
        case 'portfolios':
          _ts.$router.push({
            path: '/portfolios?page=1'
          })
          break;
        case 'interview-simulate':
          _ts.$router.push({
            path: '/chats/interviewContainer'
          })
          break;
        case 'milvus-database':
          _ts.$router.push({
            path: '/milvusDatabase/database'
          })
          break;
        case 'interview-officer':
          _ts.$router.push({
            path: '/interviewer/interviewer'
          })
          break;
        case 'products':
          _ts.$router.push({
            path: '/products?page=1'
          })
          break;
        case 'github':
          window.open("https://github.com/rymcu");
          break;
        case 'taobao':
          window.open("https://rymcu.taobao.com?utm_source=rymcu.com");
          break;
        case 'open-data':
          _ts.$router.push({
            path: '/open-data'
          })
          break;
        case 'oj':
          _ts.$router.push({
            path: '/oj'
          })
          break;
        case 'oj-add':
          _ts.$router.push({
            path: '/oj/add'
          })
          break;
        default:
          _ts.$router.push(
            {
              path: '/'
            }
          )
      }
    },
    handleCommand(item) {
      let _ts = this;
      switch (item) {
        case 'user':
          _ts.$router.push({
            path: '/user/' + _ts.user.account
          })
          break;
        case 'user-info':
          _ts.$router.push({
            path: '/user/settings/account'
          })
          break;
        case 'yuumi':
          _ts.$router.push({
            path: '/chats/App'
          })
          break;
        case 'logout':
          _ts.$auth.logout()
          item = 'login';
          break;
        default:
          _ts.$router.push({
            name: item
          })
      }
    },
    getUnreadNotifications() {
      let _ts = this;
      if (_ts.user) {
        _ts.$axios.$get('/api/notification/unread').then(function (res) {
          if (res) {
            _ts.$set(_ts, 'notifications', res.list);
            _ts.$set(_ts, 'notificationNumbers', res.total === 0 ? "" : res.total);
          }
        })
      }
    },
    login() {
      this.$router.push({
        path: '/login',
        query: {
          historyUrl: window.location.href
        }
      })
    },
    browserFingerprint() {
      let _ts = this
      let canvas = document.createElement('canvas');
      let ctx = canvas.getContext('2d');
      let txt = 'https://rymcu.com/';
      ctx.textBaseline = "top";
      ctx.font = "14px 'Arial'";
      ctx.textBaseline = "rymcu";
      ctx.fillStyle = "#f60";
      ctx.fillRect(125, 1, 62, 20);
      ctx.fillStyle = "#069";
      ctx.fillText(txt, 2, 15);
      ctx.fillStyle = "rgba(102, 204, 0, 0.7)";
      ctx.fillText(txt, 4, 17);
      let b64 = canvas.toDataURL().replace("data:image/png;base64,", "");
      let bin = atob(b64);
      let fingerprint = _ts.bin2hex(bin.slice(-16, -12));
      _ts.$store.commit('setFingerprint', fingerprint);
    },
    bin2hex(str) {
      let _ts = this
      let result = "";
      for (let i = 0; i < str.length; i++) {
        let c = str.charCodeAt(i);
        // 高字节
        result += _ts.byte2Hex(c >> 8 & 0xff);
        // 低字节
        result += _ts.byte2Hex(c & 0xff);
      }
      return result;
    },
    byte2Hex(b) {
      if (b < 0x10) {
        return "0" + b.toString(16);
      } else {
        return b.toString(16);
      }
    }
  },
  mounted() {
    let _ts = this;
    let user = _ts.user;
    if (user) {
      _ts.getUnreadNotifications();
      _ts.$store.dispatch('follow/fetchUserFollowerList');
      _ts.$store.dispatch('follow/fetchUserFollowingList');
      // sockClient.initSocket(this.$store.state.auth.user);
    }
    let fingerprint = _ts.$store.state.fingerprint
    if (!fingerprint) {
      _ts.browserFingerprint();
    }
  }

}
</script>

<style scoped>
.navbar-brand {
  color: inherit;
  margin-right: 1rem;
  font-size: 1.25rem;
  white-space: nowrap;
  font-weight: 600;
  padding: 0;
  transition: .3s opacity;
  line-height: 3rem;
}

.navbar-brand-img {
  height: 45px;
  object-fit: cover;
  width: auto;
  margin-top: 10px;
  margin-left: 50px;
  outline: none;
}

.el-menu {
  background-color: transparent !important;
}

/* 现代化菜单样式 - 增加字体大小和间距 */
.modern-submenu >>> .el-submenu__title {
  font-weight: 600;
  color: #333;
  font-size: 16px;
  padding: 0 24px;
  transition: all 0.3s ease;
  height: 60px;
  line-height: 60px;
}

.modern-submenu >>> .el-submenu__title:hover {
  color: #409EFF;
  background-color: rgba(64, 158, 255, 0.05);
}

/* 下拉菜单容器增加间距 */
.modern-submenu >>> .el-menu--popup {
  padding: 12px 0;
  min-width: 200px;
  border-radius: 12px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12) !important;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.modern-menu-item {
  font-weight: 500;
  font-size: 15px;
  transition: all 0.3s ease;
  padding: 0 20px !important;
  height: 48px !important;
  line-height: 48px !important;
  margin: 4px 8px !important;
  border-radius: 8px;
}

.modern-menu-item:hover {
  background-color: rgba(64, 158, 255, 0.08) !important;
  color: #409EFF !important;
  transform: translateX(4px);
}

/* 现代化搜索弹出框 */
.modern-search-container {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 12px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.search-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.modern-search-input {
  flex: 1;
}

.modern-search-input >>> .el-input__inner {
  border-radius: 25px;
  border: 2px solid #e4e7ed;
  transition: all 0.3s ease;
  font-size: 14px;
  padding-left: 45px;
  height: 40px;
}

.modern-search-input >>> .el-input__inner:focus {
  border-color: #409EFF;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.modern-search-btn {
  border-radius: 20px !important;
  padding: 10px 20px !important;
  font-weight: 500;
  background: rgba(64, 158, 255, 0.1);
  border: 1px solid rgba(64, 158, 255, 0.2);
  color: #409EFF;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.modern-search-btn:hover {
  background: #409EFF;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 现代化图标按钮 */
.modern-icon-btn {
  background: rgba(64, 158, 255, 0.1);
  border: 1px solid rgba(64, 158, 255, 0.2);
  color: #409EFF;
  transition: all 0.3s ease;
}

.modern-icon-btn:hover {
  background: #409EFF;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 现代化导航链接 */
.modern-nav-link {
  padding: 5px 18px;
  border-radius: 20px;
  margin: 0 6px;
  
  color: #606266;
  font-weight: 500;
  font-size: 15px;
  transition: all 0.3s ease;
}

.modern-nav-link:hover {
  background: rgba(64, 158, 255, 0.1);
  color: #409EFF;
  transform: translateY(-1px);
}

.modern-auth-link {
  padding: 10px 18px;
  border-radius: 20px;
  margin: 0 6px;
  color: #606266;
  font-weight: 500;
  font-size: 15px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.modern-auth-link:hover {
  background: linear-gradient(135deg, #409EFF 0%, #667eea 100%);
  color: white;
  border-color: #409EFF;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 现代化用户头像容器 */
.user-avatar-container {
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.user-avatar-container:hover {
  border-color: #409EFF;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

/* 现代化通知下拉框 - 增加间距和字体 */
.modern-notification-dropdown {
  min-width: 320px;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  border: none;
  overflow: hidden;
  padding: 0;
}

.notification-header {
  padding:16px 24px;
  background: linear-gradient(135deg, #7cb7ffb9 0%, #99aaff 100%);
  color: white;
  font-weight: 600;
  font-size: 17px;
  margin: 0;
}

.notification-list {
  background: white;
  padding: 8px 0;
}

.modern-notification-item {
  padding: 16px 24px;
  transition: all 0.3s ease;
  border-bottom: 1px solid #f5f7fa;
  margin: 2px 8px;
  border-radius: 8px;
}

.modern-notification-item:hover {
  background: rgba(64, 158, 255, 0.05);
  transform: translateX(4px);
}

.notification-content {
  display: flex;
  align-items: center;
  font-size: 15px;
  color: #606266;
  }

.view-all-item {
  padding: 8px 24px;
  background: #f8f9fa;
  color: #409EFF;
  font-weight: 600;
  font-size: 15px;
  text-align: center;
  transition: all 0.3s ease;
  margin: 0px;
  border-top: 1px solid #ebeef5;
  border-radius: 8px;
}

.view-all-item:hover {
  background: rgba(64, 158, 255, 0.1);
  transform: translateY(-2px);
}

/* 现代化用户下拉框 - 增加间距和字体 */
.modern-user-dropdown {
  min-width: 280px;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  border: none;
  overflow: hidden;
  padding: 0;
}

.user-info-header {
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  display: flex;
  align-items: center;
  margin: 0;
}

.user-menu-list {
  background: white;
  padding: 8px 0;
}

.user-details {
  margin-left: 16px;
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.user-status {
  font-size: 13px;
  color: #67c23a;
  font-weight: 500;
}

.modern-dropdown-item {
  padding: 16px 24px;
  transition: all 0.3s ease;
  color: #606266;
  font-size: 15px;
  font-weight: 500;
  margin: 2px 8px;
  border-radius: 8px;
}

.modern-dropdown-item:hover {
  background: rgba(64, 158, 255, 0.05);
  color: #409EFF;
  transform: translateX(4px);
}

.logout-item {
  color: #f56c6c;
  border-top: 1px solid #ebeef5;
  margin-top: 8px;
  padding-top: 16px;
}

.logout-item:hover {
  background: rgba(245, 108, 108, 0.05);
  color: #f56c6c;
}

</style>

<style>
.modern-search-popover {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15) !important;
  padding: 0 !important;
  overflow: hidden;
}

.modern-search-popover .el-popover__reference-wrapper {
  display: inline-block;
}

/* 增加子菜单项间距 */
.el-menu--popup .el-menu-item {
  margin: 2px 4px !important;
  border-radius: 8px !important;
  font-size: 15px !important;
  font-weight: 500 !important;
  padding: 0 10px !important;
  height: 48px !important;
  line-height: 48px !important;
}

.el-menu--popup .el-menu-item:hover {
  background-color: rgba(64, 158, 255, 0.08) !important;
  color: #409EFF !important;
  transform: translateX(4px) !important;
}

/* 主菜单标题样式增强 */
.el-submenu .el-submenu__title {
  font-size: 16px !important;
  font-weight: 600 !important;
  padding: 0 24px !important;
  height: 60px !important;
  line-height: 60px !important;
}

/* 响应式布局调整 */
@media screen and (max-width: 768px) {
  .modern-nav-link, .modern-auth-link {
    padding: 5px 10px;
    margin: 0 3px;
    font-size: 14px;
  }
  
  .modern-icon-btn {
    margin-right: 5px;
  }
  
  .el-col[style*="text-align: right"] {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    flex-wrap: wrap;
  }
}

/* 超小屏幕适配 */
@media screen and (max-width: 576px) {
  .navbar-brand-img {
    margin-left: 10px;
  }
  
  .modern-nav-link, .modern-auth-link {
    padding: 5px 8px;
    margin: 0 2px;
    font-size: 13px;
  }
  
  .el-col[style*="text-align: right"] {
    position: fixed;
    top: 0;
    right: 0;
    padding: 10px;
    background: white;
    z-index: 1000;
  }
  
  .modern-icon-btn {
    transform: scale(0.9);
  }
}
</style>