create table project_status (
    id         bigserial primary key,
    project_id bigint      not null references project (id) on delete cascade,
    name       varchar(50) not null,
    sequence   integer     not null,
    unique (project_id, name)
);

insert into project_status (project_id, name, sequence)
select id, 'SUBMITTED', 1 from project
union all
select id, 'APPROVED', 2 from project
union all
select id, 'REJECTED', 3 from project;

alter table time_entry add column status_id bigint references project_status (id);

update time_entry te
set status_id = ps.id
from project_status ps
where ps.project_id = te.project_id
  and ps.name = te.status;

alter table time_entry alter column status_id set not null;
alter table time_entry drop column status;
