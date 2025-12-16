package com.example.smartcity.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartcity.entity.IotDevice;
import com.example.smartcity.entity.IotDeviceHistory;
import com.example.smartcity.entity.SysLog;
import com.example.smartcity.mapper.DeviceHistoryMapper;
import com.example.smartcity.mapper.DeviceMapper;
import com.example.smartcity.mapper.LogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/device")
@CrossOrigin(origins = "*")
public class DeviceController {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private DeviceHistoryMapper historyMapper; // 注入历史 Mapper

    // ... (list 接口保持不变) ...
    @GetMapping("/list")
    public List<IotDevice> getAllDevices(@RequestParam(required = false) Integer categoryId) {
        QueryWrapper<IotDevice> query = new QueryWrapper<>();
        if (categoryId != null) {
            query.eq("category_id", categoryId);
        }
        return deviceMapper.selectList(query);
    }

    @PostMapping("/add")
    public Map<String, Object> addDevice(@RequestBody IotDevice device, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            device.setLastUpdate(LocalDateTime.now());
            if (device.getDeviceStatus() == null) device.setDeviceStatus("正常");

            int result = deviceMapper.insert(device);
            if (result > 0) {
                // 新增设备时，记录第一条历史数据
                saveHistory(device);

                res.put("code", 200);
                res.put("msg", "Success");
                recordLog(request, "设备入库: " + device.getDeviceName(), "1");
            } else {
                res.put("code", 500);
                res.put("msg", "添加失败");
                recordLog(request, "设备入库失败: " + device.getDeviceName(), "0");
            }
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "Error: " + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }

    @PostMapping("/update")
    public Map<String, Object> updateDevice(@RequestBody IotDevice device, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            device.setLastUpdate(LocalDateTime.now());
            int result = deviceMapper.updateById(device);
            if (result > 0) {
                // 更新设备状态时，记录历史数据
                saveHistory(device);

                res.put("code", 200);
                res.put("msg", "Success");
                recordLog(request, "设备维护更新: " + device.getDeviceId(), "1");
            } else {
                res.put("code", 500);
                res.put("msg", "更新失败");
            }
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "Error: " + e.getMessage());
        }
        return res;
    }

    // ... (delete, stats, map-points 接口保持不变) ...
    @PostMapping("/delete")
    public Map<String, Object> deleteDevice(@RequestParam String id, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            int result = deviceMapper.deleteById(id);
            if (result > 0) {
                res.put("code", 200);
                res.put("msg", "Success");
                recordLog(request, "设备报废删除: " + id, "1");
            } else {
                res.put("code", 500);
                res.put("msg", "删除失败");
                recordLog(request, "设备删除失败: " + id, "0");
            }
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "Error: " + e.getMessage());
        }
        return res;
    }

    @GetMapping("/stats")
    public List<Map<String, Object>> getDeviceStats() {
        return deviceMapper.getDeviceStatsByCategory();
    }

    @GetMapping("/map-points")
    public List<Map<String, Object>> getMapPoints() {
        QueryWrapper<IotDevice> query = new QueryWrapper<>();
        query.select("device_id as deviceId", "device_name as deviceName", "device_status as deviceStatus",
                "longitude", "latitude", "category_id as categoryId",
                "address", "current_value as currentValue", "last_update as lastUpdate");
        query.isNotNull("longitude");
        return deviceMapper.selectMaps(query);
    }

    /**
     * 7. 获取真实报警趋势 (用于折线图)
     * 逻辑：从历史表中查询过去24小时的数据，提取数值部分计算平均值
     */
    @GetMapping("/trend")
    public Map<String, Object> getAlarmTrend() {
        Map<String, Object> result = new HashMap<>();
        List<String> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:00");
        LocalDateTime now = LocalDateTime.now();

        // 1. 获取过去24小时的历史数据
        List<IotDeviceHistory> historyList = historyMapper.getLast24hRecords();

        // 第一次运行时如果没有数据，生成一些模拟历史数据以防图表空白
        if (historyList.isEmpty()) {
            generateMockHistory(now);
            historyList = historyMapper.getLast24hRecords();
        }

        // 2. 按小时分组数据 (LinkedHashMap 保持顺序)
        Map<String, List<Double>> groupedData = new LinkedHashMap<>();

        // 初始化时间轴：每4小时一个点 (24, 20, 16, 12, 8, 4, 0)
        for (int i = 24; i >= 0; i -= 4) {
            String key = now.minusHours(i).format(formatter);
            groupedData.put(key, new ArrayList<>());
        }

        // 3. 将历史数据填入对应的时间桶
        for (IotDeviceHistory h : historyList) {
            // 提取数值 (例如 "220V" -> 220.0)
            Double val = extractValue(h.getValue());
            if (val != null) {
                String recordKey = h.getRecordTime().format(formatter);
                // 简单归类：寻找最近的 key
                for (String key : groupedData.keySet()) {
                    if (key.equals(recordKey)) {
                        groupedData.get(key).add(val);
                        break;
                    }
                }
            }
        }

        // 4. 计算每个时间点的平均值
        for (Map.Entry<String, List<Double>> entry : groupedData.entrySet()) {
            times.add(entry.getKey());
            List<Double> vals = entry.getValue();
            if (vals.isEmpty()) {
                values.add(0.0); // 无数据补0
            } else {
                // 计算平均值并保留1位小数
                double avg = vals.stream().mapToDouble(d -> d).average().orElse(0.0);
                values.add((double) Math.round(avg * 10) / 10);
            }
        }

        result.put("xAxis", times);
        result.put("series", values);
        return result;
    }

    // --- 辅助方法 ---

    // 从字符串提取数字 (支持 "220V", "35.5°C" 等格式)
    private Double extractValue(String valStr) {
        if (valStr == null) return null;
        try {
            Matcher m = Pattern.compile("(\\d+(\\.\\d+)?)").matcher(valStr);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception e) {}
        return null;
    }

    // 保存历史记录
    private void saveHistory(IotDevice device) {
        IotDeviceHistory h = new IotDeviceHistory();
        h.setDeviceId(device.getDeviceId());
        h.setValue(device.getCurrentValue());
        h.setStatus(device.getDeviceStatus());
        h.setRecordTime(LocalDateTime.now());
        historyMapper.insert(h);
    }

    // 生成初始模拟数据 (仅用于演示，生产环境可删除)
    private void generateMockHistory(LocalDateTime end) {
        Random random = new Random();
        for (int i = 0; i < 24; i++) { // 过去24小时
            LocalDateTime time = end.minusHours(i);
            for (int j = 0; j < 3; j++) {
                IotDeviceHistory h = new IotDeviceHistory();
                h.setDeviceId("INIT-" + j);
                h.setValue((random.nextInt(30) + 10) + ""); // 模拟数值
                h.setStatus("正常");
                h.setRecordTime(time);
                historyMapper.insert(h);
            }
        }
    }

    private void recordLog(HttpServletRequest request, String action, String status) {
        try {
            SysLog log = new SysLog();
            log.setUsername("admin");
            log.setIpAddress(request.getRemoteAddr());
            log.setAction(action);
            log.setStatus(status);
            log.setCreateTime(LocalDateTime.now());
            logMapper.insert(log);
        } catch (Exception e) {
            System.err.println("日志记录失败: " + e.getMessage());
        }
    }
}