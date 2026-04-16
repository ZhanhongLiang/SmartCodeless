package com.robotlive.smartcodeless.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.mapper.UserMapper;
import com.robotlive.smartcodeless.model.dto.user.UserQueryRequest;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.UserRoleEnum;
import com.robotlive.smartcodeless.model.vo.LoginUserVO;
import com.robotlive.smartcodeless.model.vo.UserVO;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.robotlive.smartcodeless.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author <a href="https://github.com/ZhanhongLiang">Jean</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 用户注册
        // 1.先判断目前checkPassword是否是userPassword
        // 还需要调用MD5加密代码，加密后添加到数据库中
        // 1. 校验参数
        //    检查输入参数是否为空
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不存在");
        }
        //    检查账户名字是否过短
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户信息过短");
        }
        //     检查密码是否过短
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        //    检查再次确认密码是否与第一次输入的密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }
        // 2. 检查用户账户是否和数据库中已有的重复, 检查是否重名，不能存在重名
        QueryWrapper queryWrapper = new QueryWrapper(); // 语法变了，myBatis-plus语法是另外一种
        // select count(*) from user where userAccount=?
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名重复");
        }
        // 否则需要先加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 加密后再添加到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean result = this.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户注册失败");
        return user.getId();
    }

    /**
     * controller是通过UserLoginRequest接受的
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不存在");
        }
        //    检查账户名字是否过短
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户信息过短");
        }
        //     检查密码是否过短
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        // 需要密码先加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 因为是需要返回LoginUserVO信息，是脱敏后的信息
        // 需要先通过用户名查到该用户的信息
        QueryWrapper queryWrapper = new QueryWrapper();
        // select * from user where userAccount=? and userPassword=?
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper); //得到用户
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在或密码错误");
        // 通过session记录登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        // 通过密码和userAccount检测到是否有该用户
        // 且需要记录该request
        return this.getLoginUserVo(user);
    }

    /**
     * 所谓用户注销其实就是logout操作
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 用户注销, 其实就是让session不记住当前的user
        // 将session取消掉登录状态即可
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        // 需要删除当前登录状态, 删除当前的登录状态即可
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 通过User获得LoginUserVO脱敏信息，其实就是除掉了几个不用给用户看的信息而已
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVo(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO); // 使用Hutool工具类中的BeanUtil复制user中存在的参数到LoginUserVo脱敏数据中
        return loginUserVO;
    }

    /**
     * 管理员根据User数据获得脱敏后的UserVO数据
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO); // 使用Hutool工具类中的BeanUtil复制user中存在的参数到LoginUserVo脱敏数据中
        return userVO;
    }


    /**
     * 管理员根据User列表获得脱敏的UserVO
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVoList(List<User> userList) {
        // 需要将userList里面的遍历出来
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        // 用stream流转换
        // 需要练习Stream流转换
        List<UserVO> userVOList = userList.stream().map(this::getUserVO).collect(Collectors.toList());
        return userVOList;
    }


    /**
     * 针对于分页查询生成SQL语句
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 根据userQueryRequest请求参数封装成QueryWrapper语句
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        int pageNum = userQueryRequest.getPageNum();
        int pageSize = userQueryRequest.getPageSize();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

//        QueryWrapper queryWrapper = new QueryWrapper();//MyBatis-flex的特性
        // create是什么意思
        // create是利用QueryWrapper进行实例化，只是new了一个对象而已
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userName", userName)
                .like("userAccount", userAccount)
                .like("userProfile", userProfile)
                .orderBy(sortField, "order".equals(sortOrder));
    }

    /**
     * 通过当前的HttpServletRequest中登录设置的Session中的User，找到当前的用户，并获得当前登录用户信息
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 通过request获得User用户，返回当前登录的用户
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        // 虽然得到了currentUser,但是不一定是存在数据库，需要确认是否在数据库中
        User loginUser = this.getById(currentUser.getId());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return loginUser;
    }


    // 加密方法设计
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "lzh";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 判断用户是否是管理员
     *
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}
