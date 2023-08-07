package com.xqcoder.easypan.controller;

import com.xqcoder.easypan.constants.Constants;
import com.xqcoder.easypan.entity.dto.CreateImageCode;
import com.xqcoder.easypan.entity.vo.ResponseVO;
import com.xqcoder.easypan.exception.BusinessException;
import com.xqcoder.easypan.service.EmailCodeService;
import com.xqcoder.easypan.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@RestController("accountController")
public class AccountController extends ABaseController{

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private EmailCodeService emailCodeService;

    @GetMapping(value = "/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws
            IOException {
        /**
         * @description: 获取验证码
         * @param response
         * @param session
         * @param type 为null或者0的时候是登录功能、找回密码，为1或其他是注册时的邮箱验证码
         * @return void
         * @author: HuaXian
         * @date: 2023/8/6 14:29
         */
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    @RequestMapping("/sendEmailCode")
    public ResponseVO sendEmailCode(HttpSession session, String email, String checkCode, Integer type) {
        /**
         * @description: 注册功能中的发送邮箱验证码
         * @param session
         * @param email
         * @param checkCode
         * @param type 为0是注册，为1是找回密码
         * @return com.xqcoder.easypan.entity.vo.ResponseVO
         * @author: HuaXian
         * @date: 2023/8/7 9:19
         */
        // TODO 图片验证码没有忽视大小写，找找原因
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图片验证码不正确");
            }
            emailCodeService.sendEmailCode(email, type);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    @RequestMapping("/register")
    public ResponseVO register(HttpSession session, String email, String nickName, String password, String checkCode,
                               String emailCode) {
        /**
         * @description: 注册
         * @param session
         * @param email
         * @param nickName
         * @param password
         * @param checkCode
         * @param emailCode
         * @return com.xqcoder.easypan.entity.vo.ResponseVO
         * @author: HuaXian
         * @date: 2023/8/7 9:20
         */
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.register(email, nickName, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
}
