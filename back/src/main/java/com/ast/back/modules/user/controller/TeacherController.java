package com.ast.back.modules.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ast.back.shared.common.Result;
import com.ast.back.modules.classmgmt.application.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private ClassService classService;

    @GetMapping("/stats")
    public Result getStats() {
        Long teacherId = StpUtil.getLoginIdAsLong();
        Map<String, Object> stats = classService.getTeacherStats(teacherId);
        return Result.success(stats);
    }
}