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
            // 从数据库中获取用户使用的总空间大小
            Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
            // 将用户使用的总空间大小（useSpace）存入UserSpaceDto中
            spaceDto.setUseSpace(useSpace);
            // 从redis系统设置中获取用户初始化空间大小并扩大到MB大小
            spaceDto.setTotalSpace(getSysSettingsDto().getUserInitUseSpace() * Constants.MB);
            // 将用户使用空间存入Redis中easypan:user:spaceuse:userId，并设置过期时间为1天
            redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        }
        return spaceDto;
    }

    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
    }

    /**
     * @description: 获取临时文件大小
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return java.lang.Long 临时文件大小,每个用户每个文件的临时大小都不一样，所以需要根据用户ID和文件ID来获取临时文件大小
     * @author: HuaXian
     * @date: 2023/12/22 14:51
     */
    public Long getFileTempSize(String userId, String fileId) {
        // 从Redis中根据“easypan:user:file:temp:userId+fileId”获取文件临时大小
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }

    /**
     * @description: 从Redis中获取文件大小
     * @param key Redis中的key
     * @return java.lang.Long 文件大小，返回值类型为Long
     * @author: HuaXian
     * @date: 2023/12/22 14:53
     */
    private Long getFileSizeFromRedis(String key) {
        // 从Redis中根据key获取value对象
        Object sizeObj = redisUtils.get(key);
        // 如果value对象为空，返回0L
        if (sizeObj == null) {
            return 0L;
        }
        // 如果value对象是Integer类型，返回value对象转换成Long类型
        // 如果value对象是Long类型，返回value对象
        // 如果value对象是其他类型，返回0L
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }
        return 0L;
    }

    /**
     * @description: 保存文件临时大小
     * @param userId 用户ID
     * @param fileId 文件ID
     * @param fileSize 文件大小
     * @return void 无返回值，保存文件临时大小到Redis中，每个用户每个文件的临时大小都不一样，所以需要根据用户ID和文件ID来保存临时文件大小到Redis中
     * @author: HuaXian
     * @date: 2023/12/22 15:15
     */
    public void saveFileTempSize(String userId, String fileId, long fileSize) {
        // 获取文件临时大小
        Long currentSize = getFileTempSize(userId, fileId);
        // key为“easypan:user:file:temp:userId+fileId（userId和fileId是字符串类型，因此是串起来）”，value为currentSize + fileSize，过期时间为1小时
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId, currentSize + fileSize, Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
    }
}
