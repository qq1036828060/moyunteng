package com.ruoyi.system.cloud;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.CloudHost;
import com.ruoyi.system.domain.CloudSdkCallLog;
import com.ruoyi.system.mapper.CloudSdkCallLogMapper;

@Component
public class MoyuntengSdkClientImpl implements MoyuntengSdkClient
{
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Autowired
    private CloudSdkCallLogMapper sdkCallLogMapper;

    @Override
    public Map<String, Object> getInfo(CloudHost host)
    {
        JSONObject data = request(host, "GET", "/info", null);
        Map<String, Object> result = new HashMap<>();
        result.put("latestVersion", data.getInteger("latestVersion"));
        result.put("currentVersion", data.getInteger("currentVersion"));
        return result;
    }

    @Override
    public List<MytAndroidContainer> listAndroid(CloudHost host)
    {
        JSONObject data = request(host, "GET", "/android", null);
        JSONArray list = data.getJSONArray("list");
        List<MytAndroidContainer> result = new ArrayList<>();
        if (list == null)
        {
            return result;
        }
        for (int i = 0; i < list.size(); i++)
        {
            JSONObject item = list.getJSONObject(i);
            MytAndroidContainer container = new MytAndroidContainer();
            container.setId(item.getString("id"));
            container.setName(item.getString("name"));
            container.setStatus(item.getString("status"));
            container.setAndroidType(item.getString("androidType"));
            container.setIndexNum(item.getInteger("indexNum"));
            container.setIp(item.getString("ip"));
            container.setNetworkName(item.getString("networkName"));
            container.setImage(item.getString("image"));
            container.setWidth(item.getInteger("doboxWidth"));
            container.setHeight(item.getInteger("doboxHeight"));
            container.setDpi(item.getInteger("doboxDpi"));
            container.setAdbPort(readHostPort(item, "5555/tcp"));
            container.setWebrtcTcpPort(readHostPort(item, "10008/tcp"));
            container.setWebrtcUdpPort(readHostPort(item, "10008/udp"));
            fillWebrtcPortByIndex(container);
            container.setRawJson(item.toJSONString());
            result.add(container);
        }
        return result;
    }

    @Override
    public void startAndroid(CloudHost host, String containerName)
    {
        command(host, "/android/start", containerName);
    }

    @Override
    public void stopAndroid(CloudHost host, String containerName)
    {
        command(host, "/android/stop", containerName);
    }

    @Override
    public void restartAndroid(CloudHost host, String containerName)
    {
        command(host, "/android/restart", containerName);
    }

    private void command(CloudHost host, String path, String containerName)
    {
        JSONObject body = new JSONObject();
        body.put("name", containerName);
        request(host, "POST", path, body.toJSONString());
    }

    private JSONObject request(CloudHost host, String method, String path, String body)
    {
        long start = System.currentTimeMillis();
        String responseBody = null;
        Integer resultCode = null;
        try
        {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl(host) + path))
                    .timeout(Duration.ofSeconds(10));
            if ("POST".equals(method))
            {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            }
            else
            {
                builder.GET();
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();
            JSONObject json = JSONObject.parseObject(responseBody);
            resultCode = json.getInteger("code");
            if (resultCode == null || resultCode != 0)
            {
                throw new ServiceException(json.getString("message"));
            }
            log(host, path, method, body, responseBody, resultCode, true, null, start);
            JSONObject data = json.getJSONObject("data");
            return data == null ? new JSONObject() : data;
        }
        catch (Exception e)
        {
            log(host, path, method, body, responseBody, resultCode, false, e.getMessage(), start);
            if (e instanceof ServiceException)
            {
                throw (ServiceException) e;
            }
            throw new ServiceException("调用魔云腾SDK失败: " + e.getMessage());
        }
    }

    private String baseUrl(CloudHost host)
    {
        if (StringUtils.isNotEmpty(host.getApiBaseUrl()))
        {
            return host.getApiBaseUrl();
        }
        Integer port = host.getApiPort() == null ? 8000 : host.getApiPort();
        return "http://" + host.getHostIp() + ":" + port;
    }

    private Integer readHostPort(JSONObject item, String key)
    {
        JSONObject portBindings = item.getJSONObject("portBindings");
        if (portBindings == null)
        {
            return null;
        }
        JSONArray bindings = portBindings.getJSONArray(key);
        if (bindings == null || bindings.isEmpty())
        {
            return null;
        }
        return bindings.getJSONObject(0).getInteger("HostPort");
    }

    private void fillWebrtcPortByIndex(MytAndroidContainer container)
    {
        if (container.getIndexNum() == null)
        {
            return;
        }
        int base = 30000 + (container.getIndexNum() - 1) * 100;
        if (container.getWebrtcTcpPort() == null)
        {
            container.setWebrtcTcpPort(base + 7);
        }
        if (container.getWebrtcUdpPort() == null)
        {
            container.setWebrtcUdpPort(base + 8);
        }
    }

    private void log(CloudHost host, String path, String method, String requestBody, String responseBody,
            Integer resultCode, boolean success, String errorMsg, long start)
    {
        CloudSdkCallLog log = new CloudSdkCallLog();
        log.setHostId(host == null ? null : host.getHostId());
        log.setApiPath(path);
        log.setHttpMethod(method);
        log.setRequestBody(requestBody);
        log.setResponseBody(responseBody);
        log.setResultCode(resultCode);
        log.setSuccess(success ? "1" : "0");
        log.setErrorMsg(errorMsg);
        log.setCostMs((int) (System.currentTimeMillis() - start));
        sdkCallLogMapper.insertCloudSdkCallLog(log);
    }
}

