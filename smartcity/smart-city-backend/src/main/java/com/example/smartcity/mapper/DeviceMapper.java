package com.example.smartcity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcity.entity.IotDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceMapper extends BaseMapper<IotDevice> {

    /**
     * 统计各分类下的设备数量（用于前端饼图）
     * 联表查询 iot_device 和 iot_device_category
     */
    @Select("SELECT c.category_name as name, COUNT(d.device_id) as value " +
            "FROM iot_device d " +
            "LEFT JOIN iot_device_category c ON d.category_id = c.category_id " +
            "GROUP BY c.category_name")
    List<Map<String, Object>> getDeviceStatsByCategory();

    /**
     * 统计各状态的设备数量
     */
    @Select("SELECT device_status as name, COUNT(*) as value FROM iot_device GROUP BY device_status")
    List<Map<String, Object>> getDeviceStatsByStatus();
}