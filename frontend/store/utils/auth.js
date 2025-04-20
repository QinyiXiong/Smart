export default {
    // 获取token
    getToken() {
      return localStorage.getItem('auth._token.local') || '';
    },

  // 检查token是否过期
  isTokenExpired() {
    const storedExpiration = localStorage.getItem('auth._token_expiration.local');
    if (storedExpiration) {
      const isExpired = Date.now() >= parseInt(storedExpiration, 10);
      if (isExpired) {
        console.warn('[Auth] Token expired (based on localStorage expiration)');
        return true;
      }
    }
    let token = this.getToken()
    console.log(token)
    if (token == "false") return true;
  },

    // 清除token
    clearToken() {
      localStorage.removeItem('auth._token.local');
      localStorage.removeItem('refreshToken');
    },

    // 保存token (用于登录成功后)
    saveToken(token, refreshToken) {
      localStorage.setItem('auth._token.local', token);
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      }
    }
  };
