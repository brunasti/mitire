create table time_entry_transition (
    id            bigserial primary key,
    time_entry_id bigint      not null references time_entry (id) on delete cascade,
    old_status_id bigint      not null references project_entry_status (id),
    new_status_id bigint      not null references project_entry_status (id),
    changed_by_id bigint      not null references app_user (id),
    created_at    timestamptz not null default now()
);

create index idx_time_entry_transition_time_entry on time_entry_transition (time_entry_id);
