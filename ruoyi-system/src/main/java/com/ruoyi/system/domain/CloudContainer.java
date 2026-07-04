package com.ruoyi.system.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

public class CloudContainer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long containerId;
    private Long hostId;
    private String providerContainerId;
    private String containerName;
    private String instanceId;
    private Integer indexNum;
    private String containerStatus;
    private String scheduleStatus;
    private String androidType;
    private String containerIp;
    private String networkName;
    private String image;
    private Integer width;
    private Integer height;
    private Integer dpi;
    private Integer webrtcTcpPort;
    private Integer webrtcUdpPort;
    private Integer adbPort;
    private Long currentTaskId;
    private Long boundAccountId;
    private String lockOwner;
    private Date lockExpireTime;
    private Date lastHeartbeatTime;
    private String rawJson;
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
    public Long getCurrentTaskId() { return currentTaskId; }
    public void setCurrentTaskId(Long currentTaskId) { this.currentTaskId = currentTaskId; }
    public Long getBoundAccountId() { return boundAccountId; }
    public void setBoundAccountId(Long boundAccountId) { this.boundAccountId = boundAccountId; }
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

