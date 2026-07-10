package com.ruoyi.system.cloud;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudContainerProxyRequest;
import com.ruoyi.system.domain.CloudHost;

public interface MoyuntengSdkClient
{
    Map<String, Object> getInfo(CloudHost host);

    List<MytAndroidContainer> listAndroid(CloudHost host);

    void startAndroid(CloudHost host, String containerName);

    void stopAndroid(CloudHost host, String containerName);

    void restartAndroid(CloudHost host, String containerName);

    void connectRpa(CloudHost host, String containerName);

    void openApp(CloudHost host, String containerName, String packageName);

    void shutDownApp(CloudHost host, String containerName, String packageName);

    void click(CloudHost host, String containerName, int x, int y);

    Map<String, Object> shell(CloudHost host, String containerName, String command, int timeoutSeconds);

    Map<String, Object> updateS5Proxy(CloudHost host, CloudContainer container, CloudContainerProxyRequest request);

    Map<String, Object> queryS5Proxy(CloudHost host, CloudContainer container);

    MytAndroidContainer getAndroidByName(CloudHost host, String containerName);

    String uploadAndroidFile(CloudHost host, CloudContainer container, MultipartFile file, String remoteFileName);

    void setVirtualCameraSource(CloudHost host, CloudContainer container, String type, String path, Integer resolution);

    void startVirtualCamera(CloudHost host, CloudContainer container, String path);
}
