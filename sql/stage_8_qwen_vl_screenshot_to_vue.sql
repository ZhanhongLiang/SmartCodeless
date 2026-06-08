create table if not exists reference_image (
    id varchar(64) primary key,
    appId bigint null,
    userId bigint not null,
    originalName varchar(255) null,
    storagePath varchar(1024) not null,
    mimeType varchar(64) not null,
    fileSize bigint not null,
    width int null,
    height int null,
    createTime datetime default current_timestamp,
    updateTime datetime default current_timestamp on update current_timestamp,
    isDelete tinyint default 0,
    index idx_reference_image_app (appId),
    index idx_reference_image_user (userId)
);
