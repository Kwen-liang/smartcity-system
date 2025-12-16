package com.example.smartcity.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartcity.entity.SysLog;
import com.example.smartcity.entity.SysUser;
import com.example.smartcity.mapper.LogMapper;
import com.example.smartcity.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // 允许前端跨域
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LogMapper logMapper; // 1. 注入日志 Mapper

    /**
     * 登录接口
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData, HttpServletRequest request) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Map<String, Object> result = new HashMap<>();

        // 查询用户
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", username);
        SysUser user = userMapper.selectOne(query);

        // 准备日志对象
        SysLog log = new SysLog();
        log.setUsername(username);
        log.setIpAddress(request.getRemoteAddr()); // 获取真实 IP
        log.setCreateTime(LocalDateTime.now());

        if (user != null && user.getPassword().equals(password)) {
            // 登录成功
            result.put("code", 200);
            result.put("msg", "登录成功");
            result.put("role", user.getRoleId() == 1 ? "admin" : "user");
            result.put("token", "mock-token-" + System.currentTimeMillis());
            result.put("userInfo", user);

            // 记录成功日志
            log.setAction("登录系统");
            log.setStatus("1"); // 1 表示成功
            logMapper.insert(log);
        } else {
            // 登录失败
            result.put("code", 401);
            result.put("msg", "用户名或密码错误");

            // 记录失败日志
            log.setAction("尝试登录失败 (密码错误)");
            log.setStatus("0"); // 0 表示失败
            logMapper.insert(log);
        }
        return result;
    }
}