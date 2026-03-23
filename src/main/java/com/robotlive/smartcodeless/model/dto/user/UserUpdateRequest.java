package com.robotlive.smartcodeless.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 25212
 * @description TODO
 * @date 2026/1/29 17:50
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}

