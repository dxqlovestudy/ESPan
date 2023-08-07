package com.xqcoder.easypan.service;



/**
 * 用户信息(UserInfo)表服务接口
 *
 * @author makejava
 * @since 2023-08-05 21:07:07
 */
public interface UserInfoService {

    void register(String email, String nickName, String password, String emailCode);
}

