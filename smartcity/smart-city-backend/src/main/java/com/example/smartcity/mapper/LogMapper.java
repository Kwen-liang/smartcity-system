package com.example.smartcity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcity.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogMapper extends BaseMapper<SysLog> {
}