package com.xqcoder.easypan.controller;

import com.xqcoder.easypan.annotation.GlobalInterceptor;
import com.xqcoder.easypan.annotation.VerifyParam;
import com.xqcoder.easypan.entity.dto.SessionWebUserDto;
import com.xqcoder.easypan.entity.dto.UploadResultDto;
import com.xqcoder.easypan.entity.enums.FileCategoryEnums;
import com.xqcoder.easypan.entity.enums.FileDelFlagEnums;
import com.xqcoder.easypan.entity.query.FileInfoQuery;
import com.xqcoder.easypan.entity.vo.FileInfoVO;
import com.xqcoder.easypan.entity.vo.PaginationResultVO;
import com.xqcoder.easypan.entity.vo.ResponseVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

@RestController("fileInfoController")
@RequestMapping("/file")
public class FileInfoController extends CommonFileController{

    /**
     * @description: 根据条件分页查询
     * @param session
     * @param query
     * @param category 文件类型，1-视频，2-音频，3-图片，4-文档，5-其他
     * @return com.xqcoder.easypan.entity.vo.ResponseVO
     * @author: HuaXian
     * @date: 2023/11/27 10:16
     */
    @RequestMapping("/loadDataList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadDataList(HttpSession session, FileInfoQuery query, String category) {
        FileCategoryEnums categoryEnums = FileCategoryEnums.getByCode(category);
        if (null != categoryEnums) {
            query.setFileCategory(categoryEnums.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }
    /**
     * @description: 上传文件功能
     * @param session 使用session获取用户信息，HttpSession允许在同一个浏览器中的多个请求之间共享数据
     * @param fileId 文件id，如果是断点续传，需要传入文件id
     * @param file 上传的文件，MultipartFile是spring的文件上传类
     * @param fileName 文件名
     * @param filePid 文件父id
     * @param fileMd5 文件md5，用于断点续传，如果是断点续传，需要传入文件md5，否则不需要。
     *                MD作用：1.防止重复上传，2.断点续传 3.文件完整性校验，防止文件损坏，4.文件秒传
     *                5.文件完整性校验，防止文件损坏，
     * @param chunkIndex 当前分片索引，从0开始，如果是断点续传，需要传入当前分片索引，否则不需要。
     * @param chunks 分片总数 如果是断点续传，需要传入分片总数，否则不需要。
     * @return com.xqcoder.easypan.entity.vo.ResponseVO
     * @author: HuaXian
     * @date: 2023/12/20 14:15
     */
    @RequestMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO uploadFile(HttpSession session,
                                 String fileId,
                                 MultipartFile file,
                                 @VerifyParam(required = true) String fileName,
                                 @VerifyParam(required = true) String filePid,
                                 @VerifyParam(required = true) String fileMd5,
                                 @VerifyParam(required = true) Integer chunkIndex,
                                 @VerifyParam(required = true) Integer chunks) {
        // 从session中获取用户信息
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        // 调用上传文件方法
        UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return getSuccessResponseVO(resultDto);
    }
}
