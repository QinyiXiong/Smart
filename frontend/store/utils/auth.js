export default {
    // 获取token
    getToken() {
      return localStorage.getItem('token') || '';
    },
    
    // 检查token是否过期
    isTokenExpired() {
      const token = this.getToken();
      if (!token) return true;
      console.log(token);
      
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return Date.now() >= payload.exp * 1000;
      } catch (e) {
        return true;
      }
    },
    
    // 清除token
    clearToken() {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
    },
    
    // 保存token (用于登录成功后)
    saveToken(token, refreshToken) {
      localStorage.setItem('token', token);
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      }
    }
  };