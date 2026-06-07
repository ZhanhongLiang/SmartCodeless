package com.robotlive.smartcodeless.service;

import com.mybatisflex.core.service.IService;
import com.robotlive.smartcodeless.model.entity.BuildLog;

import java.util.List;

public interface BuildLogService extends IService<BuildLog> {

    List<BuildLog> listLogs(Long taskId, Long lastId, int pageSize);
}
