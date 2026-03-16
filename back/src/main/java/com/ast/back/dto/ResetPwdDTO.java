package com.ast.back.dto;

import lombok.Data;

@Data
public class ResetPwdDTO {
    private String email;
    private String code;
    private String newPassword;
}