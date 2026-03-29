package com.ast.back.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.ast.back.service.CurrentUserService;
import org.springframework.stereotype.Service;

@Service
public class SaTokenCurrentUserService implements CurrentUserService {

    @Override
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
