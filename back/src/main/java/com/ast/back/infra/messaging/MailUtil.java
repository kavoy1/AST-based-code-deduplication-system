package com.ast.back.infra.messaging;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MailUtil {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.mail.from-name:AST 智能查重}")
    private String fromName;

    public void sendVerificationCodeMail(String to, String code) {
        String subject = "【AST 智能查重】邮箱验证码";
        String html = buildVerificationCodeHtml(code);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()).toString());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
        } catch (MessagingException | java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException("发送验证码邮件失败", exception);
        }
        mailSender.send(message);
    }

    private String buildVerificationCodeHtml(String code) {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>AST 智能查重验证码</title>
            </head>
            <body style="margin:0;padding:0;background:#eef3f8;font-family:'Microsoft YaHei','PingFang SC',Arial,sans-serif;color:#1f2a37;">
              <div style="padding:32px 16px;">
                <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 18px 40px rgba(32,51,84,0.12);border:1px solid rgba(208,218,232,0.75);">
                  <div style="padding:28px 32px;background:linear-gradient(135deg,#11285f 0%,#1a4ecb 55%,#25d8e6 100%);color:#ffffff;">
                    <div style="font-size:14px;letter-spacing:0.08em;opacity:0.88;margin-bottom:10px;">AST INTELLIGENT PLAGIARISM CHECK</div>
                    <div style="font-size:28px;font-weight:700;line-height:1.3;">身份验证通知</div>
                    <div style="margin-top:8px;font-size:15px;line-height:1.7;opacity:0.92;">你正在使用 AST 智能查重 平台进行邮箱验证，请使用下方验证码完成本次操作。</div>
                  </div>
                  <div style="padding:32px;">
                    <div style="font-size:15px;color:#5b6780;line-height:1.8;">本次验证码为</div>
                    <div style="margin:18px 0 22px;padding:20px 24px;border-radius:20px;background:linear-gradient(180deg,#f7faff 0%,#eef4ff 100%);border:1px solid #dbe5fb;text-align:center;">
                      <span style="display:inline-block;font-size:36px;font-weight:800;letter-spacing:0.26em;color:#17326b;text-indent:0.26em;">__CODE__</span>
                    </div>
                    <div style="display:flex;gap:12px;flex-wrap:wrap;margin-bottom:22px;">
                      <span style="display:inline-flex;align-items:center;padding:8px 14px;border-radius:999px;background:#f3f6fb;color:#4f5f7c;font-size:13px;">5 分钟内有效</span>
                      <span style="display:inline-flex;align-items:center;padding:8px 14px;border-radius:999px;background:#f3f6fb;color:#4f5f7c;font-size:13px;">仅用于本次验证</span>
                    </div>
                    <div style="padding:16px 18px;border-radius:18px;background:#f8fafc;border:1px solid #e8eef7;color:#6c7a92;font-size:14px;line-height:1.8;">
                      为保障账号安全，请勿将验证码泄露给他人。若本次操作并非你本人发起，请直接忽略本邮件，无需进行任何处理。
                    </div>
                  </div>
                  <div style="padding:18px 32px 28px;color:#8b97aa;font-size:12px;line-height:1.8;">
                    此邮件由 <strong style="color:#50607e;">AST 智能查重</strong> 平台自动发送，请勿直接回复。<br/>
                    如需帮助，请联系系统管理员或通过平台内反馈入口提交问题。
                  </div>
                </div>
              </div>
            </body>
            </html>
            """.replace("__CODE__", code);
    }
}
