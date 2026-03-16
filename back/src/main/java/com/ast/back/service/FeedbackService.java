package com.ast.back.service;

import com.ast.back.entity.Feedback;
import com.baomidou.mybatisplus.extension.service.IService;

public interface FeedbackService extends IService<Feedback> {
    void submitFeedback(Long userId, String title, String content);
    void resolveFeedback(Long id);
    void replyFeedback(Long id, String reply);
    void updateStatus(Long id, String status);
    long getTodayCount();
}
