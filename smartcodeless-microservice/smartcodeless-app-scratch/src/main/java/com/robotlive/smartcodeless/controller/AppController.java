package com.robotlive.smartcodeless.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.robotlive.smartcodeless.annotation.AuthCheck;
import com.robotlive.smartcodeless.common.BaseResponse;
import com.robotlive.smartcodeless.common.DeleteRequest;
import com.robotlive.smartcodeless.common.ResultUtils;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.constant.UserConstant;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.innerservice.InnerUserService;
import com.robotlive.smartcodeless.model.dto.app.*;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.AppVO;
import com.robotlive.smartcodeless.ratelimiter.annotation.RateLimit;
import com.robotlive.smartcodeless.ratelimiter.enums.RateLimitType;
import com.robotlive.smartcodeless.service.AppService;
import com.robotlive.smartcodeless.service.ProjectDownloadService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/ZhanhongLiang">Jean</a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

//    @Resource
//    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    /**
     * 添加对话App
     * @param appAddRequest 创建应用请求
     * @param request 请求
     * @return
     */
    @PostMapping("add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request){
        // 1. 校验参数
//        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 获得当前登录用户
        User loginUser = InnerUserService.getLoginUser(request);
//        // 封装到Service层
//        Long newId = appService.addApp(appAddRequest, loginUser);
//        return ResultUtils.success(newId);
        Long appId = appService.createApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
    }


    /**
     * 修改应用名称，现在只允许用户修改自己的应用名称, 其他修改不行
     * @param appUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request){
        // 先判断request是否存在，校验参数
        if (appUpdateRequest == null || appUpdateRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获得当前登录用户
        User loginUser = InnerUserService.getLoginUser(request);
        // 需要判断当前的用户id是否是该空间的userId, 也就是当前登录用户需要是能修改该用户名称
        // 需要更新空间，只能更新, 请求参数是spaceUpdateRequest，那是返回的参数，这个参数只有name、introduction、category、tags
        // 并且里面是List<String> tags的形式, 还需要将tags转换为非tags形式，
        // 获得当前的请求id
        Long id = appUpdateRequest.getId();
        // 获得老app
        App oldApp = appService.getById(id);
        // 判断当前的空间是否存在
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR, "空间为空");
        // 仅本人可更新
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
//        BeanUtil.copyProperties(appUpdateRequest, app); //重新赋值
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());
        app.setEditTime(LocalDateTime.now());
        // 需要更新空间,操作数据库
        boolean result = appService.updateById(app); // 更新空间, 但是只更新里面其他内容，不更新id
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 根据id删除应用
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 获得当前请求的id
        Long id = deleteRequest.getId();
        // 获得当前的登录用户信息
        User loginUser = InnerUserService.getLoginUser(request);

        // 获得当前的空间
        App oldApp = appService.getById(id);
        // 判断当前的空间是否存在
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR, "空间为空");
        // 仅仅本人 或 管理员才能删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR); // 没有权限
        }
//        spaceService.checkSpaceAuth(loginUser, oldSpace);
        // 根据id删除空间
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 【用户】根据id 查看应用详情, 由于
     */

    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVoById(Long id){
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);

        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获得封装类, 返回AppVO信息，里面是包含UserVO信息的
        return ResultUtils.success(appService.getAppVO(app));
    }



//    /**
//     * 普通用户获得分页脱敏信息, 用户只能看到过审的空间，不过审核的空间是不能看到的
//     * 1.展示分页数据是高频度的动作，是读多写少的操作，适合利用Redis进行操作，所以需要改为用Redis
//     * 2.如果是需要超高性能的读操作，还是需要Caffeine进行操作，该技术是本地缓存技术
//     *
//     * @param appQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/my/list/page/vo")
//    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
//        // 获得一页大小
//        int pageSize = appQueryRequest.getPageSize();
//        // 获得页数
//        int pageNum = appQueryRequest.getPageNum();
//        // 限制爬虫
//        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR); // 限制爬虫
//        // 设置新的分页
//        Page<App> appPage = appService.page(Page.of(pageNum,pageSize), appService.getQueryWrapper(appQueryRequest));
//        // 将spacePage分页转换为spaceVOPage分页
//        Page<AppVO> appVOPage = appService.getAppVOPage(appPage, request);
//        return ResultUtils.success(appVOPage);
//    }

    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = InnerUserService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }


    /**
     * 分页获取精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    @Cacheable(
            value = "good_app_page",
            key = "T(com.robotlive.smartcodeless.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }


    /**
     * 管理员根据id删除任意应用
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 获得当前请求的id
        Long id = deleteRequest.getId();
        // 获得当前的空间
        App oldApp = appService.getById(id);
        // 判断当前的空间是否存在
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR, "空间为空");
//        spaceService.checkSpaceAuth(loginUser, oldSpace);
        // 根据id删除空间
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员可以任意修改应用的名称、封面、应用优先级
     * @param appAdminUpdateRequest
     * @return
     */
    @PostMapping("/admin/update")
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest){
        // 先判断request是否存在，校验参数
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 需要判断当前的用户id是否是该空间的userId, 也就是当前登录用户需要是能修改该用户名称
        // 需要更新空间，只能更新, 请求参数是spaceUpdateRequest，那是返回的参数，这个参数只有name、introduction、category、tags
        // 并且里面是List<String> tags的形式, 还需要将tags转换为非tags形式，
        // 获得当前的请求id
        Long id = appAdminUpdateRequest.getId();
        // 获得老app
        App oldApp = appService.getById(id);
        // 判断当前的空间是否存在
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR, "空间为空");
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app); //重新赋值
//        app.setId(id);
//        app.setAppName(appAdminUpdateRequest.getAppName());
        app.setEditTime(LocalDateTime.now());
        // 需要更新空间,操作数据库
        boolean result = appService.updateById(app); // 更新空间, 但是只更新里面其他内容，不更新id
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 【管理员】分页查询应用列表(支持根据除时间外的任何字段查询,每页数量不限)
     * @param appQueryRequest
     * @return
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 添加contentFlux.map()操作, 需要包装成JSON数据形式，相当于key:value方式，防止后端输出数据缺少空格
     * @param appId
     * @param message
     * @param request
     * @return
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 这个没有定义请求类，因为GetMapping接受参数的特殊性
        // 先校验参数
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 获取当前登录用户
        User loginUser = InnerUserService.getLoginUser(request);
        // 通过获得
        // 调用服务生成代码（SSE 流式返回）
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件，添加done操作，就是为了方便告诉前端数据结束的位置
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 检查部署请求是否为空
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取应用 ID
        Long appId = appDeployRequest.getAppId();
        // 检查应用 ID 是否为空
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = InnerUserService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        // 返回部署 URL
        return ResultUtils.success(deployUrl);
    }

    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = InnerUserService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }


}
