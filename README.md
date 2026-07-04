# 魔云腾云机容器调度平台后端

本分支为后端工程，基于 RuoYi v3.9.2 / Spring Boot 4 改造，目标是建设云机提供商侧的主机、容器、账号、任务调度、WebRTC 控制与 SDK 调用管理能力。

原若依说明文档已保留为 `README_RUOYI.md`。

## 当前开发任务

### 第一阶段：资源纳管

- [x] 保留原若依 README，新建项目 README。
- [x] 编写魔云腾容器调度设计文档。
- [x] 接入魔云腾主机 `192.168.137.63:8000` 的 SDK API 设计。
- [x] 新增主机表、容器表、SDK 调用日志表。
- [x] 实现主机连接测试：`GET /info`。
- [x] 实现容器同步：`GET /android`。
- [x] 实现容器启动、停止、重启。
- [x] 实现后台主机管理和容器资源池接口。

### 第二阶段：账号与任务调度

- [ ] 新增账号表、任务表、WebRTC 会话表。
- [ ] 实现账号绑定容器。
- [ ] 实现任务状态机。
- [ ] 实现容器锁与实例位互斥。
- [ ] 实现任务完成、失败、取消、超时释放资源。

### 第三阶段：WebRTC 控制

- [ ] 根据容器 `portBindings` 或 `indexNum` 生成 WebRTC 地址。
- [ ] 实现 WebRTC 会话创建、查询、关闭。
- [ ] 实现用户只能访问自己任务会话。

### 第四阶段：RPA / Android API

- [ ] 抽象统一执行指令。
- [ ] 接入魔云腾 RPA / Android API。
- [ ] 支持任务执行日志、失败重试、结果回传。

### 第五阶段：对外接入

- [ ] 设计 `/openapi` 接入接口。
- [ ] 实现 AccessKey / Secret 签名。
- [ ] 增加限流、配额、调用审计。

## 本地开发

1. 导入 `sql/ry_20260417.sql` 初始化若依基础库。
2. 导入 `sql/cloud_myt.sql` 初始化魔云腾业务表和菜单。
3. 修改 `ruoyi-admin/src/main/resources/application-druid.yml` 中的数据库配置。
4. 启动 Redis。
5. 运行 `com.ruoyi.RuoYiApplication`。

## 重要文档

- `doc/魔云腾云机管理系统设计文档.md`
- `README_RUOYI.md`
