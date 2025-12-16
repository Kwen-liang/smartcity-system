package com.example.smartcity.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartcity.entity.SysLog;
import com.example.smartcity.mapper.LogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys")
@CrossOrigin(origins = "*")
public class SystemController {

    @Autowired
    private LogMapper logMapper;

    /**
     * 获取系统日志列表 (最近 20 条)
     * GET /api/sys/logs
     */
    @GetMapping("/logs")
    public List<SysLog> getLogs() {
        System.out.println("收到获取日志请求 /api/sys/logs"); // 添加日志

        QueryWrapper<SysLog> query = new QueryWrapper<>();
        // 确保数据库中是 create_time
        query.orderByDesc("create_time");
        query.last("limit 20"); // 只取最近20条

        List<SysLog> logs = logMapper.selectList(query);
        System.out.println("查询到日志数量: " + (logs != null ? logs.size() : 0)); // 添加日志

        return logs;
    }
}