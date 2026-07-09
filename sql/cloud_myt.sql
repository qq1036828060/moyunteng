-- 魔云腾云机容器调度平台业务表

drop table if exists cloud_host;
create table cloud_host (
  host_id bigint not null auto_increment comment '主机ID',
  host_name varchar(100) not null comment '主机名称',
  host_ip varchar(64) not null comment '主机IP',
  api_port int not null default 8000 comment 'SDK API端口',
  api_base_url varchar(255) default null comment 'SDK API地址',
  device_id varchar(100) default null comment '设备ID',
  model varchar(50) default null comment '设备型号',
  current_version int default null comment '当前版本',
  latest_version int default null comment '最新版本',
  online_status varchar(20) default 'UNKNOWN' comment '在线状态',
  enabled char(1) default '1' comment '是否启用',
  container_capacity int default 0 comment '容器容量',
  last_sync_time datetime default null comment '最近同步时间',
  remark varchar(500) default null comment '备注',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  del_flag char(1) default '0' comment '删除标志',
  primary key (host_id),
  key idx_cloud_host_ip (host_ip)
) engine=innodb auto_increment=100 comment='魔云腾主机表';

drop table if exists cloud_container;
create table cloud_container (
  container_id bigint not null auto_increment comment '容器ID',
  host_id bigint not null comment '主机ID',
  provider_container_id varchar(128) not null comment '魔云腾容器ID',
  container_name varchar(100) not null comment '容器名称',
  instance_id varchar(100) default null comment '业务实例ID',
  index_num int default null comment '实例位',
  container_status varchar(30) default null comment '容器状态',
  schedule_status varchar(30) default 'IDLE' comment '调度状态',
  android_type varchar(20) default null comment '安卓类型',
  container_ip varchar(64) default null comment '容器IP',
  network_name varchar(100) default null comment '网络名称',
  image varchar(255) default null comment '镜像',
  width int default null comment '宽度',
  height int default null comment '高度',
  dpi int default null comment 'DPI',
  webrtc_tcp_port int default null comment 'WebRTC TCP端口',
  webrtc_udp_port int default null comment 'WebRTC UDP端口',
  adb_port int default null comment 'ADB端口',
  current_task_id bigint default null comment '当前任务ID',
  bound_account_id bigint default null comment '绑定账号ID',
  lock_owner varchar(100) default null comment '锁持有者',
  lock_expire_time datetime default null comment '锁过期时间',
  last_heartbeat_time datetime default null comment '最近心跳时间',
  raw_json text comment 'SDK原始返回',
  remark varchar(500) default null comment '备注',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  del_flag char(1) default '0' comment '删除标志',
  primary key (container_id),
  unique key uk_cloud_container_provider (host_id, provider_container_id),
  key idx_cloud_container_host (host_id),
  key idx_cloud_container_schedule (schedule_status),
  key idx_cloud_container_index (host_id, index_num)
) engine=innodb auto_increment=100 comment='魔云腾容器实例表';

drop table if exists cloud_sdk_call_log;
create table cloud_sdk_call_log (
  log_id bigint not null auto_increment comment '日志ID',
  host_id bigint default null comment '主机ID',
  task_id bigint default null comment '任务ID',
  container_id bigint default null comment '容器ID',
  api_path varchar(255) default null comment 'SDK API路径',
  http_method varchar(10) default null comment '请求方式',
  request_body longtext comment '请求摘要',
  response_body longtext comment '响应摘要',
  result_code int default null comment 'SDK返回码',
  success char(1) default '0' comment '是否成功',
  error_msg varchar(1000) default null comment '错误信息',
  cost_ms int default null comment '耗时毫秒',
  create_time datetime default null comment '创建时间',
  primary key (log_id),
  key idx_cloud_sdk_host (host_id),
  key idx_cloud_sdk_create_time (create_time)
) engine=innodb auto_increment=100 comment='魔云腾SDK调用日志表';

insert into cloud_host(host_name, host_ip, api_port, api_base_url, online_status, enabled, create_by, create_time, remark)
values ('魔云腾主机-本地', '192.168.137.63', 8000, 'http://192.168.137.63:8000', 'UNKNOWN', '1', 'admin', sysdate(), '初始化接入主机');

-- 菜单可按需导入；如果菜单ID冲突，请调整后执行。
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '云机管理', 0, 6, 'cloud', null, null, '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), '魔云腾云机管理目录'
where not exists (select 1 from sys_menu where menu_name = '云机管理' and parent_id = 0);

set @cloud_menu_id := (select menu_id from sys_menu where menu_name = '云机管理' and parent_id = 0 limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '主机管理', @cloud_menu_id, 1, 'host', 'cloud/host/index', null, '', 1, 0, 'C', '0', '0', 'cloud:host:list', 'server', 'admin', sysdate(), '魔云腾主机管理'
where not exists (select 1 from sys_menu where menu_name = '主机管理' and parent_id = @cloud_menu_id);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '容器资源池', @cloud_menu_id, 2, 'container', 'cloud/container/index', null, '', 1, 0, 'C', '0', '0', 'cloud:container:list', 'online', 'admin', sysdate(), '安卓容器资源池'
where not exists (select 1 from sys_menu where menu_name = '容器资源池' and parent_id = @cloud_menu_id);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'SDK调用日志', @cloud_menu_id, 9, 'sdk-log', 'cloud/sdkLog/index', null, '', 1, 0, 'C', '0', '0', 'cloud:sdklog:list', 'log', 'admin', sysdate(), '魔云腾SDK调用日志'
where not exists (select 1 from sys_menu where menu_name = 'SDK调用日志' and parent_id = @cloud_menu_id);

set @cloud_host_menu_id := (select menu_id from sys_menu where menu_name = '主机管理' and parent_id = @cloud_menu_id limit 1);
set @cloud_container_menu_id := (select menu_id from sys_menu where menu_name = '容器资源池' and parent_id = @cloud_menu_id limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '主机查询', @cloud_host_menu_id, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:host:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:host:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '主机新增', @cloud_host_menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:host:add', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:host:add');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '主机修改', @cloud_host_menu_id, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:host:edit', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:host:edit');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '主机删除', @cloud_host_menu_id, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:host:remove', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:host:remove');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '测试连接', @cloud_host_menu_id, 5, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:host:test', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:host:test');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '同步容器', @cloud_host_menu_id, 6, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:host:sync', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:host:sync');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '容器查询', @cloud_container_menu_id, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:container:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:container:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '容器启动', @cloud_container_menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:container:start', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:container:start');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '容器停止', @cloud_container_menu_id, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:container:stop', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:container:stop');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '容器重启', @cloud_container_menu_id, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:container:restart', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:container:restart');
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '释放容器', @cloud_container_menu_id, 5, '#', '', null, '', 1, 0, 'F', '0', '0', 'cloud:container:release', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'cloud:container:release');
