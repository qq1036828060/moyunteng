package com.ruoyi.system.domain;

public class CloudContainerAssignRequest
{
    private Long hostId;
    private Integer[] indexNums;
    private Long userId;
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
