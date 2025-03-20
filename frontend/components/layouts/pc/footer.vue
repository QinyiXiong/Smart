<template>
  <el-row class="footer">
  </el-row>
</template>

<script>
import wangBeiImage from '~/assets/wangbei.png'

export default {
  name: "PcFooter",
  data() {
    return {
      systemTitle: '\u7f57\u5409\u7f51\u0020\u002d\u0020\u5185\u5bb9\u5206\u4eab\u751f\u6001\u5e73\u53f0',
      systemName: 'RYMCU',
      systemUrl: 'https://rymcu.com',
      slogan: 'RYMCU · 嵌入式知识学习交流平台 ',
      beiAn: '赣ICP备2021003088号-1',
      markdownGuide: '/guides/markdown',
      rules: '/article/20',
      email: 'mailto:support@rymcu.com',
      aboutMe: '/article/115',
      github: 'https://github.com/rymcu',
      gitee: 'https://gitee.com/rymcu',
      currencyRule: '/rules/currency',
      wangBeiUrl: 'http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=36012302000055',
      wangBei: '赣公网安备 36012302000055号',
      wangBeiImage: wangBeiImage
    }
  }
}
</script>

<style scoped>
.footer {
  font-size: 14px;
  align-items: center;
  display: flex;
}

.footer .footer-link {
  margin-left: 2rem;
}
</style>
