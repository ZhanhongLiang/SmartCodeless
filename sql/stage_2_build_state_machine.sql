use ai_code_mother;

-- Stage 2: async build state fields for app.
-- Keep this migration rerunnable for local debugging.
drop procedure if exists add_stage_2_app_column;
delimiter $$
create procedure add_stage_2_app_column(
    in p_column_name varchar(64),
    in p_column_definition varchar(1024)
)
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'app'
          and column_name = p_column_name
    ) then
        set @sql = concat('alter table app add column ', p_column_name, ' ', p_column_definition);
        prepare stmt from @sql;
        execute stmt;
        deallocate prepare stmt;
    end if;
end$$
delimiter ;

call add_stage_2_app_column(
    'deployStatus',
    'varchar(32) not null default ''NONE'' comment ''build/deploy status: NONE/QUEUED/RUNNING/SUCCESS/FAILED/CANCELED'''
);
call add_stage_2_app_column(
    'buildTaskId',
    'bigint null comment ''latest build task id'''
);
call add_stage_2_app_column(
    'buildErrorMessage',
    'varchar(1024) null comment ''latest build error message'''
);

drop procedure if exists add_stage_2_app_column;

-- Stage 2: async build task table.
create table if not exists build_task
(
    id           bigint auto_increment comment 'id' primary key,
    appId        bigint                             not null comment 'app id',
    userId       bigint                             not null comment 'user id',
    triggerType  varchar(32) default 'DEPLOY'       not null comment 'trigger type: DEPLOY/REBUILD/MANUAL',
    status       varchar(32) default 'QUEUED'       not null comment 'status: QUEUED/RUNNING/SUCCESS/FAILED/CANCELED',
    sourceDir    varchar(512)                       null comment 'source directory',
    distDir      varchar(512)                       null comment 'dist directory',
    deployKey    varchar(64)                        null comment 'deploy key',
    errorMessage varchar(1024)                      null comment 'error message',
    queuedTime   datetime default CURRENT_TIMESTAMP not null comment 'queued time',
    startTime    datetime                           null comment 'start time',
    finishTime   datetime                           null comment 'finish time',
    createTime   datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete     tinyint  default 0                 not null comment 'logic delete',
    INDEX idx_appId (appId),
    INDEX idx_userId (userId),
    INDEX idx_status (status),
    INDEX idx_app_status (appId, status),
    INDEX idx_createTime (createTime)
) comment 'async build task' collate = utf8mb4_unicode_ci;

-- Stage 2: async build log table.
create table if not exists build_log
(
    id         bigint auto_increment comment 'id' primary key,
    taskId     bigint                             not null comment 'build task id',
    appId      bigint                             not null comment 'app id',
    logType    varchar(32) default 'INFO'         not null comment 'INFO/STDOUT/STDERR/ERROR/SYSTEM',
    content    text                               not null comment 'log content',
    lineNo     int                                null comment 'line number',
    createTime datetime default CURRENT_TIMESTAMP not null comment 'create time',
    isDelete   tinyint  default 0                 not null comment 'logic delete',
    INDEX idx_taskId (taskId),
    INDEX idx_appId (appId),
    INDEX idx_task_line (taskId, lineNo),
    INDEX idx_createTime (createTime)
) comment 'async build log' collate = utf8mb4_unicode_ci;
