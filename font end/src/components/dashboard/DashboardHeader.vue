<template>
  <header class="header">
    <!-- 左侧：系统管理入口 -->
    <div class="nav-group nav-left">
      <a href="javascript:;" class="btn-tech" @click="$emit('open-modal', 'system')">
        <i class="fas fa-cogs"></i> 系统管理中心
      </a>
      <a href="javascript:;" class="btn-tech" @click="$emit('open-modal', 'device')">
        <i class="fas fa-server"></i> 设备运维中心
      </a>
    </div>

    <!-- 中间：标题 -->
    <div class="header-title-box">
      <div class="header-title">智慧城市综合监控平台</div>
      <div class="header-subtitle">INTELLIGENT CITY MONITORING SYSTEM</div>
    </div>

    <!-- 右侧：时间与状态 -->
    <div class="nav-group nav-right">
      <div class="btn-tech no-hover">
        <i class="far fa-clock"></i> <span>{{ currentTime }}</span>
      </div>
      <div class="btn-tech active">
        <i class="fas fa-satellite-dish"></i> 监控模式: 实时
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const currentTime = ref('');
let timer = null;

const updateTime = () => {
  currentTime.value = new Date().toLocaleTimeString();
};

onMounted(() => {
  updateTime();
  timer = setInterval(updateTime, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.header {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 90px;
  background: linear-gradient(to bottom, rgba(5, 10, 21, 0.95), transparent);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 15px;
  z-index: 100;
  border-bottom: 1px solid rgba(0, 242, 255, 0.1);
  pointer-events: auto;
  /* 允许点击 */
}

.header-title-box {
  text-align: center;
  position: relative;
}

.header-title {
  font-size: 36px;
  font-weight: 800;
  letter-spacing: 6px;
  background: linear-gradient(180deg, #fff, #00f2ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 20px rgba(0, 242, 255, 0.6);
  text-transform: uppercase;
}

.header-subtitle {
  font-size: 12px;
  color: #00f2ff;
  letter-spacing: 8px;
  margin-top: -5px;
  opacity: 0.8;
}

.nav-group {
  position: absolute;
  top: 25px;
  display: flex;
  gap: 20px;
}

.nav-left {
  left: 40px;
}

.nav-right {
  right: 40px;
}

.btn-tech {
  background: rgba(0, 242, 255, 0.05);
  border: 1px solid rgba(0, 242, 255, 0.3);
  color: #00f2ff;
  padding: 10px 25px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.4s;
  position: relative;
  overflow: hidden;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 8px;
  clip-path: polygon(15px 0, 100% 0, 100% calc(100% - 15px), calc(100% - 15px) 100%, 0 100%, 0 15px);
}

.btn-tech:hover:not(.no-hover),
.btn-tech.active {
  background: #00f2ff;
  color: #000;
  box-shadow: 0 0 20px rgba(0, 242, 255, 0.4);
}

.btn-tech.no-hover {
  cursor: default;
  border-color: transparent;
  background: transparent;
}
</style>
