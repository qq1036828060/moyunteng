package com.ruoyi.system.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 魔云腾物理主机/盒子信息。
 * 用于维护云机提供商侧接入的主机地址、SDK 地址、在线状态和容器容量。
 */
public class CloudHost extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主机主键ID */
    private Long hostId;
    /** 主机名称，后台展示和运维识别使用 */
    private String hostName;
    /** 主机IP，Android API/WebRTC 默认使用该地址拼接访问入口 */
    private String hostIp;
    /** 魔云腾盒子 SDK API 端口，默认 8000 */
    private Integer apiPort;
    /** 魔云腾盒子 SDK API 基础地址，为空时按 hostIp + apiPort 自动生成 */
    private String apiBaseUrl;
    /** 魔云腾设备ID */
    private String deviceId;
    /** 主机型号 */
    private String model;
    /** 当前 SDK/系统版本 */
    private Integer currentVersion;
    /** 可升级到的最新版本 */
    private Integer latestVersion;
    /** 在线状态：ONLINE/OFFLINE/UNKNOWN */
    private String onlineStatus;
    /** 是否启用：1 启用，0 停用 */
    private String enabled;
    /** 当前同步到的容器数量 */
    private Integer containerCapacity;
    /** 最近一次连接测试或容器同步时间 */
    private Date lastSyncTime;
    /** 删除标志：0 正常，1 删除 */
    private String delFlag;

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getHostIp() { return hostIp; }
    public void setHostIp(String hostIp) { this.hostIp = hostIp; }
    public Integer getApiPort() { return apiPort; }
    public void setApiPort(Integer apiPort) { this.apiPort = apiPort; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
    public Integer getLatestVersion() { return latestVersion; }
    public void setLatestVersion(Integer latestVersion) { this.latestVersion = latestVersion; }
    public String getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(String onlineStatus) { this.onlineStatus = onlineStatus; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public Integer getContainerCapacity() { return containerCapacity; }
    public void setContainerCapacity(Integer containerCapacity) { this.containerCapacity = containerCapacity; }
    public Date getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(Date lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
