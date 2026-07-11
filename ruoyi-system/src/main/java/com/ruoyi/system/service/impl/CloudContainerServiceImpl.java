package com.ruoyi.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.system.cloud.MoyuntengSdkClient;
import com.ruoyi.system.cloud.MytAndroidContainer;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudContainerAssignRequest;
import com.ruoyi.system.domain.CloudContainerProxyRequest;
import com.ruoyi.system.domain.CloudHost;
import com.ruoyi.system.mapper.CloudContainerMapper;
import com.ruoyi.system.mapper.CloudHostMapper;
import com.ruoyi.system.service.ICloudContainerService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 云机容器资源池和用户端云机操作服务实现。
 */
@Service
public class CloudContainerServiceImpl implements ICloudContainerService
{
    private static final String QQ_PACKAGE_NAME = "com.tencent.mobileqq";

    @Autowired
    private CloudContainerMapper cloudContainerMapper;

    @Autowired
    private CloudHostMapper cloudHostMapper;

    @Autowired
    private MoyuntengSdkClient moyuntengSdkClient;

    @Autowired
    private ISysUserService userService;

    /** 查询容器明细列表，并补齐展示字段。 */
    @Override
    public List<CloudContainer> selectCloudContainerList(CloudContainer container)
    {
        List<CloudContainer> list = cloudContainerMapper.selectCloudContainerList(container);
        enrichContainers(list);
        return list;
    }

    /** 查询按实例位聚合的资源池列表，并补齐展示字段。 */
    @Override
    public List<CloudContainer> selectCloudContainerSlotList(CloudContainer container)
    {
        List<CloudContainer> list = cloudContainerMapper.selectCloudContainerSlotList(container);
        enrichContainers(list);
        return list;
    }

    /** 查询单个容器详情，并补齐端口、类型、代理状态等派生字段。 */
    @Override
    public CloudContainer selectCloudContainerById(Long containerId)
    {
        return enrichContainer(cloudContainerMapper.selectCloudContainerById(containerId));
    }

    /** 查询当前用户被分配到的云机列表。 */
    @Override
    public List<CloudContainer> selectMyCloudContainerList(Long userId, CloudContainer container)
    {
        container.setAssignedUserId(userId);
        List<CloudContainer> list = cloudContainerMapper.selectCloudContainerList(container);
        enrichContainers(list);
        return list;
    }

    /** 汇总当前用户云机数量、运行中数量和空闲数量。 */
    @Override
    public Map<String, Object> selectMyCloudContainerSummary(Long userId)
    {
        CloudContainer query = new CloudContainer();
        query.setAssignedUserId(userId);
        List<CloudContainer> containers = cloudContainerMapper.selectCloudContainerList(query);
        int runningCount = 0;
        int idleCount = 0;
        for (CloudContainer container : containers)
        {
            if ("running".equalsIgnoreCase(container.getContainerStatus()))
            {
                runningCount++;
            }
            if ("IDLE".equalsIgnoreCase(container.getScheduleStatus()))
            {
                idleCount++;
            }
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("total", containers.size());
        summary.put("runningCount", runningCount);
        summary.put("idleCount", idleCount);
        summary.put("busyCount", containers.size() - idleCount);
        return summary;
    }

    /** 进入控制前准备云机状态，并生成 WebRTC 播放地址。 */
    @Override
    public Map<String, Object> selectMyContainerWebrtcInfo(Long userId, Long containerId)
    {
        CloudContainer container = requireOwnedContainer(userId, containerId);
        CloudHost host = requireHost(container.getHostId());
        String controlMessage = prepareContainerForControl(host, container);
        container = requireOwnedContainer(userId, containerId);
        if (!"running".equalsIgnoreCase(container.getContainerStatus()))
        {
            throw new ServiceException("当前云机启动失败，请同步容器状态后重试");
        }

        Integer tcpPort = container.getWebrtcTcpPort();
        Integer udpPort = container.getWebrtcUdpPort();
        if ((tcpPort == null || udpPort == null) && container.getIndexNum() != null)
        {
            int base = 30000 + (container.getIndexNum() - 1) * 100;
            if (tcpPort == null)
            {
                tcpPort = base + 7;
            }
            if (udpPort == null)
            {
                udpPort = base + 8;
            }
        }
        if (StringUtils.isBlank(host.getHostIp()) || tcpPort == null || udpPort == null)
        {
            throw new ServiceException("云机 WebRTC 地址不完整，请先同步容器状态");
        }

        String hostIp = host.getHostIp();
        String query = "shost=" + encode(hostIp)
                + "&sport=" + tcpPort
                + "&q=1&v=h264"
                + "&rtc_i=" + encode(hostIp)
                + "&rtc_p=" + udpPort;
        String playPath = "/webplayer/play-myt.html?" + query;
        Map<String, Object> info = new HashMap<>();
        info.put("containerId", container.getContainerId());
        info.put("containerName", container.getContainerName());
        info.put("containerStatus", container.getContainerStatus());
        info.put("hostId", host.getHostId());
        info.put("hostIp", hostIp);
        info.put("indexNum", container.getIndexNum());
        info.put("tcpPort", tcpPort);
        info.put("udpPort", udpPort);
        info.put("playUrl", playPath);
        info.put("playPath", playPath);
        info.put("query", query);
        info.put("controlMessage", controlMessage);
        return info;
    }

    /** 保证同一主机同一实例位只有目标云机处于运行状态，避免控制串流冲突。 */
    private String prepareContainerForControl(CloudHost host, CloudContainer target)
    {
        if (target.getIndexNum() == null)
        {
            throw new ServiceException("云机实例位为空，无法进入控制");
        }

        refreshHostContainersFromSdk(host);
        target = requireContainer(target.getContainerId());
        List<CloudContainer> slotContainers = selectSlotContainers(target);
        if (slotContainers.isEmpty())
        {
            throw new ServiceException("当前实例位没有可控制的云机");
        }

        boolean targetRunning = false;
        List<CloudContainer> runningOthers = new java.util.ArrayList<>();
        for (CloudContainer item : slotContainers)
        {
            if (!"running".equalsIgnoreCase(item.getContainerStatus()))
            {
                continue;
            }
            if (item.getContainerId().equals(target.getContainerId()))
            {
                targetRunning = true;
            }
            else
            {
                runningOthers.add(item);
            }
        }

        if (targetRunning && runningOthers.isEmpty())
        {
            return "当前云机已启动，可直接进入控制";
        }

        for (CloudContainer item : runningOthers)
        {
            moyuntengSdkClient.stopAndroid(host, item.getContainerName());
        }
        if (!runningOthers.isEmpty())
        {
            sleepAfterCommand();
            refreshHostContainersFromSdk(host);
        }

        if (!targetRunning)
        {
            moyuntengSdkClient.startAndroid(host, target.getContainerName());
            sleepAfterCommand();
            refreshHostContainersFromSdk(host);
        }

        if (!runningOthers.isEmpty() && !targetRunning)
        {
            return "已关闭同实例位其他运行云机，并启动当前云机";
        }
        if (!runningOthers.isEmpty())
        {
            return "已关闭同实例位其他运行云机，当前云机保持运行";
        }
        return "当前实例位暂无运行云机，已启动当前云机";
    }

    /** 查询目标云机所在实例位下的全部容器。 */
    private List<CloudContainer> selectSlotContainers(CloudContainer container)
    {
        CloudContainer query = new CloudContainer();
        query.setHostId(container.getHostId());
        query.setIndexNum(container.getIndexNum());
        return cloudContainerMapper.selectCloudContainerByHostIndex(query);
    }

    /** 按主机实例位批量分配云机给指定用户。 */
    @Override
    public int assignContainerByHostIndex(CloudContainerAssignRequest request, String operator)
    {
        if (request.getHostId() == null)
        {
            throw new ServiceException("主机ID不能为空");
        }
        if (request.getUserId() == null)
        {
            throw new ServiceException("分配用户不能为空");
        }
        if (request.getIndexNums() == null || request.getIndexNums().length == 0)
        {
            throw new ServiceException("实例位不能为空");
        }
        SysUser user = userService.selectUserById(request.getUserId());
        if (user == null)
        {
            throw new ServiceException("分配用户不存在");
        }

        int count = 0;
        for (Integer indexNum : request.getIndexNums())
        {
            if (indexNum == null)
            {
                continue;
            }
            CloudContainer assign = new CloudContainer();
            assign.setHostId(request.getHostId());
            assign.setIndexNum(indexNum);
            assign.setAssignedUserId(user.getUserId());
            assign.setAssignedUserName(user.getUserName());
            assign.setUpdateBy(operator);
            int updated = cloudContainerMapper.assignCloudContainerSlot(assign);
            if (updated == 0)
            {
                throw new ServiceException("主机 " + request.getHostId() + " 的实例位 " + indexNum + " 不存在");
            }
            count += updated;
        }
        return count;
    }

    /** 取消单个容器的用户分配关系。 */
    @Override
    public int unassignContainer(Long containerId)
    {
        requireContainer(containerId);
        return cloudContainerMapper.unassignCloudContainer(containerId);
    }

    /** 按主机实例位批量取消分配。 */
    @Override
    public int unassignContainerByHostIndex(CloudContainerAssignRequest request)
    {
        if (request.getHostId() == null)
        {
            throw new ServiceException("主机ID不能为空");
        }
        if (request.getIndexNums() == null || request.getIndexNums().length == 0)
        {
            throw new ServiceException("实例位不能为空");
        }
        int count = 0;
        for (Integer indexNum : request.getIndexNums())
        {
            if (indexNum == null)
            {
                continue;
            }
            CloudContainer unassign = new CloudContainer();
            unassign.setHostId(request.getHostId());
            unassign.setIndexNum(indexNum);
            count += cloudContainerMapper.unassignCloudContainerSlot(unassign);
        }
        return count;
    }

    /** 用户端绑定业务账号到自己的云机。 */
    @Override
    public int bindMyContainerAccount(Long userId, Long containerId, String boundAccountNo)
    {
        CloudContainer container = requireOwnedContainer(userId, containerId);
        CloudContainer update = new CloudContainer();
        update.setContainerId(container.getContainerId());
        update.setAssignedUserId(userId);
        update.setBoundAccountNo(StringUtils.trimToNull(boundAccountNo));
        return cloudContainerMapper.bindCloudContainerAccount(update);
    }

    /** 准备用户云机控制环境并打开 QQ。 */
    @Override
    public Map<String, Object> openMyContainerQq(Long userId, Long containerId)
    {
        CloudContainer container = requireOwnedContainer(userId, containerId);
        CloudHost host = requireHost(container.getHostId());
        String controlMessage = prepareContainerForControl(host, container);
        container = requireOwnedContainer(userId, containerId);
        openQq(host, container);

        Map<String, Object> result = new HashMap<>();
        result.put("containerId", container.getContainerId());
        result.put("containerName", container.getContainerName());
        result.put("controlMessage", controlMessage);
        result.put("message", "QQ open command sent");
        return result;
    }

    /** 上传二维码、设置虚拟摄像头并触发 QQ 扫一扫流程。 */
    @Override
    public Map<String, Object> scanMyContainerQq(Long userId, Long containerId, MultipartFile file, String filePath) throws Exception
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("Please upload a QR code image");
        }
        String qrText = decodeQrText(file);
        CloudContainer container = requireOwnedContainer(userId, containerId);
        CloudHost host = requireHost(container.getHostId());

        String controlMessage = prepareContainerForControl(host, container);
        container = requireOwnedContainer(userId, containerId);
        String localFilePath = FileUploadUtils.upload(RuoYiConfig.getUploadPath() + "/cloud-qq-scan", file,
                MimeTypeUtils.IMAGE_EXTENSION);
        String sourceFilePath = StringUtils.trimToNull(filePath);
        String remoteFileName = buildRemoteQrFileName(container, file);
        String cameraPath = moyuntengSdkClient.uploadAndroidFile(host, container, file, remoteFileName);
        moyuntengSdkClient.setVirtualCameraSource(host, container, "image", cameraPath, 1);
        moyuntengSdkClient.startVirtualCamera(host, container, cameraPath);
        openQq(host, container);
        clickQqScanEntry(host, container);

        Map<String, Object> result = new HashMap<>();
        result.put("containerId", container.getContainerId());
        result.put("containerName", container.getContainerName());
        result.put("filePath", localFilePath);
        result.put("sourceFilePath", sourceFilePath);
        result.put("cameraPath", cameraPath);
        result.put("remotePath", cameraPath);
        result.put("androidApiPort", container.getAndroidApiPort());
        result.put("cameraTcpPort", container.getCameraTcpPort());
        result.put("cameraUdpPort", container.getCameraUdpPort());
        result.put("qrText", qrText);
        result.put("controlMessage", controlMessage);
        result.put("message", StringUtils.isBlank(qrText) ? "QQ scan entry command sent"
                : "QQ scan entry command sent, QR content parsed");
        return result;
    }

    /** 设置或关闭当前用户云机的 S5 代理，并保存查询后的代理状态。 */
    @Override
    public CloudContainer setMyContainerS5Proxy(Long userId, Long containerId, CloudContainerProxyRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("S5代理参数不能为空");
        }
        CloudContainer container = requireOwnedContainer(userId, containerId);
        CloudHost host = requireHost(container.getHostId());
        normalizeS5ProxyRequest(request);
        Map<String, Object> proxyStatus = moyuntengSdkClient.updateS5Proxy(host, container, request);
        updateLocalS5ProxyStatus(container, request, proxyStatus);
        return requireOwnedContainer(userId, containerId);
    }

    /** 查询当前用户云机的 S5 代理状态，并回写到 rawJson。 */
    @Override
    public CloudContainer getMyContainerS5ProxyStatus(Long userId, Long containerId)
    {
        CloudContainer container = requireOwnedContainer(userId, containerId);
        CloudHost host = requireHost(container.getHostId());
        Map<String, Object> proxyStatus = moyuntengSdkClient.queryS5Proxy(host, container);
        updateLocalS5ProxyStatus(container, null, proxyStatus);
        return requireOwnedContainer(userId, containerId);
    }

    /** 将 S5 代理配置和 Android API 查询结果合并写入容器 rawJson。 */
    private void updateLocalS5ProxyStatus(CloudContainer container, CloudContainerProxyRequest request, Map<String, Object> proxyStatus)
    {
        JSONObject raw = new JSONObject();
        if (StringUtils.isNotBlank(container.getRawJson()))
        {
            try
            {
                raw = JSONObject.parseObject(container.getRawJson());
            }
            catch (Exception e)
            {
                raw = new JSONObject();
            }
        }
        if (request != null)
        {
            raw.put("s5IP", request.getS5Ip());
            raw.put("s5Port", request.getS5Port());
            raw.put("s5User", request.getS5User());
            raw.put("s5Password", request.getS5Password());
            raw.put("s5Type", request.getS5Type());
        }
        if (proxyStatus != null)
        {
            raw.put("proxyStatus", proxyStatus.get("status"));
            raw.put("proxyStatusText", proxyStatus.get("statusText"));
            raw.put("proxyAddr", proxyStatus.get("addr"));
            raw.put("proxyType", proxyStatus.get("type"));
            Object status = proxyStatus.get("status");
            if (status != null && "0".equals(String.valueOf(status)))
            {
                raw.put("s5Type", "0");
            }
        }
        CloudContainer update = new CloudContainer();
        update.setContainerId(container.getContainerId());
        update.setRawJson(raw.toJSONString());
        cloudContainerMapper.updateCloudContainer(update);
    }

    /** 校验并规整 S5 代理请求参数。 */
    private void normalizeS5ProxyRequest(CloudContainerProxyRequest request)
    {
        request.setS5Type(StringUtils.isBlank(request.getS5Type()) ? "1" : request.getS5Type().trim());
        if ("0".equals(request.getS5Type()))
        {
            request.setS5Ip("");
            request.setS5Port("");
            request.setS5User("");
            request.setS5Password("");
            return;
        }
        request.setS5Ip(StringUtils.defaultString(request.getS5Ip()).trim());
        request.setS5Port(StringUtils.defaultString(request.getS5Port()).trim());
        request.setS5User(StringUtils.defaultString(request.getS5User()).trim());
        request.setS5Password(StringUtils.defaultString(request.getS5Password()).trim());
        if (StringUtils.isBlank(request.getS5Ip()))
        {
            throw new ServiceException("S5代理IP不能为空");
        }
        if (StringUtils.isBlank(request.getS5Port()))
        {
            throw new ServiceException("S5代理端口不能为空");
        }
    }

    /** 建立 RPA 连接并打开 QQ 应用。 */
    private void openQq(CloudHost host, CloudContainer container)
    {
        moyuntengSdkClient.connectRpa(host, container.getContainerName());
        sleepRpaStep(600);
        moyuntengSdkClient.openApp(host, container.getContainerName(), QQ_PACKAGE_NAME);
    }

    /** 按屏幕分辨率比例点击 QQ 右上角加号和扫一扫入口。 */
    private void clickQqScanEntry(CloudHost host, CloudContainer container)
    {
        int width = valueOrDefault(container.getWidth(), 720);
        int height = valueOrDefault(container.getHeight(), 1280);

        sleepRpaStep(3200);
        moyuntengSdkClient.click(host, container.getContainerName(), scale(width, 0.93D), scale(height, 0.06D));
        sleepRpaStep(800);
        moyuntengSdkClient.click(host, container.getContainerName(), scale(width, 0.78D), scale(height, 0.38D));
        sleepRpaStep(8000);
        moyuntengSdkClient.click(host, container.getContainerName(), scale(width, 0.50D), scale(height, 0.85D));
        sleepRpaStep(1800);
        moyuntengSdkClient.shutDownApp(host, container.getContainerName(), QQ_PACKAGE_NAME);
    }

    /** 生成覆盖式上传到 Android 实例的二维码文件名。 */
    private String buildRemoteQrFileName(CloudContainer container, MultipartFile file)
    {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.isBlank(extension))
        {
            extension = "png";
        }
        return "qq_scan_" + container.getContainerId() + "." + extension.toLowerCase();
    }

    /** 尝试解析上传图片中的二维码文本，解析失败不阻断扫码流程。 */
    private String decodeQrText(MultipartFile file)
    {
        try (InputStream inputStream = file.getInputStream())
        {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null)
            {
                return null;
            }
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            return result == null ? null : StringUtils.trimToNull(result.getText());
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** 尝试直接打开二维码文本链接，保留为旧版本扫码能力的补充路径。 */
    private void openQrTextIfPossible(CloudHost host, CloudContainer container, String qrText)
    {
        if (StringUtils.isBlank(qrText))
        {
            return;
        }
        String command = "am start -a android.intent.action.VIEW -d " + shellQuote(qrText);
        try
        {
            moyuntengSdkClient.shell(host, container.getContainerName(), command, 10);
        }
        catch (ServiceException e)
        {
            // Keep the scan-page automation even if Android cannot open the decoded content directly.
        }
    }

    /** 对 shell 命令参数做单引号转义。 */
    private String shellQuote(String value)
    {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    /** 返回有效正整数，否则返回默认值。 */
    private int valueOrDefault(Integer value, int defaultValue)
    {
        return value == null || value <= 0 ? defaultValue : value;
    }

    /** 按屏幕尺寸比例换算点击坐标。 */
    private int scale(int value, double ratio)
    {
        return Math.max(1, (int) Math.round(value * ratio));
    }

    /** RPA 步骤之间等待，保证应用界面完成跳转。 */
    private void sleepRpaStep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("RPA step interrupted");
        }
    }

    /** 主端启动容器。 */
    @Override
    public void startContainer(Long containerId)
    {
        startContainer(requireContainer(containerId));
    }

    /** 用户端启动自己的云机。 */
    @Override
    public void startMyContainer(Long userId, Long containerId)
    {
        startContainer(requireOwnedContainer(userId, containerId));
    }

    /** 主端停止容器。 */
    @Override
    public void stopContainer(Long containerId)
    {
        stopContainer(requireContainer(containerId));
    }

    /** 用户端停止自己的云机。 */
    @Override
    public void stopMyContainer(Long userId, Long containerId)
    {
        stopContainer(requireOwnedContainer(userId, containerId));
    }

    /** 主端重启容器。 */
    @Override
    public void restartContainer(Long containerId)
    {
        restartContainer(requireContainer(containerId));
    }

    /** 用户端重启自己的云机。 */
    @Override
    public void restartMyContainer(Long userId, Long containerId)
    {
        restartContainer(requireOwnedContainer(userId, containerId));
    }

    /** 释放容器分配、账号绑定和调度状态。 */
    @Override
    public int releaseContainer(Long containerId)
    {
        return cloudContainerMapper.releaseCloudContainer(containerId);
    }

    /** 查询容器，不存在时抛出业务异常。 */
    private CloudContainer requireContainer(Long containerId)
    {
        CloudContainer container = cloudContainerMapper.selectCloudContainerById(containerId);
        if (container == null)
        {
            throw new ServiceException("Cloud container does not exist");
        }
        return enrichContainer(container);
    }

    /** 查询并校验容器是否归属当前用户。 */
    private CloudContainer requireOwnedContainer(Long userId, Long containerId)
    {
        CloudContainer container = requireContainer(containerId);
        if (container.getAssignedUserId() == null || !container.getAssignedUserId().equals(userId))
        {
            throw new ServiceException("云机不属于当前用户");
        }
        return enrichContainer(container);
    }

    /** 批量补齐容器展示字段。 */
    private void enrichContainers(List<CloudContainer> containers)
    {
        if (containers == null)
        {
            return;
        }
        for (CloudContainer container : containers)
        {
            enrichContainer(container);
        }
    }

    /** 补齐云机类型、S5 状态和实例位推导端口等展示/控制字段。 */
    private CloudContainer enrichContainer(CloudContainer container)
    {
        if (container == null)
        {
            return null;
        }
        if ("V2".equalsIgnoreCase(container.getAndroidType()))
        {
            container.setCloudMachineType("容器云机");
        }
        else if ("V3".equalsIgnoreCase(container.getAndroidType()))
        {
            container.setCloudMachineType("模拟器云机");
        }
        else
        {
            container.setCloudMachineType(StringUtils.isBlank(container.getAndroidType()) ? "未知" : container.getAndroidType());
        }
        fillS5ProxyStatus(container);
        fillDerivedPorts(container);
        return container;
    }

    /** 从 rawJson 读取 S5 代理配置和状态并写入展示字段。 */
    private void fillS5ProxyStatus(CloudContainer container)
    {
        if (StringUtils.isBlank(container.getRawJson()))
        {
            container.setS5Status("未开启");
            return;
        }
        try
        {
            JSONObject json = JSONObject.parseObject(container.getRawJson());
            container.setS5User(json.getString("s5User"));
            container.setS5Password(json.getString("s5Password"));
            container.setS5Ip(json.getString("s5IP"));
            container.setS5Port(json.getString("s5Port"));
            container.setS5Type(json.getString("s5Type"));
            String proxyStatusText = json.getString("proxyStatusText");
            String proxyAddr = json.getString("proxyAddr");
            if (StringUtils.isNotBlank(proxyStatusText))
            {
                container.setS5Status(StringUtils.isBlank(proxyAddr) ? proxyStatusText : proxyStatusText + " " + proxyAddr);
            }
            else if ("0".equals(container.getS5Type()) || StringUtils.isBlank(container.getS5Ip()))
            {
                container.setS5Status("未开启");
            }
            else
            {
                container.setS5Status(container.getS5Ip() + ":" + StringUtils.defaultString(container.getS5Port()));
            }
        }
        catch (Exception e)
        {
            container.setS5Status("未知");
        }
    }

    /** 按实例位推导 Android API、RPA、摄像头和 WebRTC 端口。 */
    private void fillDerivedPorts(CloudContainer container)
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
        container.setAndroidApiPort(base + 1);
        container.setAndroidRpaPort(base + 2);
        container.setCameraTcpPort(base + 5);
        container.setCameraUdpPort(base + 6);
        if (container.getWebrtcTcpPort() == null)
        {
            container.setWebrtcTcpPort(base + 7);
        }
        if (container.getWebrtcUdpPort() == null)
        {
            container.setWebrtcUdpPort(base + 8);
        }
    }

    /** 调用 SDK 启动容器并刷新本地状态。 */
    private void startContainer(CloudContainer container)
    {
        CloudHost host = requireHost(container.getHostId());
        moyuntengSdkClient.startAndroid(host, container.getContainerName());
        refreshContainerFromSdk(host, container);
    }

    /** 调用 SDK 停止容器并刷新本地状态。 */
    private void stopContainer(CloudContainer container)
    {
        CloudHost host = requireHost(container.getHostId());
        moyuntengSdkClient.stopAndroid(host, container.getContainerName());
        refreshContainerFromSdk(host, container);
    }

    /** 调用 SDK 重启容器并刷新本地状态。 */
    private void restartContainer(CloudContainer container)
    {
        CloudHost host = requireHost(container.getHostId());
        moyuntengSdkClient.restartAndroid(host, container.getContainerName());
        refreshContainerFromSdk(host, container);
    }

    /** 查询主机，不存在时抛出业务异常。 */
    private CloudHost requireHost(Long hostId)
    {
        CloudHost host = cloudHostMapper.selectCloudHostById(hostId);
        if (host == null)
        {
            throw new ServiceException("Cloud host does not exist");
        }
        return host;
    }

    /** 从魔云腾列表中刷新单个容器的运行状态和端口信息。 */
    private void refreshContainerFromSdk(CloudHost host, CloudContainer container)
    {
        sleepAfterCommand();
        List<MytAndroidContainer> containers = moyuntengSdkClient.listAndroid(host);
        for (MytAndroidContainer item : containers)
        {
            if (container.getProviderContainerId().equals(item.getId())
                    || container.getContainerName().equals(item.getName()))
            {
                CloudContainer update = new CloudContainer();
                update.setContainerId(container.getContainerId());
                update.setContainerName(item.getName());
                update.setInstanceId(item.getId());
                update.setIndexNum(item.getIndexNum());
                update.setContainerStatus(item.getStatus());
                update.setAndroidType(item.getAndroidType());
                update.setContainerIp(item.getIp());
                update.setNetworkName(item.getNetworkName());
                update.setImage(item.getImage());
                update.setWidth(item.getWidth());
                update.setHeight(item.getHeight());
                update.setDpi(item.getDpi());
                update.setWebrtcTcpPort(item.getWebrtcTcpPort());
                update.setWebrtcUdpPort(item.getWebrtcUdpPort());
                update.setAdbPort(item.getAdbPort());
                update.setRawJson(item.getRawJson());
                cloudContainerMapper.updateCloudContainer(update);
                return;
            }
        }
    }

    /** 从魔云腾列表中刷新当前主机下已入库容器的状态。 */
    private void refreshHostContainersFromSdk(CloudHost host)
    {
        List<MytAndroidContainer> sdkContainers = moyuntengSdkClient.listAndroid(host);
        CloudContainer query = new CloudContainer();
        query.setHostId(host.getHostId());
        List<CloudContainer> localContainers = cloudContainerMapper.selectCloudContainerList(query);
        for (MytAndroidContainer item : sdkContainers)
        {
            for (CloudContainer local : localContainers)
            {
                if (isSameContainer(local, item))
                {
                    CloudContainer update = new CloudContainer();
                    update.setContainerId(local.getContainerId());
                    update.setContainerName(item.getName());
                    update.setInstanceId(item.getId());
                    update.setIndexNum(item.getIndexNum());
                    update.setContainerStatus(item.getStatus());
                    update.setAndroidType(item.getAndroidType());
                    update.setContainerIp(item.getIp());
                    update.setNetworkName(item.getNetworkName());
                    update.setImage(item.getImage());
                    update.setWidth(item.getWidth());
                    update.setHeight(item.getHeight());
                    update.setDpi(item.getDpi());
                    update.setWebrtcTcpPort(item.getWebrtcTcpPort());
                    update.setWebrtcUdpPort(item.getWebrtcUdpPort());
                    update.setAdbPort(item.getAdbPort());
                    update.setRawJson(item.getRawJson());
                    cloudContainerMapper.updateCloudContainer(update);
                    break;
                }
            }
        }
    }

    /** 判断本地容器和魔云腾返回容器是否为同一个实例。 */
    private boolean isSameContainer(CloudContainer local, MytAndroidContainer remote)
    {
        if (StringUtils.isNotEmpty(local.getProviderContainerId())
                && local.getProviderContainerId().equals(remote.getId()))
        {
            return true;
        }
        return StringUtils.isNotEmpty(local.getContainerName())
                && local.getContainerName().equals(remote.getName());
    }

    /** SDK 启停命令后短暂等待，让魔云腾状态有时间更新。 */
    private void sleepAfterCommand()
    {
        try
        {
            Thread.sleep(1200L);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    /** URL 参数编码。 */
    private String encode(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
