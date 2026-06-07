create table if not exists quality_check_task (
    id bigint primary key auto_increment,
    appId bigint not null,
    userId bigint not null,
    versionId bigint null,
    commitId varchar(128) null,
    buildTaskId bigint null,
    triggerType varchar(64) not null,
    status varchar(32) not null,
    currentStage varchar(64) null,
    failureCategory varchar(64) null,
    score int null,
    errorMessage varchar(1024) null,
    createTime datetime default current_timestamp,
    updateTime datetime default current_timestamp on update current_timestamp,
    finishTime datetime null,
    isDelete tinyint default 0,
    index idx_qct_app (appId),
    index idx_qct_user (userId),
    index idx_qct_status (status)
);

create table if not exists quality_check_report (
    id bigint primary key auto_increment,
    taskId bigint not null,
    appId bigint not null,
    userId bigint not null,
    dependencyPolicyStatus varchar(32) null,
    scriptPolicyStatus varchar(32) null,
    buildStatus varchar(32) null,
    previewSmokeStatus varchar(32) null,
    repairStatus varchar(32) null,
    failureCategory varchar(64) null,
    score int null,
    summaryJson text null,
    sanitizedLogSummary text null,
    createTime datetime default current_timestamp,
    updateTime datetime default current_timestamp on update current_timestamp,
    isDelete tinyint default 0,
    index idx_qcr_app (appId),
    index idx_qcr_task (taskId)
);

create table if not exists self_healing_attempt (
    id bigint primary key auto_increment,
    taskId bigint not null,
    appId bigint not null,
    userId bigint not null,
    attemptNo int not null,
    status varchar(32) not null,
    failureCategory varchar(64) null,
    patchTargetFile varchar(512) null,
    beforeSnippet mediumtext null,
    afterSnippet mediumtext null,
    unifiedDiff mediumtext null,
    diagnosis varchar(1024) null,
    errorMessage varchar(1024) null,
    createTime datetime default current_timestamp,
    updateTime datetime default current_timestamp on update current_timestamp,
    finishTime datetime null,
    isDelete tinyint default 0,
    index idx_sha_task (taskId),
    index idx_sha_app (appId)
);

create table if not exists dependency_policy_violation (
    id bigint primary key auto_increment,
    taskId bigint not null,
    appId bigint not null,
    packageName varchar(256) null,
    versionSpec varchar(128) null,
    violationType varchar(64) not null,
    severity varchar(32) not null,
    message varchar(1024) null,
    createTime datetime default current_timestamp,
    isDelete tinyint default 0,
    index idx_dpv_task (taskId),
    index idx_dpv_app (appId)
);
