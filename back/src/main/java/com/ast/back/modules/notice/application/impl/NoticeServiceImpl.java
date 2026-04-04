package com.ast.back.modules.notice.application.impl;

import com.ast.back.shared.common.BusinessException;
import com.ast.back.modules.notice.persistence.entity.Notice;
import com.ast.back.modules.user.persistence.entity.User;
import com.ast.back.modules.notice.persistence.mapper.NoticeMapper;
import com.ast.back.modules.notice.application.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public Notice getLatestNotice() {
        // 只获取全局通知（receiver_id 为 null）
        return this.getOne(new LambdaQueryWrapper<Notice>()
                .isNull(Notice::getReceiverId)
                .orderByDesc(Notice::getCreateTime)
                .last("LIMIT 1"));
    }
    @Override
    public List<Notice> listAllNotices() {
        return this.list(new LambdaQueryWrapper<Notice>()
                .orderByAsc(Notice::getId));
    }
}