package com.ast.back.service.impl;

import com.ast.back.common.BusinessException;
import com.ast.back.entity.Notice;
import com.ast.back.entity.User;
import com.ast.back.mapper.NoticeMapper;
import com.ast.back.service.NoticeService;
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