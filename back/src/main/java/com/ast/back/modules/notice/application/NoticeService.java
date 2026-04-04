package com.ast.back.modules.notice.application;

import com.ast.back.modules.auth.dto.LoginDTO;
import com.ast.back.modules.auth.dto.RegisterDTO;
import com.ast.back.modules.notice.persistence.entity.Notice;
import com.ast.back.modules.user.persistence.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NoticeService extends IService<Notice> {
    Notice getLatestNotice();
    List<Notice> listAllNotices();
}