<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="主机ID" prop="hostId">
        <el-input v-model="queryParams.hostId" placeholder="请输入主机ID" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="容器名" prop="containerName">
        <el-input v-model="queryParams.containerName" placeholder="请输入容器名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="运行状态" prop="containerStatus">
        <el-select v-model="queryParams.containerStatus" placeholder="请选择" clearable style="width: 150px">
          <el-option label="running" value="running" />
          <el-option label="created" value="created" />
          <el-option label="stopped" value="stopped" />
        </el-select>
      </el-form-item>
      <el-form-item label="调度状态" prop="scheduleStatus">
        <el-select v-model="queryParams.scheduleStatus" placeholder="请选择" clearable style="width: 150px">
          <el-option label="IDLE" value="IDLE" />
          <el-option label="RUNNING" value="RUNNING" />
          <el-option label="CONTROLLING" value="CONTROLLING" />
          <el-option label="DISABLED" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="containerList">
      <el-table-column label="容器ID" align="center" prop="containerId" width="90" />
      <el-table-column label="主机ID" align="center" prop="hostId" width="90" />
      <el-table-column label="容器名" align="center" prop="containerName" min-width="140" />
      <el-table-column label="实例位" align="center" prop="indexNum" width="80" />
      <el-table-column label="运行状态" align="center" prop="containerStatus" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.containerStatus === 'running' ? 'success' : 'info'">{{ scope.row.containerStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="调度状态" align="center" prop="scheduleStatus" width="120" />
      <el-table-column label="容器IP" align="center" prop="containerIp" width="140" />
      <el-table-column label="WebRTC" align="center" width="150">
        <template #default="scope">{{ scope.row.webrtcTcpPort || '-' }} / {{ scope.row.webrtcUdpPort || '-' }}</template>
      </el-table-column>
      <el-table-column label="ADB端口" align="center" prop="adbPort" width="100" />
      <el-table-column label="镜像" align="center" prop="image" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="260" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="VideoPlay" @click="handleStart(scope.row)" v-hasPermi="['cloud:container:start']">启动</el-button>
          <el-button link type="primary" icon="SwitchButton" @click="handleStop(scope.row)" v-hasPermi="['cloud:container:stop']">停止</el-button>
          <el-button link type="primary" icon="Refresh" @click="handleRestart(scope.row)" v-hasPermi="['cloud:container:restart']">重启</el-button>
          <el-button link type="primary" icon="Unlock" @click="handleRelease(scope.row)" v-hasPermi="['cloud:container:release']">释放</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="CloudContainer">
import { listContainer, startContainer, stopContainer, restartContainer, releaseContainer } from "@/api/cloud/container"

const { proxy } = getCurrentInstance()
const containerList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    hostId: undefined,
    containerName: undefined,
    containerStatus: undefined,
    scheduleStatus: undefined
  }
})

const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listContainer(queryParams.value).then(response => {
    containerList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function runAction(message, action) {
  proxy.$modal.confirm(message).then(action).then(() => {
    proxy.$modal.msgSuccess("操作成功")
    getList()
  }).catch(() => {})
}

function handleStart(row) {
  runAction("确认启动容器 " + row.containerName + "？", () => startContainer(row.containerId))
}

function handleStop(row) {
  runAction("确认停止容器 " + row.containerName + "？", () => stopContainer(row.containerId))
}

function handleRestart(row) {
  runAction("确认重启容器 " + row.containerName + "？", () => restartContainer(row.containerId))
}

function handleRelease(row) {
  runAction("确认释放容器 " + row.containerName + " 的调度锁？", () => releaseContainer(row.containerId))
}

getList()
</script>

