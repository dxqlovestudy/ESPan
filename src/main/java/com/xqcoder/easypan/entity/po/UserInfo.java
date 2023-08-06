package com.xqcoder.easypan.entity.po;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
//import com.baomidou.mybatisplus.annotation.TableId;
//import com.baomidou.mybatisplus.annotation.TableName;
/**
 * 用户信息(UserInfo)表实体类
 *
 * @author makejava
 * @since 2023-08-05 21:07:07
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo  implements Serializable {


    /**
     * 用户ID
     */
    private String userId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * qq 头像
     */
    private String qqAvatar;

    /**
     * qq openID
     */
    private String qqOpenId;

    /**
     * 密码
     */
    private String password;

    /**
     * 加入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date joinTime;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    /**
     * 0:禁用 1:正常
     */
    private Integer status;

    /**
     * 使用空间单位byte
     */
    private Long useSpace;

    /**
     * 总空间单位byte
     */
    private Long totalSpace;
}

