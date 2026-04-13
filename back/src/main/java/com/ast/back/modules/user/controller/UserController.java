package com.ast.back.modules.user.controller;

import com.ast.back.modules.auth.dto.UpdatePasswordDTO;
import com.ast.back.modules.user.application.UserService;
import com.ast.back.modules.user.persistence.entity.User;
import com.ast.back.shared.common.Result;
import com.ast.back.shared.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/info")
    public Result getUserInfo() {
        Long userId = currentUserService.getCurrentUserId();
        User user = userService.getById(userId);
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/password")
    public Result updatePassword(@RequestBody UpdatePasswordDTO dto) {
        Long userId = currentUserService.getCurrentUserId();
        userService.updatePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功");
    }
}
