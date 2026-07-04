<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="主机名称" prop="hostName">
        <el-input v-model="queryParams.hostName" placeholder="请输入主机名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="主机IP" prop="hostIp">
        <el-input v-model="queryParams.hostIp" placeholder="请输入主机IP" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="在线状态" prop="onlineStatus">
        <el-select v-model="queryParams.onlineStatus" placeholder="请选择" clearable style="width: 160px">
          <el-option label="在线" value="ONLINE" />
          <el-option label="离线" value="OFFLINE" />
          <el-option label="未知" value="UNKNOWN" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cloud:host:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['cloud:host:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['cloud:host:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="hostList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主机ID" align="center" prop="hostId" width="90" />
      <el-table-column label="主机名称" align="center" prop="hostName" min-width="140" />
      <el-table-column label="主机IP" align="center" prop="hostIp" width="150" />
      <el-table-column label="端口" align="center" prop="apiPort" width="80" />
      <el-table-column label="在线状态" align="center" prop="onlineStatus" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.onlineStatus === 'ONLINE' ? 'success' : 'info'">{{ scope.row.onlineStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="SDK版本" align="center" width="130">
        <template #default="scope">{{ scope.row.currentVersion || '-' }} / {{ scope.row.latestVersion || '-' }}</template>
      </el-table-column>
      <el-table-column label="容器数" align="center" prop="containerCapacity" width="90" />
      <el-table-column label="最近同步" align="center" prop="lastSyncTime" width="180">
        <template #default="scope">{{ parseTime(scope.row.lastSyncTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="320" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Connection" @click="handleTest(scope.row)" v-hasPermi="['cloud:host:test']">测试</el-button>
          <el-button link type="primary" icon="Refresh" @click="handleSync(scope.row)" v-hasPermi="['cloud:host:sync']">同步容器</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cloud:host:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cloud:host:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="hostRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="主机名称" prop="hostName">
          <el-input v-model="form.hostName" placeholder="请输入主机名称" />
        </el-form-item>
        <el-form-item label="主机IP" prop="hostIp">
          <el-input v-model="form.hostIp" placeholder="例如 192.168.137.63" />
        </el-form-item>
        <el-form-item label="SDK端口" prop="apiPort">
          <el-input-number v-model="form.apiPort" :min="1" :max="65535" controls-position="right" />
        </el-form-item>
        <el-form-item label="启用状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio value="1">启用</el-radio>
            <el-radio value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CloudHost">
import { listHost, getHost, addHost, updateHost, delHost, testHost, syncContainers } from "@/api/cloud/host"

const { proxy } = getCurrentInstance()
const hostList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    hostName: undefined,
    hostIp: undefined,
    onlineStatus: undefined
  },
  rules: {
    hostName: [{ required: true, message: "主机名称不能为空", trigger: "blur" }],
    hostIp: [{ required: true, message: "主机IP不能为空", trigger: "blur" }],
    apiPort: [{ required: true, message: "SDK端口不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listHost(queryParams.value).then(response => {
    hostList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function reset() {
  form.value = {
    hostId: undefined,
    hostName: undefined,
    hostIp: "192.168.137.63",
    apiPort: 8000,
    enabled: "1",
    remark: undefined
  }
  proxy.resetForm("hostRef")
}

function cancel() {
  open.value = false
  reset()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.hostId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增魔云腾主机"
}

function handleUpdate(row) {
  reset()
  const hostId = row?.hostId || ids.value[0]
  getHost(hostId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改魔云腾主机"
  })
}

function submitForm() {
  proxy.$refs["hostRef"].validate(valid => {
    if (!valid) return
    const request = form.value.hostId ? updateHost(form.value) : addHost(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess("保存成功")
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const hostIds = row?.hostId || ids.value
  proxy.$modal.confirm('是否确认删除主机编号为"' + hostIds + '"的数据项？').then(() => {
    return delHost(hostIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleTest(row) {
  testHost(row.hostId).then(response => {
    proxy.$modal.msgSuccess("连接成功，当前版本：" + response.data.currentVersion)
    getList()
  })
}

function handleSync(row) {
  syncContainers(row.hostId).then(response => {
    proxy.$modal.msgSuccess("同步完成，共 " + response.data + " 个容器")
    getList()
  })
}

getList()
</script>

