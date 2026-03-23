package com.robotlive.smartcodeless.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 25212
 * @description 用户注册请求，接受请求参数类
 * @date 2026/1/21 15:25
 */
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = -4260919781868760019L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
