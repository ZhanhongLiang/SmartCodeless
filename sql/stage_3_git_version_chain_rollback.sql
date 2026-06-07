use ai_code_mother;

drop procedure if exists add_stage_3_chat_history_column;
delimiter $$
create procedure add_stage_3_chat_history_column(
    in p_column_name varchar(64),
    in p_column_definition varchar(1024)
)
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = 'chat_history'
          and column_name = p_column_name
    ) then
        set @sql = concat('alter table chat_history add column ', p_column_name, ' ', p_column_definition);
        prepare stmt from @sql;
        execute stmt;
        deallocate prepare stmt;
    end if;
end$$
delimiter ;

call add_stage_3_chat_history_column(
    'roundNo',
    'int null comment ''AI generation round number'''
);
call add_stage_3_chat_history_column(
    'commitId',
    'varchar(64) null comment ''Git commit id associated with this message/round'''
);

drop procedure if exists add_stage_3_chat_history_column;

create table if not exists app_version
(
    id                   bigint auto_increment comment 'id' primary key,
    appId                bigint                              not null comment 'app id',
    userId               bigint                              not null comment 'owner/user id',
    roundNo              int                                 not null comment 'AI generation round number',
    commitId             varchar(64)                         not null comment 'git commit id',
    commitMessage        varchar(512)                        not null comment 'git commit message',
    promptSummary        varchar(1024)                       null comment 'prompt summary',
    codeGenType          varchar(64)                         null comment 'code generation type',
    versionType          varchar(32) default 'AI_GENERATION' not null comment 'AI_GENERATION/ROLLBACK/MANUAL',
    rollbackFromCommitId varchar(64)                         null comment 'rollback source commit id',
    buildTaskId          bigint                              null comment 'build task id triggered by this version',
    createTime           datetime default CURRENT_TIMESTAMP  not null comment 'create time',
    updateTime           datetime default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete             tinyint  default 0                  not null comment 'logic delete',
    index idx_appId_roundNo (appId, roundNo),
    index idx_appId_createTime (appId, createTime),
    index idx_commitId (commitId),
    index idx_userId (userId)
) comment 'app git version records' collate = utf8mb4_unicode_ci;

