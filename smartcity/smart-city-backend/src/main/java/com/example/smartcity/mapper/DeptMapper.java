package com.example.smartcity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartcity.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeptMapper extends BaseMapper<SysDept> {
}