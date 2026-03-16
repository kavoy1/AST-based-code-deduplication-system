package com.ast.back.service.impl;

import com.ast.back.entity.StudentInfo;
import com.ast.back.mapper.StudentInfoMapper;
import com.ast.back.service.StudentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StudentInfoServiceImpl extends ServiceImpl<StudentInfoMapper, StudentInfo> implements StudentInfoService {
    
    @Override
    public List<Map<String, Object>> getStudentList(String keyword, String college, String className) {
        return baseMapper.getStudentList(keyword, college, className);
    }

    @Override
    public List<String> getColleges() {
        return baseMapper.getDistinctColleges();
    }

    @Override
    public List<String> getClasses(String college) {
        return baseMapper.getDistinctClasses(college);
    }
}
