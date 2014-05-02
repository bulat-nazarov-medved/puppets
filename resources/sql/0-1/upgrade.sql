create type race_type as enum ('clojure', 'groovy', 'scala');

create table users (
  id integer primary key not null,
  login varchar (255),
  email varchar (255),
  race race_type
);

update keys set value = 1 where key = 'version';
