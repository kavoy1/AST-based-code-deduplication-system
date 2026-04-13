package com.ast.back.modules.auth.controller;

import cn.hutool.core.util.RandomUtil;
import com.ast.back.infra.messaging.MailUtil;
import com.ast.back.modules.auth.dto.LoginDTO;
import com.ast.back.modules.auth.dto.RegisterDTO;
import com.ast.back.modules.auth.dto.ResetPwdDTO;
import com.ast.back.modules.auth.session.AuthTokens;
import com.ast.back.modules.auth.session.JwtSessionService;
import com.ast.back.modules.notice.application.NoticeService;
import com.ast.back.modules.notice.persistence.entity.Notice;
import com.ast.back.modules.user.application.UserService;
import com.ast.back.modules.user.persistence.entity.User;
import com.ast.back.shared.common.Result;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    private final UserService userService;
    private final NoticeService noticeService;
    private final MailUtil mailUtil;
    private final JwtSessionService jwtSessionService;

    public LoginController(UserService userService,
                           NoticeService noticeService,
                           MailUtil mailUtil,
                           JwtSessionService jwtSessionService) {
        this.userService = userService;
        this.noticeService = noticeService;
        this.mailUtil = mailUtil;
        this.jwtSessionService = jwtSessionService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        User user = userService.login(loginDTO);
        AuthTokens tokens = jwtSessionService.createSession(user);
        writeRefreshCookie(response, tokens);

        Notice latestNotice = noticeService.getLatestNotice();

        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("notice", latestNotice);
        map.put("token", tokens.accessToken());
        map.put("accessToken", tokens.accessToken());
        map.put("accessTokenExpiresAt", ISO_INSTANT.format(tokens.accessTokenExpiresAt()));
        map.put("refreshTokenExpiresAt", ISO_INSTANT.format(tokens.refreshTokenExpiresAt()));
        return Result.success(map);
    }

    @PostMapping("/auth/refresh")
    public Result<Map<String, Object>> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshToken(request);
        AuthTokens tokens = jwtSessionService.refreshSession(refreshToken);
        writeRefreshCookie(response, tokens);

        Map<String, Object> map = new HashMap<>();
        map.put("accessToken", tokens.accessToken());
        map.put("accessTokenExpiresAt", ISO_INSTANT.format(tokens.accessTokenExpiresAt()));
        map.put("refreshTokenExpiresAt", ISO_INSTANT.format(tokens.refreshTokenExpiresAt()));
        return Result.success(map);
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        jwtSessionService.logout(resolveAccessToken(request), readRefreshToken(request));
        clearRefreshCookie(response);
        return Result.success("已退出登录");
    }

    @PostMapping("/code")
    public Result<String> sendCode(@RequestParam String email, HttpSession session) {
        String code = RandomUtil.randomNumbers(6);

        session.setAttribute("emailCode", code);
        session.setAttribute("emailTarget", email);
        session.setAttribute("emailTime", System.currentTimeMillis());

        mailUtil.sendVerificationCodeMail(email, code);
        return Result.success("验证码已发送");
    }

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

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO registerDTO, HttpSession session) {
        userService.register(registerDTO, session);
        return Result.success("注册成功");
    }

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

    private void writeRefreshCookie(HttpServletResponse response, AuthTokens tokens) {
        int maxAge = (int) Math.max(0, tokens.refreshTokenExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
        Cookie cookie = new Cookie(jwtSessionService.getProperties().getRefreshCookieName(), tokens.refreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtSessionService.getProperties().isRefreshCookieSecure());
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
        response.addHeader(HttpHeaders.SET_COOKIE,
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax%s".formatted(
                        jwtSessionService.getProperties().getRefreshCookieName(),
                        tokens.refreshToken(),
                        maxAge,
                        jwtSessionService.getProperties().isRefreshCookieSecure() ? "; Secure" : ""
                ));
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(jwtSessionService.getProperties().getRefreshCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtSessionService.getProperties().isRefreshCookieSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.addHeader(HttpHeaders.SET_COOKIE,
                "%s=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax%s".formatted(
                        jwtSessionService.getProperties().getRefreshCookieName(),
                        jwtSessionService.getProperties().isRefreshCookieSecure() ? "; Secure" : ""
                ));
    }

    private String readRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        String cookieName = jwtSessionService.getProperties().getRefreshCookieName();
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        String legacyToken = request.getHeader("satoken");
        return legacyToken == null || legacyToken.isBlank() ? null : legacyToken.trim();
    }
}
