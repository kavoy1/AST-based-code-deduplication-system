package com.ast.back.modules.notice.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ast.back.shared.common.Result;
import com.ast.back.modules.notice.persistence.entity.Notice;
import com.ast.back.modules.notice.application.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping("/list")
    public Result list() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 查询该用户的通知（包括全局公告和私人通知）
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Notice::getReceiverId, userId).or().isNull(Notice::getReceiverId));
        wrapper.orderByDesc(Notice::getCreateTime);
        
        List<Notice> list = noticeService.list(wrapper);
        return Result.success(list);
    }
    
    @PostMapping("/read/{id}")
    public Result markAsRead(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        Long userId = StpUtil.getLoginIdAsLong();
        if (notice != null && Objects.equals(userId, notice.getReceiverId())) {
            notice.setIsRead(1);
            noticeService.updateById(notice);
            return Result.success();
        }
        return Result.error("操作失败");
    }
}
