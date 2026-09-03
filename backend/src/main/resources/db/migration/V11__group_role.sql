alter table app_group
    add column role varchar(20) not null default 'MEMBER';
