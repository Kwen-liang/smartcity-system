<template>
  <div class="container-wrapper">
    <!-- 地图挂载容器 -->
    <div id="cesiumContainer" ref="cesiumContainer"></div>

    <!-- 错误提示层 -->
    <div v-if="errorMessage" class="error-overlay">
      <h3>Cesium 加载失败</h3>
      <p>{{ errorMessage }}</p>
      <small>请检查控制台 (F12) 获取详细信息</small>
    </div>

    <!-- 3D 地图弹窗 (Overlay) -->
    <div v-show="popupVisible" class="cesium-popup" :style="{ left: popupPos.x + 'px', top: popupPos.y + 'px' }">
      <div class="popup-header">
        <span>{{ selectedDevice.deviceName }}</span>
        <i class="fas fa-times close-btn" @click="closePopup"></i>
      </div>
      <div class="popup-content">
        <div class="info-row">
          <label>ID:</label> <span>{{ selectedDevice.deviceId }}</span>
        </div>
        <div class="info-row">
          <label>状态:</label>
          <span :style="{ color: getStatusColor(selectedDevice.deviceStatus) }">{{ selectedDevice.deviceStatus }}</span>
        </div>
        <div class="info-row">
          <label>读数:</label> <span>{{ selectedDevice.currentValue || '--' }}</span>
        </div>
        <div class="info-row">
          <label>位置:</label> <span>{{ selectedDevice.address || '未知位置' }}</span>
        </div>
        <div class="info-row">
          <label>负责人:</label> <span>系统管理员 (运维部)</span> <!-- 模拟数据 -->
        </div>
        <div class="info-row">
          <label>更新:</label> <span>{{ formatTime(selectedDevice.lastUpdate) }}</span>
        </div>

        <!-- 模拟视频监控 -->
        <div class="video-monitor">
          <div class="video-header"><i class="fas fa-video"></i> 实时监控</div>
          <div class="video-placeholder">
            <!-- 使用动态占位图模拟视频画面 -->
            <img src="https://placehold.co/300x160/000000/00f2ff?text=LIVE+MONITOR&font=roboto" alt="监控画面">
            <div class="rec-dot">REC</div>
          </div>
        </div>
      </div>
      <!-- 底部装饰箭头 -->
      <div class="popup-arrow"></div>
    </div>

    <div class="ui-overlay">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, reactive } from 'vue';
import * as Cesium from 'cesium';
import 'cesium/Build/Cesium/Widgets/widgets.css';
import request from '@/utils/request';
import eventBus from '@/utils/eventBus';

const CESIUM_TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI4MTRmYTFhMi05NDNhLTQ1NTAtYjBhYi0xNTBkMmMxODgzMjMiLCJpZCI6MzY0NTI3LCJpYXQiOjE3NjQyNTUzNTd9.kHgK7AA76l1D8WH4b-erR4B4-J4EAunbjFVPSbwvUGg';
const CITY_CENTER = {
  lng: 111.691347,
  lat: 29.040225,
  height: 3500,
  heading: 0.0,
  pitch: -35.0,
};

const cesiumContainer = ref(null);
const errorMessage = ref('');
let viewer = null;
let clickHandler = null;

// 弹窗相关状态
const popupVisible = ref(false);
const popupPos = reactive({ x: 0, y: 0 });
const selectedDevice = ref({});
let selectedEntity = null;

onMounted(() => {
  initCesium();
  eventBus.on('refreshDeviceList', reloadMarkers);
});

onUnmounted(() => {
  if (clickHandler) {
    clickHandler.destroy();
  }
  if (viewer && !viewer.isDestroyed()) {
    viewer.destroy();
    window.viewer = null;
  }
  eventBus.off('refreshDeviceList', reloadMarkers);
});

const reloadMarkers = () => {
  if (viewer) {
    viewer.entities.removeAll();
    loadDeviceMarkers();
    closePopup(); // 刷新数据时关闭弹窗
  }
};

const initCesium = () => {
  if (!cesiumContainer.value) return;
  Cesium.Ion.defaultAccessToken = CESIUM_TOKEN;

  try {
    viewer = new Cesium.Viewer(cesiumContainer.value, {
      animation: false,
      timeline: false,
      baseLayerPicker: false,
      geocoder: false,
      navigationHelpButton: false,
      homeButton: false,
      sceneModePicker: false,
      fullscreenButton: false,
      infoBox: false, // 禁用自带的信息框，使用自定义弹窗
      selectionIndicator: false,
      terrain: Cesium.Terrain.fromWorldTerrain({
        requestWaterMask: true,
        requestVertexNormals: true
      }),
      requestRenderMode: true,
      maximumRenderTimeChange: Infinity,
      contextOptions: {
        webgl: { preserveDrawingBuffer: true, alpha: true },
      },
    });

    const creditContainer = viewer.cesiumWidget.creditContainer;
    if (creditContainer) creditContainer.style.display = "none";

    viewer.scene.globe.depthTestAgainstTerrain = true;
    viewer.scene.globe.enableLighting = true;
    viewer.scene.highDynamicRange = true;
    viewer.scene.backgroundColor = Cesium.Color.TRANSPARENT;
    viewer.scene.globe.baseColor = Cesium.Color.BLACK;

    flyToChangde();
    loadDeviceMarkers();
    initInteraction(); // 初始化点击交互

    viewer.resize();
    window.viewer = viewer;

  } catch (error) {
    console.error("Cesium 初始化严重错误:", error);
    errorMessage.value = error.message;
  }
};

// 初始化交互逻辑
const initInteraction = () => {
  // 1. 监听点击事件
  clickHandler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas);
  clickHandler.setInputAction((movement) => {
    const pickedObject = viewer.scene.pick(movement.position);
    if (Cesium.defined(pickedObject) && pickedObject.id instanceof Cesium.Entity) {
      // 选中了设备
      const entity = pickedObject.id;
      showPopup(entity);
    } else {
      // 点击空白处关闭
      closePopup();
    }
  }, Cesium.ScreenSpaceEventType.LEFT_CLICK);

  // 2. 监听渲染事件，实时更新弹窗位置 (实现跟随)
  viewer.scene.postRender.addEventListener(() => {
    if (popupVisible.value && selectedEntity) {
      updatePopupPosition();
    }
  });
};

const showPopup = (entity) => {
  selectedEntity = entity;
  // 从 entity.properties 中获取数据
  // 注意：Cesium 的 properties 可能是 PropertyBag，需要 getValue
  const props = {};
  const propertyNames = entity.properties.propertyNames;
  propertyNames.forEach(name => {
    props[name] = entity.properties[name].getValue();
  });

  selectedDevice.value = {
    ...props,
    deviceName: entity.label.text.getValue() // 获取标签名称
  };

  popupVisible.value = true;
  // 立即更新一次位置
  updatePopupPosition();
};

const closePopup = () => {
  popupVisible.value = false;
  selectedEntity = null;
};

// 将 3D 坐标转换为屏幕 2D 坐标
const updatePopupPosition = () => {
  if (!selectedEntity || !viewer) return;

  const position = selectedEntity.position.getValue(viewer.clock.currentTime);
  if (position) {
    // 修复：wgs84ToWindowCoordinates 已弃用，改用 worldToWindowCoordinates
    const canvasPosition = Cesium.SceneTransforms.worldToWindowCoordinates(
      viewer.scene,
      position
    );

    if (canvasPosition) {
      // 这里的偏移量是为了让弹窗显示在点的上方
      popupPos.x = canvasPosition.x;
      popupPos.y = canvasPosition.y - 50;
    }
  }
};

const flyToChangde = () => {
  if (!viewer) return;
  viewer.camera.flyTo({
    destination: Cesium.Cartesian3.fromDegrees(CITY_CENTER.lng, CITY_CENTER.lat, CITY_CENTER.height),
    orientation: {
      heading: Cesium.Math.toRadians(CITY_CENTER.heading),
      pitch: Cesium.Math.toRadians(CITY_CENTER.pitch),
      roll: 0.0
    },
    duration: 3
  });
};

const loadDeviceMarkers = async () => {
  try {
    const points = await request.get('/api/device/map-points');
    if (!points || !viewer) return;

    points.forEach(point => {
      let color = Cesium.Color.WHITE;
      let pixelSize = 10;
      const catId = point.categoryId;

      if (catId === 1) color = Cesium.Color.GOLD;
      else if (catId === 2) color = Cesium.Color.LIMEGREEN;
      else if (catId === 3) color = Cesium.Color.ORANGE;
      else if (catId === 4) color = Cesium.Color.AQUA;

      if (point.deviceStatus === '告警') {
        color = Cesium.Color.RED;
        pixelSize = 15;
      }

      viewer.entities.add({
        position: Cesium.Cartesian3.fromDegrees(Number(point.longitude), Number(point.latitude), 50),
        point: {
          pixelSize: pixelSize,
          color: color,
          outlineColor: Cesium.Color.BLACK,
          outlineWidth: 2,
        },
        label: {
          text: point.deviceName,
          font: '12px Microsoft YaHei',
          style: Cesium.LabelStyle.FILL_AND_OUTLINE,
          fillColor: Cesium.Color.WHITE,
          outlineColor: Cesium.Color.BLACK,
          outlineWidth: 2,
          verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
          pixelOffset: new Cesium.Cartesian2(0, -15),
          distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 5000)
        },
        // 关键：将详细数据绑定到实体上，供点击时读取
        properties: {
          deviceId: point.deviceId,
          deviceStatus: point.deviceStatus,
          address: point.address,
          currentValue: point.currentValue,
          lastUpdate: point.lastUpdate
        }
      });
    });
  } catch (error) {
    console.error("加载地图点位失败", error);
  }
};

// 工具函数
const formatTime = (time) => {
  if (!time) return 'N/A';
  if (Array.isArray(time)) return `${time[0]}-${String(time[1]).padStart(2,'0')}-${String(time[2]).padStart(2,'0')} ${String(time[3]).padStart(2,'0')}:${String(time[4]).padStart(2,'0')}`;
  return time.replace('T', ' ');
};

const getStatusColor = (status) => {
  if (status === '正常') return '#0f0';
  if (status === '告警') return '#f00';
  return '#ffcc00';
};
</script>

<style scoped>
/* 容器样式 */
.container-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #000;
}

#cesiumContainer {
  width: 100%;
  height: 100%;
  display: block;
}

/* 错误层 */
.error-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 0, 0, 0.8);
  color: white;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  z-index: 999;
}

/* UI 插槽层 */
.ui-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}
.ui-overlay > * {
  pointer-events: auto;
}

/* 弹窗样式 */
.cesium-popup {
  position: absolute;
  width: 320px;
  background: rgba(13, 26, 60, 0.9);
  border: 1px solid #00f2ff;
  border-radius: 4px;
  box-shadow: 0 0 20px rgba(0, 242, 255, 0.3);
  color: #fff;
  z-index: 99; /* 高于地图 */
  transform: translate(-50%, -100%); /* 居中并显示在上方 */
  pointer-events: auto;
  font-family: 'Microsoft YaHei', sans-serif;
  backdrop-filter: blur(5px);
}

.popup-header {
  padding: 10px 15px;
  background: rgba(0, 242, 255, 0.1);
  border-bottom: 1px solid rgba(0, 242, 255, 0.3);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
  color: #00f2ff;
}

.close-btn {
  cursor: pointer;
  transition: 0.3s;
}
.close-btn:hover {
  color: #ff2a2a;
}

.popup-content {
  padding: 15px;
  font-size: 13px;
}

.info-row {
  margin-bottom: 8px;
  display: flex;
}

.info-row label {
  color: #aaa;
  width: 60px;
  text-align: right;
  margin-right: 10px;
}

/* 视频监控模拟 */
.video-monitor {
  margin-top: 15px;
  border-top: 1px dashed rgba(255, 255, 255, 0.2);
  padding-top: 10px;
}

.video-header {
  margin-bottom: 8px;
  color: #00f2ff;
  font-size: 12px;
}

.video-placeholder {
  width: 100%;
  height: 160px;
  background: #000;
  border: 1px solid #00f2ff;
  position: relative;
  overflow: hidden;
}

.video-placeholder img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.8;
}

.rec-dot {
  position: absolute;
  top: 10px;
  right: 10px;
  color: #f00;
  font-weight: bold;
  font-size: 10px;
  animation: blink 1s infinite;
  background: rgba(0, 0, 0, 0.5);
  padding: 2px 5px;
  border-radius: 2px;
}

@keyframes blink {
  0% { opacity: 1; }
  50% { opacity: 0; }
  100% { opacity: 1; }
}

/* 小三角 */
.popup-arrow {
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #00f2ff;
}
</style>
