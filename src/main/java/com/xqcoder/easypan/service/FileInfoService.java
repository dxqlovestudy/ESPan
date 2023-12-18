package com.xqcoder.easypan.service;

import com.xqcoder.easypan.entity.dto.SessionWebUserDto;
import com.xqcoder.easypan.entity.dto.UploadResultDto;
import com.xqcoder.easypan.entity.po.FileInfo;
import com.xqcoder.easypan.entity.query.FileInfoQuery;
import com.xqcoder.easypan.entity.vo.PaginationResultVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/**
 * 文件信息 业务接口
 */
public interface FileInfoService {
    /**
     * 分页查询
     */
    PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param);

    int findCountByParam(FileInfoQuery param);

    List<FileInfo> findListByParam(FileInfoQuery param);

    UploadResultDto uploadFile(SessionWebUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    void transferFile(String fileId, SessionWebUserDto webUserDto);
}