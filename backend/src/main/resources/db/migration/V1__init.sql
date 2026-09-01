create table app_user (
    id            bigserial primary key,
    username      varchar(50)  not null unique,
    full_name     varchar(150) not null,
    email         varchar(150) not null unique,
    password_hash varchar(200) not null,
    role          varchar(20)  not null,
    enabled       boolean      not null default true
);

create table project (
    id     bigserial primary key,
    code   varchar(30)  not null unique,
    name   varchar(150) not null,
    active boolean      not null default true
);

create table project_member (
    user_id    bigint not null references app_user (id) on delete cascade,
    project_id bigint not null references project (id) on delete cascade,
    primary key (user_id, project_id)
);

create table time_entry (
    id          bigserial primary key,
    user_id     bigint         not null references app_user (id),
    project_id  bigint         not null references project (id),
    work_date   date           not null,
    hours       numeric(4, 2)  not null,
    description varchar(1000),
    status      varchar(20)    not null,
    created_at  timestamptz    not null,
    updated_at  timestamptz    not null
);

create index idx_time_entry_user on time_entry (user_id);
create index idx_time_entry_project on time_entry (project_id);
create index idx_time_entry_work_date on time_entry (work_date);
