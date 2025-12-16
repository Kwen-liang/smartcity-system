package com.example.smartcity.task;

import com.example.smartcity.entity.IotDevice;
import com.example.smartcity.entity.IotDeviceHistory;
import com.example.smartcity.mapper.DeviceHistoryMapper;
import com.example.smartcity.mapper.DeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DeviceTask {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceHistoryMapper historyMapper;

    /**
     * 每小时执行一次 (整点)
     * 记录所有设备当前的状态快照
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void recordDeviceSnapshot() {
        List<IotDevice> devices = deviceMapper.selectList(null);
        LocalDateTime now = LocalDateTime.now();

        for (IotDevice device : devices) {
            IotDeviceHistory history = new IotDeviceHistory();
            history.setDeviceId(device.getDeviceId());
            history.setValue(device.getCurrentValue());
            history.setStatus(device.getDeviceStatus());
            history.setRecordTime(now);

            historyMapper.insert(history);
        }
        System.out.println("执行定时任务：已记录 " + devices.size() + " 条设备历史数据 - " + now);
    }
}