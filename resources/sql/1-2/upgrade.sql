create table resourceholder (
    id integer,
    current_value integer,
    max_value integer,
    d_value integer,
    cell integer references cell (id)
);

create table cell (
    id integer,
    x integer,
    y integer,
    d_food integer
);

update keys set value = 2 where key = 'version';
