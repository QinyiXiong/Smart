<template>
  <el-container>
    <el-header>
      <header-view v-bind="$attrs" v-on="$listeners"/>
    </el-header>
    <el-main>
      <nuxt/>
    </el-main>
    <div v-if="isPostArticle && isShow" style="position: fixed;bottom: 10vh;right: 3vw;">
      <el-col style="padding-top: 1rem;" :xs="0" :xl="24">
        <el-button circle @click="backTop" icon="el-icon-caret-top"></el-button>
      </el-col>
    </div>
  </el-container>
</template>

<script>
import {mapState} from 'vuex';
import HeaderView from "./header";
import FooterView from "./footer";
import wx from "~/assets/weixin.png";

export default {
  name: "PcMain",
  components: {
    HeaderView,
    FooterView
  },
  props: [],

  computed: {
    ...mapState('global', []),
    isPostArticle() {
      if (this.$route.name === 'article-post-article_id') {
        return false;
      }
      return true;
    }
  },
    data() {
      return {
        isShow: false,
        wx: wx,
      }
    },
    methods: {
      backTop() {
        window.scrollTo(0, 0);
      },
      handleScroll() {
        let _ts = this;
        let scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
        if (scrollTop > 100) {
          _ts.$set(_ts, 'isShow', true);
        } else {
          _ts.$set(_ts, 'isShow', false);
        }
      }
    },
    mounted() {
      window.addEventListener('scroll', this.handleScroll, true);
    }
  }
</script>

<style scoped>
  .el-header {
    /*padding-bottom: 1rem;*/
    /*background: #fff;*/
    border-bottom: 1px solid rgba(0, 40, 100, 0.12);
    z-index: 80;
  }

  .el-main {
    padding: 0;
    background-attachment: fixed;
    min-height: 85vh;
    overflow-x: hidden;
  }

  .el-footer {
    position: relative;
    width: 100%;
    padding-top: 1.2rem;
    /*background: #fff;*/
    border-top: 1px solid rgba(0, 40, 100, 0.12);
    z-index: 80;
  }
</style>
