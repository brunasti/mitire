alter table project_entity_status
    add column active          boolean not null default true,
    add column starting_status boolean not null default false,
    add column description     text;

update project_entity_status pes
set starting_status = true
from (
    select distinct on (project_id) id
    from project_entity_status
    order by project_id, sequence
) first_status
where pes.id = first_status.id;
