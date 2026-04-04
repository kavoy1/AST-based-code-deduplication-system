package com.ast.back.modules.auth.dto;

import lombok.Data;

@Data
public class ResetPwdDTO {
    private String email;
    private String code;
    private String newPassword;
}