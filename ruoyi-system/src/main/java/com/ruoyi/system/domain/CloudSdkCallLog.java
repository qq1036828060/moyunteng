package com.ruoyi.system.domain;

import java.util.Date;

/**
 * 魔云腾 SDK / Android API 调用日志。
 * 用于排查接口请求、响应、耗时和失败原因。
 */
public class CloudSdkCallLog
{
    /** 日志主键ID */
    private Long logId;
    /** 调用所属主机ID */
    private Long hostId;
    /** 预留任务ID，后续任务调度接入后写入 */
    private Long taskId;
    /** 调用所属容器ID */
    private Long containerId;
    /** API 路径 */
    private String apiPath;
    /** HTTP 方法 */
    private String httpMethod;
    /** 请求体或关键请求参数 */
    private String requestBody;
    /** 响应体 */
    private String responseBody;
    /** 业务返回码 */
    private Integer resultCode;
    /** 是否成功：1 成功，0 失败 */
    private String success;
    /** 失败原因 */
    private String errorMsg;
    /** 调用耗时，单位毫秒 */
    private Integer costMs;
    /** 创建时间 */
    private Date createTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getContainerId() { return containerId; }
    public void setContainerId(Long containerId) { this.containerId = containerId; }
    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public Integer getResultCode() { return resultCode; }
    public void setResultCode(Integer resultCode) { this.resultCode = resultCode; }
    public String getSuccess() { return success; }
    public void setSuccess(String success) { this.success = success; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Integer getCostMs() { return costMs; }
    public void setCostMs(Integer costMs) { this.costMs = costMs; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
