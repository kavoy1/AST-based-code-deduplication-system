package com.ast.back.service;

import com.ast.back.dto.LoginDTO;
import com.ast.back.dto.RegisterDTO;
import com.ast.back.entity.Notice;
import com.ast.back.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NoticeService extends IService<Notice> {
    Notice getLatestNotice();
    List<Notice> listAllNotices();
}