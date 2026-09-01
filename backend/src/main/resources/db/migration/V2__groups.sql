create table app_group (
    id   bigserial primary key,
    name varchar(150) not null unique
);

create table group_project (
    group_id   bigint not null references app_group (id) on delete cascade,
    project_id bigint not null references project (id) on delete cascade,
    primary key (group_id, project_id)
);

alter table app_user add column group_id bigint references app_group (id);

drop table project_member;
