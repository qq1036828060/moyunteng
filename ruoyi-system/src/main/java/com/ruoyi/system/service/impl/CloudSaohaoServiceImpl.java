package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.cloud.MoyuntengSdkClient;
import com.ruoyi.system.cloud.MytAndroidContainer;
import com.ruoyi.system.domain.CloudContainer;
import com.ruoyi.system.domain.CloudHost;
import com.ruoyi.system.mapper.CloudContainerMapper;
import com.ruoyi.system.mapper.CloudHostMapper;
import com.ruoyi.system.service.ICloudSaohaoService;

/**
 * 扫号业务云机操作服务实现。
 */
@Service
public class CloudSaohaoServiceImpl implements ICloudSaohaoService
{
    private static final String ALIPAY_PACKAGE_NAME = "com.eg.android.AlipayGphone";
    private static final int DESIGN_WIDTH = 720;
    private static final int DESIGN_HEIGHT = 1280;

    /** 支付宝首页“扫一扫”设计坐标。 */
    private static final Point SCAN_ENTRY = new Point(90, 214);
    /** 支付确认页“极速付款”设计坐标。 */
    private static final Point FAST_PAY = new Point(360, 1150);
    /** 支付密码键盘数字设计坐标。 */
    private static final Map<Character, Point> PASSWORD_KEYPAD = createPasswordKeypad();

    @Autowired
    private CloudContainerMapper cloudContainerMapper;

    @Autowired
    private CloudHostMapper cloudHostMapper;

    @Autowired
    private MoyuntengSdkClient moyuntengSdkClient;

    /**
     * 准备当前用户的云机，设置二维码虚拟摄像头并按固定坐标完成扫码付款。
     */
    @Override
    public Map<String, Object> scanAndPay(Long userId, Long containerId, MultipartFile file,
            String filePath, String payPassword)
    {
        validateRequest(file, payPassword);

        CloudContainer container = resolveOwnedContainer(userId, containerId);
        CloudHost host = requireHost(container.getHostId());
        String controlMessage = prepareContainerForOperation(host, container);
        container = requireOwnedContainer(userId, container.getContainerId());

        String remoteFileName = buildRemoteQrFileName(container, file);
        String cameraPath = moyuntengSdkClient.uploadAndroidFile(host, container, file, remoteFileName);
        moyuntengSdkClient.setVirtualCameraSource(host, container, "image", cameraPath, 1);
        moyuntengSdkClient.startVirtualCamera(host, container, cameraPath);

        moyuntengSdkClient.connectRpa(host, container.getContainerName());
        waitForUi(600L);
        moyuntengSdkClient.openApp(host, container.getContainerName(), ALIPAY_PACKAGE_NAME);
        executePaymentClicks(host, container, payPassword);

        Map<String, Object> result = new HashMap<>();
        result.put("containerId", container.getContainerId());
        result.put("containerName", container.getContainerName());
        result.put("hostId", container.getHostId());
        result.put("indexNum", container.getIndexNum());
        result.put("cameraPath", cameraPath);
        result.put("sourceFilePath", StringUtils.trimToNull(filePath));
        result.put("payPasswordLength", payPassword.length());
        result.put("controlMessage", controlMessage);
        result.put("message", "Alipay scan and payment commands sent");
        return result;
    }

    /** 校验二维码和六位数字支付密码。 */
    private void validateRequest(MultipartFile file, String payPassword)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请上传二维码图片");
        }
        if (StringUtils.isBlank(payPassword) || !payPassword.matches("\\d{6}"))
        {
            throw new ServiceException("支付密码必须是6位数字");
        }
    }

    /**
     * 根据当前用户归属选择云机。未指定云机时，当前用户必须只分配一个实例位。
     */
    private CloudContainer resolveOwnedContainer(Long userId, Long containerId)
    {
        if (containerId != null)
        {
            return requireOwnedContainer(userId, containerId);
        }

        CloudContainer query = new CloudContainer();
        query.setAssignedUserId(userId);
        List<CloudContainer> assignedContainers = cloudContainerMapper.selectCloudContainerList(query);
        if (assignedContainers == null || assignedContainers.isEmpty())
        {
            throw new ServiceException("当前用户未分配云机实例");
        }

        Map<String, List<CloudContainer>> containersBySlot = new LinkedHashMap<>();
        for (CloudContainer item : assignedContainers)
        {
            if (item.getHostId() == null || item.getIndexNum() == null)
            {
                continue;
            }
            String slotKey = item.getHostId() + ":" + item.getIndexNum();
            containersBySlot.computeIfAbsent(slotKey, key -> new ArrayList<>()).add(item);
        }
        if (containersBySlot.isEmpty())
        {
            throw new ServiceException("当前用户分配的云机缺少主机或实例位信息");
        }
        if (containersBySlot.size() > 1)
        {
            throw new ServiceException("当前用户分配了多个云机实例，请传入 containerId");
        }

        List<CloudContainer> slotContainers = containersBySlot.values().iterator().next();
        for (CloudContainer item : slotContainers)
        {
            if ("running".equalsIgnoreCase(item.getContainerStatus()))
            {
                return item;
            }
        }
        return slotContainers.get(0);
    }

    /** 校验指定云机属于当前用户。 */
    private CloudContainer requireOwnedContainer(Long userId, Long containerId)
    {
        CloudContainer container = cloudContainerMapper.selectCloudContainerById(containerId);
        if (container == null)
        {
            throw new ServiceException("云机不存在");
        }
        if (container.getAssignedUserId() == null || !container.getAssignedUserId().equals(userId))
        {
            throw new ServiceException("云机不属于当前用户");
        }
        fillDerivedPorts(container);
        return container;
    }

    /** 查询并校验主机存在。 */
    private CloudHost requireHost(Long hostId)
    {
        CloudHost host = cloudHostMapper.selectCloudHostById(hostId);
        if (host == null)
        {
            throw new ServiceException("云机主机不存在");
        }
        return host;
    }

    /**
     * 保证目标实例位只有目标云机运行；如果当前未运行则自动启动。
     */
    private String prepareContainerForOperation(CloudHost host, CloudContainer target)
    {
        if (target.getIndexNum() == null)
        {
            throw new ServiceException("云机实例位为空，无法执行支付操作");
        }

        refreshHostContainersFromSdk(host);
        target = requireOwnedContainer(target.getAssignedUserId(), target.getContainerId());
        CloudContainer slotQuery = new CloudContainer();
        slotQuery.setHostId(target.getHostId());
        slotQuery.setIndexNum(target.getIndexNum());
        List<CloudContainer> slotContainers = cloudContainerMapper.selectCloudContainerByHostIndex(slotQuery);

        boolean targetRunning = false;
        List<CloudContainer> runningOthers = new ArrayList<>();
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

        for (CloudContainer item : runningOthers)
        {
            moyuntengSdkClient.stopAndroid(host, item.getContainerName());
        }
        if (!runningOthers.isEmpty())
        {
            waitForStateChange();
            refreshHostContainersFromSdk(host);
        }

        if (!targetRunning)
        {
            moyuntengSdkClient.startAndroid(host, target.getContainerName());
            waitForStateChange();
            refreshHostContainersFromSdk(host);
        }

        CloudContainer refreshedTarget = cloudContainerMapper.selectCloudContainerById(target.getContainerId());
        if (refreshedTarget == null || !"running".equalsIgnoreCase(refreshedTarget.getContainerStatus()))
        {
            throw new ServiceException("目标云机启动失败，请同步容器状态后重试");
        }
        if (!runningOthers.isEmpty() && !targetRunning)
        {
            return "已关闭同实例位其他云机并启动目标云机";
        }
        if (!runningOthers.isEmpty())
        {
            return "已关闭同实例位其他运行云机";
        }
        return targetRunning ? "目标云机已在运行" : "目标云机已启动";
    }

    /** 从魔云腾容器列表刷新当前主机的本地运行状态。 */
    private void refreshHostContainersFromSdk(CloudHost host)
    {
        List<MytAndroidContainer> remoteContainers = moyuntengSdkClient.listAndroid(host);
        CloudContainer query = new CloudContainer();
        query.setHostId(host.getHostId());
        List<CloudContainer> localContainers = cloudContainerMapper.selectCloudContainerList(query);
        for (MytAndroidContainer remote : remoteContainers)
        {
            for (CloudContainer local : localContainers)
            {
                if (!isSameContainer(local, remote))
                {
                    continue;
                }
                CloudContainer update = new CloudContainer();
                update.setContainerId(local.getContainerId());
                update.setContainerName(remote.getName());
                update.setInstanceId(remote.getId());
                update.setIndexNum(remote.getIndexNum());
                update.setContainerStatus(remote.getStatus());
                update.setAndroidType(remote.getAndroidType());
                update.setContainerIp(remote.getIp());
                update.setNetworkName(remote.getNetworkName());
                update.setImage(remote.getImage());
                update.setWidth(remote.getWidth());
                update.setHeight(remote.getHeight());
                update.setDpi(remote.getDpi());
                update.setWebrtcTcpPort(remote.getWebrtcTcpPort());
                update.setWebrtcUdpPort(remote.getWebrtcUdpPort());
                update.setAdbPort(remote.getAdbPort());
                update.setRawJson(remote.getRawJson());
                cloudContainerMapper.updateCloudContainer(update);
                break;
            }
        }
    }

    /** 判断本地与魔云腾返回项是否为同一云机。 */
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

    /** 按参考图坐标依次点击扫一扫、极速付款和六位密码。 */
    private void executePaymentClicks(CloudHost host, CloudContainer container, String payPassword)
    {
        waitForUi(3000L);
        clickDesignPoint(host, container, SCAN_ENTRY);
        waitForUi(8000L);
        clickDesignPoint(host, container, FAST_PAY);
        waitForUi(2000L);
        for (char digit : payPassword.toCharArray())
        {
            clickDesignPoint(host, container, PASSWORD_KEYPAD.get(digit));
            waitForUi(250L);
        }
    }

    /** 将720x1280参考图坐标按云机实际分辨率缩放后点击。 */
    private void clickDesignPoint(CloudHost host, CloudContainer container, Point point)
    {
        int width = valueOrDefault(container.getWidth(), DESIGN_WIDTH);
        int height = valueOrDefault(container.getHeight(), DESIGN_HEIGHT);
        int x = Math.max(1, (int) Math.round(point.x * width / (double) DESIGN_WIDTH));
        int y = Math.max(1, (int) Math.round(point.y * height / (double) DESIGN_HEIGHT));
        moyuntengSdkClient.click(host, container.getContainerName(), x, y);
    }

    /** 生成覆盖上传使用的固定二维码文件名。 */
    private String buildRemoteQrFileName(CloudContainer container, MultipartFile file)
    {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.isBlank(extension))
        {
            extension = "png";
        }
        return "saohao_pay_" + container.getContainerId() + "." + extension.toLowerCase();
    }

    /** 补齐 Android API 等实例位推导端口。 */
    private void fillDerivedPorts(CloudContainer container)
    {
        if (container.getIndexNum() == null)
        {
            return;
        }
        int base = 30000 + (container.getIndexNum() - 1) * 100;
        container.setAndroidApiPort(base + 1);
        container.setAndroidRpaPort(base + 2);
        container.setCameraTcpPort(base + 5);
        container.setCameraUdpPort(base + 6);
    }

    /** 创建数字密码键盘坐标映射。 */
    private static Map<Character, Point> createPasswordKeypad()
    {
        Map<Character, Point> keypad = new HashMap<>();
        keypad.put('1', new Point(120, 875));
        keypad.put('2', new Point(360, 875));
        keypad.put('3', new Point(600, 875));
        keypad.put('4', new Point(120, 991));
        keypad.put('5', new Point(360, 991));
        keypad.put('6', new Point(600, 991));
        keypad.put('7', new Point(120, 1108));
        keypad.put('8', new Point(360, 1108));
        keypad.put('9', new Point(600, 1108));
        keypad.put('0', new Point(360, 1223));
        return keypad;
    }

    /** 云机状态切换等待。 */
    private void waitForStateChange()
    {
        waitForUi(1200L);
    }

    /** 等待 Android 界面或云机状态完成切换。 */
    private void waitForUi(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("云机操作被中断");
        }
    }

    /** 数值为空或无效时返回默认值。 */
    private int valueOrDefault(Integer value, int defaultValue)
    {
        return value == null || value <= 0 ? defaultValue : value;
    }

    /** 参考图片中的二维坐标。 */
    private static final class Point
    {
        private final int x;
        private final int y;

        private Point(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }
}
