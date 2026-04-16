package com.robotlive.smartcodeless.aop;

import com.robotlive.smartcodeless.annotation.AuthCheck;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.innerservice.InnerUserService;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author 25212
 * @description TODO
 * @date 2026/1/29 17:14
 */
@Aspect
@Component
public class AuthInterceptor {

//    @Lazy
//    private InnerUserService userService;

    /**
     * 用拦截器实现
     *
     * @param joinPoint
     * @param authCheck
     * @return
     * @throws Throwable
     */
    // 设置AOP切面方法
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // joinPoint是切点，AuthCheck是校验注解
        String mustRole = authCheck.mustRole(); // 需要先获得该注解打上的权限
        // 获得上下文的request
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        //  将其强制转换为Severlet类型的
        ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes; // 强制向下转型
        // 获得当前的request
        HttpServletRequest request = attributes.getRequest(); // 获得当前的request对象
        // 通过request获得当前的登录用户
        User loginUser = InnerUserService.getLoginUser(request);
        // 其实就是校验当前的登录用户是否是管理员
//        boolean result = userService.isAdmin(loginUser);
        // 这个其实就是校验管理员的, 所以只需要判定当前的登录用户是否是管理员的身份
        // 提前通过枚举类对象获得UserRoleEnum的枚举类
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (mustRoleEnum == null) {
            // 如果没有，那么就是不需要权限
            return joinPoint.proceed();
        }
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR); // 如果不存在，那么需要抛出异常
        }
        // 如果当前mustRoleEnum不是管理员或者, 当前登录者不是管理员，则需要抛出异常
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有管理员权限");
        }
        return joinPoint.proceed();
    }
}