package com.xqcoder.easypan.controller;

import com.xqcoder.easypan.annotation.GlobalInterceptor;
import com.xqcoder.easypan.entity.po.UserInfo;
import com.xqcoder.easypan.entity.query.UserInfoQuery;
import com.xqcoder.easypan.entity.vo.PaginationResultVO;
import com.xqcoder.easypan.entity.vo.ResponseVO;
import com.xqcoder.easypan.entity.vo.UserInfoVO;
import com.xqcoder.easypan.service.UserInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("adminController")
@RequestMapping("/admin")
public class AdminController extends CommonFileController {
    @Resource
    private UserInfoService userInfoService;
    // TODO 报错
    @RequestMapping("/loadUserList")
    @GlobalInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO<UserInfo> resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(convert2PaginationVO(resultVO, UserInfoVO.class));
    }

}
