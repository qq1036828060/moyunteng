package com.ruoyi.system.domain;

/**
 * 手机银行卡支付命令。
 */
public class CloudBankPayRequest
{
    /** 可选云机ID，用户只有一个实例位时可以不传。 */
    private Long containerId;

    public Long getContainerId()
    {
        return containerId;
    }

    public void setContainerId(Long containerId)
    {
        this.containerId = containerId;
    }

}
