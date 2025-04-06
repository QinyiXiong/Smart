<template>
  <div 
  class="background-layer" 
  :style="{ backgroundImage: `url(${require('@/assets/background.png')})` }"
>
  <el-row class="wrapper verify" style="background: url('a.png') no-repeat center center fixed;
   background-size: cover; padding-top: 10%; min-height: 100px;">
    <el-col :xs="24" :sm="24" :xl="24" class="verify-wrap flex-inline">
      <div class="login-form-container">
        
        <el-form :model="user" ref="user" status-icon  style="width: 400px;">
          <el-form-item style="text-align: center;" label-width="10px">
            <img src="~/assets/logo.svg" alt="logo" class="icon-rymcu">
          </el-form-item>

          <el-form-item style="text-align: center;" label="账号" prop="account" :rules="[{ required: true, message: '请输入账号', trigger: 'blur' }]">
            <el-input v-model="user.account" autocomplete="off"></el-input>
          </el-form-item>
          <el-form-item style="text-align: center;" label="密码" prop="password" :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]">
            <el-input type="password" v-model="user.password" autocomplete="off" show-password></el-input>
          </el-form-item>

          <!-- <el-form-item>
            <el-link rel="nofollow" style="float: right;" :underline="false" @click="forgetPassword">忘记密码</el-link>
          </el-form-item> -->
          <div style="height: 50px;"></div>

          <el-form-item>
            <el-button style="width: 60%; border-radius: 20px;" color="#66b1ff"  type="primary" @click="login" :loading="loginLoading" plain>立即登录</el-button>
            <el-button style="width: 32%; border-radius: 20px; background-color: white;" @click="register" plain>注册</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-col>


    <el-dialog
      title="找回密码"
      :visible.sync="forget"
      width="475px"
      :before-close="hideForgetPasswordDialog"
      center>
      <el-form :model="forgetForm" ref="forgetForm" status-icon label-width="100px"
               style="align-items: center;max-width: 375px;">
        <el-form-item label="邮箱" prop="email"
                      :rules="[
                  { required: true, message: '请输入邮箱地址', trigger: 'blur' },
                  { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }]">
          <el-input v-model="forgetForm.email" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item style="text-align: center;">
          <el-button :loading="loading" @click.native="sendEmailCode" type="success" plain>发送</el-button>
          <el-button :loading="loading" @click.native="hideForgetPasswordDialog" plain>取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </el-row>
  </div>
</template>

<script>
import {mapState} from 'vuex';

export default {
  name: "login",
  middleware: 'auth',
  auth: 'guest',
  data() {
    return {
      user: {
        account: '',
        password: ''
      },
      forgetForm: {
        email: ''
      },
      forget: false,
      loading: false,
      loginLoading: false,
      historyUrl: ''
    }
  },
  computed: {
    ...mapState({
      article: state => state.article.detail.data
    })
  },
  methods: {
    login() {
      let _ts = this;
      _ts.$refs.user.validate(async (valid) => {
        if (valid) {
          _ts.$set(_ts, 'loginLoading', true);
          let data = {
            account: _ts.user.account,
            password: _ts.user.password
          }
          try {
            let response = await _ts.$auth.loginWith('local', {data: data})
            console.log(response)
            if (response.success) {
              _ts.$auth.setUserToken(response.data.token, response.data.refreshToken);
              // if (_ts.historyUrl) {
              //   window.location.href = _ts.historyUrl
              // }
              await _ts.$router.push('/hot-posts');
            }
            _ts.$set(_ts, 'loginLoading', false);
          } catch (err) {
            _ts.$set(_ts, 'loginLoading', false);
            console.log(err)
          }
        } else {
          return false;
        }
      });
    },
    register() {
      this.$router.push(
        {
          name: 'register'
        }
      )
    },
    forgetPassword() {
      this.forget = true;
    },
    hideForgetPasswordDialog() {
      this.forget = false;
    },
    sendEmailCode() {
      let _ts = this;
      _ts.loading = true;
      let email = _ts.forgetForm.email;
      if (!email) {
        return false
      }
      let data = {
        email: email
      };
      _ts.$axios.$get('/api/console/get-forget-password-email', {
        params: data
      }).then(function (res) {
        _ts.loading = false;
        _ts.forget = false;
        if (res) {
          _ts.$message(res.message)
        }
      })
    }
  },
  mounted() {
    let _ts = this
    _ts.$store.commit('setActiveMenu', 'login');
    _ts.$set(_ts, 'historyUrl', _ts.$route.query.historyUrl || '');
    // if (_ts.$auth.loggedIn) {
    //   _ts.$router.push({
    //     name: 'index'
    //   })
    // }
  }
}
</script>

<style scoped>
.icon-rymcu {
  margin: 0 auto;
  display: block;
  height: 150px;
}

.background-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.wrapper.verify {
  position: relative; 
  z-index: 1; 
}

.verify .verify-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px; 
}

.flex-inline {
  display: flex;
  align-items: center;
}

.login-form-container {
  background: rgba(255, 255, 255, 0.95);
  padding:10px 60px 20px 40px;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 575px;
  max-height: 500px;
  min-height: 400px;
  margin: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center; /* 垂直居中 */
  align-items: center;
  position: relative; /* 确保它位于最上层 */
  z-index: 2;
}

.verify .intro {
  padding: 50px;
  color: #616161;
}

.intro-content {
  background: rgba(255, 255, 255, 0.85);
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.verify__sign {
  background-color: transparent !important;
}

.vditor-reset {
  font-variant-ligatures: no-common-ligatures;
  font-family: Helvetica Neue, Luxi Sans, DejaVu Sans, Tahoma, Hiragino Sans GB, Microsoft Yahei, sans-serif, Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, Segoe UI Symbol, Android Emoji, EmojiSymbols;
  word-wrap: break-word;
  overflow: auto;
  line-height: 1.5;
  font-size: 16px;
  word-break: break-word;
}
</style>