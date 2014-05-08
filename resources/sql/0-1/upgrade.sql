create table users (
  id integer primary key not null,
  login varchar (255),
  password varchar (255),
  email varchar (255),
  race varchar (255),
  active boolean,
  activation_code varchar(64),
  email_sent boolean
);

insert into keys values ('general_pool', 0);
update keys set value = 1 where key = 'version';
