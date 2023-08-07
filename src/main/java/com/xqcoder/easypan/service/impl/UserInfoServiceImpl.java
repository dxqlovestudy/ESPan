package com.xqcoder.easypan.service.impl;

import com.xqcoder.easypan.component.RedisComponent;
import com.xqcoder.easypan.constants.Constants;
import com.xqcoder.easypan.entity.dto.SysSettingsDto;
import com.xqcoder.easypan.entity.enums.UserStatusEnum;
import com.xqcoder.easypan.entity.po.UserInfo;
import com.xqcoder.easypan.entity.query.UserInfoQuery;
import com.xqcoder.easypan.exception.BusinessException;
import com.xqcoder.easypan.mappers.UserInfoMapper;
import com.xqcoder.easypan.service.EmailCodeService;
import com.xqcoder.easypan.service.UserInfoService;
import com.xqcoder.easypan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 用户信息(UserInfo)表服务实现类
 *
 * @author makejava
 * @since 2023-08-05 21:07:08
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Resource
    private EmailCodeService emailCodeService;
    @Resource
    private RedisComponent redisComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickName, String password, String emailCode) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null != userInfo) {
            throw new BusinessException("邮箱账号已存在");
        }
        UserInfo nickNameUser = this.userInfoMapper.selectByNickName(nickName);
        if (null != nickNameUser) {
            throw new BusinessException("昵称已存在");
        }
        // 邮箱验证
        emailCodeService.checkCode(email, emailCode);
        // 随机产生userId
        String userId = StringTools.getRandomNumber(Constants.LENGTH_10);
        userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setNickName(nickName);
        userInfo.setEmail(email);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfo.setJoinTime(new Date());
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        // 获取系统默认的设置参数，例如用户初始云盘空间大小
        SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto();
        userInfo.setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);
        userInfo.setUseSpace(0L);
        this.userInfoMapper.insert(userInfo);
    }
}

