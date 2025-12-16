// 简单的发布订阅模式实现
const eventBus = {
  events: new Map(),

  // 订阅事件
  on(event, callback) {
    if (!this.events.has(event)) {
      this.events.set(event, [])
    }
    this.events.get(event).push(callback)
  },

  // 发布事件
  emit(event, data) {
    if (this.events.has(event)) {
      this.events.get(event).forEach(callback => callback(data))
    }
  },

  // 取消订阅
  off(event, callback) {
    if (this.events.has(event)) {
      const callbacks = this.events.get(event)
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }
}

export default eventBus
