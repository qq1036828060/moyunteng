package com.ruoyi.system.cloud;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudContainerProxyRequest;
import com.ruoyi.system.domain.CloudHost;

/**
 * 魔云腾主机 SDK 与 Android API 的统一客户端。
 * 上层业务只依赖本接口，不直接拼接第三方请求。
 */
public interface MoyuntengSdkClient
{
    /** 查询主机基础信息和版本信息。 */
    Map<String, Object> getInfo(CloudHost host);

    /** 查询主机下所有安卓容器/模拟器实例。 */
    List<MytAndroidContainer> listAndroid(CloudHost host);

    /** 启动指定容器。 */
    void startAndroid(CloudHost host, String containerName);

    /** 停止指定容器。 */
    void stopAndroid(CloudHost host, String containerName);

    /** 重启指定容器。 */
    void restartAndroid(CloudHost host, String containerName);

    /** 建立 RPA 控制连接，旧版本 SDK 不支持时允许降级。 */
    void connectRpa(CloudHost host, String containerName);

    /** 打开指定包名的 Android 应用。 */
    void openApp(CloudHost host, String containerName, String packageName);

    /** 关闭指定包名的 Android 应用。 */
    void shutDownApp(CloudHost host, String containerName, String packageName);

    /** 在云机屏幕上执行点击。 */
    void click(CloudHost host, String containerName, int x, int y);

    /** 在云机内执行 shell 命令。 */
    Map<String, Object> shell(CloudHost host, String containerName, String command, int timeoutSeconds);

    /** 通过 Android API 设置或关闭 S5 代理，并返回最新代理状态。 */
    Map<String, Object> updateS5Proxy(CloudHost host, CloudContainer container, CloudContainerProxyRequest request);

    /** 通过 Android API 查询 S5 代理状态。 */
    Map<String, Object> queryS5Proxy(CloudHost host, CloudContainer container);

    /** 按容器名称从魔云腾列表中查找单个安卓实例。 */
    MytAndroidContainer getAndroidByName(CloudHost host, String containerName);

    /** 上传文件到指定安卓实例，用于扫码虚拟摄像头等本地资源场景。 */
    String uploadAndroidFile(CloudHost host, CloudContainer container, MultipartFile file, String remoteFileName);

    /** 设置虚拟摄像头资源来源。 */
    void setVirtualCameraSource(CloudHost host, CloudContainer container, String type, String path, Integer resolution);

    /** 启动虚拟摄像头。 */
    void startVirtualCamera(CloudHost host, CloudContainer container, String path);
}
