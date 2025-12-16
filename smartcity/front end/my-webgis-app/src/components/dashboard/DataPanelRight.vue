<template>
  <div class="side-panel">
    <!-- 模块：电子仪表监测 -->
    <div class="panel-box" style="flex: 1.2;">
      <div class="panel-header">
        <span><i class="fas fa-tachometer-alt"></i> 实时环境仪表监测</span>
        <span style="margin-left:auto; font-size:12px; color:#ffcc00;">LIVE</span>
      </div>
      <div class="gauge-grid">
        <div ref="g1" class="gauge-item"></div>
        <div ref="g2" class="gauge-item"></div>
        <div ref="g3" class="gauge-item"></div>
        <div ref="g4" class="gauge-item"></div>
      </div>
    </div>

    <!-- 模块：实时列表 -->
    <div class="panel-box" style="flex: 1.5; overflow: hidden;">
      <div class="panel-header">
        <span><i class="fas fa-list"></i> 设备状态列表</span>

        <!-- 导出下拉菜单 -->
        <div class="export-dropdown" @mouseleave="showExportMenu = false">
          <button class="action-btn" @click="showExportMenu = !showExportMenu">
            <i class="fas fa-download"></i> 导出数据 <i class="fas fa-chevron-down"
              style="font-size: 10px; margin-left: 4px;"></i>
          </button>

          <transition name="fade">
            <div class="export-menu" v-if="showExportMenu">
              <div class="export-item" @click="handleExport('csv')">
                <i class="fas fa-file-csv" style="color: #00f2ff;"></i> 导出 CSV
              </div>
              <div class="export-item" @click="handleExport('excel')">
                <i class="fas fa-file-excel" style="color: #0f0;"></i> 导出 Excel
              </div>
              <div class="export-item" @click="handleExport('pdf')">
                <i class="fas fa-file-pdf" style="color: #ff2a2a;"></i> 导出 PDF
              </div>
            </div>
          </transition>
        </div>
      </div>

      <!-- 滚动容器 -->
      <div class="list-container">
        <!-- 增加 loading 状态 -->
        <div v-if="loading" style="text-align: center; color: #00f2ff; padding: 20px;">
          <i class="fas fa-spinner fa-spin"></i> 数据加载中...
        </div>

        <table v-else class="tech-table" id="device-table" ref="deviceTableRef">
          <thead>
            <tr>
              <th>设备ID</th>
              <th>名称</th>
              <th>读数</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in deviceList" :key="item.deviceId">
              <td>{{ item.deviceId }}</td>
              <td>{{ item.deviceName }}</td>
              <td>{{ item.currentValue }}</td>
              <td :style="{ color: getStatusColor(item.deviceStatus), fontWeight: 'bold' }">{{ item.deviceStatus }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import request from '@/utils/request';
import eventBus from '@/utils/eventBus'; // 1. 引入 eventBus

// DOM refs
const g1 = ref(null);
const g2 = ref(null);
const g3 = ref(null);
const g4 = ref(null);
const deviceTableRef = ref(null);
const showExportMenu = ref(false);

const deviceList = ref([]);
const loading = ref(false);

let charts = [];

onMounted(() => {
  initGauges();
  fetchDeviceList();
  window.addEventListener('resize', handleResize);

  // 2. 订阅刷新事件
  eventBus.on('refreshDeviceList', fetchDeviceList);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  charts.forEach(c => c.dispose());

  // 3. 取消订阅
  eventBus.off('refreshDeviceList', fetchDeviceList);
});

// --- 获取数据 ---
const fetchDeviceList = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/device/list');
    deviceList.value = res || [];
  } catch (error) {
    console.error("获取设备列表失败", error);
  } finally {
    loading.value = false;
  }
};

// 辅助函数：状态颜色判断
const getStatusColor = (status) => {
  if (status === '正常') return '#0f0';
  if (status === '告警' || status === '离线') return '#f00';
  if (status === '波动' || status === '维修中') return 'orange';
  return '#fff';
};

const handleResize = () => {
  charts.forEach(c => c.resize());
};

// --- 导出逻辑 (保持不变) ---
const handleExport = (type) => {
  showExportMenu.value = false;
  const timestamp = new Date().toISOString().slice(0, 10);
  const fileName = `智慧城市_设备报表_${timestamp}`;

  if (type === 'csv') exportToCSV(fileName);
  else if (type === 'excel') exportToExcel(fileName);
  else if (type === 'pdf') exportToPDF(fileName);
};

// 1. CSV 导出
const exportToCSV = (fileName) => {
  if (!deviceTableRef.value) return;
  const table = deviceTableRef.value;
  let csvContent = "data:text/csv;charset=utf-8,\uFEFF"; // BOM for UTF-8

  let headers = [];
  table.querySelectorAll('th').forEach(th => headers.push(th.innerText));
  csvContent += headers.join(",") + "\r\n";

  table.querySelectorAll('tbody tr').forEach(tr => {
    let row = [];
    tr.querySelectorAll('td').forEach(td => row.push(td.innerText));
    csvContent += row.join(",") + "\r\n";
  });

  downloadFile(encodeURI(csvContent), `${fileName}.csv`);
};

// 2. Excel 导出 (利用 HTML Table 结构)
const exportToExcel = (fileName) => {
  if (!deviceTableRef.value) return;

  const tableHtml = deviceTableRef.value.outerHTML;
  const excelTemplate = `
    <html xmlns:o="urn:schemas-microsoft-com:office:office"
          xmlns:x="urn:schemas-microsoft-com:office:excel"
          xmlns="http://www.w3.org/TR/REC-html40">
      <head>
        <meta charset="UTF-8">
        <style>
          table { border-collapse: collapse; width: 100%; }
          th { background-color: #003366; color: #ffffff; border: 1px solid #000; padding: 10px; }
          td { border: 1px solid #000; padding: 8px; text-align: center; }
        </style>
      </head>
      <body>${tableHtml}</body>
    </html>
  `;

  const blob = new Blob([excelTemplate], { type: 'application/vnd.ms-excel' });
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = `${fileName}.xls`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

// 3. PDF 导出 (利用浏览器打印预览)
const exportToPDF = () => {
  if (!deviceTableRef.value) return;

  const printWindow = window.open('', '_blank', 'height=600,width=800');
  const tableHtml = deviceTableRef.value.outerHTML;
  const closeScript = '<' + '/script>';

  printWindow.document.write(`
    <html>
      <head>
        <title>设备状态报表打印</title>
        <style>
          body { font-family: 'Microsoft YaHei', sans-serif; padding: 20px; }
          h1 { text-align: center; color: #333; }
          table { width: 100%; border-collapse: collapse; margin-top: 20px; }
          th, td { border: 1px solid #666; padding: 10px; text-align: left; font-size: 14px; }
          th { background-color: #f2f2f2; font-weight: bold; }
          tr:nth-child(even) { background-color: #f9f9f9; }
          @media print {
            button { display: none; }
          }
        </style>
      </head>
      <body>
        <h1>智慧城市 - 设备状态报表</h1>
        ${tableHtml}
        <div style="margin-top: 20px; font-size: 12px; color: #666; text-align: right;">
          导出时间: ${new Date().toLocaleString()}
        </div>
        <script>
          window.onload = function() { window.print(); window.close(); }
        ${closeScript}
      </body>
    </html>
  `);
  printWindow.document.close();
};

const downloadFile = (uri, name) => {
  const link = document.createElement("a");
  link.setAttribute("href", uri);
  link.setAttribute("download", name);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

const initGauges = () => {
  const configs = [
    { el: g1, name: '温度', color: '#ffcc00', value: 24, unit: '°C' },
    { el: g2, name: '湿度', color: '#00f2ff', value: 65, unit: '%' },
    { el: g3, name: '水压', color: '#0078ff', value: 80, unit: 'Pa' },
    { el: g4, name: '电压', color: '#bc13fe', value: 22, unit: '0V' }
  ];

  configs.forEach(cfg => {
    if (cfg.el.value) {
      const chart = echarts.init(cfg.el.value);
      chart.setOption({
        series: [{
          type: 'gauge',
          startAngle: 180, endAngle: 0,
          min: 0, max: 100,
          splitNumber: 2,
          itemStyle: { color: cfg.color },
          progress: { show: true, width: 8 },
          pointer: { show: false },
          axisLine: { lineStyle: { width: 8 } },
          axisTick: { show: false },
          splitLine: { length: 2, lineStyle: { width: 2, color: '#999' } },
          axisLabel: { show: false },
          title: { show: true, offsetCenter: [0, '30%'], fontSize: 10, color: '#aaa' },
          detail: { valueAnimation: true, offsetCenter: [0, '-20%'], fontSize: 16, fontWeight: 'bolder', formatter: `{value}${cfg.unit}`, color: '#fff' },
          data: [{ value: cfg.value, name: cfg.name }]
        }]
      });
      charts.push(chart);
    }
  });
};
</script>

<style scoped>
/* 样式保持不变 */
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
  flex-shrink: 0;
  position: relative;
}

.panel-header i {
  color: #00f2ff;
}

.export-dropdown {
  margin-left: auto;
  position: relative;
  padding-bottom: 10px;
  margin-bottom: -10px;
}

.action-btn {
  font-size: 12px;
  color: #00f2ff;
  border: 1px solid #00f2ff;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  background: rgba(0, 242, 255, 0.1);
  transition: 0.3s;
  display: flex;
  align-items: center;
  gap: 5px;
}

.action-btn:hover {
  background: #00f2ff;
  color: #000;
}

.export-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 0;
  width: 120px;
  background: rgba(5, 10, 21, 0.95);
  border: 1px solid #00f2ff;
  border-radius: 4px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.5);
  z-index: 100;
  overflow: hidden;
}

.export-menu::before {
  content: '';
  position: absolute;
  top: -10px;
  left: 0;
  width: 100%;
  height: 10px;
  background: transparent;
}

.export-item {
  padding: 10px 15px;
  font-size: 12px;
  color: #fff;
  cursor: pointer;
  transition: 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.export-item:hover {
  background: rgba(0, 242, 255, 0.2);
  color: #00f2ff;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.gauge-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  height: 100%;
}

.gauge-item {
  position: relative;
  height: 100%;
  min-height: 120px;
}

.list-container {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
}

.list-container::-webkit-scrollbar {
  width: 4px;
}

.list-container::-webkit-scrollbar-thumb {
  background: rgba(0, 242, 255, 0.3);
  border-radius: 2px;
}

.list-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.tech-table {
  width: 100%;
  font-size: 13px;
  border-collapse: collapse;
  color: #a6adb4;
}

.tech-table th {
  text-align: left;
  padding: 8px;
  color: #00f2ff;
  border-bottom: 1px solid rgba(0, 242, 255, 0.2);
  position: sticky;
  top: 0;
  background: rgba(13, 26, 60, 0.95);
  z-index: 10;
}

.tech-table td {
  padding: 8px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.05);
}

.tech-table tr:hover td {
  background: rgba(0, 242, 255, 0.05);
  color: #fff;
}
</style>
