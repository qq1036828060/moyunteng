<template>
  <div class="app-container">
    <el-row :gutter="12" class="mb8">
      <el-col :xs="12" :sm="6">
        <el-statistic title="拥有云机" :value="summary.total || 0" />
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-statistic title="运行中" :value="summary.runningCount || 0" />
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-statistic title="空闲" :value="summary.idleCount || 0" />
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-statistic title="占用中" :value="summary.busyCount || 0" />
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="绑定账号" prop="boundAccountNo">
        <el-input
          v-model="queryParams.boundAccountNo"
          placeholder="输入账号查询绑定云机"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="运行状态" prop="containerStatus">
        <el-select v-model="queryParams.containerStatus" placeholder="请选择" clearable style="width: 140px">
          <el-option label="running" value="running" />
          <el-option label="created" value="created" />
          <el-option label="stopped" value="stopped" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="refreshAll" />
    </el-row>

    <el-table v-loading="loading" :data="containerList">
      <el-table-column label="主机ID" align="center" prop="hostId" width="80" />
      <el-table-column label="实例位" align="center" prop="indexNum" width="80" />
      <el-table-column label="云机类型" align="center" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.androidType === 'V3' ? 'warning' : 'success'">
            {{ scope.row.cloudMachineType || scope.row.androidType || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="云机名称" align="center" prop="containerName" min-width="150" show-overflow-tooltip />
      <el-table-column label="绑定账号" align="center" prop="boundAccountNo" min-width="150" show-overflow-tooltip />
      <el-table-column label="运行状态" align="center" prop="containerStatus" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.containerStatus === 'running' ? 'success' : 'info'">
            {{ scope.row.containerStatus || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="调度状态" align="center" prop="scheduleStatus" width="110" />
      <el-table-column label="WebRTC端口" align="center" width="140">
        <template #default="scope">{{ scope.row.webrtcTcpPort || '-' }} / {{ scope.row.webrtcUdpPort || '-' }}</template>
      </el-table-column>
      <el-table-column label="安卓API端口" align="center" prop="androidApiPort" width="110" />
      <el-table-column label="摄像头端口" align="center" width="140">
        <template #default="scope">{{ scope.row.cameraTcpPort || '-' }} / {{ scope.row.cameraUdpPort || '-' }}</template>
      </el-table-column>
      <el-table-column label="ADB端口" align="center" prop="adbPort" width="90" />
      <el-table-column label="操作" align="center" width="380" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Connection"
            @click="handleBind(scope.row)"
            v-hasPermi="['cloud:mycontainer:bind']"
          >绑定账号</el-button>
          <el-button
            link
            type="primary"
            icon="Monitor"
            @click="handleControl(scope.row)"
            v-hasPermi="['cloud:mycontainer:operate']"
          >进入控制</el-button>
          <el-button
            link
            type="primary"
            icon="Camera"
            @click="handleScan(scope.row)"
            v-hasPermi="['cloud:mycontainer:operate']"
          >扫码</el-button>
          <el-button
            link
            type="primary"
            icon="VideoPlay"
            @click="handleStart(scope.row)"
            v-hasPermi="['cloud:mycontainer:operate']"
          >启动</el-button>
          <el-button
            link
            type="primary"
            icon="SwitchButton"
            @click="handleStop(scope.row)"
            v-hasPermi="['cloud:mycontainer:operate']"
          >停止</el-button>
          <el-button
            link
            type="primary"
            icon="Refresh"
            @click="handleRestart(scope.row)"
            v-hasPermi="['cloud:mycontainer:operate']"
          >重启</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="绑定账号" v-model="bindOpen" width="460px" append-to-body>
      <el-form ref="bindRef" :model="bindForm" label-width="90px">
        <el-form-item label="云机">
          <span>{{ bindForm.containerName }}</span>
        </el-form-item>
        <el-form-item label="账号" prop="boundAccountNo">
          <el-input v-model="bindForm.boundAccountNo" placeholder="请输入要绑定的账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitBind">确定</el-button>
          <el-button @click="bindOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="扫码" v-model="scanOpen" width="480px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="云机">
          <span>{{ scanForm.containerName }}</span>
        </el-form-item>
        <el-form-item label="二维码">
          <el-upload
            drag
            :action="scanUploadUrl"
            accept=".png,.jpg,.jpeg,.bmp,.gif"
            :auto-upload="false"
            :limit="1"
            :file-list="scanFileList"
            :on-change="onScanFileChange"
            :on-remove="onScanFileRemove"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽图片到此处，或点击上传</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="scanLoading" @click="submitScan">开始扫码</el-button>
          <el-button @click="scanOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CloudMyContainer">
import axios from 'axios'
import { getToken } from '@/utils/auth'
import {
  listMyContainer,
  getMyContainerSummary,
  startMyContainer,
  stopMyContainer,
  restartMyContainer,
  bindMyContainerAccount,
  scanMyContainerQq,
} from '@/api/cloud/myContainer'

const router = useRouter()
const { proxy } = getCurrentInstance()
const loading = ref(true)
const showSearch = ref(true)
const containerList = ref([])
const total = ref(0)
const summary = ref({})
const bindOpen = ref(false)
const scanOpen = ref(false)
const scanLoading = ref(false)
const scanFile = ref(null)
const scanFileList = ref([])
const scanUploadUrl = 'https://h5.61xm.cn/prod-api/upload/image'

console.log('CloudMyContainer setup')

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    boundAccountNo: undefined,
    containerStatus: undefined,
  },
  bindForm: {
    containerId: undefined,
    containerName: undefined,
    boundAccountNo: undefined,
  },
  scanForm: {
    containerId: undefined,
    containerName: undefined,
  },
})

const { queryParams, bindForm, scanForm } = toRefs(data)

function getList() {
  loading.value = true
  listMyContainer(queryParams.value)
    .then((response) => {
      containerList.value = response.rows
      total.value = response.total
      loading.value = false
    })
    .catch(() => {
      loading.value = false
    })
}

function getSummary() {
  getMyContainerSummary().then((response) => {
    summary.value = response.data || {}
  })
}

function refreshAll() {
  getSummary()
  getList()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleBind(row) {
  bindForm.value = {
    containerId: row.containerId,
    containerName: row.containerName,
    boundAccountNo: row.boundAccountNo,
  }
  bindOpen.value = true
}

function submitBind() {
  bindMyContainerAccount(bindForm.value.containerId, bindForm.value.boundAccountNo).then(() => {
    proxy.$modal.msgSuccess('绑定成功，正在进入控制并打开 QQ')
    bindOpen.value = false
    refreshAll()
    router.push({
      path: '/cloud/my-container-control/control/' + bindForm.value.containerId,
      query: { autoOpenQq: '1' },
    })
  })
}

function handleControl(row) {
  router.push('/cloud/my-container-control/control/' + row.containerId)
}

function handleScan(row) {
  scanForm.value = {
    containerId: row.containerId,
    containerName: row.containerName,
  }
  scanFile.value = null
  scanFileList.value = []
  scanOpen.value = true
}

function onScanFileChange(file) {
  scanFile.value = file.raw || file
  scanFileList.value = [file]
}

function onScanFileRemove() {
  scanFile.value = null
  scanFileList.value = []
}

function uploadScanImage(file) {
  const rawFile = file.raw || file
  const data = new FormData()
  data.append('file', rawFile, rawFile.name)
  return axios.post(scanUploadUrl, data, {
    headers: {
      'Content-Type': 'multipart/form-data',
      Authorization: 'Bearer ' + getToken(),
    },
    transformRequest: [(formData) => formData],
  }).then((response) => {
    const res = response.data || {}
    if (res.code !== 200) {
      return Promise.reject(new Error(res.msg || '图片上传失败'))
    }
    return res.data || {}
  })
}

function submitScan() {
  if (!scanFile.value) {
    proxy.$modal.msgError('请先上传二维码图片')
    return
  }
  scanLoading.value = true
  uploadScanImage(scanFile.value)
    .then((uploadData) => {
      return scanMyContainerQq(scanForm.value.containerId, scanFile.value, uploadData.filePath)
    })
    .then((response) => {
      const data = response.data || {}
      proxy.$modal.msgSuccess(data.qrText ? '已解析二维码并下发扫码操作' : '已下发扫码操作')
      scanOpen.value = false
      refreshAll()
    })
    .catch((error) => {
      proxy.$modal.msgError(error?.message || '图片上传失败')
    })
    .finally(() => {
      scanLoading.value = false
    })
}

function runAction(message, action) {
  proxy.$modal
    .confirm(message)
    .then(action)
    .then(() => {
      proxy.$modal.msgSuccess('操作成功')
      refreshAll()
    })
    .catch(() => {})
}

function handleStart(row) {
  runAction('确认启动云机 ' + row.containerName + '？', () => startMyContainer(row.containerId))
}

function handleStop(row) {
  runAction('确认停止云机 ' + row.containerName + '？', () => stopMyContainer(row.containerId))
}

function handleRestart(row) {
  runAction('确认重启云机 ' + row.containerName + '？', () => restartMyContainer(row.containerId))
}

refreshAll()
</script>
