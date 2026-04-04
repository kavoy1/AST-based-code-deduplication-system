package com.ast.back.modules.user.controller;

import com.ast.back.shared.common.Result;
import com.ast.back.modules.user.application.StudentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentInfoService studentInfoService;

    @GetMapping("/list")
    public Result getStudentList(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String college,
                                 @RequestParam(required = false) String className) {
        List<Map<String, Object>> list = studentInfoService.getStudentList(keyword, college, className);
        return Result.success(list);
    }

    @GetMapping("/colleges")
    public Result getColleges() {
        return Result.success(studentInfoService.getColleges());
    }

    @GetMapping("/class-options")
    public Result getClasses(@RequestParam(required = false) String college) {
        return Result.success(studentInfoService.getClasses(college));
    }
}
