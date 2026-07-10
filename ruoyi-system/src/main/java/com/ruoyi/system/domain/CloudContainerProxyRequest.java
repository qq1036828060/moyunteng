package com.ruoyi.system.domain;

import java.io.Serializable;

/**
 * 用户端设置 S5 代理的请求参数。
 * s5Type 为 0 时表示关闭代理，其余类型按魔云腾 Android API 透传。
 */
public class CloudContainerProxyRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** S5 代理IP */
    private String s5Ip;
    /** S5 代理端口 */
    private String s5Port;
    /** S5 代理用户名 */
    private String s5User;
    /** S5 代理密码 */
    private String s5Password;
    /** S5 代理类型，0 表示关闭 */
    private String s5Type;

    public String getS5Ip() { return s5Ip; }
    public void setS5Ip(String s5Ip) { this.s5Ip = s5Ip; }
    public String getS5Port() { return s5Port; }
    public void setS5Port(String s5Port) { this.s5Port = s5Port; }
    public String getS5User() { return s5User; }
    public void setS5User(String s5User) { this.s5User = s5User; }
    public String getS5Password() { return s5Password; }
    public void setS5Password(String s5Password) { this.s5Password = s5Password; }
    public String getS5Type() { return s5Type; }
    public void setS5Type(String s5Type) { this.s5Type = s5Type; }
}
