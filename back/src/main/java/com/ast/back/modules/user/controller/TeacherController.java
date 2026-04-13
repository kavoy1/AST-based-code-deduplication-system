package com.ast.back.modules.user.controller;

import com.ast.back.modules.classmgmt.application.ClassService;
import com.ast.back.shared.common.Result;
import com.ast.back.shared.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final ClassService classService;
    private final CurrentUserService currentUserService;

    public TeacherController(ClassService classService, CurrentUserService currentUserService) {
        this.classService = classService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/stats")
    public Result getStats() {
        Long teacherId = currentUserService.getCurrentUserId();
        Map<String, Object> stats = classService.getTeacherStats(teacherId);
        return Result.success(stats);
    }
}
