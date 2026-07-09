package com.ruoyi.system.cloud;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudContainerProxyRequest;
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
            container.setAndroidApiPort(readHostPort(item, "9082/tcp"));
            container.setAndroidRpaPort(readHostPort(item, "9083/tcp"));
            container.setCameraTcpPort(readHostPort(item, "10006/tcp"));
            container.setCameraUdpPort(readHostPort(item, "10007/udp"));
            container.setWebrtcTcpPort(readHostPort(item, "10008/tcp"));
            container.setWebrtcUdpPort(readHostPort(item, "10008/udp"));
            container.setS5User(item.getString("s5User"));
            container.setS5Password(item.getString("s5Password"));
            container.setS5Ip(item.getString("s5IP"));
            container.setS5Port(item.getString("s5Port"));
            container.setS5Type(item.getString("s5Type"));
            fillPortsByIndex(container);
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

    @Override
    public void connectRpa(CloudHost host, String containerName)
    {
        try
        {
            command(host, "/rpa/connect", containerName);
        }
        catch (ServiceException e)
        {
            if (!isNotFound(e))
            {
                throw e;
            }
        }
    }

    @Override
    public void openApp(CloudHost host, String containerName, String packageName)
    {
        try
        {
            JSONObject body = new JSONObject();
            body.put("name", containerName);
            body.put("pkg", packageName);
            request(host, "POST", "/rpa/open_app", body.toJSONString());
        }
        catch (ServiceException e)
        {
            if (!isNotFound(e))
            {
                throw e;
            }
            androidExec(host, containerName,
                    "monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1");
        }
    }

    @Override
    public void shutDownApp(CloudHost host, String containerName, String packageName)
    {
        try
        {
            JSONObject body = new JSONObject();
            body.put("name", containerName);
            body.put("pkg", packageName);
            request(host, "POST", "/rpa/stop_app", body.toJSONString());
        }
        catch (ServiceException e)
        {
            if (!isNotFound(e))
            {
                throw e;
            }
            androidExec(host, containerName,
                    "monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1");
        }
    }

    @Override
    public void click(CloudHost host, String containerName, int x, int y)
    {
        try
        {
            JSONObject body = new JSONObject();
            body.put("name", containerName);
            body.put("x", x);
            body.put("y", y);
            request(host, "POST", "/rpa/click", body.toJSONString());
        }
        catch (ServiceException e)
        {
            if (!isNotFound(e))
            {
                throw e;
            }
            androidExec(host, containerName, "input tap " + x + " " + y);
        }
    }

    @Override
    public Map<String, Object> shell(CloudHost host, String containerName, String command, int timeoutSeconds)
    {
        try
        {
            JSONObject body = new JSONObject();
            body.put("name", containerName);
            body.put("cmd", command);
            body.put("timeout", timeoutSeconds);
            return request(host, "POST", "/rpa/shell", body.toJSONString());
        }
        catch (ServiceException e)
        {
            if (!isNotFound(e))
            {
                throw e;
            }
            return androidExec(host, containerName, command);
        }
    }

    @Override
    public void updateS5Proxy(CloudHost host, String containerName, CloudContainerProxyRequest request)
    {
        JSONObject body = new JSONObject();
        body.put("name", containerName);
        String s5Type = StringUtils.isBlank(request.getS5Type()) ? "1" : request.getS5Type();
        body.put("s5Type", s5Type);
        body.put("s5IP", StringUtils.defaultString(request.getS5Ip()));
        body.put("s5Port", StringUtils.defaultString(request.getS5Port()));
        body.put("s5User", StringUtils.defaultString(request.getS5User()));
        body.put("s5Password", StringUtils.defaultString(request.getS5Password()));
        request(host, "PUT", "/android", body.toJSONString());
    }

    @Override
    public MytAndroidContainer getAndroidByName(CloudHost host, String containerName)
    {
        List<MytAndroidContainer> containers = listAndroid(host);
        for (MytAndroidContainer item : containers)
        {
            if (containerName.equals(item.getName()))
            {
                return item;
            }
        }
        return null;
    }

    @Override
    public String uploadAndroidFile(CloudHost host, CloudContainer container, MultipartFile file, String remoteFileName)
    {
        String boundary = "----MytBoundary" + System.currentTimeMillis();
        try
        {
            byte[] payload = buildMultipartPayload(boundary, "file", remoteFileName, file);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(androidApiBaseUrl(host, container) + "/upload"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("安卓API文件上传失败: HTTP " + response.statusCode() + " "
                        + StringUtils.defaultString(response.body()));
            }
            String remotePath = "/sdcard/upload/" + remoteFileName;
            log(host, "/android-api/upload", "POST", remoteFileName, response.body(), response.statusCode(), true, null,
                    System.currentTimeMillis());
            return remotePath;
        }
        catch (Exception e)
        {
            if (e instanceof ServiceException)
            {
                throw (ServiceException) e;
            }
            throw new ServiceException("安卓API文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void setVirtualCameraSource(CloudHost host, CloudContainer container, String type, String path, Integer resolution)
    {
        String query = "/modifydev?cmd=4&type=" + encode(type) + "&path=" + encode(path);
        if (resolution != null)
        {
            query += "&resolution=" + resolution;
        }
        androidApiGet(host, container, query);
    }

    @Override
    public void startVirtualCamera(CloudHost host, CloudContainer container, String path)
    {
        androidApiGet(host, container, "/camera?cmd=start&path=" + encode(path));
    }

    private void command(CloudHost host, String path, String containerName)
    {
        JSONObject body = new JSONObject();
        body.put("name", containerName);
        request(host, "POST", path, body.toJSONString());
    }

    private JSONObject androidExec(CloudHost host, String containerName, String command)
    {
        JSONObject body = new JSONObject();
        JSONArray commands = new JSONArray();
        commands.add("sh");
        commands.add("-c");
        commands.add(command);
        body.put("name", containerName);
        body.put("command", commands);
        return request(host, "POST", "/android/exec", body.toJSONString());
    }

    private JSONObject androidApiGet(CloudHost host, CloudContainer container, String path)
    {
        long start = System.currentTimeMillis();
        String responseBody = null;
        Integer resultCode = null;
        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(androidApiBaseUrl(host, container) + path))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("安卓API接口不可用: " + path + " HTTP " + response.statusCode()
                        + " " + StringUtils.defaultString(responseBody));
            }
            JSONObject json = JSONObject.parseObject(responseBody);
            resultCode = json.getInteger("code");
            if (resultCode == null || resultCode != 200)
            {
                throw new ServiceException(firstMessage(json));
            }
            log(host, "/android-api" + path, "GET", null, responseBody, resultCode, true, null, start);
            return json;
        }
        catch (Exception e)
        {
            log(host, "/android-api" + path, "GET", null, responseBody, resultCode, false, e.getMessage(), start);
            if (e instanceof ServiceException)
            {
                throw (ServiceException) e;
            }
            throw new ServiceException("调用安卓API失败: " + e.getMessage());
        }
    }

    private String androidApiBaseUrl(CloudHost host, CloudContainer container)
    {
        String ip = host.getHostIp();
        if (StringUtils.isBlank(ip))
        {
            ip = container.getContainerIp();
        }
        Integer port = container.getAndroidApiPort();
        if (port == null && container.getIndexNum() != null)
        {
            port = 30000 + (container.getIndexNum() - 1) * 100 + 1;
        }
        if (port == null)
        {
            port = 9082;
        }
        return "http://" + ip + ":" + port;
    }

    private byte[] buildMultipartPayload(String boundary, String fieldName, String filename, MultipartFile file) throws Exception
    {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] fileBytes = file.getBytes();
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, payload, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, payload, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, payload, headerBytes.length + fileBytes.length, footerBytes.length);
        return payload;
    }

    private String firstMessage(JSONObject json)
    {
        String message = json.getString("msg");
        if (StringUtils.isNotBlank(message))
        {
            return message;
        }
        message = json.getString("reason");
        if (StringUtils.isNotBlank(message))
        {
            return message;
        }
        message = json.getString("error");
        return StringUtils.isBlank(message) ? json.toJSONString() : message;
    }

    private String encode(String value)
    {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
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
            if ("POST".equals(method) || "PUT".equals(method))
            {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            }
            else
            {
                builder.GET();
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new ServiceException("魔云腾SDK接口不可用: " + path + " HTTP " + response.statusCode()
                        + " " + StringUtils.defaultString(responseBody));
            }
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

    private boolean isNotFound(ServiceException e)
    {
        String message = e.getMessage();
        return message != null && (message.contains("HTTP 404") || message.contains("Not Found"));
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

    private void fillPortsByIndex(MytAndroidContainer container)
    {
        if (container.getIndexNum() == null)
        {
            return;
        }
        int base = 30000 + (container.getIndexNum() - 1) * 100;
        if (container.getAdbPort() == null)
        {
            container.setAdbPort(base);
        }
        if (container.getAndroidApiPort() == null)
        {
            container.setAndroidApiPort(base + 1);
        }
        if (container.getAndroidRpaPort() == null)
        {
            container.setAndroidRpaPort(base + 2);
        }
        if (container.getCameraTcpPort() == null)
        {
            container.setCameraTcpPort(base + 5);
        }
        if (container.getCameraUdpPort() == null)
        {
            container.setCameraUdpPort(base + 6);
        }
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
