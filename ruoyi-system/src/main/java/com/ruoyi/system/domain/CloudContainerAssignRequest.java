package com.ruoyi.system.domain;

/**
 * 主端按主机实例位批量分配/取消分配云机的请求参数。
 */
public class CloudContainerAssignRequest
{
    /** 主机ID */
    private Long hostId;
    /** 实例位序号数组，同一实例位可能包含多个容器云机 */
    private Integer[] indexNums;
    /** 被分配的系统用户ID */
    private Long userId;
    /** 可选的业务账号编号，当前主端分配不主动绑定账号 */
    private String boundAccountNo;

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public Integer[] getIndexNums() { return indexNums; }
    public void setIndexNums(Integer[] indexNums) { this.indexNums = indexNums; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBoundAccountNo() { return boundAccountNo; }
    public void setBoundAccountNo(String boundAccountNo) { this.boundAccountNo = boundAccountNo; }
}
