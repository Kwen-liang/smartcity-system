package com.example.smartcity.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartcity.entity.SysDept;
import com.example.smartcity.entity.SysLog;
import com.example.smartcity.mapper.DeptMapper;
import com.example.smartcity.mapper.LogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dept")
@CrossOrigin(origins = "*")
public class DeptController {

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private LogMapper logMapper; // 1. 注入日志 Mapper

    // 1. 获取部门列表
    @GetMapping("/list")
    public List<SysDept> getDeptList() {
        return deptMapper.selectList(null);
    }

    // 2. 新增部门
    @PostMapping("/add")
    public Map<String, Object> addDept(@RequestBody SysDept dept, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            dept.setCreateTime(LocalDateTime.now());
            int result = deptMapper.insert(dept);
            if (result > 0) {
                res.put("code", 200);
                res.put("msg", "Success");
                // 记录成功日志
                recordLog(request, "新增部门: " + dept.getDeptName(), "1");
            } else {
                res.put("code", 500);
                res.put("msg", "添加失败");
                // 记录失败日志
                recordLog(request, "新增部门失败: " + dept.getDeptName(), "0");
            }
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "Error: " + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }

    // 3. 更新部门
    @PostMapping("/update")
    public Map<String, Object> updateDept(@RequestBody SysDept dept, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            int result = deptMapper.updateById(dept);
            if (result > 0) {
                res.put("code", 200);
                res.put("msg", "Success");
                // 记录更新日志
                recordLog(request, "更新部门ID: " + dept.getDeptId(), "1");
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

    // 4. 删除部门
    @PostMapping("/delete")
    public Map<String, Object> deleteDept(@RequestParam Integer id, HttpServletRequest request) {
        Map<String, Object> res = new HashMap<>();
        try {
            int result = deptMapper.deleteById(id);
            if (result > 0) {
                res.put("code", 200);
                res.put("msg", "Success");
                // 记录删除日志
                recordLog(request, "删除部门ID: " + id, "1");
            } else {
                res.put("code", 500);
                res.put("msg", "删除失败");
                recordLog(request, "删除部门失败ID: " + id, "0");
            }
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "Error: " + e.getMessage());
        }
        return res;
    }

    // 辅助方法：记录日志
    private void recordLog(HttpServletRequest request, String action, String status) {
        try {
            SysLog log = new SysLog();
            // 这里简单记录为 admin，如果有 token 解析逻辑可以获取真实用户名
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