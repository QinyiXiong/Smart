<template>
  <div class="interview-container">
    <!-- 左侧导航栏 -->
    <div class="left-sidebar" :class="{ 'collapsed': isSidebarCollapsed }">
      <div class="panel-header">
        <span>AI面试官</span>
        <el-button 
          class="collapse-btn" 
          type="text" 
          @click="toggleSidebar"
          :icon="isSidebarCollapsed ? 'el-icon-d-arrow-right' : 'el-icon-d-arrow-left'"
        ></el-button>
      </div>

      <!-- 切换标签 -->
      <div class="tab-switcher">
        <div class="custom-tabs">
          <div
            class="tab-button"
            :class="{ active: activeTab === 'interviewer' }"
            @click="handleTabSwitch('interviewer')"
          >
            <i class="el-icon-user-solid"></i>
            <span>面试官</span>
          </div>
          <div
            class="tab-button"
            :class="{ active: activeTab === 'record' }"
            @click="handleTabSwitch('record')"
          >
            <i class="el-icon-chat-dot-round"></i>
            <span>面试记录</span>
          </div>
          <div class="slider" :class="activeTab"></div>
        </div>
      </div>

      <!-- AI面试官列表 -->
      <div class="ai-interviewer-section" v-show="activeTab === 'interviewer'">
        <el-scrollbar class="no-horizontal-scroll" style="height:100%">
          <div class="interviewer-list">
            <div
              class="interviewer-list"
              v-for="interviewer in aiList"
              :key="interviewer.interviewerId"
              :class="['interviewer-item', { 'active': activeInterviewer === interviewer.interviewerId }]"
              @click="handleInterviewerSelect(interviewer.interviewerId)"
            >
              <div class="interviewer-avatar">
                <i class="el-icon-user-solid"></i>
              </div>
              <div class="interviewer-name">{{ interviewer.name }}</div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <!-- 聊天记录列表 -->
      <div class="chat-records-section" v-show="activeTab === 'record' && showChatRecords">
        <div class="section-header">
          <h3 class="section-title">话题</h3>
          <!-- <el-button
            type="primary"
            size="mini"
            @click="handleNewChat"
            v-if="currentInterviewer"
            icon="el-icon-plus"
            circle
          ></el-button> -->

        </div>
        <el-scrollbar style="height:calc(100% - 110px)" class="no-horizontal-scroll-1">
          <div class="chat-records-list">
            <div
              v-for="record in chatRecords"
              :key="record.chatId"
              :class="['chat-record-item', { 'active': activeChatRecord === record.chatId.toString() }]"
              @click="handleChatRecordSelect(record.chatId.toString())"
            >
              <div class="record-content">
                <i class="el-icon-chat-dot-round"></i>
                <span class="record-title">{{ record.topic || '未命名对话' }}</span>
              </div>

              <div class="record-actions">
                <el-popover
                  placement="right"
                  width="110"
                  trigger="hover"
                  :popper-class="'record-popover'"
                >
                  <div class="action-menu">
                    <div class="action-item" @click.stop="handleRenameRecord(record)">
                      <i class="el-icon-edit"></i>
                      <span>重命名</span>
                    </div>
                    <div class="action-item delete" @click.stop="handleDeleteRecord(record.chatId)">
                      <i class="el-icon-delete"></i>
                      <span>删除</span>
                    </div>
                  </div>
                  <i slot="reference" class="el-icon-more record-more"></i>
                </el-popover>
              </div>
            </div>
          </div>
        </el-scrollbar>
        <div class="new-chat-button-container" v-if="currentInterviewer">
          <button class="new-chat-button" @click="handleNewChat">
            <i class="el-icon-plus"></i>
            <span>新建面试对话</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="main-content-area">
      <chat-area
        :current-interviewer="currentInterviewer"
        :chat-record-id="activeChatRecord"
        @polling-completed="handlePollingCompleted"
        ref="chatArea"
      />
      
      <!-- 右侧评估tresultult区域 -->
      <div class="valuation-area" v-if="activeChatRecord && valuationData">
        <div class="valuation-header">
          <h3>面试评估结果</h3>
        </div>
        <div class="valuation-content">
          <div class="radar-chart-container" ref="radarChart">
            <!-- 雷达图将在这里渲染 -->
          </div>
          <div class="valuation-details">
            <div v-for="(item, index) in valuationData.valuationRanks" :key="index" class="valuation-item">
              <span class="valuation-name">{{ item.valuation.valuationName }}</span>
              <div class="valuation-progress">
                <div class="valuation-progress-bar" :style="{ width: item.rank + '%' }"></div>
              </div>
              <span class="valuation-score">{{ item.rank }}/100</span>
              <el-tooltip :content="item.valuation.valuationDescription" placement="left">
                <i class="el-icon-question"></i>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog
      title="重命名对话"
      :visible.sync="renameDialogVisible"
      width="30%"
      :close-on-click-modal="false"
    >
      <el-input v-model="newTopicName" placeholder="请输入新的话题名称"></el-input>
      <span slot="footer" class="dialog-footer">
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import ChatArea from './chatArea.vue'
import axios from '../../store/utils/interceptor'
import * as echarts from 'echarts'

export default {
  props: {
    value: {
      type: String,
      default: 'interviewer'
    }
  },
  components: {
    ChatArea
  },
  data() {
    return {
      aiList: [],
      chatRecords: [],
      activeInterviewer: null,
      activeChatRecord: null,
      currentInterviewer: null,
      showChatRecords: false,
      activeTab: this.value,
      renameDialogVisible: false,
      newTopicName: '',
      currentRecordToRename: null,
      valuationData: null,
      radarChart: null,
      isSidebarCollapsed: false,
      showSkeleton: false
    }
  },
  watch: {
    value(newVal) {
      this.activeTab = newVal;
    },
    activeChatRecord(newVal) {
      if (newVal) {
        this.fetchValuationData(newVal);
      } else {
        this.valuationData = null;
      }
    }
  },
  async mounted() {
    await this.fetchAiList()
    if(this.$route.query.formData){
      const formData = JSON.parse(this.$route.query.formData);
    }
  },
  
  updated() {
    // 当数据更新后，如果有评估数据，则初始化雷达图
    this.$nextTick(() => {
      if (this.valuationData && this.valuationData.valuationRanks && this.valuationData.valuationRanks.length > 0) {
        this.initRadarChart();
      }
    });
  },
  methods: {
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed;
    },
    async handlePollingCompleted() {
      try {
        if (!this.activeChatRecord) return;
        
        const res = await axios.get('/api/chat/getActions', {
          params: { chatId: this.activeChatRecord }
        });
        
        if (res.data && res.data.length > 0) {
          console.log('获取到的actions:', res.data);
          
          // 收集所有需要处理的评估变化
          const valuationChanges = [];
          const pageChanges = [];
          // 处理每个action
          for (const actionData of res.data) {
            try {
              // 解析action数据字符串
              const actionObj = JSON.parse(actionData);
              
              // 判断action类型
              if (actionObj.action === 'update_valuation' && 
                  actionObj.valuationName && 
                  actionObj.delta !== undefined) {
                
                // 保存评估变化信息
                valuationChanges.push({
                  valuationName: actionObj.valuationName,
                  delta: parseFloat(actionObj.delta)
                });
              }
              
              // 处理action171_push类型数据
              if (actionObj.action === 'push' && 
                  actionObj.chatId && 
                  actionObj.problemId) {
    
                // 调用chatArea组件的handleActionPush方法
                // 添加检查确保chatArea组件存在
                if (this.$refs.chatArea) {
               
                  this.$refs.chatArea.handleActionPush(actionObj.problemId);
                } else {
                  console.warn('chatArea组件未找到，无法调用handleActionPush方法');
                }
              }
              
            } catch (parseError) {
              console.error('解析action数据失败:', parseError);
            }
          }
          
          // 如果有评估变化，调用新的处理方法
          if (valuationChanges.length > 0) {
            await this.handleValuationUpdate(valuationChanges);
          }
        }
      } catch (error) {
        console.error('获取actions失败:', error);
        this.$message.error('获取面试动作失败');
      }
    },

    async handleValuationUpdate(valuationChanges) {
      // 保存当前评估数据的副本（如果存在）
      const originalValuationData = this.valuationData ? JSON.parse(JSON.stringify(this.valuationData)) : null;

      // 获取最新的评估数据
      await this.fetchValuationData(this.activeChatRecord);

      // 应用动画效果
      for (const change of valuationChanges) {
        // 找到对应的评估项
        const valuationItem = this.valuationData.valuationRanks.find(
          item => item.valuation.valuationName === change.valuationName
        );

        if (valuationItem) {
          // 查找原始数据中的对应项
          let originalValue = null;
          if (originalValuationData) {
            const originalItem = originalValuationData.valuationRanks.find(
              item => item.valuation.valuationName === change.valuationName
            );
            if (originalItem) {
              originalValue = originalItem.rank;
            }
          }

          // 如果找到原始值，使用原始值和当前值计算动画
          if (originalValue !== null) {
            // 创建动画效果 - 从原始值到当前值
            this.animateValuationChange(valuationItem, originalValue);

            // 如果有雷达图，也更新雷达图
            if (this.radarChart) {
              this.animateRadarChart(change.valuationName, originalValue);
            }
          }
        }
      }
    },
    
    // 动画显示评估值变化
    animateValuationChange(valuationItem, originalValue) {
      // 目标值是当前评估项的值
      const targetValue = valuationItem.rank;
      const delta = targetValue - originalValue;
      
      // 创建进度条元素的引用
      const valuationName = valuationItem.valuation.valuationName;
      const progressBars = document.querySelectorAll('.valuation-item');
      let targetBar = null;
      
      // 找到对应的进度条元素
      for (const bar of progressBars) {
        const nameElement = bar.querySelector('.valuation-name');
        if (nameElement && nameElement.textContent === valuationName) {
          targetBar = bar;
          break;
        }
      }
      
      if (!targetBar) return;
      
      const progressBar = targetBar.querySelector('.valuation-progress-bar');
      const scoreElement = targetBar.querySelector('.valuation-score');
      
      if (!progressBar || !scoreElement) return;
      
      // 添加高亮效果类
      const highlightClass = delta > 0 ? 'highlight-increase' : 'highlight-decrease';
      targetBar.classList.add(highlightClass);
      
      // 使用requestAnimationFrame实现平滑动画
      let startTime = null;
      const duration = 1000; // 动画持续1秒
      
      const animate = (timestamp) => {
        if (!startTime) startTime = timestamp;
        const elapsed = timestamp - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // 计算当前动画帧的值 - 从原始值到目标值的过渡
        const currentValue = originalValue + (delta * progress);
        
        // 更新UI
        progressBar.style.width = `${currentValue}%`;
    scoreElement.textContent = `${Math.round(currentValue)}/100`;
        
        // 继续动画或结束
        if (progress < 1) {
          requestAnimationFrame(animate);
        } else {
          // 动画结束后移除高亮效果
          setTimeout(() => {
            targetBar.classList.remove(highlightClass);
          }, 500);
        }
      };
      
      requestAnimationFrame(animate);
    },
    
    // 动画更新雷达图
    animateRadarChart(valuationName, originalValue) {
      if (!this.radarChart) return;
      
      // 获取当前雷达图配置
      const option = this.radarChart.getOption();
      const indicators = option.radar[0].indicator;
      const seriesData = option.series[0].data[0].value;
      
      // 找到对应指标的索引
      const index = indicators.findIndex(item => item.name === valuationName);
      if (index === -1) return;
      
      // 目标值是当前雷达图中的值
      const targetValue = seriesData[index];
      const delta = targetValue - originalValue;
      
      // 动画更新雷达图
      let startTime = null;
      const duration = 1000; // 与进度条动画同步
      
      const animate = (timestamp) => {
        if (!startTime) startTime = timestamp;
        const elapsed = timestamp - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // 计算当前动画帧的值 - 从原始值到目标值的过渡
        const currentValue = originalValue + (delta * progress);
        
        // 更新雷达图数据
        const newSeriesData = [...seriesData];
        newSeriesData[index] = currentValue;
        
        // 应用新数据
        this.radarChart.setOption({
          series: [{
            data: [{
              value: newSeriesData,
              name: '评分'
            }]
          }]
        });
        
        // 继续动画或结束
        if (progress < 1) {
          requestAnimationFrame(animate);
        }
      };
      
      requestAnimationFrame(animate);
    },
    async fetchAiList() {
      try {
        const res = await axios.get('/api/Interviewer/list')
        this.aiList = res.data || []
      } catch (error) {
        this.$message.error('获取面试官列表失败')
        console.error(error)
      }
    },
    async loadChatRecords() {
      try {
        const res = await axios.post('/api/chat/getChatRecords', {
          interviewerId: this.activeInterviewer
        })
        // 添加排序逻辑，按创建时间倒序
        this.chatRecords = (res.data || []).sort((a, b) => {
          return new Date(b.createdAt) - new Date(a.createdAt)
        })
      } catch (error) {
        this.$message.error('获取聊天记录失败')
        console.error(error)
      }
    },
    async handleNewChat() {
      try {
        const res = await axios.post('/api/chat/saveChatRecords', {
          interviewerId: this.activeInterviewer,
          userId: this.$store.state.user.userId
        })

        this.chatRecords.unshift(res.data)
        this.$message.success('新建对话成功')
        this.activeChatRecord = res.data.chatId.toString()
        
        // 清空评估数据并重新获取
        this.valuationData = null;
        if (this.radarChart) {
          this.radarChart.dispose();
          this.radarChart = null;
        }
        await this.fetchValuationData(res.data.chatId);
      } catch (error) {
        this.$message.error('新建对话失败')
        console.error(error)
      }
    },
    async handleInterviewerSelect(interviewerId) {
      this.activeInterviewer = interviewerId
      this.currentInterviewer = this.aiList.find(item => item.interviewerId === interviewerId)
      this.showChatRecords = true

      if (this.activeTab === 'record') {
        await this.loadChatRecords()
      }
    },
    async handleDeleteRecord(chatId) {
      try {
        await this.$confirm('确认删除该对话记录吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const res = await axios.post('/api/chat/deleteChatRecords', {
          chatId: chatId
        })

        this.$message.success('删除成功')
        // 删除后重新加载记录
        await this.loadChatRecords()
        // 如果删除的是当前激活的记录，则清空activeChatRecord和相关数据
        if (this.activeChatRecord === chatId.toString()) {
          this.activeChatRecord = null
          this.currentInterviewer = null
          this.valuationData = null
          if (this.radarChart) {
            this.radarChart.dispose()
            this.radarChart = null
          }
        }

      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败')
          console.error(error)
        }
      }
    },
    handleChatRecordSelect(chatId) {
      // 添加过渡效果
      this.$nextTick(() => {
        const valuationArea = this.$refs.valuationArea;
        if (valuationArea) {
          valuationArea.style.transition = 'opacity 0.3s ease';
          valuationArea.style.opacity = 0;
        }
        
        setTimeout(() => {
          // 清空评估数据
          this.valuationData = null;
          if (this.radarChart) {
            this.radarChart.dispose();
            this.radarChart = null;
          }
          this.activeChatRecord = chatId;
          
          // 强制重新获取评估数据并初始化雷达图
          this.fetchValuationData(chatId).then(() => {
            if (valuationArea) {
              valuationArea.style.opacity = 1;
            }
          });
        }, 300);
      });
    },
    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
    },
    handleTabSwitch(tab) {
      this.activeTab = tab;
      this.$emit('input', tab);
      this.$emit('change', tab);
      if (tab === 'record' && this.activeInterviewer) {
        this.loadChatRecords()
      }
    },
    handleRenameRecord(record) {
      this.currentRecordToRename = record;
      this.newTopicName = record.topic || '';
      this.renameDialogVisible = true;

      
    },
    async confirmRename() {
      if (!this.currentRecordToRename) return;

      try {
        const res = await axios.post('/api/chat/updateChatTopic', null, {
          params: {
            chatId: this.currentRecordToRename.chatId,
            newTopic: this.newTopicName
          }
        });

        this.$message.success('重命名成功');
        await this.loadChatRecords();
        this.renameDialogVisible = false;
      } catch (error) {
        this.$message.error('重命名失败');
        console.error(error);
      }
    },
    
    // 获取面试评估数据
    async fetchValuationData(chatId) {
      try {
        // 显示骨架屏
        this.showSkeleton = true;
        
        const res = await axios.post('/api/chat/getValutionByChatId', {
          chatId: parseInt(chatId)
        });
        
        if (res.data) {
          // 使用动画过渡
          this.$nextTick(() => {
            this.valuationData = res.data;
            this.initRadarChart();
            this.showSkeleton = false;
          });
        } else {
          this.valuationData = null;
          this.showSkeleton = false;
        }
      } catch (error) {
        console.error('获取评估数据失败', error);
        this.valuationData = null;
        this.showSkeleton = false;
      }
    },
    
    // 初始化雷达图
    initRadarChart() {
      if (!this.valuationData || !this.valuationData.valuationRanks || this.valuationData.valuationRanks.length === 0) {
        return;
      }
      
      // 销毁之前的图表实例
      if (this.radarChart) {
        this.radarChart.dispose();
      }
      
      // 确保DOM元素已经渲染
      if (!this.$refs.radarChart) {
        return;
      }
      
      // 初始化echarts实例
      this.radarChart = echarts.init(this.$refs.radarChart);
      
      // 准备雷达图数据
      const indicator = this.valuationData.valuationRanks.map(item => ({
        name: item.valuation.valuationName,
        max: 100
      }));
      
      const seriesData = [{
        value: this.valuationData.valuationRanks.map(item => item.rank), // 直接使用100分制数据
        name: '评分'
      }];
      
      // 配置雷达图选项
      const option = {
        tooltip: {
          trigger: 'item'
        },
        radar: {
          indicator: indicator,
          radius: '65%',
          splitNumber: 5,
          axisName: {
            color: '#606266',
            fontSize: 12
          },
          splitArea: {
            areaStyle: {
              color: ['#f5f7fa', '#e4e7ed', '#ebeef5', '#f2f6fc'],
              shadowColor: 'rgba(0, 0, 0, 0.05)',
              shadowBlur: 10
            }
          },
          axisLine: {
            lineStyle: {
              color: '#dcdfe6'
            }
          },
          splitLine: {
            lineStyle: {
              color: '#dcdfe6'
            }
          }
        },
        series: [{
          type: 'radar',
          data: seriesData,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: {
            width: 2,
            color: '#409eff'
          },
          areaStyle: {
            color: 'rgba(64, 158, 255, 0.2)'
          },
          itemStyle: {
            color: '#409eff'
          }
        }]
      };
      
      // 设置图表选项并渲染
      this.radarChart.setOption(option);
      
      // 添加窗口大小变化的监听，以便调整图表大小
      window.addEventListener('resize', this.resizeRadarChart);
    },
    
    // 调整雷达图大小
    resizeRadarChart() {
      if (this.radarChart) {
        this.radarChart.resize();
      }
    }
  }
}
</script>

<style scoped>
.interview-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #f5f7fa;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin: 30px 40px;
  overflow: hidden;
  overflow-x: hidden;
}

/* 主内容区域样式 */
.main-content-area {
  flex: 1;
  display: flex;
  height: 100%;
  transition: margin-left 0.3s ease;
}

.main-content-area.expanded {
  margin-left: -220px;
}

/* 确保雷达图在边栏折叠时正确显示 */
.valuation-area {
  transition: width 0.3s ease;
}

/* 评估区域样式 */
.valuation-area {
  width: 300px;
  height: 100%;
  background: #fff;
  border-left: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.valuation-header {
  padding: 22px 20px;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
}

.valuation-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 500;
}

.valuation-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.radar-chart-container {
  width: 100%;
  height: 300px;
  margin-bottom: 20px;
}

.valuation-details {
  flex: 1;
}

.valuation-item {
  margin-bottom: 15px;
  padding: 16px;
  border-radius: 8px;
  background-color: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  position: relative;
}

.valuation-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.1);
}

.valuation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.valuation-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.valuation-score {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  margin-left: 10px;
  transition: color 0.3s ease;
}

.valuation-progress {
  margin-top: 8px;
  height: 8px;
  border-radius: 4px;
  background-color: #ebeef5;
  overflow: hidden;
}

.valuation-progress-bar {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #409eff, #66b1ff);
  transition: width 0.6s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.valuation-item .el-icon-question {
  margin-left: 8px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}

.valuation-item .el-icon-question:hover {
  color: #409eff;
}

/* 评分变化动画效果 */
@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

.highlight-increase {
  background-color: rgba(103, 194, 58, 0.1);
  animation: pulse 0.5s ease-in-out;
}

.highlight-increase .valuation-progress-bar {
  background: linear-gradient(90deg, #67c23a, #85ce61);
  box-shadow: 0 0 8px rgba(103, 194, 58, 0.5);
}

.highlight-increase .valuation-score {
  color: #67c23a;
  font-weight: bold;
}

.highlight-decrease {
  background-color: rgba(245, 108, 108, 0.1);
  animation: pulse 0.5s ease-in-out;
}

.highlight-decrease .valuation-progress-bar {
  background: linear-gradient(90deg, #f56c6c, #f78989);
  box-shadow: 0 0 8px rgba(245, 108, 108, 0.5);
}

.highlight-decrease .valuation-score {
  color: #f56c6c;
  font-weight: bold;
}

.left-sidebar {
  width: 350px;
  height: 100%;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
}

.left-sidebar.collapsed {
  width: 40px;
}

.left-sidebar.collapsed .panel-header,
.left-sidebar.collapsed .tab-switcher,
.left-sidebar.collapsed .ai-interviewer-section,
.left-sidebar.collapsed .chat-records-section {
  visibility: hidden;
}

.left-sidebar.collapsed .collapse-btn {
  visibility: visible;
  z-index: 10;
}

.left-sidebar.collapsed .interviewer-item,
.left-sidebar.collapsed .chat-record-item {
  justify-content: center;
  padding: 12px 0;
}

.left-sidebar.collapsed .new-chat-button {
  padding: 8px 0;
  justify-content: center;
}

.panel-header {
  padding: 21px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
}

.left-sidebar.collapsed .panel-header {
  padding: 22px;
  justify-content: center;
  visibility: visible;
}

.left-sidebar.collapsed .panel-header span {
  display: none;
}

.collapse-btn {
  font-size: 16px;
  padding: 0;
  z-index: 10;
}

.panel-header span {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.tab-switcher {
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
}

.custom-tabs {
  position: relative;
  display: flex;
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.tab-button {
  flex: 1;
  padding: 10px 0;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tab-button i {
  font-size: 16px;
}

.tab-button.active {
  color: #409eff;
}

.slider {
  position: absolute;
  height: calc(100% - 8px);
  width: calc(50% - 8px);
  background-color: white;
  border-radius: 6px;
  top: 4px;
  left: 4px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 1;
}

.slider.record {
  transform: translateX(calc(100% + 8px));
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
}

.section-header .section-title {
  margin: 0;
  font-size: 14px;
  color: #606266;
  font-weight: 600;
}

.ai-interviewer-section,
.chat-records-section {
  flex: 1;
  overflow: hidden;
  overflow-x: hidden;
}

/* 面试官列表样式 */
.interviewer-list {
  padding: 10px;
}

.interviewer-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.interviewer-item:hover {
  background-color: #f5f7fa;
}

.interviewer-item.active {
  background-color: #ecf5ff;
}

.interviewer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #e6f1fc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.interviewer-avatar i {
  font-size: 20px;
  color: #409eff;
}

.interviewer-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

/* 聊天记录列表样式 */
.chat-records-list {
  padding: 10px;
  overflow-x: hidden;
}

.chat-record-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  margin-bottom: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-sizing: border-box;
  width: 100%;
}

.chat-record-item:hover {
  background-color: #f5f7fa;
}

.chat-record-item.active {
  background-color: #ecf5ff;
}

.record-content {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.record-content i {
  font-size: 16px;
  color: #909399;
  margin-right: 10px;
}

.record-title {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.record-more {
  font-size: 16px;
  color: #909399;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.chat-record-item:hover .record-more {
  opacity: 1;
}

/* 操作菜单样式 */
.action-menu {
  padding: 4px 0;

}

.action-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
  /* 添加圆角 */
  margin: 0 4px;
}

.action-item:hover {
  background-color: #f5f7fa;
}

.action-item.delete:hover {
  color: #f56c6c;
}

.action-item i {
  margin-right: 8px;
  font-size: 14px;
}

/* 自定义弹窗样式 */
.record-popover {
  padding: 0;
  min-width: 110px;
}

/* 对话框样式 */
.el-dialog__body {
  padding: 20px 20px 10px !important;
}

.record-popover {
  padding: 0;
  min-width: 110px;
  border-radius: 22px !important;
  /* 增加圆角半径 */
  overflow: hidden;
  /* 确保内容不会溢出圆角 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  /* 可选：添加更柔和的阴影 */
}

.new-chat-button-container {
  padding: 16px;
  margin-top: auto;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: center;
}

.new-chat-button {
  width: auto;
  min-width: 180px;
  height: 40px;
  /* 固定高度代替min-height */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  /* 移除顶部padding，保持左右padding */
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  background: linear-gradient(to right, #409eff, #1890ff);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
  line-height: 1;
  /* 确保文字行高正常 */
}

.new-chat-button:hover {
  background: linear-gradient(to right, #66b1ff, #40a9ff);
  transform: translateY(-1px);
  box-shadow: 0 6px 12px rgba(64, 158, 255, 0.4);
}

.new-chat-button:active {
  transform: translateY(1px);
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.4);
}

.new-chat-button i {
  margin-right: 8px;
  font-size: 16px;
}

/* 隐藏水平滚动条 */
.no-horizontal-scroll ::v-deep .el-scrollbar__wrap {
  overflow-x: hidden !important;
}

.no-horizontal-scroll-1 ::v-deep .el-scrollbar__wrap {
  overflow-x: hidden !important;
}

/* 确保内容不会溢出触发水平滚动 */
.interviewer-list {
  white-space: nowrap;
  /* 如果不需要横向排列则移除 */
  width: 100%;
  box-sizing: border-box;
}
</style>


