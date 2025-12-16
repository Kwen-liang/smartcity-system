<template>
  <div class="modal-overlay" :class="{ active: visible }">
    <div class="modal-content">
      <!-- 头部 -->
      <div class="modal-header">
        <div class="modal-title">
          <i :class="iconClass"></i> {{ titleText }}
          <span v-if="isLoggedIn" class="user-badge" :class="currentUserRole">
            {{ currentUserRole === 'admin' ? '超级管理员' : '普通用户' }}
          </span>
        </div>
        <div class="modal-controls">
          <span v-if="isLoggedIn" class="logout-btn" @click="handleLogout">
            <i class="fas fa-sign-out-alt"></i> 退出
          </span>
          <div class="modal-close" @click="$emit('close')">
            <i class="fas fa-times"></i>
          </div>
        </div>
      </div>

      <!-- 1. 登录界面 -->
      <div class="modal-body login-body" v-if="!isLoggedIn">
        <div class="login-box">
          <h3><i class="fas fa-shield-alt"></i> 身份验证</h3>
          <p class="login-tip">请登录以访问管理中心</p>
          <div class="login-form">
            <div class="mock-form-row">
              <label class="mock-label">账号</label>
              <input type="text" v-model="loginForm.username" class="mock-input" placeholder="示例:admin"
                @keyup.enter="handleLogin">
            </div>
            <div class="mock-form-row">
              <label class="mock-label">密码</label>
              <input type="password" v-model="loginForm.password" class="mock-input" placeholder="示例:123"
                @keyup.enter="handleLogin">
            </div>
            <div class="error-msg" v-if="loginError"><i class="fas fa-exclamation-circle"></i> {{ loginError }}</div>
            <button class="btn-tech login-btn" @click="handleLogin" :disabled="loading">
              <i class="fas" :class="loading ? 'fa-spinner fa-spin' : 'fa-unlock-alt'"></i> {{ loading ? '登录中...' :
                '立即登录' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 2. 主管理界面 -->
      <div class="modal-body" v-else>
        <!-- 左侧菜单 -->
        <div class="modal-sidebar">
          <div v-for="(item, index) in menuItems" :key="index" class="modal-menu-item"
            :class="{ active: activeIndex === index }" @click="switchMenu(index)">
            {{ item }}
          </div>
        </div>

        <!-- 右侧主区域 -->
        <div class="modal-main">

          <!-- 标题栏 + 操作按钮 -->
          <div class="form-title">
            <span>{{ currentMenuName }}</span>
            <div class="title-actions">
              <!-- 系统管理按钮 -->
              <button v-if="isUserPage && currentUserRole === 'admin'" class="btn-tech small" @click="openAddUserForm">
                <i class="fas fa-user-plus"></i> 新增用户
              </button>
              <button v-if="isDeptPage && currentUserRole === 'admin'" class="btn-tech small" @click="openAddDeptForm">
                <i class="fas fa-folder-plus"></i> 新增部门
              </button>

              <!-- 设备管理按钮 -->
              <button v-if="isDevicePage && currentUserRole === 'admin'" class="btn-tech small"
                @click="openAddDeviceForm">
                <i class="fas fa-plus-circle"></i> 新增设备
              </button>

              <span class="read-only-tag" v-if="currentUserRole === 'user' && !isLogPage && !isMaintenancePage">
                <i class="fas fa-eye"></i> 仅查看模式
              </span>
            </div>
          </div>

          <!-- A. 用户管理列表 -->
          <div v-if="isUserPage" class="list-view">
            <div class="table-wrapper">
              <table class="tech-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>用户名</th>
                    <th>真实姓名</th>
                    <th>角色</th>
                    <th>部门ID</th>
                    <th v-if="currentUserRole === 'admin'">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="user in userList" :key="user.userId">
                    <td>{{ user.userId }}</td>
                    <td>{{ user.username }}</td>
                    <td>{{ user.realName || '-' }}</td>
                    <td><span class="status-tag" :class="user.roleId === 1 ? 'admin-tag' : 'user-tag'">{{ user.roleId
                        === 1 ? '管理员' : '用户' }}</span></td>
                    <td>{{ user.deptId || '-' }}</td>
                    <td v-if="currentUserRole === 'admin'"><button class="action-text delete"
                        @click="handleDeleteUser(user.userId)">删除</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="userList.length === 0" class="empty-tip">暂无用户数据</div>
            </div>
          </div>

          <!-- B. 部门管理列表 -->
          <div v-else-if="isDeptPage" class="list-view">
            <div class="table-wrapper">
              <table class="tech-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>部门名称</th>
                    <th>部门编号</th>
                    <th>负责人</th>
                    <th>创建时间</th>
                    <th v-if="currentUserRole === 'admin'">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="dept in deptList" :key="dept.deptId">
                    <td>{{ dept.deptId }}</td>
                    <td>{{ dept.deptName }}</td>
                    <td>{{ dept.deptCode || '-' }}</td>
                    <td>{{ dept.manager || '-' }}</td>
                    <td>{{ formatTime(dept.createTime) }}</td>
                    <td v-if="currentUserRole === 'admin'"><button class="action-text delete"
                        @click="handleDeleteDept(dept.deptId)">删除</button></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="deptList.length === 0" class="empty-tip">暂无部门数据</div>
            </div>
          </div>

          <!-- C. 设备管理列表 -->
          <div v-else-if="isDevicePage" class="list-view">
            <div class="table-wrapper">
              <table class="tech-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>设备名称</th>
                    <th>状态</th>
                    <th>当前读数</th>
                    <th>位置(经,纬)</th>
                    <th>更新时间</th>
                    <th v-if="currentUserRole === 'admin'">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="dev in deviceList" :key="dev.deviceId">
                    <td>{{ dev.deviceId }}</td>
                    <td>{{ dev.deviceName }}</td>
                    <td>
                      <span :style="{ color: getStatusColor(dev.deviceStatus) }">{{ dev.deviceStatus }}</span>
                    </td>
                    <td>{{ dev.currentValue || '--' }}</td>
                    <td>{{ dev.longitude }}, {{ dev.latitude }}</td>
                    <td>{{ formatTime(dev.lastUpdate) }}</td>
                    <td v-if="currentUserRole === 'admin'">
                      <button class="action-text delete" @click="handleDeleteDevice(dev.deviceId)">报废</button>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-if="deviceList.length === 0" class="empty-tip">暂无设备数据</div>
            </div>
          </div>

          <!-- D. 系统/维护日志列表 -->
          <div v-else-if="isLogPage || isMaintenancePage" class="list-view">
            <div class="table-wrapper">
              <table class="tech-table">
                <thead>
                  <tr>
                    <th>时间</th>
                    <th>操作人</th>
                    <th>IP</th>
                    <th>内容</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="log in systemLogs" :key="log.logId">
                    <td>{{ formatTime(log.createTime) }}</td>
                    <td>{{ log.username }}</td>
                    <td>{{ log.ipAddress }}</td>
                    <td>{{ log.action }}</td>
                    <td><span class="status-tag" :class="log.status === '1' ? 'success' : 'fail'">{{ log.status === '1'
                        ? '成功' : '失败' }}</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- E. 默认占位 -->
          <div v-else class="empty-tip">请选择菜单进行操作</div>

        </div>
      </div>

      <!-- 弹窗：新增用户 -->
      <div class="sub-modal" v-if="showUserForm">
        <div class="sub-modal-box">
          <h3>新增用户</h3>
          <div class="sub-form">
            <div class="mock-form-row"><label class="mock-label">用户名</label><input v-model="newUser.username"
                type="text" class="mock-input"></div>
            <div class="mock-form-row"><label class="mock-label">真实姓名</label><input v-model="newUser.realName"
                type="text" class="mock-input"></div>
            <div class="mock-form-row"><label class="mock-label">密码</label><input v-model="newUser.password"
                type="password" class="mock-input" placeholder="默认123456"></div>
            <div class="mock-form-row"><label class="mock-label">角色</label><select v-model.number="newUser.roleId"
                class="mock-input">
                <option :value="2">普通用户</option>
                <option :value="1">超级管理员</option>
              </select></div>
            <div class="mock-form-row"><label class="mock-label">部门ID</label><input v-model.number="newUser.deptId"
                type="number" class="mock-input"></div>
          </div>
          <div class="sub-actions"><button class="btn-tech btn-cancel" @click="showUserForm = false">取消</button><button
              class="btn-tech" @click="submitAddUser">确认</button></div>
        </div>
      </div>

      <!-- 弹窗：新增部门 -->
      <div class="sub-modal" v-if="showDeptForm">
        <div class="sub-modal-box">
          <h3>新增部门</h3>
          <div class="sub-form">
            <div class="mock-form-row"><label class="mock-label">部门名称</label><input v-model="newDept.deptName"
                type="text" class="mock-input"></div>
            <div class="mock-form-row"><label class="mock-label">部门编号</label><input v-model="newDept.deptCode"
                type="text" class="mock-input"></div>
            <div class="mock-form-row"><label class="mock-label">负责人</label><input v-model="newDept.manager" type="text"
                class="mock-input"></div>
          </div>
          <div class="sub-actions"><button class="btn-tech btn-cancel" @click="showDeptForm = false">取消</button><button
              class="btn-tech" @click="submitAddDept">确认</button></div>
        </div>
      </div>

      <!-- 弹窗：新增设备 -->
      <div class="sub-modal" v-if="showDeviceForm">
        <div class="sub-modal-box">
          <h3>新增{{ currentDeviceTypeName }}</h3>
          <div class="sub-form">
            <div class="mock-form-row"><label class="mock-label">设备ID</label><input v-model="newDevice.deviceId"
                type="text" class="mock-input" placeholder="例如: L-088"></div>
            <div class="mock-form-row"><label class="mock-label">设备名称</label><input v-model="newDevice.deviceName"
                type="text" class="mock-input"></div>
            <div class="mock-form-row"><label class="mock-label">状态</label>
              <select v-model="newDevice.deviceStatus" class="mock-input">
                <option value="正常">正常</option>
                <option value="告警">告警</option>
                <option value="离线">离线</option>
                <option value="维修中">维修中</option>
              </select>
            </div>
            <div class="mock-form-row"><label class="mock-label">当前读数</label><input v-model="newDevice.currentValue"
                type="text" class="mock-input" placeholder="例如: 220V 或 24°C"></div>
            <div class="mock-form-row" style="display:flex; gap:10px;">
              <div style="flex:1"><label class="mock-label">经度</label><input v-model="newDevice.longitude" type="text"
                  class="mock-input"></div>
              <div style="flex:1"><label class="mock-label">纬度</label><input v-model="newDevice.latitude" type="text"
                  class="mock-input"></div>
            </div>
            <div class="mock-form-row"><label class="mock-label">地址</label><input v-model="newDevice.address"
                type="text" class="mock-input"></div>
          </div>
          <div class="sub-actions"><button class="btn-tech btn-cancel"
              @click="showDeviceForm = false">取消</button><button class="btn-tech" @click="submitAddDevice">入库</button>
          </div>
        </div>
      </div>

      <!-- Toast -->
      <transition name="fade">
        <div class="success-toast" v-if="showSuccessToast">
          <div class="toast-content"><i class="fas fa-check-circle"></i> <span>{{ toastMsg }}</span></div>
        </div>
      </transition>

    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, reactive } from 'vue';
import request from '@/utils/request';
import eventBus from '@/utils/eventBus'; // 1. 引入 eventBus

const props = defineProps({ visible: Boolean, type: String });
defineEmits(['close']);

// --- 登录 ---
const isLoggedIn = ref(false);
const currentUserRole = ref('');
const loginForm = ref({ username: '', password: '' });
const loginError = ref('');
const loading = ref(false);

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) return;
  loading.value = true;
  try {
    const res = await request.post('/api/auth/login', loginForm.value);
    if (res.code === 200) {
      isLoggedIn.value = true;
      currentUserRole.value = res.role;
      localStorage.setItem('token', res.token);
      localStorage.setItem('userInfo', JSON.stringify(res.userInfo));
      // 登录成功后如果是系统管理，默认刷新一下日志
      if (props.type === 'system') { fetchLogs(); }
    } else {
      loginError.value = res.msg;
    }
  } catch (err) { console.error(err); loginError.value = '连接失败'; }
  finally { loading.value = false; }
};

const handleLogout = () => {
  isLoggedIn.value = false;
  localStorage.clear();
};

// --- 菜单与路由 ---
const activeIndex = ref(0);
const menus = {
  system: ['部门管理', '用户管理', '系统日志'],
  device: ['电子仪器管理', '下水井盖布局', '路灯布局', '消防栓布局', '维护记录']
};
const menuItems = computed(() => menus[props.type] || []);
const currentMenuName = computed(() => menuItems.value[activeIndex.value] || '');

// 页面类型判断
const isDeptPage = computed(() => props.type === 'system' && currentMenuName.value === '部门管理');
const isUserPage = computed(() => props.type === 'system' && currentMenuName.value === '用户管理');
const isLogPage = computed(() => props.type === 'system' && currentMenuName.value === '系统日志');

// 设备页面判断 (类型为device且不是维护记录)
const isDevicePage = computed(() => props.type === 'device' && currentMenuName.value !== '维护记录');
const isMaintenancePage = computed(() => props.type === 'device' && currentMenuName.value === '维护记录');

const titleText = computed(() => props.type === 'system' ? '系统管理中心' : '设备运维中心');
const iconClass = computed(() => props.type === 'system' ? 'fas fa-cogs' : 'fas fa-server');

// 设备分类映射
const categoryMap = {
  '电子仪器管理': 4,
  '下水井盖布局': 2,
  '路灯布局': 1,
  '消防栓布局': 3
};
const currentDeviceTypeName = computed(() => {
  if (isDevicePage.value) return currentMenuName.value.replace('管理', '').replace('布局', '');
  return '设备';
});

// 切换菜单
const switchMenu = async (index) => {
  activeIndex.value = index;
  const menuName = menuItems.value[index];

  if (props.type === 'system') {
    if (menuName === '用户管理') await fetchUsers();
    else if (menuName === '部门管理') await fetchDepts();
    else if (menuName === '系统日志') await fetchLogs();
  } else if (props.type === 'device') {
    if (menuName === '维护记录') {
      await fetchLogs();
    } else {
      // 获取对应分类的设备列表
      const catId = categoryMap[menuName];
      if (catId) await fetchDevices(catId);
    }
  }
};

// --- 用户管理逻辑 ---
const userList = ref([]);
const showUserForm = ref(false);
const newUser = reactive({ username: '', realName: '', password: '', roleId: 2, deptId: 101 });

const fetchUsers = async () => {
  try {
    const res = await request.get('/api/user/list');
    userList.value = res || [];
  } catch (e) { console.error(e); }
};
const openAddUserForm = () => {
  Object.assign(newUser, { username: '', realName: '', password: '', roleId: 2, deptId: 101 });
  showUserForm.value = true;
};
const submitAddUser = async () => {
  if (!newUser.username) return showToast("用户名不能为空");
  try {
    const res = await request.post('/api/user/add', { ...newUser, roleId: Number(newUser.roleId), deptId: Number(newUser.deptId) });
    if (res?.code === 200) {
      showToast("用户新增成功");
      showUserForm.value = false;
      fetchUsers();
    } else { showToast("失败: " + (res?.msg || '未知')); }
  } catch (e) {
    console.error("请求异常", e);
    showToast("请求异常");
  }
};
const handleDeleteUser = async (id) => {
  if (!confirm("确定要删除吗？")) return;
  try {
    const res = await request.post(`/api/user/delete?id=${id}`);
    if (res?.code === 200) { showToast("删除成功"); fetchUsers(); }
  } catch (e) { console.error(e); }
};

// --- 部门管理逻辑 ---
const deptList = ref([]);
const showDeptForm = ref(false);
const newDept = reactive({ deptName: '', deptCode: '', manager: '' });

const fetchDepts = async () => {
  try {
    const res = await request.get('/api/dept/list');
    deptList.value = res || [];
  } catch (e) { console.error("获取部门失败:", e); }
};
const openAddDeptForm = () => {
  Object.assign(newDept, { deptName: '', deptCode: '', manager: '' });
  showDeptForm.value = true;
};
const submitAddDept = async () => {
  if (!newDept.deptName) return showToast("名称不能为空");
  try {
    const res = await request.post('/api/dept/add', newDept);
    if (res?.code === 200) {
      showToast("部门新增成功");
      showDeptForm.value = false;
      fetchDepts();
    } else { showToast("失败: " + (res?.msg || '未知')); }
  } catch (e) {
    console.error("请求异常", e);
    showToast("请求异常");
  }
};
const handleDeleteDept = async (id) => {
  if (!confirm("确定删除？")) return;
  try {
    const res = await request.post(`/api/dept/delete?id=${id}`);
    if (res?.code === 200) { showToast("删除成功"); fetchDepts(); }
  } catch (e) { console.error(e); }
};

// --- 设备管理逻辑 (新增) ---
const deviceList = ref([]);
const showDeviceForm = ref(false);
const newDevice = reactive({ deviceId: '', deviceName: '', deviceStatus: '正常', currentValue: '', longitude: '', latitude: '', address: '' });

const fetchDevices = async (categoryId) => {
  try {
    const res = await request.get(`/api/device/list?categoryId=${categoryId}`);
    deviceList.value = res || [];
  } catch (e) { console.error("获取设备失败:", e); }
};

const openAddDeviceForm = () => {
  // 生成一些默认坐标
  const baseLat = 29.04 + Math.random() * 0.02;
  const baseLng = 111.69 + Math.random() * 0.02;

  Object.assign(newDevice, {
    deviceId: '',
    deviceName: '',
    deviceStatus: '正常',
    currentValue: '',
    longitude: baseLng.toFixed(6),
    latitude: baseLat.toFixed(6),
    address: '模拟地址'
  });
  showDeviceForm.value = true;
};

const submitAddDevice = async () => {
  if (!newDevice.deviceId || !newDevice.deviceName) return showToast("ID和名称必填");

  const currentCatId = categoryMap[currentMenuName.value];
  if (!currentCatId) return showToast("分类获取失败，请重新选择菜单");

  // 确保数字字段是数字或 null
  const payload = {
    ...newDevice,
    categoryId: currentCatId,
    longitude: newDevice.longitude ? Number(newDevice.longitude) : null,
    latitude: newDevice.latitude ? Number(newDevice.latitude) : null
  };

  try {
    const res = await request.post('/api/device/add', payload);
    if (res?.code === 200) {
      showToast("设备入库成功");
      showDeviceForm.value = false;
      fetchDevices(currentCatId);
      // 触发刷新事件，通知外部组件更新
      eventBus.emit('refreshDeviceList');
    } else {
      showToast("入库失败: " + (res?.msg || '未知错误'));
    }
  } catch (e) {
    console.error("设备入库异常:", e);
    const errMsg = e.response?.data?.message || e.message || "请求异常";
    showToast("操作失败: " + errMsg);
  }
};

const handleDeleteDevice = async (id) => {
  if (!confirm("确定要报废该设备吗？此操作将记录日志。")) return;
  try {
    const res = await request.post(`/api/device/delete?id=${id}`);
    if (res?.code === 200) {
      showToast("设备已报废");
      fetchDevices(categoryMap[currentMenuName.value]);
      eventBus.emit('refreshDeviceList');
    } else {
      showToast("删除失败");
    }
  } catch (e) { console.error(e); }
};

// --- 日志/维护记录逻辑 ---
const systemLogs = ref([]);
const fetchLogs = async () => {
  try {
    const res = await request.get('/api/sys/logs?t=' + new Date().getTime());
    systemLogs.value = res || [];
  } catch (err) { console.error("获取日志失败:", err); }
};

const formatTime = (time) => {
  if (Array.isArray(time)) return `${time[0]}-${String(time[1]).padStart(2, '0')}-${String(time[2]).padStart(2, '0')} ${String(time[3]).padStart(2, '0')}:${String(time[4]).padStart(2, '0')}`;
  return time ? String(time).replace('T', ' ') : '-';
};

const getStatusColor = (status) => {
  if (status === '正常') return '#0f0';
  if (status === '告警' || status === '离线') return '#f00';
  return '#ffcc00';
};

// --- 通用交互 ---
const toastMsg = ref('');
const showSuccessToast = ref(false);
const showToast = (msg) => {
  toastMsg.value = msg;
  showSuccessToast.value = true;
  setTimeout(() => showSuccessToast.value = false, 1500);
};

watch(() => props.visible, (val) => {
  if (val) {
    activeIndex.value = 0;
    // 重置状态
    if (props.type === 'device') {
      // 默认加载第一个：电子仪器
      switchMenu(0);
    } else {
      switchMenu(0);
    }
  }
});
</script>

<style scoped>
/* 保持样式不变，复用即可 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.85);
  z-index: 999;
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s;
  backdrop-filter: blur(5px);
}

.modal-overlay.active {
  opacity: 1;
  pointer-events: auto;
}

.modal-content {
  width: 900px;
  height: 600px;
  background: #0b1221;
  border: 1px solid #00f2ff;
  box-shadow: 0 0 50px rgba(0, 242, 255, 0.15);
  display: flex;
  flex-direction: column;
  position: relative;
}

.modal-header {
  height: 60px;
  background: rgba(0, 242, 255, 0.05);
  border-bottom: 1px solid rgba(0, 242, 255, 0.3);
  display: flex;
  align-items: center;
  padding: 0 20px;
  justify-content: space-between;
}

.modal-title {
  font-size: 20px;
  color: #00f2ff;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  color: #000;
  font-weight: bold;
}

.user-badge.admin {
  background: #ffcc00;
}

.user-badge.user {
  background: #00f2ff;
}

.modal-controls {
  display: flex;
  gap: 20px;
  align-items: center;
}

.logout-btn {
  color: #a6adb4;
  cursor: pointer;
  font-size: 14px;
}

.logout-btn:hover {
  color: #ff2a2a;
}

.modal-close {
  font-size: 24px;
  color: #fff;
  cursor: pointer;
}

.modal-close:hover {
  color: #ff2a2a;
}

.modal-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.login-body {
  justify-content: center;
  align-items: center;
}

.login-box {
  width: 360px;
  padding: 40px;
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(0, 242, 255, 0.3);
  border-radius: 8px;
  text-align: center;
}

.login-box h3 {
  color: #00f2ff;
}

.mock-input,
select.mock-input {
  width: 100%;
  background: rgba(13, 26, 60, 0.6);
  border: 1px solid rgba(0, 242, 255, 0.3);
  color: #fff;
  padding: 10px;
  border-radius: 4px;
  outline: none;
  margin-top: 5px;
  box-sizing: border-box;
}

.mock-label {
  display: block;
  color: #00f2ff;
  text-align: left;
  font-size: 13px;
  margin-bottom: 5px;
}

.btn-tech {
  background: rgba(0, 242, 255, 0.1);
  border: 1px solid #00f2ff;
  color: #00f2ff;
  padding: 8px 30px;
  cursor: pointer;
  border-radius: 2px;
}

.btn-tech:hover {
  background: #00f2ff;
  color: #000;
}

.modal-sidebar {
  width: 220px;
  border-right: 1px solid rgba(0, 242, 255, 0.2);
  background: rgba(0, 242, 255, 0.02);
}

.modal-menu-item {
  padding: 15px 20px;
  color: #a6adb4;
  cursor: pointer;
  border-left: 3px solid transparent;
}

.modal-menu-item.active {
  background: rgba(0, 242, 255, 0.1);
  color: #00f2ff;
  border-left-color: #00f2ff;
}

.modal-main {
  flex: 1;
  padding: 30px;
  display: flex;
  flex-direction: column;
  background: rgba(0, 0, 0, 0.2);
}

.form-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  font-size: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 15px;
  margin-bottom: 20px;
}

.list-view {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.table-wrapper {
  flex: 1;
  overflow-y: auto;
}

.tech-table {
  width: 100%;
  border-collapse: collapse;
  color: #a6adb4;
  font-size: 13px;
}

.tech-table th {
  text-align: left;
  padding: 10px;
  color: #00f2ff;
  border-bottom: 1px solid rgba(0, 242, 255, 0.2);
  background: rgba(13, 26, 60, 0.95);
  position: sticky;
  top: 0;
}

.tech-table td {
  padding: 10px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.05);
}

.status-tag {
  padding: 2px 6px;
  border-radius: 2px;
  font-size: 12px;
  border: 1px solid;
}

.admin-tag {
  color: #ffcc00;
  border-color: #ffcc00;
  background: rgba(255, 204, 0, 0.1);
}

.user-tag {
  color: #00f2ff;
  border-color: #00f2ff;
  background: rgba(0, 242, 255, 0.1);
}

.action-text {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
}

.action-text.delete {
  color: #ff2a2a;
}

.action-text.delete:hover {
  text-decoration: underline;
}

.sub-modal {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10;
}

.sub-modal-box {
  width: 400px;
  background: #0b1221;
  border: 1px solid #00f2ff;
  padding: 20px;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.8);
}

.sub-modal-box h3 {
  color: #fff;
  text-align: center;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(0, 242, 255, 0.3);
  padding-bottom: 10px;
}

.sub-form {
  margin-bottom: 20px;
}

.sub-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-tech.small {
  padding: 5px 15px;
  font-size: 12px;
}

.login-form,
.mock-form-row {
  margin-bottom: 15px;
}

.success-toast {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(5, 10, 21, 0.95);
  border: 1px solid #00f2ff;
  padding: 15px 30px;
  z-index: 20;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #00f2ff;
  font-weight: bold;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
