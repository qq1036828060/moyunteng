-- 魔云腾云机管理系统第二阶段升级脚本
-- 用途：在已导入 cloud_myt.sql 的数据库上执行一次，增加用户分配与普通用户我的云机菜单。

alter table cloud_container
  add column assigned_user_id bigint default null comment '分配用户ID' after bound_account_id,
  add column assigned_user_name varchar(64) default null comment '分配用户名称' after assigned_user_id,
  add column bound_account_no varchar(100) default null comment '绑定业务账号' after assigned_user_name;

alter table cloud_container
  add key idx_cloud_container_owner (assigned_user_id),
  add key idx_cloud_container_account (bound_account_no);

set @cloud_menu_id := (select menu_id from sys_menu where menu_name in ('云机管理', '浜戞満绠＄悊') and parent_id = 0 limit 1);
set @cloud_container_menu_id := (select menu_id from sys_menu where perms = 'cloud:container:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '我的云机', @cloud_menu_id, 3, 'my-container', 'cloud/myContainer/index', null, '', 1, 0, 'C', '0', '0', 'cloud:mycontainer:list', 'mobile', 'admin', sysdate(), '普通用户查看自己名下云机'
where @cloud_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cloud:mycontainer:list');

set @cloud_my_container_menu_id := (select menu_id from sys_menu where perms = 'cloud:mycontainer:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '分配容器', @cloud_container_menu_id, 6, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:container:assign', '#', 'admin', sysdate(), ''
where @cloud_container_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cloud:container:assign');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '我的云机操作', @cloud_my_container_menu_id, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:mycontainer:operate', '#', 'admin', sysdate(), ''
where @cloud_my_container_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cloud:mycontainer:operate');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '绑定账号', @cloud_my_container_menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:mycontainer:bind', '#', 'admin', sysdate(), ''
where @cloud_my_container_menu_id is not null and not exists (select 1 from sys_menu where perms = 'cloud:mycontainer:bind');
