package com.example.smartcity.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@TableName("iot_device")
public class IotDevice {
    @TableId
    private String deviceId;
    private Integer categoryId; // 1:路灯, 2:井盖, 3:消防, 4:传感
    private String deviceName;
    private String deviceStatus;
    private String currentValue;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private LocalDateTime lastUpdate;
}