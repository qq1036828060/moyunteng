package com.ruoyi.system.service.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.cloud.MoyuntengSdkClient;
import com.ruoyi.system.cloud.MytAndroidContainer;
import com.ruoyi.system.domain.CloudBankCodeRequest;
import com.ruoyi.system.domain.CloudBankPayRequest;
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
    private static final Logger log = LoggerFactory.getLogger(CloudSaohaoServiceImpl.class);
    private static final String ALIPAY_PACKAGE_NAME = "com.eg.android.AlipayGphone";
    private static final int DESIGN_WIDTH = 720;
    private static final int DESIGN_HEIGHT = 1280;
    private static final int SLOT_LOCK_SECONDS = 120;
    private static final int BANK_WAIT_CODE_LOCK_SECONDS = 600;
    private static final Pattern UI_BOUNDS_PATTERN = Pattern.compile("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]");

    /** 支付宝首页“扫一扫”设计坐标。 */
    private static final Point SCAN_ENTRY = new Point(90, 214);
    /** 支付确认页“极速付款”设计坐标。 */
    private static final Point FAST_PAY = new Point(360, 1150);
    private static final Point PENDING_PAYMENT = new Point(180, 186);
    private static final Point OTHER_PAYMENT = new Point(360, 965);
    private static final Point FIRST_BANK_CARD = new Point(360, 889);
    private static final Point IMMEDIATE_BANK_PAYMENT = new Point(360, 1145);
    private static final Point GET_BANK_CODE = new Point(600, 1015);
    private static final Point BANK_CODE_INPUT = new Point(330, 1015);
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
        String lockOwner = "saohao:" + userId + ":" + UUID.randomUUID();
        acquireSlotLock(container, lockOwner);
        try
        {
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
        finally
        {
            releaseSlotLock(container, lockOwner);
        }
    }

    /**
     * 执行银行卡支付第一阶段，默认选择银行卡支付区域中的第一张已绑定银行卡。
     * 成功发送短信验证码后保留实例位锁，等待 bankcode 接口继续任务。
     */
    @Override
    public Map<String, Object> startBankPayment(Long userId, CloudBankPayRequest request)
    {
        Long containerId = request == null ? null : request.getContainerId();
        CloudContainer container = resolveOwnedContainer(userId, containerId);
        String taskId = UUID.randomUUID().toString();
        String lockOwner = bankWaitLockOwner(userId, taskId);
        acquireSlotLock(container, lockOwner, BANK_WAIT_CODE_LOCK_SECONDS);

        boolean keepLockForCode = false;
        try
        {
            CloudHost host = requireHost(container.getHostId());
            String controlMessage = prepareContainerForOperation(host, container);
            container = requireOwnedContainer(userId, container.getContainerId());
            executeBankPaymentToCode(host, container);

            int renewed = cloudContainerMapper.renewCloudContainerSlotLock(container.getHostId(),
                    container.getIndexNum(), lockOwner, BANK_WAIT_CODE_LOCK_SECONDS);
            if (renewed != 1)
            {
                throw new ServiceException("银行卡支付任务锁已失效，请重新发起支付");
            }
            keepLockForCode = true;

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("state", "WAITING_BANK_CODE");
            result.put("containerId", container.getContainerId());
            result.put("containerName", container.getContainerName());
            result.put("hostId", container.getHostId());
            result.put("indexNum", container.getIndexNum());
            result.put("lockExpireSeconds", BANK_WAIT_CODE_LOCK_SECONDS);
            result.put("controlMessage", controlMessage);
            result.put("message", "短信验证码已请求，请调用 /cloud/saohao/pay/bankcode 提交验证码");
            return result;
        }
        finally
        {
            if (!keepLockForCode)
            {
                releaseSlotLock(container, lockOwner);
            }
        }
    }

    /** 接收验证码，原子接管第一阶段任务锁并确认支付。 */
    @Override
    public Map<String, Object> submitBankCode(Long userId, CloudBankCodeRequest request)
    {
        validateBankCodeRequest(request);
        String taskId = request.getTaskId().trim();
        String expectedOwner = bankWaitLockOwner(userId, taskId);
        CloudContainer lockContainer = cloudContainerMapper.selectCloudContainerByUserLockOwner(userId, expectedOwner);
        if (lockContainer == null)
        {
            throw new ServiceException("银行卡支付任务不存在、已完成或已超时，请重新发起支付");
        }

        String codeOwner = "bankcode:" + userId + ":" + UUID.randomUUID();
        int transferred = cloudContainerMapper.transferCloudContainerSlotLock(lockContainer.getHostId(),
                lockContainer.getIndexNum(), expectedOwner, codeOwner, SLOT_LOCK_SECONDS);
        if (transferred != 1)
        {
            throw new ServiceException("验证码正在提交或任务已超时，请勿重复提交");
        }

        try
        {
            CloudContainer container = requireRunningContainerInLockedSlot(userId, lockContainer);
            CloudHost host = requireHost(container.getHostId());
            executeBankCodeSubmit(host, container, request.getCode().trim());

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("state", "SUBMITTED");
            result.put("containerId", container.getContainerId());
            result.put("containerName", container.getContainerName());
            result.put("hostId", container.getHostId());
            result.put("indexNum", container.getIndexNum());
            result.put("message", "验证码已输入，确认支付指令已下发");
            return result;
        }
        finally
        {
            releaseSlotLock(lockContainer, codeOwner);
        }
    }

    /** 原子获取主机实例位锁，避免同一云机被并发控制。 */
    private void acquireSlotLock(CloudContainer container, String lockOwner)
    {
        acquireSlotLock(container, lockOwner, SLOT_LOCK_SECONDS);
    }

    /** 按指定超时时间原子获取主机实例位锁。 */
    private void acquireSlotLock(CloudContainer container, String lockOwner, int lockSeconds)
    {
        if (container.getHostId() == null || container.getIndexNum() == null)
        {
            throw new ServiceException("云机缺少主机或实例位信息，无法获取操作锁");
        }
        int locked = cloudContainerMapper.tryLockCloudContainerSlot(container.getHostId(), container.getIndexNum(),
                lockOwner, lockSeconds);
        if (locked != 1)
        {
            throw new ServiceException("当前云机正在执行其他任务，请稍后重试");
        }
    }

    /** 构造银行卡支付等待验证码阶段的锁持有者。 */
    private String bankWaitLockOwner(Long userId, String taskId)
    {
        return "bank:" + userId + ":" + taskId;
    }

    /** 校验验证码任务参数，验证码允许4至8位数字。 */
    private void validateBankCodeRequest(CloudBankCodeRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getTaskId()))
        {
            throw new ServiceException("银行卡支付任务ID不能为空");
        }
        try
        {
            UUID.fromString(request.getTaskId().trim());
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException("银行卡支付任务ID格式不正确");
        }
        if (StringUtils.isBlank(request.getCode()) || !request.getCode().trim().matches("\\d{4,8}"))
        {
            throw new ServiceException("短信验证码必须是4至8位数字");
        }
    }

    /**
     * 仅释放当前请求持有的实例位锁；释放失败不覆盖原始业务异常，锁会在超时后自动失效。
     */
    private void releaseSlotLock(CloudContainer container, String lockOwner)
    {
        try
        {
            int unlocked = cloudContainerMapper.unlockCloudContainerSlot(container.getHostId(),
                    container.getIndexNum(), lockOwner);
            if (unlocked == 0)
            {
                log.warn("Saohao slot lock was not released by owner, hostId={}, indexNum={}, lockOwner={}",
                        container.getHostId(), container.getIndexNum(), lockOwner);
            }
        }
        catch (Exception e)
        {
            log.error("Failed to release saohao slot lock, hostId={}, indexNum={}, lockOwner={}",
                    container.getHostId(), container.getIndexNum(), lockOwner, e);
        }
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

    /**
     * 验证等待验证码的实例位仍然只有当前用户的一台云机在运行。
     */
    private CloudContainer requireRunningContainerInLockedSlot(Long userId, CloudContainer lockContainer)
    {
        CloudContainer slotQuery = new CloudContainer();
        slotQuery.setHostId(lockContainer.getHostId());
        slotQuery.setIndexNum(lockContainer.getIndexNum());
        List<CloudContainer> slotContainers = cloudContainerMapper.selectCloudContainerByHostIndex(slotQuery);

        CloudContainer runningContainer = null;
        for (CloudContainer item : slotContainers)
        {
            if (!userId.equals(item.getAssignedUserId())
                    || !"running".equalsIgnoreCase(item.getContainerStatus()))
            {
                continue;
            }
            if (runningContainer != null)
            {
                throw new ServiceException("同一实例位存在多个运行云机，无法继续银行卡支付");
            }
            runningContainer = item;
        }
        if (runningContainer == null)
        {
            throw new ServiceException("银行卡支付云机已停止，请重新发起支付");
        }
        fillDerivedPorts(runningContainer);
        return runningContainer;
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

    /**
     * 执行银行卡支付的第一阶段，固定选择支付方式列表中的第一张已绑定银行卡并请求短信验证码。
     */
    private void executeBankPaymentToCode(CloudHost host, CloudContainer container)
    {
        moyuntengSdkClient.connectRpa(host, container.getContainerName());
        waitForUi(800L);

        clickUiTextOrFallback(host, container, "待付款", PENDING_PAYMENT, 3, 600L);
        waitForUi(300L);
        clickUiTextOrFallback(host, container, "待付款", PENDING_PAYMENT, 2, 300L);
        waitForUi(500L);
        swipeDesign(host, container, 360, 360, 360, 950, 650);
        waitForUi(2200L);

        UiNode orderPay = waitForUiNode(host, container, "立即付款", true, 5, 900L);
        if (orderPay == null)
        {
            throw new ServiceException("刷新后未找到待付款订单，请确认当前账号存在待付款订单");
        }
        clickUiNode(host, container, orderPay);
        waitForUi(1200L);

        clickUiTextOrFallback(host, container, "其他支付方式", OTHER_PAYMENT, 4, 700L);
        waitForUi(700L);
        swipeDesign(host, container, 360, 1060, 360, 600, 550);
        waitForUi(700L);

        // 业务约束为只绑定一张银行卡，因此始终点击支付方式列表中的第一张卡。
        clickDesignPoint(host, container, FIRST_BANK_CARD);
        waitForUi(500L);
        clickUiTextOrFallback(host, container, "立即支付", IMMEDIATE_BANK_PAYMENT, 4, 700L);
        waitForUi(1500L);
        clickUiTextOrFallback(host, container, "获取验证码", GET_BANK_CODE, 4, 700L);
        waitForUi(700L);
    }

    /** 输入短信验证码，并按弹出键盘后的实际控件位置确认支付。 */
    private void executeBankCodeSubmit(CloudHost host, CloudContainer container, String code)
    {
        UiNode codeInput = waitForUiNode(host, container, "请输入验证码", false, 4, 600L);
        if (codeInput != null)
        {
            clickUiNode(host, container, codeInput);
        }
        else
        {
            clickDesignPoint(host, container, BANK_CODE_INPUT);
        }
        waitForUi(350L);
        moyuntengSdkClient.shell(host, container.getContainerName(), "input text " + code, 10);
        waitForUi(500L);

        UiNode confirmPay = waitForUiNode(host, container, "确认支付", true, 5, 600L);
        if (confirmPay == null)
        {
            throw new ServiceException("验证码已输入，但未找到确认支付按钮，请重新发起支付");
        }
        clickUiNode(host, container, confirmPay);
        waitForUi(1200L);
    }

    /** 按参考图分辨率缩放并执行滑动。 */
    private void swipeDesign(CloudHost host, CloudContainer container, int startX, int startY,
            int endX, int endY, int durationMillis)
    {
        int width = valueOrDefault(container.getWidth(), DESIGN_WIDTH);
        int height = valueOrDefault(container.getHeight(), DESIGN_HEIGHT);
        int actualStartX = scaleCoordinate(startX, width, DESIGN_WIDTH);
        int actualStartY = scaleCoordinate(startY, height, DESIGN_HEIGHT);
        int actualEndX = scaleCoordinate(endX, width, DESIGN_WIDTH);
        int actualEndY = scaleCoordinate(endY, height, DESIGN_HEIGHT);
        String command = "input swipe " + actualStartX + " " + actualStartY + " "
                + actualEndX + " " + actualEndY + " " + durationMillis;
        moyuntengSdkClient.shell(host, container.getContainerName(), command, 10);
    }

    /** 优先按界面文本点击，无法读取界面结构时使用参考图坐标兜底。 */
    private void clickUiTextOrFallback(CloudHost host, CloudContainer container, String text,
            Point fallback, int attempts, long intervalMillis)
    {
        UiNode node = waitForUiNode(host, container, text, true, attempts, intervalMillis);
        if (node != null)
        {
            clickUiNode(host, container, node);
            return;
        }
        clickDesignPoint(host, container, fallback);
    }

    /** 轮询 Android UI 层级，查找包含指定文本且具有有效边界的控件。 */
    private UiNode waitForUiNode(CloudHost host, CloudContainer container, String expectedText,
            boolean exact, int attempts, long intervalMillis)
    {
        for (int attempt = 1; attempt <= attempts; attempt++)
        {
            try
            {
                UiNode node = findUiNode(dumpUiXml(host, container), expectedText, exact);
                if (node != null)
                {
                    return node;
                }
            }
            catch (Exception e)
            {
                log.debug("读取云机UI层级失败，containerName={}, attempt={}/{}: {}",
                        container.getContainerName(), attempt, attempts, e.getMessage());
            }
            if (attempt < attempts)
            {
                waitForUi(intervalMillis);
            }
        }
        return null;
    }

    /** 通过 uiautomator 获取当前 Android 界面 XML。 */
    private String dumpUiXml(CloudHost host, CloudContainer container)
    {
        Map<String, Object> result = moyuntengSdkClient.shell(host, container.getContainerName(),
                "uiautomator dump /sdcard/saohao_window.xml >/dev/null 2>&1 && cat /sdcard/saohao_window.xml", 10);
        String xml = extractUiXml(result);
        if (StringUtils.isBlank(xml))
        {
            throw new ServiceException("未获取到云机UI层级");
        }
        return xml;
    }

    /** 从 SDK 的多层响应结构中递归提取 UI XML。 */
    private String extractUiXml(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Map)
        {
            for (Object child : ((Map<?, ?>) value).values())
            {
                String xml = extractUiXml(child);
                if (xml != null)
                {
                    return xml;
                }
            }
            return null;
        }
        if (value instanceof Collection)
        {
            for (Object child : (Collection<?>) value)
            {
                String xml = extractUiXml(child);
                if (xml != null)
                {
                    return xml;
                }
            }
            return null;
        }

        String text = String.valueOf(value).replace("\u0000", "");
        int start = text.indexOf("<?xml");
        if (start < 0)
        {
            start = text.indexOf("<hierarchy");
        }
        int end = text.lastIndexOf("</hierarchy>");
        return start >= 0 && end >= start ? text.substring(start, end + "</hierarchy>".length()) : null;
    }

    /** 解析 UI XML，按文本或无障碍描述查找控件中心点。 */
    private UiNode findUiNode(String xml, String expectedText, boolean exact) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        NodeList nodes = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)))
                .getElementsByTagName("node");
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Element element = (Element) nodes.item(i);
            String text = element.getAttribute("text");
            String description = element.getAttribute("content-desc");
            if (!matchesUiText(text, expectedText, exact) && !matchesUiText(description, expectedText, exact))
            {
                continue;
            }
            Matcher matcher = UI_BOUNDS_PATTERN.matcher(element.getAttribute("bounds"));
            if (matcher.matches())
            {
                return new UiNode(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
            }
        }
        return null;
    }

    /** 判断 UI 文本是否满足精确或包含匹配。 */
    private boolean matchesUiText(String actual, String expected, boolean exact)
    {
        if (StringUtils.isBlank(actual))
        {
            return false;
        }
        return exact ? expected.equals(actual.trim()) : actual.contains(expected);
    }

    /** 点击 UI 层级返回的实际屏幕坐标。 */
    private void clickUiNode(CloudHost host, CloudContainer container, UiNode node)
    {
        moyuntengSdkClient.click(host, container.getContainerName(), node.centerX(), node.centerY());
    }

    /** 将720x1280参考图坐标按云机实际分辨率缩放后点击。 */
    private void clickDesignPoint(CloudHost host, CloudContainer container, Point point)
    {
        int width = valueOrDefault(container.getWidth(), DESIGN_WIDTH);
        int height = valueOrDefault(container.getHeight(), DESIGN_HEIGHT);
        int x = scaleCoordinate(point.x, width, DESIGN_WIDTH);
        int y = scaleCoordinate(point.y, height, DESIGN_HEIGHT);
        moyuntengSdkClient.click(host, container.getContainerName(), x, y);
    }

    /** 将参考图单轴坐标缩放为云机实际坐标。 */
    private int scaleCoordinate(int coordinate, int actualSize, int designSize)
    {
        return Math.max(1, (int) Math.round(coordinate * actualSize / (double) designSize));
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

    /** Android UI 层级节点边界。 */
    private static final class UiNode
    {
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        private UiNode(int left, int top, int right, int bottom)
        {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        private int centerX()
        {
            return left + (right - left) / 2;
        }

        private int centerY()
        {
            return top + (bottom - top) / 2;
        }
    }
}
