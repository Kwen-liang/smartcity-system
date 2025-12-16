package com.example.smartcity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private String username;
    private String ipAddress;
    private String action;
    private String status; // 1成功 0失败
    private LocalDateTime createTime;
}