package com.ast.back.modules.user.application.impl;

import com.ast.back.modules.user.persistence.entity.StudentInfo;
import com.ast.back.modules.user.persistence.mapper.StudentInfoMapper;
import com.ast.back.modules.user.application.StudentInfoService;
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
