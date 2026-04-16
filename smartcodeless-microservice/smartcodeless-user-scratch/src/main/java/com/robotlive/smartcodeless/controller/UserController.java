package com.robotlive.smartcodeless.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.robotlive.smartcodeless.annotation.AuthCheck;
import com.robotlive.smartcodeless.common.BaseResponse;
import com.robotlive.smartcodeless.common.DeleteRequest;
import com.robotlive.smartcodeless.common.ResultUtils;
import com.robotlive.smartcodeless.constant.UserConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.dto.user.*;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.LoginUserVO;
import com.robotlive.smartcodeless.model.vo.UserVO;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 控制层。
 *
 * @author <a href="https://github.com/ZhanhongLiang">Jean</a>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册接口
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(userId);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 检查request
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 通过request获得用户脱敏信息, 这个其实就是适用于后面调取各类信息用的
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 需要执行userService中的获取方法
        User user = userService.getLoginUser(request);
        // 执行脱敏类即可
        LoginUserVO loginUserVo = userService.getLoginUserVo(user);
        return ResultUtils.success(loginUserVo);
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    // 管理员添加用户
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 添加管理员权限
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 1.校验参数
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 需要查询目前数据库是否存在同名的
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user); // 将userAddRequest赋给user
        // 但是需要额外设置user里面的其他数据
        String DEFAULTPASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULTPASSWORD);
        user.setUserPassword(encryptPassword);
        // 需要保存到数据库中
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }


    // 管理员根据用户id删除用户, 将数据库中的isDelete设为False
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    // 管理员更新用户信息
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 需要根据UserUpdateRequest更新参数
        if (userUpdateRequest == null || userUpdateRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 管理员可以更新编辑某个用户里面的信息
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 管理员根据id获得未脱敏信息
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    // 普通用户获得脱敏用户信息
    @PostMapping("/get/vo")
    public BaseResponse<UserVO> getUserVoById(Long id) {
        BaseResponse<User> userById = getUserById(id);
        User data = userById.getData();
        UserVO userVo = userService.getUserVO(data);
        return ResultUtils.success(userVo);
    }

    /**
     * IService 的接口提供了 page、pageAs 方法，用于分页查询数据：
     *
     * page(page)：分页查询所有数据。
     * page(page, query)：根据 QueryWrapper 构建的条件分页查询数据。
     * page(page, condition)：根据 QueryCondition 构建的条件分页查询数据。
     * pageAs(page, query, asType)：根据 QueryWrapper 构建的条件分页查询数据，并通过 asType 进行接收。
     *
     * @param userQueryRequest
     * @return
     */
    // 管理员分页获取用户列表(需要脱敏)
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 需要根据UserUpdateRequest更新参数
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int pageNum = userQueryRequest.getPageNum();
        int pageSize = userQueryRequest.getPageSize();
        // 需要调用userService中的getQueryWrapper获得分页后的QueryWrapper对象
        // 但是需要脱敏
        // 调用userService继承的UserMapper中的BaseMapper的分页函数，Page
        // 调用MyBatis-flex的分页函数, Page
        Page<User> userPage = userService.page(Page.of(pageNum,pageSize), userService.getQueryWrapper(userQueryRequest));
        // 通过封装号的page分页查询函数, 得到Page<User>分页
        // 但是需要得到UserVO类的分页
        // 然后再将userVOPage封装好返回
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<User> userList = userPage.getRecords(); //
        List<UserVO> userVoList = userService.getUserVoList(userList);
        userVOPage.setRecords(userVoList);
        return ResultUtils.success(userVOPage);
    }

}
