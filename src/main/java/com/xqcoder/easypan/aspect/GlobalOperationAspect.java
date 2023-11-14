package com.xqcoder.easypan.aspect;

import com.xqcoder.easypan.annotation.GlobalInterceptor;
import com.xqcoder.easypan.annotation.VerifyParam;
import com.xqcoder.easypan.constants.Constants;
import com.xqcoder.easypan.entity.config.AppConfig;
import com.xqcoder.easypan.entity.dto.SessionWebUserDto;
import com.xqcoder.easypan.entity.enums.ResponseCodeEnum;
import com.xqcoder.easypan.entity.po.UserInfo;
import com.xqcoder.easypan.entity.query.UserInfoQuery;
import com.xqcoder.easypan.exception.BusinessException;
import com.xqcoder.easypan.service.UserInfoService;
import com.xqcoder.easypan.utils.StringTools;
import com.xqcoder.easypan.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.*;
import java.util.List;


@Component("operationAspect")
public class GlobalOperationAspect {
    // 日志
    private static Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);
    // 参数类型
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";

    @Resource
    private AppConfig appConfig;
    @Resource
    private UserInfoService userInfoService;

    /***
     * @description: 定义一个切点，切入com.xqcoder.easypan.annotation.GlobalInterceptor注解的方法
     * @param
     * @return void
     * @author: HuaXian
     * @date: 2023/11/14 13:11
     */
    @Pointcut("@annotation(com.xqcoder.easypan.annotation.GlobalInterceptor)")
    private void requestInterceptor() {
    }

    /***
     * @description: // 定义requestInterceptor()方法的前置通知，意思就是在requestInterceptor（）方法
     * 执行前执行interceptorDo（）方法
     * @param point
     * @return void
     * @author: HuaXian
     * @date: 2023/11/14 13:10
     */
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) {
        try {
            Object target = point.getTarget();
            Object[] arguments = point.getArgs();
            String methodName = point.getSignature().getName();
            Class<?> parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getAnnotatedReturnType().getClass();
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (null == interceptor) {
                return;
            }
            /***
             * 校验登录和校验是校验否是Admin权限
             */
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
            /***
             * 校验参数
             */
            if (interceptor.checkParams()) {
                validateParams(method, arguments);
            }
        } catch (BusinessException e) {
            logger.error("全局拦截器异常", e);
            throw e;
        } catch (Exception e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        } catch (Throwable e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }

    // 校验登录
    private void checkLogin(Boolean chekAdmin) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto sessionUser = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        // 本地开发环境不校验登录
        if (sessionUser == null && appConfig.getDev() != null && appConfig.getDev()) {
            List<UserInfo> userInfoList = userInfoService.findListByParam(new UserInfoQuery());
            // 如果数据库中有用户信息，则默认登录第一个用户
            if (!userInfoList.isEmpty()) {
                UserInfo userInfo = userInfoList.get(0);
                sessionUser = new SessionWebUserDto();
                sessionUser.setUserId(userInfo.getUserId());
                sessionUser.setNickName(userInfo.getNickName());
                sessionUser.setIsAdmin(true);
                session.setAttribute(Constants.SESSION_KEY, sessionUser);
            }
        }
        // 如果需要校验登录，但是用户未登录，抛出”登录超时，请重新登录“
        if (null == sessionUser) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        // 如果需要校验是否是管理员权限，但是用户不是管理员，抛出“请求地址不存在”
        if (chekAdmin && !sessionUser.getIsAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }

    // 校验参数
    // TODO 用处还未理清楚
    public void validateParams(Method m, Object[] arguments) {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }
            if (TYPE_STRING.equals(parameter.getParameterizedType().getTypeName())
                    || TYPE_LONG.equals(parameter.getParameterizedType().getTypeName())
                    || TYPE_INTEGER.equals(parameter.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
            } else {
                checkObjValue(parameter, value);
            }
        }
    }
    /***
     * @description: 校验参数
     * @param value
     * @param verifyParam
     * @return void
     * @author: HuaXian
     * @date: 2023/11/14 12:56
     */
    private void checkValue(Object value, VerifyParam verifyParam) {
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        int length = value == null ? 0 : value.toString().length();
        // 如果是空，且必填，抛出“参数错误”
        if (isEmpty && verifyParam.required()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 如果不是空，且长度不在范围内，抛出“参数错误”
        if (!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1
                && verifyParam.min() > length)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 如果不是空，且正则不匹配，抛出“参数错误”
        if (!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex()
                , String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    private void checkObjValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();
            for (Field field :
                    fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue, fieldVerifyParam);
            }
        }catch (BusinessException e) {
            logger.error("校验参数失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("校验参数失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
}
