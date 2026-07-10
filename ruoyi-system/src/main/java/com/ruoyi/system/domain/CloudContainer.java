package com.ruoyi.system.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 云机容器资源。
 * 一条记录对应魔云腾返回的一个安卓容器/模拟器云机，并带有本系统的分配、账号绑定和调度状态。
 */
public class CloudContainer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 容器主键ID */
    private Long containerId;
    /** 所属主机ID */
    private Long hostId;
    /** 魔云腾侧容器ID */
    private String providerContainerId;
    /** 魔云腾侧容器名称，启动/停止等 SDK 指令使用 */
    private String containerName;
    /** 实例ID，当前与 providerContainerId 保持一致 */
    private String instanceId;
    /** 实例位序号，同一主机下一个实例位同时只允许一个云机运行 */
    private Integer indexNum;
    /** 容器运行状态，例如 running/stopped */
    private String containerStatus;
    /** 调度状态，例如 IDLE/BUSY */
    private String scheduleStatus;
    /** 安卓类型：V2 容器云机，V3 模拟器云机 */
    private String androidType;
    /** 容器内部IP，仅作为备用信息，外部控制优先使用主机IP+实例位端口 */
    private String containerIp;
    /** Docker/容器网络名称 */
    private String networkName;
    /** 镜像名称 */
    private String image;
    /** 屏幕宽度 */
    private Integer width;
    /** 屏幕高度 */
    private Integer height;
    /** 屏幕 DPI */
    private Integer dpi;
    /** WebRTC TCP 端口 */
    private Integer webrtcTcpPort;
    /** WebRTC UDP 端口 */
    private Integer webrtcUdpPort;
    /** ADB 端口 */
    private Integer adbPort;
    /** Android API 端口，按 30000 + (index - 1) * 100 + 1 推导 */
    private Integer androidApiPort;
    /** Android RPA 端口 */
    private Integer androidRpaPort;
    /** 虚拟摄像头 TCP 端口 */
    private Integer cameraTcpPort;
    /** 虚拟摄像头 UDP 端口 */
    private Integer cameraUdpPort;
    /** 展示用云机类型名称 */
    private String cloudMachineType;
    /** S5 代理用户名，来自 rawJson 或用户设置 */
    private String s5User;
    /** S5 代理密码，来自 rawJson 或用户设置 */
    private String s5Password;
    /** S5 代理IP，来自 rawJson 或用户设置 */
    private String s5Ip;
    /** S5 代理端口，来自 rawJson 或用户设置 */
    private String s5Port;
    /** S5 代理类型，0 表示未开启/关闭 */
    private String s5Type;
    /** 展示用 S5 代理状态 */
    private String s5Status;
    /** 当前执行中的任务ID，预留给后续任务调度 */
    private Long currentTaskId;
    /** 已绑定业务账号ID，预留字段 */
    private Long boundAccountId;
    /** 已绑定业务账号编号 */
    private String boundAccountNo;
    /** 分配给的系统用户ID */
    private Long assignedUserId;
    /** 分配给的系统用户名 */
    private String assignedUserName;
    /** 聚合展示：同实例位容器总数 */
    private Integer containerCount;
    /** 聚合展示：同实例位运行中容器数 */
    private Integer runningCount;
    /** 聚合展示：同实例位已分配容器数 */
    private Integer assignedCount;
    /** 锁持有者，预留给任务互斥控制 */
    private String lockOwner;
    /** 锁过期时间，预留给任务互斥控制 */
    private Date lockExpireTime;
    /** 最近一次心跳时间，预留给 Agent 上报 */
    private Date lastHeartbeatTime;
    /** 魔云腾原始 JSON，以及 S5 代理等扩展状态缓存 */
    private String rawJson;
    /** 删除标志：0 正常，1 删除 */
    private String delFlag;

    public Long getContainerId() { return containerId; }
    public void setContainerId(Long containerId) { this.containerId = containerId; }
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public String getProviderContainerId() { return providerContainerId; }
    public void setProviderContainerId(String providerContainerId) { this.providerContainerId = providerContainerId; }
    public String getContainerName() { return containerName; }
    public void setContainerName(String containerName) { this.containerName = containerName; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public Integer getIndexNum() { return indexNum; }
    public void setIndexNum(Integer indexNum) { this.indexNum = indexNum; }
    public String getContainerStatus() { return containerStatus; }
    public void setContainerStatus(String containerStatus) { this.containerStatus = containerStatus; }
    public String getScheduleStatus() { return scheduleStatus; }
    public void setScheduleStatus(String scheduleStatus) { this.scheduleStatus = scheduleStatus; }
    public String getAndroidType() { return androidType; }
    public void setAndroidType(String androidType) { this.androidType = androidType; }
    public String getContainerIp() { return containerIp; }
    public void setContainerIp(String containerIp) { this.containerIp = containerIp; }
    public String getNetworkName() { return networkName; }
    public void setNetworkName(String networkName) { this.networkName = networkName; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getDpi() { return dpi; }
    public void setDpi(Integer dpi) { this.dpi = dpi; }
    public Integer getWebrtcTcpPort() { return webrtcTcpPort; }
    public void setWebrtcTcpPort(Integer webrtcTcpPort) { this.webrtcTcpPort = webrtcTcpPort; }
    public Integer getWebrtcUdpPort() { return webrtcUdpPort; }
    public void setWebrtcUdpPort(Integer webrtcUdpPort) { this.webrtcUdpPort = webrtcUdpPort; }
    public Integer getAdbPort() { return adbPort; }
    public void setAdbPort(Integer adbPort) { this.adbPort = adbPort; }
    public Integer getAndroidApiPort() { return androidApiPort; }
    public void setAndroidApiPort(Integer androidApiPort) { this.androidApiPort = androidApiPort; }
    public Integer getAndroidRpaPort() { return androidRpaPort; }
    public void setAndroidRpaPort(Integer androidRpaPort) { this.androidRpaPort = androidRpaPort; }
    public Integer getCameraTcpPort() { return cameraTcpPort; }
    public void setCameraTcpPort(Integer cameraTcpPort) { this.cameraTcpPort = cameraTcpPort; }
    public Integer getCameraUdpPort() { return cameraUdpPort; }
    public void setCameraUdpPort(Integer cameraUdpPort) { this.cameraUdpPort = cameraUdpPort; }
    public String getCloudMachineType() { return cloudMachineType; }
    public void setCloudMachineType(String cloudMachineType) { this.cloudMachineType = cloudMachineType; }
    public String getS5User() { return s5User; }
    public void setS5User(String s5User) { this.s5User = s5User; }
    public String getS5Password() { return s5Password; }
    public void setS5Password(String s5Password) { this.s5Password = s5Password; }
    public String getS5Ip() { return s5Ip; }
    public void setS5Ip(String s5Ip) { this.s5Ip = s5Ip; }
    public String getS5Port() { return s5Port; }
    public void setS5Port(String s5Port) { this.s5Port = s5Port; }
    public String getS5Type() { return s5Type; }
    public void setS5Type(String s5Type) { this.s5Type = s5Type; }
    public String getS5Status() { return s5Status; }
    public void setS5Status(String s5Status) { this.s5Status = s5Status; }
    public Long getCurrentTaskId() { return currentTaskId; }
    public void setCurrentTaskId(Long currentTaskId) { this.currentTaskId = currentTaskId; }
    public Long getBoundAccountId() { return boundAccountId; }
    public void setBoundAccountId(Long boundAccountId) { this.boundAccountId = boundAccountId; }
    public String getBoundAccountNo() { return boundAccountNo; }
    public void setBoundAccountNo(String boundAccountNo) { this.boundAccountNo = boundAccountNo; }
    public Long getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }
    public String getAssignedUserName() { return assignedUserName; }
    public void setAssignedUserName(String assignedUserName) { this.assignedUserName = assignedUserName; }
    public Integer getContainerCount() { return containerCount; }
    public void setContainerCount(Integer containerCount) { this.containerCount = containerCount; }
    public Integer getRunningCount() { return runningCount; }
    public void setRunningCount(Integer runningCount) { this.runningCount = runningCount; }
    public Integer getAssignedCount() { return assignedCount; }
    public void setAssignedCount(Integer assignedCount) { this.assignedCount = assignedCount; }
    public String getLockOwner() { return lockOwner; }
    public void setLockOwner(String lockOwner) { this.lockOwner = lockOwner; }
    public Date getLockExpireTime() { return lockExpireTime; }
    public void setLockExpireTime(Date lockExpireTime) { this.lockExpireTime = lockExpireTime; }
    public Date getLastHeartbeatTime() { return lastHeartbeatTime; }
    public void setLastHeartbeatTime(Date lastHeartbeatTime) { this.lastHeartbeatTime = lastHeartbeatTime; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
