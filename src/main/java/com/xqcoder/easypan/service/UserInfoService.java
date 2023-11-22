package com.xqcoder.easypan.service;


import com.xqcoder.easypan.entity.dto.SessionWebUserDto;
import com.xqcoder.easypan.entity.po.UserInfo;
import com.xqcoder.easypan.entity.query.UserInfoQuery;

import java.util.List;

/**
 * 用户信息(UserInfo)表服务接口
 *
 * @author makejava
 * @since 2023-08-05 21:07:07
 */
public interface UserInfoService {

    void register(String email, String nickName, String password, String emailCode);

    SessionWebUserDto login(String email, String password);

    List<UserInfo> findListByParam(UserInfoQuery userInfoQuery);

    Integer updateUserInfoByUserId(UserInfo bean, String userId);
}

