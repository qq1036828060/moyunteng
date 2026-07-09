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

- [x] 主端按 `主机ID + 实例位` 聚合展示容器资源池。
- [x] 主端支持按实例位批量分配/取消分配用户。
- [x] 用户端“我的云机”只查询当前登录用户名下云机。
- [x] 用户端支持绑定业务账号。
- [ ] 新增账号表、任务表、WebRTC 会话表。
- [ ] 实现任务状态机。
- [ ] 实现容器锁与实例位互斥。
- [ ] 实现任务完成、失败、取消、超时释放资源。

### 第三阶段：WebRTC 控制

- [x] 用户端按归属校验生成当前用户云机 WebRTC 播放地址。
- [x] 根据容器 `webrtc_tcp_port` / `webrtc_udp_port` 生成地址，缺失时按 `indexNum` 计算端口。
- [x] 新增用户端接口 `GET /cloud/my/container/{containerId}/webrtc`。
- [x] 进入控制前同步 SDK 容器状态，未运行时返回明确提示，避免播放器黑屏。
- [x] 返回 WebRTC 播放器路径与流参数，由前端选择本地或独立部署的播放器地址。
- [x] 用户进入控制时自动检查同主机同实例位运行状态，关闭其他运行容器并启动当前容器，保证单实例位只控制一个云机。
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

## WebRTC 控制说明

魔云腾 WebRTC 播放器地址按官方文档拼接：

```text
/webplayer/play-myt.html?shost={hostIp}&sport={tcpPort}&q=1&v=h264&rtc_i={hostIp}&rtc_p={udpPort}
```

后端接口会校验容器必须属于当前用户，并在返回前同步一次魔云腾 SDK 容器状态：

- 同主机同实例位没有容器时，提示“当前实例位没有可控制的云机”。
- 当前云机已运行且无其他运行容器时，直接返回播放地址。
- 当前云机未运行时，自动启动当前云机。
- 同主机同实例位存在其他 `running` 容器时，先关闭其他容器，再启动或保留当前容器。
- 准备完成后返回 `playPath` / `playUrl` / `query` / `tcpPort` / `udpPort` / `controlMessage` 等信息。

魔云腾主机的 SDK API 服务不一定提供 `/webplayer/play.html` 静态页面，播放器应部署在前端 `public/webplayer`，或由前端配置独立播放器静态服务地址。

## 重要文档

- `doc/魔云腾云机管理系统设计文档.md`
- `README_RUOYI.md`

## 2026-07-06 Phase 4 RPA 用户操作更新

- [x] 接入魔云腾官方 RPA 接口：`/rpa/connect`、`/rpa/open_app`、`/rpa/click`、`/rpa/shell`。
- [x] 新增用户端接口 `POST /cloud/my/container/{containerId}/qq/open`，用于准备云机并打开 QQ，用户在 WebRTC 中手动完成登录。
- [x] 新增用户端接口 `POST /cloud/my/container/{containerId}/qq/scan`，支持上传二维码图片并下发打开 QQ、点击右上角加号、进入扫一扫、尝试二次确认的 RPA 操作。
- [x] QQ 操作复用用户云机归属校验和同主机同实例位互斥控制逻辑，避免普通用户越权或同实例位资源抢占。
- [x] 兼容当前魔云腾 SDK `currentVersion=102` 未暴露 `/rpa/*` 的情况：RPA 接口 404 时自动降级为 `/android/exec` 执行 `monkey -p com.tencent.mobileqq` 和 `input tap`。
- [x] SDK 客户端遇到非 JSON 的 `Not Found` 响应时返回明确错误，不再触发 fastjson 解析异常。
- [x] 扫码接口新增二维码图片解析能力，解析到链接或 scheme 后会尝试在云机内通过 Android VIEW intent 打开，作为旧版 SDK 缺少图片投喂能力时的补充路径。
- [x] 识别魔云腾 `androidType`：`V2` 展示为容器云机，`V3` 展示为模拟器云机。
- [x] 按官方 Android API 端口公式补全实例位端口：Android API `30000 + (index - 1) * 100 + 1`，摄像头 TCP/UDP 为 `+5/+6`。
- [x] 扫码流程接入 Android API：上传二维码图片到安卓实例 `/upload`，设置虚拟摄像头源 `modifydev?cmd=4&type=image`，启动虚拟摄像头 `camera?cmd=start`，再进入 QQ 扫一扫。
- [ ] 后续按真实 QQ 版本校准“扫一扫”菜单坐标，并补充失败截图/重试日志。
## 2026-07-07 实例位分配同步修复

- [x] 同步容器时，如果同一主机同一实例位已有明确归属用户，新同步进来的容器会继承该实例位归属，用户端下次查询即可看到新增云机。
- [x] 对已经同步进库但归属为空的容器，再次同步时会在不覆盖已有归属的前提下补齐 `assigned_user_id` / `assigned_user_name`。
- [x] 主端把实例位重新分配给同一用户时，不再清空 `bound_account_id` / `bound_account_no`；只有实例位从其他用户转移给新用户时才清理旧账号绑定。
## 2026-07-07 扫码图片本地上传适配

- [x] 根据魔云腾 Android API 文档，虚拟摄像头 `modifydev?cmd=4&type=image&path=...` 的 `path` 必须使用安卓实例本地资源路径。
- [x] 用户端扫码接口可接收前端三方图片上传返回的 `filePath`，该地址仅作为来源记录返回，不直接作为虚拟摄像头源。
- [x] 扫码时后端仍会通过安卓实例 `/upload` 上传图片，拿到本地路径后再调用 `modifydev` 设置虚拟摄像头源。
- [x] 二维码图片上传采用覆盖模式：同一容器固定写入 `qq_scan_{containerId}.{ext}`，避免每次扫码都在安卓实例中生成新文件。
- [x] `/upload/qq_scan_{containerId}.{ext}` 是 Android API 资源路径，不等同于安卓 shell 文件系统目录；后端不再用 shell 校验 `/upload` 目录，上传 HTTP 成功后直接把该路径传给 `modifydev`。
- [x] 安卓 API 地址优先使用魔云腾主机 IP + 实例位端口，例如实例位 1 使用 `30001`，避免误连不可达的容器内网 IP。
## 2026-07-09 S5 proxy update

- [x] Parse S5 proxy fields from Moyunteng `GET /android`: `s5IP`, `s5Port`, `s5User`, `s5Password`, `s5Type`.
- [x] Add user-side APIs:
  - `PUT /cloud/my/container/{containerId}/proxy/s5` to set or disable S5 proxy.
  - `GET /cloud/my/container/{containerId}/proxy/s5` to refresh and query current S5 proxy status.
- [x] S5 status is derived from the latest Moyunteng cloud machine list response and returned in user cloud machine list as `s5Status`.
- [x] No database schema change is required; S5 values are read from `raw_json` after SDK sync/refresh.
