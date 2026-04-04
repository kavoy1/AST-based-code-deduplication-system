package com.ast.back.modules.user.application;

import com.ast.back.modules.auth.dto.LoginDTO;
import com.ast.back.modules.auth.dto.RegisterDTO;
import com.ast.back.modules.user.persistence.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserService extends IService<User> {
    User login(LoginDTO loginDTO);
    void register(RegisterDTO dto, jakarta.servlet.http.HttpSession session);
    void resetPassword(String email, String newPassword);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);
}
