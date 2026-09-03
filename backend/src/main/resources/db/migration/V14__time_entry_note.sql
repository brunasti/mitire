create table time_entry_note (
    id            bigserial primary key,
    time_entry_id bigint      not null references time_entry (id) on delete cascade,
    author_id     bigint      not null references app_user (id),
    text          text        not null,
    created_at    timestamptz not null default now()
);

create index idx_time_entry_note_time_entry on time_entry_note (time_entry_id);
