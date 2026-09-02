alter table project
    add column approver_id bigint references app_user (id);
