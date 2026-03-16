package com.ast.back.service;

import com.ast.back.entity.StudentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface StudentInfoService extends IService<StudentInfo> {
    List<Map<String, Object>> getStudentList(String keyword, String college, String className);
    List<String> getColleges();
    List<String> getClasses(String college);
}
