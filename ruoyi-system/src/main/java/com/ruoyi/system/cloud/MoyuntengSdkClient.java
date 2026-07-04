package com.ruoyi.system.cloud;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.CloudHost;

public interface MoyuntengSdkClient
{
    Map<String, Object> getInfo(CloudHost host);

    List<MytAndroidContainer> listAndroid(CloudHost host);

    void startAndroid(CloudHost host, String containerName);

    void stopAndroid(CloudHost host, String containerName);

    void restartAndroid(CloudHost host, String containerName);
}

