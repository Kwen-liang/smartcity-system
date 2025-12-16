package com.example.smartcity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_device_history")
public class IotDeviceHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String value;
    private String status;
    private LocalDateTime recordTime;
}