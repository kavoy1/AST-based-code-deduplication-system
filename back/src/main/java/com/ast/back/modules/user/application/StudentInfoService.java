package com.ast.back.modules.user.application;

import com.ast.back.modules.user.persistence.entity.StudentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface StudentInfoService extends IService<StudentInfo> {
    List<Map<String, Object>> getStudentList(String keyword, String college, String className);
    List<String> getColleges();
    List<String> getClasses(String college);
}
