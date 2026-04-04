package com.ast.back.modules.feedback.application.impl;

import com.ast.back.shared.common.BusinessException;
import com.ast.back.modules.feedback.persistence.entity.Feedback;
import com.ast.back.modules.feedback.persistence.mapper.FeedbackMapper;
import com.ast.back.modules.feedback.application.FeedbackService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    @Override
    public void submitFeedback(Long userId, String title, String content) {
        if (content == null || content.length() < 10) {
            throw new BusinessException("反馈内容不能少于10个字");
        }
        
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setTitle(title);
        feedback.setContent(content);
        feedback.setStatus("PENDING");
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        
        this.save(feedback);
    }

    @Override
    public void resolveFeedback(Long id) {
        Feedback feedback = this.getById(id);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        
        feedback.setStatus("RESOLVED");
        feedback.setUpdateTime(LocalDateTime.now());
        this.updateById(feedback);
    }

    @Override
    public void replyFeedback(Long id, String reply) {
        Feedback feedback = this.getById(id);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        
        feedback.setReply(reply);
        feedback.setUpdateTime(LocalDateTime.now());
        // 如果回复了，也可以自动标记为处理中或已解决，视业务逻辑而定，这里仅保存回复
        if ("PENDING".equals(feedback.getStatus())) {
            feedback.setStatus("PROCESSING");
        }
        
        this.updateById(feedback);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Feedback feedback = this.getById(id);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        feedback.setStatus(status);
        feedback.setUpdateTime(LocalDateTime.now());
        this.updateById(feedback);
    }

    @Override
    public long getTodayCount() {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Feedback::getCreateTime, LocalDate.now().atStartOfDay());
        return this.count(wrapper);
    }
}
