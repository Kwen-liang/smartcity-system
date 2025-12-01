<template>
  <div class="dashboard-container">
    <!-- 0. 底层地图 (Cesium) -->
    <!-- 注意：这里引入了之前的 CesiumContainer 组件作为背景 -->
    <CesiumContainer class="cesium-bg" />

    <!-- 1. 装饰层：雷达扫描 -->
    <div class="radar-scan"></div>

    <!-- 2. UI 层 -->
    <!-- 顶部 Header -->
    <DashboardHeader @open-modal="openModal" />

    <!-- 大屏内容区 -->
    <div class="dashboard-layer">
      <!-- 左侧面板 -->
      <DataPanelLeft />

      <!-- 右侧面板 -->
      <DataPanelRight />
    </div>

    <!-- 3. 管理模态框 -->
    <ManagementModal :visible="modalVisible" :type="modalType" @close="closeModal" />
  </div>
</template>

<script setup>
import { ref } from 'vue';
import CesiumContainer from '@/components/CesiumContainer.vue'; // 确保路径正确
import DashboardHeader from '@/components/dashboard/DashboardHeader.vue';
import DataPanelLeft from '@/components/dashboard/DataPanelLeft.vue';
import DataPanelRight from '@/components/dashboard/DataPanelRight.vue';
import ManagementModal from '@/components/dashboard/ManagementModal.vue';

// 弹窗控制状态
const modalVisible = ref(false);
const modalType = ref(''); // 'system' 或 'device'

const openModal = (type) => {
  modalType.value = type;
  modalVisible.value = true;
};

const closeModal = () => {
  modalVisible.value = false;
};
</script>

<style scoped>
/* 全局容器 */
.dashboard-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background-color: #050a15;
  color: #fff;
  font-family: 'Segoe UI', "Microsoft YaHei", sans-serif;
}

/* Cesium 背景层 */
.cesium-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

/* 雷达扫描动画 (覆盖在地图上，增加科技感，半透明) */
.radar-scan {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100vw;
  height: 100vw;
  background: conic-gradient(from 0deg, transparent 0deg, rgba(0, 242, 255, 0.05) 60deg, transparent 60deg);
  border-radius: 50%;
  animation: radar-spin 10s linear infinite;
  pointer-events: none;
  /* 必须穿透，否则无法操作地图 */
  z-index: 1;
}

@keyframes radar-spin {
  0% {
    transform: translate(-50%, -50%) rotate(0deg);
  }

  100% {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

/* UI 内容布局 */
.dashboard-layer {
  position: absolute;
  top: 90px;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  pointer-events: none;
  /* 中间区域透给地图 */
  z-index: 10;
}
</style>
