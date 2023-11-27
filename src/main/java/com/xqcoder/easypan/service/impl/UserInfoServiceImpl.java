package com.xqcoder.easypan.service.impl;

import com.xqcoder.easypan.component.RedisComponent;
import com.xqcoder.easypan.constants.Constants;
import com.xqcoder.easypan.entity.config.AppConfig;
import com.xqcoder.easypan.entity.dto.SessionWebUserDto;
import com.xqcoder.easypan.entity.dto.SysSettingsDto;
import com.xqcoder.easypan.entity.enums.PageSize;
import com.xqcoder.easypan.entity.enums.UserStatusEnum;
import com.xqcoder.easypan.entity.po.UserInfo;
import com.xqcoder.easypan.entity.query.SimplePage;
import com.xqcoder.easypan.entity.query.UserInfoQuery;
import com.xqcoder.easypan.entity.vo.PaginationResultVO;
import com.xqcoder.easypan.exception.BusinessException;
import com.xqcoder.easypan.mappers.UserInfoMapper;
import com.xqcoder.easypan.service.EmailCodeService;
import com.xqcoder.easypan.service.UserInfoService;
import com.xqcoder.easypan.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

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
    @Resource
    private AppConfig appConfig;

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

    @Override
    public SessionWebUserDto login(String email, String password) {

        // 使用selectByEmail通过email查询用户信息存储到userInfo中
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        // 如果userInfo为空或者密码不正确，抛出异常
        // 前端已经对输入的password进行了MD5加密，所以这里不需要对password进行MD5加密
        if (null == userInfo || !userInfo.getPassword().equals(password)) {
            throw new BusinessException("账号或者密码错误");
        }
        // 如果用户状态为禁用0，抛出异常
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("账号已禁用");
        }
        // 更新用户最后登录时间
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastLoginTime(new Date());
        this.userInfoMapper.updateByUserId(updateInfo, userInfo.getUserId());
        // 将用户信息存储到sessionWebUserDto中
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setNickName(userInfo.getNickName());
        sessionWebUserDto.setUserId(userInfo.getUserId());
        // 判断用户是否为管理员,从管理员邮箱列表中获取管理员邮箱，然后判断用户邮箱是否在管理员邮箱列表中
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), email)) {
            sessionWebUserDto.setIsAdmin(true);
        } else {
            sessionWebUserDto.setIsAdmin(false);
        }
        // 获取用户云存储空间信息
//        UserSpaceDto userSpaceDto = new UserSpaceDto();
//        userSpaceDto.setUseSpace();

        return sessionWebUserDto;
    }

    @Override
    public List<UserInfo> findListByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectList(param);
    }

    @Override
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        return this.userInfoMapper.updateByUserId(bean, userId);
    }

    /**
     * @description:  分页查询
     * @param param
     * @return com.xqcoder.easypan.entity.vo.PaginationResultVO<com.xqcoder.easypan.entity.po.UserInfo>
     * @author: HuaXian
     * @date: 2023/11/23 14:47
     */
    @Override
    public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
        // 查询总数
        int count = this.findCountByParam(param);
        // 如果pageSize为空，设置默认值,否则使用传入的pageSize
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();
        // 创建分页对象
        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        // 设置分页参数
        param.setSimplePage(page);
        // 查询列表
        List<UserInfo> list = findListByParam(param);
        // 创建分页结果对象
        PaginationResultVO<UserInfo> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    @Override
    public Integer findCountByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectCount(param);
    }
}

