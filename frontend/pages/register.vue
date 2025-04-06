<template>
  <div class="background-layer" :style="{ backgroundImage: `url(${require('@/assets/background.png')})` }">
    <div class="register-container">
      <el-row :gutter="20" class="wrapper">
        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" class="form-col">
          <div class="form-wrapper">
            <el-form :model="user" ref="user" status-icon>
              <el-form-item style="text-align: center;" label-width="10px">
                <img src="~/assets/logo.svg" alt="RYMCU" class="icon-rymcu">
              </el-form-item>

              <el-form-item style="text-align: center;" label="邮箱" prop="email"
                :rules="[
                  { required: true, message: '请输入邮箱地址', trigger: 'blur' },
                  { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }]">
                <el-input v-model="user.email" autocomplete="off"></el-input>
              </el-form-item>
              
              <el-form-item style="text-align: center;" label="密码" prop="password"
                :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]">
                <el-input type="password" v-model="user.password" autocomplete="off" show-password></el-input>
              </el-form-item>
              
              <el-form-item style="text-align: center;" label="确认密码" prop="confirmPassword"
                :rules="[{ required: true, message: '请输入确认密码', trigger: 'blur' }]">
                <el-input type="password" v-model="user.confirmPassword" autocomplete="off" show-password></el-input>
              </el-form-item>
              
              <el-form-item style="text-align: center;" label="验证码" prop="code"
                :rules="[{ required: true, message: '请输入验证码', trigger: 'blur' }]">
                <el-input v-model="user.code" maxlength="6" autocomplete="off">
                  <el-button type="email" size="small" slot="append" @click="sendCode" :loading="loading" plain>
                    {{ loadText }}
                  </el-button>
                </el-input>
              </el-form-item>
              <div style="height: 30px;"></div>
              <el-form-item style="text-align: center;">
                <el-button style="width: 60%; border-radius: 20px;" type="primary" @click="register" :loading="registerLoading" plain>
                  立即注册
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-col>

        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" class="intro-col">
          <div class="intro-wrapper vditor-reset">
            <h2>欢迎来到山灵智码</h2>
            <p><a rel="nofollow" href="/">山灵智码</a> 是一个基于大模型的模拟面试系统，给予用户最个性化的面试方案。</p>
            <p>我们也构建一个小型社区。供大家相互<strong>分享</strong>，以<em>平等 • 自由 • 奔放</em>的价值观进行交流讨论。最终，希望大家能够找到与自己志同道合的伙伴，共同成长。</p>
            <p>最后请大家共同爱护这个<i>自由</i>的交流环境，相信这里一定是你注册过的所有社区中用户体验最好的 😍</p>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
export default {
  name: "register",
  data() {
    return {
      user: {
        email: '',
        code: '',
        password: '',
        confirmPassword: ''
      },
      registerLoading: false,
      loading: false,
      loadText: '发送验证码',
      timeClock: null
    }
  },
  methods: {
    sendCode() {
      let _ts = this;
      _ts.timerHandler();
      let email = _ts.user.email;
      if (!email) {
        return false
      }
      let data = {
        email: email
      }
      _ts.$axios.$get('/api/console/get-email-code', {
        params: data
      }).then(response => {
        if (response.message) {
          _ts.$message(response.message);
        }
      }).catch(error => {
        console.log(error);
        _ts.$message("邮件发送失败,请检查邮箱是否正确!");
      })
    },
    timerHandler() {
      let _ts = this;
      _ts.$set(_ts, 'loading', true);
      let times = 30;
      _ts.timeClock = setInterval(function () {
        times--;
        _ts.$set(_ts, 'loadText', times + ' s');
        if (times == 0) {
          clearInterval(_ts.timeClock);
          _ts.$set(_ts, 'loading', false);
          _ts.$set(_ts, 'loadText', '发送验证码');
        }
      }, 1000)
    },
    register() {
      let _ts = this;
      _ts.$refs.user.validate((valid) => {
        if (valid) {
          _ts.$set(_ts, 'registerLoading', true);
          setTimeout(function () {
            _ts.$set(_ts, 'registerLoading', false);
          }, 10000);
          let data = {
            email: _ts.user.email,
            password: _ts.user.password,
            code: _ts.user.code
          }
          _ts.$axios.$post('/api/console/register', data).then(function (res) {
            _ts.$set(_ts, 'registerLoading', false);
            if (res) {
              _ts.$message("注册成功！");
              _ts.$router.push(
                {
                  name: 'login'
                }
              )
            }
          })
        } else {
          return false;
        }
      });
    },
    login() {
      this.$router.push(
        {
          name: 'login'
        }
      )
    }
  },
  mounted() {
    this.$store.commit('setActiveMenu', 'register')
  }
}
</script>

<style scoped>
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

.register-container {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px 20px;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wrapper {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.form-col, .intro-col {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.form-wrapper, .intro-wrapper {
  background: rgba(255, 255, 255, 0.95);
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 500px;
}

.icon-rymcu {
  margin: 0 auto;
  display: block;
  height: 150px;
  margin-top: -30px;
  margin-bottom: -30px;
}

.form-wrapper >>> .el-form-item {
  margin-bottom: 10px; /* 原默认值可能是20px或更大 */
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

/* 响应式调整 */
@media (max-width: 992px) {
  .form-col, .intro-col {
    flex: 0 0 100%;
    max-width: 100%;
  }
  
  .intro-wrapper {
    margin-top: 30px;
  }
}

@media (max-width: 576px) {
  .form-wrapper, .intro-wrapper {
    padding: 30px 20px;
  }
  
  .icon-rymcu {
    height: 120px;
  }
}
</style>