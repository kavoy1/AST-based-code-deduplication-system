package com.ast.back.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.ast.back.Utils.MailUtil;
import com.ast.back.common.Result;
import com.ast.back.dto.LoginDTO;
import com.ast.back.dto.RegisterDTO;
import com.ast.back.dto.ResetPwdDTO;
import com.ast.back.entity.Notice;
import com.ast.back.entity.User;
import com.ast.back.service.NoticeService;
import com.ast.back.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证与账号相关接口。
 *
 * 说明：
 * - 登录成功后使用 Sa-Token 建立登录态，并将 token 返回给前端。
 * - 邮箱验证码使用 HttpSession 暂存（阶段 1 方案，便于演示）。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private MailUtil mailUtil;

    /**
     * 登录。
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO, HttpSession session) {
        User user = userService.login(loginDTO);

        // 建立 Sa-Token 登录态
        StpUtil.login(user.getId());
        session.setAttribute("currentUser", user);

        // 附加最新通知（可选）
        Notice latestNotice = noticeService.getLatestNotice();

        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("notice", latestNotice);
        map.put("token", StpUtil.getTokenInfo().tokenValue);
        return Result.success(map);
    }

    /**
     * 发送邮箱验证码。
     */
    @PostMapping("/code")
    public Result<String> sendCode(@RequestParam String email, HttpSession session) {
        String code = RandomUtil.randomNumbers(6);

        session.setAttribute("emailCode", code);
        session.setAttribute("emailTarget", email);
        session.setAttribute("emailTime", System.currentTimeMillis());

        mailUtil.sendTextMail(email, "验证码", "您的验证码为：" + code + "（5分钟内有效）");
        return Result.success("验证码已发送");
    }

    /**
     * 校验邮箱验证码（用于注册第一步）。
     */
    @PostMapping("/verify-code")
    public Result<String> verifyCode(@RequestParam String email, @RequestParam String code, HttpSession session) {
        String sessionCode = (String) session.getAttribute("emailCode");
        String sessionEmail = (String) session.getAttribute("emailTarget");
        Long sessionTime = (Long) session.getAttribute("emailTime");

        if (sessionCode == null || sessionEmail == null || sessionTime == null) {
            return Result.error("请先获取验证码");
        }

        if (!sessionEmail.equals(email)) {
            return Result.error("获取验证码的邮箱与当前邮箱不一致");
        }

        if (System.currentTimeMillis() - sessionTime > 5 * 60 * 1000) {
            return Result.error("验证码已过期，请重新获取");
        }

        if (!sessionCode.equals(code)) {
            return Result.error("验证码错误");
        }

        return Result.success("验证通过");
    }

    /**
     * 注册。
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO registerDTO, HttpSession session) {
        userService.register(registerDTO, session);
        return Result.success("注册成功");
    }

    /**
     * 重置密码（示例实现）。
     *
     * 注意：严格方案应复用 verify-code 的校验逻辑（含过期校验），避免绕过。
     */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody ResetPwdDTO resetDto, HttpSession session) {
        String sessionCode = (String) session.getAttribute("emailCode");
        String sessionEmail = (String) session.getAttribute("emailTarget");

        if (sessionCode == null || sessionEmail == null) {
            return Result.error("请先获取验证码");
        }
        if (!sessionEmail.equals(resetDto.getEmail())) {
            return Result.error("获取验证码的邮箱与当前邮箱不一致");
        }
        if (!sessionCode.equals(resetDto.getCode())) {
            return Result.error("验证码错误或已过期");
        }

        userService.resetPassword(resetDto.getEmail(), resetDto.getNewPassword());
        return Result.success("密码重置成功");
    }
}