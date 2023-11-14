package com.xqcoder.easypan.component;

import com.xqcoder.easypan.constants.Constants;
import com.xqcoder.easypan.entity.dto.SysSettingsDto;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {
    @Resource
    RedisUtils redisUtils;


    public SysSettingsDto getSysSettingsDto() {
        /**
         * @description: 将一些用户信息存放到Redis中
         * @param
         * @return com.xqcoder.easypan.entity.dto.SysSettingsDto
         * @author: HuaXian
         * @date: 2023/8/6 19:57
         */
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }

}
