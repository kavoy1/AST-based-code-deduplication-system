package com.ast.back.modules.admin.controller;

import com.ast.back.shared.common.Result;
import com.ast.back.modules.notice.persistence.entity.Notice;
import com.ast.back.modules.user.persistence.entity.User;
import com.ast.back.modules.notice.application.NoticeService;
import com.ast.back.modules.user.application.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;
    @Autowired
    private NoticeService noticeService;


    /**
     * 获取所有用户
     * 对应研究思路：管理系统用户
     */
    @GetMapping("/users")
    public Result<IPage<User>> getAllUsers(@RequestParam(defaultValue = "1") Long page,
                                           @RequestParam(defaultValue = "10") Long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String sortBy,
                                           @RequestParam(required = false) String sortOrder) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String text = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, text)
                    .or().like(User::getNickname, text)
                    .or().like(User::getEmail, text)
                    .or().like(User::getUid, text));
        }
        if (role != null && !role.trim().isEmpty()) {
            wrapper.eq(User::getRole, role.trim());
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        boolean desc = "desc".equalsIgnoreCase(sortOrder);
        if ("id".equalsIgnoreCase(sortBy)) {
            wrapper.orderBy(true, !desc, User::getId);
        } else if ("username".equalsIgnoreCase(sortBy)) {
            wrapper.orderBy(true, !desc, User::getUsername);
        } else if ("role".equalsIgnoreCase(sortBy)) {
            wrapper.orderBy(true, !desc, User::getRole);
        } else if ("status".equalsIgnoreCase(sortBy)) {
            wrapper.orderBy(true, !desc, User::getStatus);
        } else if ("createTime".equalsIgnoreCase(sortBy)) {
            wrapper.orderBy(true, !desc, User::getCreateTime);
        } else {
            wrapper.orderByDesc(User::getCreateTime);
        }

        Page<User> pageInfo = new Page<>(page, size);
        IPage<User> list = userService.page(pageInfo, wrapper);
        return Result.success(list);
    }

    /**
     * 修改用户状态 (启用/禁用)
     * @param id 用户ID
     * @param status 1-启用, 0-禁用
     */
    @PutMapping("/users/status")
    public Result changeStatus(@RequestParam Long id, @RequestParam Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);

        // 调用 MyBatis-Plus 的 updateById
        boolean updated = userService.updateById(user);
        return updated ? Result.success("状态修改成功") : Result.error("修改失败");
    }

    @DeleteMapping("/users/{id}")
    public Result deleteUser(@PathVariable Long id) {
        // 权限检查：确保当前操作人是管理员（如果你集成了 Spring Security）
        // 逻辑删除或物理删除
        userService.removeById(id);
        return Result.success("用户删除成功");
    }
    /**
     * 发布系统通知
     */
    @PostMapping("/notices")
    public Result publishNotice(@RequestBody Notice notice, HttpSession session) {
        // 1. 获取当前登录管理员的 ID
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            notice.setAuthorId(currentUser.getId());
        }

        // 2. 保存通知
        boolean saved = noticeService.save(notice);
        return saved ? Result.success("通知发布成功") : Result.error("发布失败");
    }
    /**
     * 获取所有通知列表（用于后台表格展示）
     */
    @GetMapping("/notices")
    public Result getAllNotices() {
        List<Notice> list = noticeService.listAllNotices();
        return Result.success(list);
    }
    /**
     * 删除通知
     */
    @DeleteMapping("/notices/{id}")
    public Result deleteNotice(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success("通知已撤销");
    }

    /**
     * 获取首页统计数据
     */
    @GetMapping("/stats")
    public Result getStats() {
        String status = "检测中";
        long userCount = 0;
        long noticeCount = 0;
        long studentCount = 0;
        long teacherCount = 0;
        long adminCount = 0;
        List<Long> weeklyVisits = new ArrayList<>();
        
        String resource = "计算中...";
        try {
            // 尝试执行数据库查询，如果成功则说明系统和数据库连接正常
            userCount = userService.count();
            noticeCount = noticeService.count();
            
            // 统计各角色人数
            studentCount = userService.lambdaQuery().eq(User::getRole, "STUDENT").count();
            teacherCount = userService.lambdaQuery().eq(User::getRole, "TEACHER").count();
            adminCount = userService.lambdaQuery().eq(User::getRole, "ADMIN").count();
            
            // 统计本周访问量 (周一到周日) - 已废弃
            for (int i = 0; i < 7; i++) {
                weeklyVisits.add(0L);
            }

            status = "运行正常";
            
            // 获取真实内存占用
            try {
                OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                long totalMem = osBean.getTotalPhysicalMemorySize();
                long freeMem = osBean.getFreePhysicalMemorySize();
                long usedMem = totalMem - freeMem;
                
                long usedPercent = (usedMem * 100) / totalMem;
                long totalGB = totalMem / (1024 * 1024 * 1024);
                
                resource = usedPercent + "% / " + totalGB + "GB";
            } catch (Exception e) {
                // 如果无法获取系统内存，降级获取 JVM 内存
                long total = Runtime.getRuntime().totalMemory();
                long free = Runtime.getRuntime().freeMemory();
                long used = total - free;
                long usedPercent = (used * 100) / total;
                long totalMB = total / (1024 * 1024);
                resource = usedPercent + "% / JVM " + totalMB + "MB";
            }
        } catch (Exception e) {
            // 捕获异常，说明系统或数据库存在问题
            status = "服务异常";
            e.printStackTrace();
        }

        return Result.success(java.util.Map.of(
            "userCount", userCount,
            "noticeCount", noticeCount,
            "status", status,
            "resource", resource,
            "studentCount", studentCount,
            "teacherCount", teacherCount,
            "adminCount", adminCount,
            "weeklyVisits", weeklyVisits
        ));
    }
}
