package com.ast.back.modules.notice.controller;

import com.ast.back.modules.notice.application.NoticeService;
import com.ast.back.modules.notice.persistence.entity.Notice;
import com.ast.back.shared.common.Result;
import com.ast.back.shared.security.CurrentUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeService noticeService;
    private final CurrentUserService currentUserService;

    public NoticeController(NoticeService noticeService, CurrentUserService currentUserService) {
        this.noticeService = noticeService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/list")
    public Result list() {
        Long userId = currentUserService.getCurrentUserId();
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Notice::getReceiverId, userId).or().isNull(Notice::getReceiverId));
        wrapper.orderByDesc(Notice::getCreateTime);
        List<Notice> list = noticeService.list(wrapper);
        return Result.success(list);
    }

    @PostMapping("/read/{id}")
    public Result markAsRead(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        Long userId = currentUserService.getCurrentUserId();
        if (notice != null && Objects.equals(userId, notice.getReceiverId())) {
            notice.setIsRead(1);
            noticeService.updateById(notice);
            return Result.success();
        }
        return Result.error("操作失败");
    }
}
