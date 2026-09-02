create table project_entity_status_transition (
    id                bigserial primary key,
    parent_status_id  bigint not null references project_entity_status (id) on delete cascade,
    child_status_id   bigint not null references project_entity_status (id) on delete cascade,
    unique (parent_status_id, child_status_id)
);
