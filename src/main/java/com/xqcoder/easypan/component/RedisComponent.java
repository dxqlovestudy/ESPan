package com.xqcoder.easypan.component;

import com.xqcoder.easypan.constants.Constants;
import com.xqcoder.easypan.entity.dto.SysSettingsDto;
import com.xqcoder.easypan.entity.dto.UserSpaceDto;
import com.xqcoder.easypan.entity.query.FileInfoQuery;
import com.xqcoder.easypan.mappers.FileInfoMapper;
import org.apache.tomcat.jni.FileInfo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {
    @Resource
    RedisUtils redisUtils;
    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    /**
     * @description: 将一些用户信息存放到Redis中
     * @param
     * @return com.xqcoder.easypan.entity.dto.SysSettingsDto
     * @author: HuaXian
     * @date: 2023/8/6 19:57
     */
    public SysSettingsDto getSysSettingsDto() {
        // 从Redis中根据“easypan:sys:setting”获取系统设置
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        // 如果Redis中没有，将空的sysSettingsDto存入Redis "easypan:syssetting:"中
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }

    /**
     *  获取用户使用空间
     * @param userId
     * @return
     */
    public UserSpaceDto getUserSpaceUse(String userId) {
        // 从Redis中根据“easypan:user:spaceuse:userId”获取用户使用空间
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        // 如果Redis中没有，从数据库中获取,然后存入Redis中，设置过期时间为1天
        if (null == spaceDto) {
            spaceDto = new UserSpaceDto();
            Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(useSpace);
            spaceDto.setTotalSpace(getSysSettingsDto().getUserInitUseSpace() + Constants.MB);
            redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        }
        return spaceDto;
    }

}
