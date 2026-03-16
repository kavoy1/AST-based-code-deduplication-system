package com.ast.back.controller;

import com.ast.back.common.Result;
import com.ast.back.entity.Feedback;
import com.ast.back.entity.User;
import com.ast.back.service.FeedbackService;
import com.ast.back.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;
    
    @Autowired
    private UserService userService;

    /**
     * 提交反馈
     */
    @PostMapping("/submit")
    public Result submit(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        
        feedbackService.submitFeedback(userId, title, content);
        return Result.success();
    }

    /**
     * 获取用户自己的反馈列表
     */
    @GetMapping("/my")
    public Result myFeedback(@RequestParam Long userId) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Feedback::getUserId, userId);
        wrapper.orderByDesc(Feedback::getCreateTime);
        List<Feedback> list = feedbackService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 获取所有反馈列表（管理员）
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page, 
                       @RequestParam(defaultValue = "10") Integer size,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword) {
        
        Page<Feedback> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Feedback::getStatus, status);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like(Feedback::getTitle, keyword)
                                   .or()
                                   .like(Feedback::getContent, keyword));
        }
        
        queryWrapper.orderByDesc(Feedback::getCreateTime);
        
        Page<Feedback> resultPage = feedbackService.page(pageParam, queryWrapper);
        
        // 填充提交人信息
        List<Feedback> records = resultPage.getRecords();
        if (!records.isEmpty()) {
            List<Long> userIds = records.stream().map(Feedback::getUserId).collect(Collectors.toList());
            Map<Long, User> userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
            
            for (Feedback feedback : records) {
                User user = userMap.get(feedback.getUserId());
                if (user != null) {
                    feedback.setSubmitterName(user.getNickname());
                    feedback.setSubmitterUid(user.getUid());
                    feedback.setSubmitterRole(user.getRole());
                } else {
                    feedback.setSubmitterName("未知用户");
                    feedback.setSubmitterUid("未知");
                    feedback.setSubmitterRole("未知");
                }
            }
        }
        
        return Result.success(resultPage);
    }

    /**
     * 标记为已解决
     */
    @PostMapping("/resolve/{id}")
    public Result resolve(@PathVariable Long id) {
        feedbackService.resolveFeedback(id);
        return Result.success();
    }

    /**
     * 回复反馈
     */
    @PostMapping("/reply/{id}")
    public Result reply(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String reply = params.get("reply");
        feedbackService.replyFeedback(id, reply);
        return Result.success();
    }

    /**
     * 更新状态
     */
    @PostMapping("/status/{id}")
    public Result updateStatus(@PathVariable Long id, @RequestParam String status) {
        feedbackService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 获取今日新增数量
     */
    @GetMapping("/stats/today")
    public Result getTodayCount() {
        long count = feedbackService.getTodayCount();
        return Result.success(count);
    }
}
