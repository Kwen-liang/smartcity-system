package com.example.smartcity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcity.entity.IotDeviceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceHistoryMapper extends BaseMapper<IotDeviceHistory> {

    /**
     * 查询过去24小时每小时的平均数值（去除单位后的数字部分）
     * 注意：这里假设 value 包含单位 (如 "220V")，我们需要在 Java 层处理，
     * 或者这里简单返回所有历史记录由 Java 处理聚合。
     * 为了简化 SQL 兼容性，我们获取最近24小时的记录，在 Service/Controller 层处理。
     */
    @Select("SELECT * FROM iot_device_history " +
            "WHERE record_time >= NOW() - INTERVAL 24 HOUR " +
            "ORDER BY record_time ASC")
    List<IotDeviceHistory> getLast24hRecords();
}