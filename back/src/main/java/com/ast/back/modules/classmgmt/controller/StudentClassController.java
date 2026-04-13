package com.ast.back.modules.classmgmt.controller;

import com.ast.back.modules.classmgmt.application.ClassService;
import com.ast.back.shared.common.Result;
import com.ast.back.shared.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/classes")
public class StudentClassController {

    private final ClassService classService;
    private final CurrentUserService currentUserService;

    public StudentClassController(ClassService classService, CurrentUserService currentUserService) {
        this.classService = classService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Result listMyClasses() {
        Long studentId = currentUserService.getCurrentUserId();
        List<Map<String, Object>> classes = classService.getStudentClasses(studentId);
        return Result.success(classes);
    }

    @PostMapping("/join")
    public Result joinClass(@RequestParam String inviteCode) {
        Long studentId = currentUserService.getCurrentUserId();
        String result = classService.joinClass(studentId, inviteCode);
        if ("success".equals(result)) {
            return Result.success("申请成功，请等待老师审核");
        }
        return Result.error(result);
    }
}
