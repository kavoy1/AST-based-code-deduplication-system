package com.ast.back.shared.security;

import cn.dev33.satoken.stp.StpUtil;
import com.ast.back.shared.security.CurrentUserService;
import org.springframework.stereotype.Service;

@Service
public class SaTokenCurrentUserService implements CurrentUserService {

    @Override
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
