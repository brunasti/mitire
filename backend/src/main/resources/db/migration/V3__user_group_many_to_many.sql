create table user_group (
    user_id  bigint not null references app_user (id) on delete cascade,
    group_id bigint not null references app_group (id) on delete cascade,
    primary key (user_id, group_id)
);

insert into user_group (user_id, group_id)
select id, group_id from app_user where group_id is not null;

alter table app_user drop column group_id;
