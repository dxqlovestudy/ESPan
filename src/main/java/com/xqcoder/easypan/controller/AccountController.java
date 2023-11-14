package com.xqcoder.easypan.controller;

import com.xqcoder.easypan.annotation.GlobalInterceptor;
import com.xqcoder.easypan.constants.Constants;
import com.xqcoder.easypan.entity.config.AppConfig;
import com.xqcoder.easypan.entity.dto.CreateImageCode;
import com.xqcoder.easypan.entity.dto.SessionWebUserDto;
import com.xqcoder.easypan.entity.vo.ResponseVO;
import com.xqcoder.easypan.exception.BusinessException;
import com.xqcoder.easypan.service.EmailCodeService;
import com.xqcoder.easypan.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@RestController("accountController")
public class AccountController extends ABaseController{
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";
    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private AppConfig appConfig;

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
        // TODO 图片验证码没有忽视大小写，找找原因，登录、注册、找回等功能的图片验证码有没有忽视大小写都需要排查
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

    /**
     * @description: 注册功能，从session中获取checkCode()方法中存放在session中的验证码，然后和用户输入的验证码进行比较。
     * 如果验证码正确，再将传入的email、nickName、password、emailCode调用userInfoService.register()方法进行注册
     *
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
    @RequestMapping("/register")
    public ResponseVO register(HttpSession session, String email, String nickName, String password, String checkCode,
                               String emailCode) {
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

    /**
     * @description: 登录功能，从session中获取checkCode()方法中存放在session中的验证码，然后和用户输入的验证码进行比较
     * @param session
     * @param email
     * @param password
     * @param checkCode
     * @return com.xqcoder.easypan.entity.vo.ResponseVO
     * @author: HuaXian
     * @date: 2023/11/12 11:05
     */
    @RequestMapping("/login")
    public ResponseVO login(HttpSession session, String email, String password, String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password);
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    // TODO 重置密码功能未完成
    @RequestMapping("/resetPwd")
    public ResponseVO resetPwd() {
        return null;
    }

    /**
     * @description: 获取头像功能
     * @param response
     * @param userId
     * @return void
     * @author: HuaXian
     * @date: 2023/11/14 15:07
     */
    // TODO getAvatar接口未测试
    @RequestMapping("/getAvatar/{userId}")
    // 检查登录设置为false、检查参数设置为true，可以去看GlobalInterceptor注解，然后利用AOP切入check方法的具体实现。
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    // 主要功能就是获取头像
    public void getAvatar(HttpServletResponse response, @PathVariable("userId") String userId) {
        // 通过拼接获取头像文件夹
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        // 判断头像文件夹是否存在，不存在则创建
        File folder = new File(appConfig.getProjectFolder() + avatarFolderName);
        if (!folder.exists()) {
            folder.mkdir();
        }
        // 拼接头像路径
        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        // 判断头像是否存在，不存在则返回默认头像
        File file = new File(avatarPath);
        if (!file.exists()) {
            // 判断默认头像是否存在，不存在则返回无默认图
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT).exists()) {
                printNoDefaultImage(response);
                return;
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");
        readFile(response, avatarPath);
    }

    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        } finally {
            writer.close();
        }
    }
}
