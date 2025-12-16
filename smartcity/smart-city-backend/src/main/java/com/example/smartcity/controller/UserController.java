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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*") // 允许跨域
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LogMapper logMapper; // 注入日志 Mapper

    /**
     * 1. 获取用户列表
     * GET /api/user/list
     */
    @GetMapping("/list")
    public List<SysUser> getUserList() {
        return userMapper.selectList(null);
    }

    /**
     * 2. 新增用户 (注册)
     * POST /api/user/add
     */
    @PostMapping("/add")
    public Map<String, Object> addUser(@RequestBody SysUser user, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();

        try {
            // 1. 校验用户名是否存在
            QueryWrapper<SysUser> query = new QueryWrapper<>();
            query.eq("username", user.getUsername());
            if (userMapper.selectCount(query) > 0) {
                res.put("code", 400);
                res.put("msg", "Error: 用户名已存在");
                return res;
            }

            // 2. 设置默认值
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword("123456");
            }
            user.setCreateTime(LocalDateTime.now());

            // 3. 执行插入
            int result = userMapper.insert(user);

            if (result > 0) {
                res.put("code", 200);
                res.put("msg", "Success");

                // 记录日志
                recordLog(request, "新增用户: " + user.getUsername(), "1");
            } else {
                res.put("code", 500);
                res.put("msg", "数据库插入失败");
                recordLog(request, "新增用户失败: " + user.getUsername(), "0");
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.put("code", 500);
            res.put("msg", "服务器内部错误: " + e.getMessage());
        }

        return res;
    }

    /**
     * 3. 修改用户
     * POST /api/user/update
     */
    @PostMapping("/update")
    public Map<String, Object> updateUser(@RequestBody SysUser user, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            int result = userMapper.updateById(user);
            if (result > 0) {
                res.put("code", 200);
                res.put("msg", "Success");
                recordLog(request, "更新用户ID: " + user.getUserId(), "1");
            } else {
                res.put("code", 500);
                res.put("msg", "更新失败，用户可能不存在");
            }
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "更新异常: " + e.getMessage());
        }
        return res;
    }

    /**
     * 4. 删除用户
     * POST /api/user/delete?id=1
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteUser(@RequestParam Integer id, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            int result = userMapper.deleteById(id);
            if (result > 0) {
                res.put("code", 200);
                res.put("msg", "Success");
                recordLog(request, "删除用户ID: " + id, "1");
            } else {
                res.put("code", 500);
                res.put("msg", "删除失败");
                recordLog(request, "删除用户失败ID: " + id, "0");
            }
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "删除异常: " + e.getMessage());
        }
        return res;
    }

    // 辅助方法：记录日志
    private void recordLog(HttpServletRequest request, String action, String status) {
        try {
            SysLog log = new SysLog();
            log.setUsername("admin"); // 这里假设只有admin能操作，简化处理
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