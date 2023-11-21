package com.xqcoder.easypan.mappers;

import org.apache.ibatis.annotations.Param;
import org.apache.tomcat.jni.FileInfo;

import java.util.List;

public interface FileInfoMapper <T, P> extends BaseMapper<T, P> {
    /***
     * 根据FileId和UserId更新
     * @param t
     * @param fileId 文件Id
     * @param userId 用户Id
     * @return
     */
    Integer updateByFileIdAndUserId(@Param("bean") T t, @Param("fileId") String fileId, @Param("userId") String userId);

    /***
     * 根据FileId和UserId删除
     * @param fileId
     * @param userId
     * @return
     */
    Integer deleteByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

    /***
     * 根据FileId和UserId获取对象
     * @param fileId
     * @param userId
     * @return
     */
    T selectByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

    void updateFileStatusWithOldStatus(@Param("fileId") String fileId, @Param("userId") String userId, @Param("bean") T t,
                                       @Param("oldStatus") Integer oldStatus);
    void updateFileDelFlagBatch(@Param("bean") FileInfo fileInfo,
                                @Param("userId") String userId,
                                @Param("filePidList") List<String> filePidList,
                                @Param("fileIdList") List<String> fileIdList,
                                @Param("oldDelFlag") Integer oldDelFlag);


    void delFileBatch(@Param("userId") String userId,
                      @Param("filePidList") List<String> filePidList,
                      @Param("fileIdList") List<String> fileIdList,
                      @Param("oldDelFlag") Integer oldDelFlag);

    Long selectUseSpace(@Param("userId") String userId);

    void deleteFileByUserId(@Param("userId") String userId);
}
