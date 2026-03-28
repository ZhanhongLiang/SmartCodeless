package com.robotlive.smartcodeless.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.robotlive.smartcodeless.constant.AppConstant;
import com.robotlive.smartcodeless.core.AiCodeGeneratorFacade;
import com.robotlive.smartcodeless.exception.BusinessException;
import com.robotlive.smartcodeless.exception.ErrorCode;
import com.robotlive.smartcodeless.exception.ThrowUtils;
import com.robotlive.smartcodeless.model.dto.app.AppAddRequest;
import com.robotlive.smartcodeless.model.dto.app.AppQueryRequest;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.mapper.AppMapper;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.enums.ChatHistoryMessageTypeEnum;
import com.robotlive.smartcodeless.model.enums.CodeGenTypeEnum;
import com.robotlive.smartcodeless.model.vo.AppVO;
import com.robotlive.smartcodeless.model.vo.UserVO;
import com.robotlive.smartcodeless.service.AppService;
import com.robotlive.smartcodeless.service.ChatHistoryService;
import com.robotlive.smartcodeless.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.Serializable;



/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/ZhanhongLiang">Jean</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    // 理论上addAPP, 需要AppService，所以这里先留着
    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService  chatHistoryService;

    @Resource
    AiCodeGeneratorFacade  aiCodeGeneratorFacade;

    @Override
    public Long addApp(AppAddRequest appAddRequest, User loginUser){
        // initPrompt就是用户初始提示词,需要先拿到该词
        String initPrompt = appAddRequest.getInitPrompt();
        // 校验提示词
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR);
        // 将App参数转换一下, 构造入库对象
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app); // 将app的initPrompt参数复制给app实例对象
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 暂时设置为多文件生成
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue()); // 这里需要暂时设置为多文件生成
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return app.getId();
    }

    /**
     *
     * 根据Picture设置User信息到PictureVO里面
     *
     * @param app
     * @return
     */
    @Override
    public AppVO getAppVO(App app) {
        // 把space封装成spaceVO类
//        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 通过userID查询完整USER信息
//        if (app == null){
//            return null;
//        }
//        AppVO appVO = new AppVO();
//        BeanUtils.copyProperties(app, appVO);
        AppVO appVO = AppVO.objToVo(app); // 其实就是封装了

        Long userId = app.getUserId(); // 获得UserID，通过UserID查询User信息
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user);
            appVO.setUser(userVo);
        }
        return appVO;
    }


    /**
     * 分页获取APP封装, 为Page<Picture>对象进行获得封装
     * 将Page<Picture>中的Picture设置变成PictureVO，且需要达到O(1)查找时间复杂度，所以需要利用Map来构造
     * 这个类很重要，需要搞懂
     */
    /**
     * 这个是第一种写法
     * @param appPage
     * @param request
     * @return
     */
    @Override
    public Page<AppVO> getAppVOPage(Page<App> appPage, HttpServletRequest request) {
        // 取出记录，里面用Array存储Space对象
        List<App> appList = appPage.getRecords();
        // 封装构建好Page<SpaceVO>对象，用new Page<>进行封装
        Page<AppVO> appVOPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        if (CollUtil.isEmpty(appList)) {
            return appVOPage; // 判断是否存在Space对象
        }
        // 对象列表 => 封装对象列表
        // spaceList.stream()变成数据流，方便利用lambda表达式
        // 这个就是用来转换成，将Space转换为SpaceVO类，然后用collectors转换为list对象
        List<AppVO> appVOList = appList.stream().map(AppVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 同理，用一个去重集合set来存储userId，每张图片代表代表一个id，但是id是有重复的
        Set<Long> userIdSet = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        // 构建一个 "userID" : List<User>的Map，方便后续直接查找，达到O(1)的查找时间复杂度
        // 这个Map底层远离应该是红黑树，理论上查找应该为O(long)或者B+树之类的，但是为什么是O(1)，没想通
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        // 这个就是将spaceVOList里面将里面每个SpaceVO对象中的UserVO填充信息，还需要将user转换为UserVO脱敏对象
        appVOList.forEach(appVO -> {
            Long userId = appVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            appVO.setUser(userService.getUserVo(user));
        });
        // 设置Page记录
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }


    /**
     *
     * @param appList
     * @return
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVo));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }



    /**
     * 获得分页查询接口，需要返回QueryWrapper
     *
     * @param appQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        // 获得所有请求参数
        // 构建查询，利用QueryWrapper
        QueryWrapper queryWrapper = new QueryWrapper();
        if (appQueryRequest == null) {
            return queryWrapper;
        }
        //  获得请求参数里面包含的所有信息
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        int pageNum = appQueryRequest.getPageNum();
        int pageSize = appQueryRequest.getPageSize();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        // eq搜索
        // 拼接查询条件
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


    /**
     * 删除应用时，关联删除对话历史
     * 这个是覆盖Java中Serializable的相关
     * @param id
     * @return
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用关联的对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }


    /**
     *  关键Service, 调用了门面类aiCodeGeneratorFacade, 里面集成了所有保存文件、流转换的细节
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // message就是传入的信息，其实就是用户填入的提示词, 为什么这里appId其实是一定存在的，是因为addApp,这个是首次添加应用，也就是输入初始提示词
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR,"应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message),ErrorCode.PARAMS_ERROR, "输入提示词为空");
        // 2. 根据appId查询app
        App app = this.getById(appId);
        // 3. 权限校验，仅本人可以和自己的应用对话
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }
        // 5. 调用 AI 生成代码， 这个是门面代码, 然后利用generateAndSaveCodeStream
//        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 记录保存是用户的聊天信息
        // 这个是用户输入的信息，addChatMessage
        chatHistoryService.addChatMessage(appId,message, ChatHistoryMessageTypeEnum.USER.getValue(),loginUser.getId());
        // 6. 调用 AI 生成代码（流式）
        Flux<String> contentFlux  = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7. 收集 AI 响应的内容，并且在完成后保存记录到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();
        // 这个是保存后面AI返回的信息, 就是需要保存AI返回的信息到chatHistoryService历史对话中
        return contentFlux
                .map(chunk ->{
                    // 实时收集 AI 响应的内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                }).doOnComplete(() ->{
                    // 流式返回完成后，保存 AI 消息到对话历史中
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId,message, ChatHistoryMessageTypeEnum.AI.getValue(),loginUser.getId());
                }).doOnError(error->{
                    // 如果 AI 回复失败，也需要保存记录到数据库中
                    String errorMessage = "AI 回复失败：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * nginx服务器端部署
     * @param appId 应用 ID
     * @param loginUser 登录用户
     * @return
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 根据appId查询App应用
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以部署自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有，则生成 6 位 deployKey（字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，获取原始代码生成路径（应用访问目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }

        // 7. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }

        // 8. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9. 返回可访问的 URL 地址
        return String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }


}
