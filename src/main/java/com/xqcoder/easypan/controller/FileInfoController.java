package com.xqcoder.easypan.controller;

import com.xqcoder.easypan.annotation.GlobalInterceptor;
import com.xqcoder.easypan.entity.enums.FileCategoryEnums;
import com.xqcoder.easypan.entity.enums.FileDelFlagEnums;
import com.xqcoder.easypan.entity.query.FileInfoQuery;
import com.xqcoder.easypan.entity.vo.FileInfoVO;
import com.xqcoder.easypan.entity.vo.PaginationResultVO;
import com.xqcoder.easypan.entity.vo.ResponseVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
