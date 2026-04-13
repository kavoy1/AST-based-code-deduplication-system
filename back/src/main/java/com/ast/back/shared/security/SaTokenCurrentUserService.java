package com.ast.back.shared.security;

import com.ast.back.modules.auth.session.AuthenticatedUser;

public class SaTokenCurrentUserService implements CurrentUserService {

    @Override
    public AuthenticatedUser getCurrentUser() {
        throw new UnsupportedOperationException("Sa-Token current user service is no longer active");
    }
}
