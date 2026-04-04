package com.ast.back.modules.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ast.back.shared.common.Result;
import com.ast.back.modules.auth.dto.UpdatePasswordDTO;
import com.ast.back.modules.user.persistence.entity.User;
import com.ast.back.modules.user.application.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    public Result getUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        // 密码不返回
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/password")
    public Result updatePassword(@RequestBody UpdatePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updatePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功");
    }
}
