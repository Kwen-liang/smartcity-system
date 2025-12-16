<template>
  <div class="side-panel">
    <!-- 图表1：饼图 -->
    <div class="panel-box">
      <div class="panel-header">
        <span><i class="fas fa-chart-pie"></i> 设备分布统计</span>
      </div>
      <div ref="pieChartRef" class="chart-container"></div>
    </div>

    <!-- 图表2：折线图 -->
    <div class="panel-box">
      <div class="panel-header">
        <span><i class="fas fa-wave-square"></i> 24小时报警趋势</span>
        <!-- 添加一个小的提示，表示实时 -->
        <span style="margin-left: auto; font-size: 12px; color: #00f2ff; opacity: 0.8;">LIVE</span>
      </div>
      <div ref="lineChartRef" class="chart-container"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import request from '@/utils/request';
import eventBus from '@/utils/eventBus';

const pieChartRef = ref(null);
const lineChartRef = ref(null);
let pieChart = null;
let lineChart = null;
let timer = null; // 定时器引用

// 通用配置
const commonTooltip = {
  trigger: 'item',
  backgroundColor: 'rgba(13, 26, 60, 0.95)',
  borderColor: '#00f2ff',
  borderWidth: 1,
  padding: 10,
  textStyle: { color: '#fff' },
  extraCssText: 'box-shadow: 0 0 20px rgba(0, 242, 255, 0.3); border-radius: 4px;'
};

onMounted(() => {
  initEmptyCharts();
  fetchData();
  window.addEventListener('resize', handleResize);
  eventBus.on('refreshDeviceList', fetchData);

  // 增加定时刷新：每分钟刷新一次趋势图，保持时间轴实时更新
  timer = setInterval(fetchData, 60000);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (pieChart) pieChart.dispose();
  if (lineChart) lineChart.dispose();
  eventBus.off('refreshDeviceList', fetchData);

  // 清除定时器
  if (timer) clearInterval(timer);
});

const handleResize = () => {
  pieChart && pieChart.resize();
  lineChart && lineChart.resize();
};

const initEmptyCharts = () => {
  if (pieChartRef.value) pieChart = echarts.init(pieChartRef.value);
  if (lineChartRef.value) lineChart = echarts.init(lineChartRef.value);
};

const fetchData = async () => {
  try {
    // 增加时间戳防止缓存
    const [statsRes, trendRes] = await Promise.all([
      request.get('/api/device/stats?t=' + new Date().getTime()),
      request.get('/api/device/trend?t=' + new Date().getTime())
    ]);

    updatePieChart(statsRes);
    updateLineChart(trendRes);

  } catch (error) {
    console.error("图表数据获取失败", error);
  }
};

const updatePieChart = (data) => {
  if (!pieChart) return;

  pieChart.setOption({
    tooltip: {
      ...commonTooltip,
      formatter: '{b}: {c} ({d}%)'
    },
    legend: { bottom: 0, textStyle: { color: '#fff' } },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      label: { show: false },
      itemStyle: { borderRadius: 5, borderColor: '#0b1221', borderWidth: 2 },
      data: data
    }]
  });
};

const updateLineChart = (data) => {
  if (!lineChart) return;

  lineChart.setOption({
    tooltip: {
      ...commonTooltip,
      trigger: 'axis'
    },
    grid: { top: 30, bottom: 20, left: 40, right: 10 },
    xAxis: {
      type: 'category',
      data: data.xAxis, // 这里现在是后端根据当前时间动态生成的
      axisLine: { lineStyle: { color: '#00f2ff' } },
      axisLabel: { color: '#ccc' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { color: '#ccc' }
    },
    series: [{
      data: data.series,
      type: 'line',
      smooth: true,
      areaStyle: {
        opacity: 0.2,
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#00f2ff' }, { offset: 1, color: 'rgba(0,242,255,0)' }])
      },
      itemStyle: { color: '#00f2ff' },
      lineStyle: { width: 3, shadowBlur: 10, shadowColor: '#00f2ff' }
    }]
  });
};
</script>

<style scoped>
.side-panel {
  width: 400px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  pointer-events: auto;
}

.panel-box {
  background: rgba(13, 26, 60, 0.75);
  border: 1px solid rgba(0, 242, 255, 0.3);
  border-radius: 4px;
  padding: 15px;
  position: relative;
  backdrop-filter: blur(12px);
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.5);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.panel-header {
  font-size: 16px;
  color: #fff;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-header i {
  color: #00f2ff;
}

.chart-container {
  width: 100%;
  height: 100%;
  flex: 1;
}
</style>
