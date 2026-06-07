package com.robotlive.smartcodeless.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.robotlive.smartcodeless.mapper.BuildLogMapper;
import com.robotlive.smartcodeless.model.entity.BuildLog;
import com.robotlive.smartcodeless.service.BuildLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildLogServiceImpl extends ServiceImpl<BuildLogMapper, BuildLog> implements BuildLogService {

    @Override
    public List<BuildLog> listLogs(Long taskId, Long lastId, int pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("taskId", taskId)
                .orderBy("id", true)
                .limit(1, Math.min(Math.max(pageSize, 1), 200));
        if (lastId != null && lastId > 0) {
            queryWrapper.gt("id", lastId);
        }
        return this.list(queryWrapper);
    }
}
