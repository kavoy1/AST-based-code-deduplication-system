package com.ast.back.modules.feedback.application;

import com.ast.back.modules.feedback.persistence.entity.Feedback;
import com.baomidou.mybatisplus.extension.service.IService;

public interface FeedbackService extends IService<Feedback> {
    void submitFeedback(Long userId, String title, String content);
    void resolveFeedback(Long id);
    void replyFeedback(Long id, String reply);
    void updateStatus(Long id, String status);
    long getTodayCount();
}
