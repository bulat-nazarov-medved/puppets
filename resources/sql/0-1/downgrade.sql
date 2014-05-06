drop table users;

delete from keys where key = 'general_pool';
update keys set value = 0 where key = 'version';
