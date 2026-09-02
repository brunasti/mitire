alter table project
    add column owner_id bigint references app_user (id);
