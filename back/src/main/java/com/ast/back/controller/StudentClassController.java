package com.ast.back.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ast.back.common.Result;
import com.ast.back.entity.Clazz;
import com.ast.back.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/classes")
public class StudentClassController {

    @Autowired
    private ClassService classService;

    @GetMapping
    public Result listMyClasses() {
        Long studentId = StpUtil.getLoginIdAsLong();
        List<Map<String, Object>> classes = classService.getStudentClasses(studentId);
        return Result.success(classes);
    }

    @PostMapping("/join")
    public Result joinClass(@RequestParam String inviteCode) {
        Long studentId = StpUtil.getLoginIdAsLong();
        String result = classService.joinClass(studentId, inviteCode);
        if ("success".equals(result)) {
            return Result.success("申请成功，请等待老师审核");
        } else {
            return Result.error(result);
        }
    }
}
