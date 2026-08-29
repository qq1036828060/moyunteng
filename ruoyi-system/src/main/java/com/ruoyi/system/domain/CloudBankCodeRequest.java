package com.ruoyi.system.domain;

/**
 * 手机银行卡支付短信验证码命令。
 */
public class CloudBankCodeRequest
{
    /** 银行卡支付第一阶段返回的任务ID。 */
    private String taskId;

    /** 银行短信验证码。 */
    private String code;

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }
}
