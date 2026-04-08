package com.ast.back.modules.classmgmt.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ast.back.modules.classmgmt.application.ClassService;
import com.ast.back.modules.classmgmt.persistence.entity.Clazz;
import com.ast.back.shared.common.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/classes")
public class ClassController {

    @Autowired
    private ClassService classService;

    @GetMapping
    public Result listClasses(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int limit) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        IPage<Clazz> result = classService.getClassesByTeacherId(teacherId, page, limit);
        return Result.success(result);
    }

    @GetMapping("/applications")
    public Result listApplications() {
        Long teacherId = StpUtil.getLoginIdAsLong();
        return Result.success(classService.getPendingApplications(teacherId));
    }

    @GetMapping("/students")
    public Result listTeacherStudents() {
        Long teacherId = StpUtil.getLoginIdAsLong();
        List<Map<String, Object>> students = classService.getStudentsByTeacherId(teacherId);
        return Result.success(students);
    }

    @PostMapping("/applications/{id}/approve")
    public Result approveApplication(@PathVariable Integer id) {
        boolean success = classService.approveApplication(id);
        return success ? Result.success() : Result.error("操作失败");
    }

    @PostMapping("/applications/{id}/reject")
    public Result rejectApplication(@PathVariable Integer id) {
        boolean success = classService.rejectApplication(id);
        return success ? Result.success() : Result.error("操作失败");
    }

    @GetMapping("/{classId}/students")
    public Result listStudents(@PathVariable Integer classId) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        List<Map<String, Object>> students = classService.getStudentsByClassId(classId, teacherId);
        return Result.success(students);
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    public Result removeStudent(@PathVariable Integer classId, @PathVariable Long studentId) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        boolean success = classService.removeStudentFromClass(classId, studentId, teacherId);
        return success ? Result.success("移除成功") : Result.error("移除失败");
    }

    @PostMapping("/{classId}/students/invite")
    public Result inviteStudent(@PathVariable Integer classId, @RequestParam Long studentId) {
        boolean success = classService.inviteStudentToClass(classId, studentId);
        return success ? Result.success("邀请成功") : Result.error("邀请失败");
    }

    @PostMapping("/create")
    public Result createClass(@RequestBody Clazz clazz) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        clazz.setTeacherId(teacherId);

        boolean success = classService.createNewClass(clazz);
        return success ? Result.success(clazz) : Result.error("创建失败");
    }

    @GetMapping("/{id}")
    public Result getClass(@PathVariable Integer id) {
        Clazz clazz = classService.getById(id);
        return Result.success(clazz);
    }

    @DeleteMapping("/{id}")
    public Result deleteClass(@PathVariable Integer id) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        boolean success = classService.deleteClass(id, teacherId);
        return success ? Result.success("班级已解散") : Result.error("班级不存在或无权解散");
    }
}
