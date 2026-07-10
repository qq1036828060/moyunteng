package com.ruoyi.system.cloud;

/**
 * 魔云腾 /android 接口返回的安卓实例信息。
 * 该对象只承载第三方接口结果，入库前会转换为 CloudContainer。
 */
public class MytAndroidContainer
{
    /** 魔云腾容器ID */
    private String id;
    /** 魔云腾容器名称 */
    private String name;
    /** 运行状态，例如 running/stopped */
    private String status;
    /** 安卓类型：V2 容器云机，V3 模拟器云机 */
    private String androidType;
    /** 实例位序号 */
    private Integer indexNum;
    /** 容器内部IP */
    private String ip;
    /** 网络名称 */
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
    /** Android API 端口 */
    private Integer androidApiPort;
    /** Android RPA 端口 */
    private Integer androidRpaPort;
    /** 摄像头 TCP 端口 */
    private Integer cameraTcpPort;
    /** 摄像头 UDP 端口 */
    private Integer cameraUdpPort;
    /** S5 代理用户名 */
    private String s5User;
    /** S5 代理密码 */
    private String s5Password;
    /** S5 代理IP */
    private String s5Ip;
    /** S5 代理端口 */
    private String s5Port;
    /** S5 代理类型 */
    private String s5Type;
    /** 魔云腾原始 JSON */
    private String rawJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAndroidType() { return androidType; }
    public void setAndroidType(String androidType) { this.androidType = androidType; }
    public Integer getIndexNum() { return indexNum; }
    public void setIndexNum(Integer indexNum) { this.indexNum = indexNum; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
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
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
