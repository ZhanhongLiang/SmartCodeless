package com.robotlive.smartcodeless.build;

import com.robotlive.smartcodeless.model.entity.BuildLog;
import com.robotlive.smartcodeless.service.BuildLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class BuildLogAppender {

    @Resource
    private BuildLogService buildLogService;

    public void append(Long taskId, Long appId, String logType, String content, Integer lineNo) {
        if (taskId == null || appId == null || content == null) {
            return;
        }
        BuildLog buildLog = BuildLog.builder()
                .taskId(taskId)
                .appId(appId)
                .logType(logType)
                .content(content)
                .lineNo(lineNo)
                .build();
        buildLogService.save(buildLog);
    }

    public void system(Long taskId, Long appId, String content) {
        append(taskId, appId, "SYSTEM", content, null);
    }

    public void error(Long taskId, Long appId, String content) {
        append(taskId, appId, "ERROR", content, null);
    }
}
