package com.robotlive.smartcodeless.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.robotlive.smartcodeless.model.dto.app.AppAddRequest;
import com.robotlive.smartcodeless.model.dto.app.AppQueryRequest;
import com.robotlive.smartcodeless.model.entity.App;
import com.robotlive.smartcodeless.model.entity.User;
import com.robotlive.smartcodeless.model.vo.AppVO;
import com.robotlive.smartcodeless.ai.stream.AgentStreamEmitter;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/ZhanhongLiang">Jean</a>
 */
public interface AppService extends IService<App> {

    Long addApp(AppAddRequest appAddRequest, User loginUser);

    AppVO getAppVO(App app);

    Page<AppVO> getAppVOPage(Page<App> appPage, HttpServletRequest request);

    List<AppVO> getAppVOList(List<App> appList);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    boolean removeById(Serializable id);

    /**
     * 通过对话生成应用代码
     *
     * @param appId 应用 ID
     * @param message 提示词
     * @param loginUser 登录用户
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    void chatToGenCodeV2(Long appId, String message, User loginUser, AgentStreamEmitter emitter, boolean enableDiff);

    /**
     * 应用部署
     *
     * @param appId 应用 ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 创建应用
     *
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);
}
