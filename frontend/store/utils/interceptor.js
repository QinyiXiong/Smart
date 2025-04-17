// src/utils/axiosInstance.js
import axios from 'axios';
import auth from './auth'; 

// 创建axios实例
const instance = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL, // 从环境变量获取基础URL
  timeout: 10000 // 请求超时时间
});

instance.interceptors.request.use(
  config => {
    // 检查token是否过期
    if (auth.isTokenExpired()) {
      auth.clearToken();
      console.log("is here")
       // 如果是客户端环境，则跳转到登录页
    if (process.client) {
        // 使用 Nuxt 的 redirect 方法
        window.location.href = '/login'; // 或者使用 nuxt 的 router
        // 如果使用了 @nuxtjs/auth 模块，可以使用 this.$auth.logout() 和 this.$auth.redirect('login')
    }
      
      // 终止请求
      return Promise.reject(new Error('Token expired'));
    }
    
    const token = auth.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  error => {
    // 请求错误处理
    return Promise.reject(error);
  }
);

// /**
//  * 响应拦截器 (Vue 2 版本)
//  */
// instance.interceptors.response.use(
//   response => {
//     // 对响应数据做统一处理
//     return response.data;
//   },
//   error => {
//     // 处理HTTP错误状态码
//     if (error.response) {
//       switch (error.response.status) {
//         case 401:
//           auth.clearToken();
//           this.$router.push({
//             path: '/login',
//             query: { 
//               redirect: router.currentRoute.fullPath,
//               message: '身份验证失败，请重新登录'
//             }
//           });
//           break;
          
//         case 403:
//           // 处理权限不足的情况
//           break;
          
//         case 404:
//           // 处理API不存在的情况
//           break;
          
//         case 500:
//           // 处理服务器错误
//           break;
//       }
//     }
    
//     // 返回错误信息
//     return Promise.reject(error);
//   }
// );

export default instance;