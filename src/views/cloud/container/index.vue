<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="主机ID" prop="hostId">
        <el-input v-model="queryParams.hostId" placeholder="请输入主机ID" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="实例位" prop="indexNum">
        <el-input-number v-model="queryParams.indexNum" :min="1" controls-position="right" style="width: 130px" />
      </el-form-item>
      <el-form-item label="分配用户ID" prop="assignedUserId">
        <el-input v-model="queryParams.assignedUserId" placeholder="用户ID" clearable style="width: 130px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="运行状态" prop="containerStatus">
        <el-select v-model="queryParams.containerStatus" placeholder="请选择" clearable style="width: 130px">
          <el-option label="running" value="running" />
          <el-option label="created" value="created" />
          <el-option label="stopped" value="stopped" />
        </el-select>
      </el-form-item>
      <el-form-item label="调度状态" prop="scheduleStatus">
        <el-select v-model="queryParams.scheduleStatus" placeholder="请选择" clearable style="width: 130px">
          <el-option label="IDLE" value="IDLE" />
          <el-option label="RUNNING" value="RUNNING" />
          <el-option label="CONTROLLING" value="CONTROLLING" />
          <el-option label="DISABLED" value="DISABLED" />
          <el-option label="MIXED" value="MIXED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="User" @click="handleAssign()" v-hasPermi="['cloud:container:assign']">按实例位分配</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="containerList">
      <el-table-column label="主机ID" align="center" prop="hostId" width="90" />
      <el-table-column label="实例位" align="center" prop="indexNum" width="90" />
      <el-table-column label="云机类型" align="center" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.androidType === 'V3' ? 'warning' : scope.row.androidType === 'MIXED' ? 'info' : 'success'">
            {{ scope.row.cloudMachineType || scope.row.androidType || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="容器数量" align="center" prop="containerCount" width="100" />
      <el-table-column label="运行中" align="center" width="100">
        <template #default="scope">{{ scope.row.runningCount || 0 }} / {{ scope.row.containerCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="运行状态" align="center" prop="containerStatus" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.containerStatus === 'running' ? 'success' : scope.row.containerStatus === 'partial' ? 'warning' : 'info'">
            {{ statusText(scope.row.containerStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="调度状态" align="center" prop="scheduleStatus" width="120" />
      <el-table-column label="分配状态" align="center" min-width="180">
        <template #default="scope">
          <el-tag v-if="!scope.row.assignedCount" type="info">未分配</el-tag>
          <span v-else-if="scope.row.assignedCount === 1">{{ scope.row.assignedUserName || '-' }}（{{ scope.row.assignedUserId }}）</span>
          <el-tag v-else type="warning">多人混合</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="User" @click="handleAssign(scope.row)" v-hasPermi="['cloud:container:assign']">分配</el-button>
          <el-button link type="primary" icon="Close" @click="handleUnassign(scope.row)" v-hasPermi="['cloud:container:assign']">取消分配</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="分配云机实例位" v-model="assignOpen" width="560px" append-to-body>
      <el-form ref="assignRef" :model="assignForm" :rules="assignRules" label-width="110px">
        <el-form-item label="主机ID" prop="hostId">
          <el-input-number v-model="assignForm.hostId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="实例位" prop="indexText">
          <el-input v-model="assignForm.indexText" placeholder="支持单个或多个，例如 1,2,3" />
        </el-form-item>
        <el-form-item label="分配用户" prop="userId">
          <el-select v-model="assignForm.userId" filterable remote reserve-keyword placeholder="输入用户名搜索" :remote-method="searchUsers" :loading="userLoading" style="width: 100%">
            <el-option v-for="user in userOptions" :key="user.userId" :label="user.userName + '（' + (user.nickName || '-') + '）'" :value="user.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAssign">确定</el-button>
          <el-button @click="assignOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CloudContainer">
import { listContainer, assignContainer, unassignContainerSlot } from "@/api/cloud/container"
import { listUser } from "@/api/system/user"

const { proxy } = getCurrentInstance()
const containerList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const assignOpen = ref(false)
const userLoading = ref(false)
const userOptions = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    hostId: undefined,
    indexNum: undefined,
    assignedUserId: undefined,
    containerStatus: undefined,
    scheduleStatus: undefined
  },
  assignForm: {
    hostId: undefined,
    indexText: undefined,
    userId: undefined
  },
  assignRules: {
    hostId: [{ required: true, message: "主机ID不能为空", trigger: "blur" }],
    indexText: [{ required: true, message: "实例位不能为空", trigger: "blur" }],
    userId: [{ required: true, message: "分配用户不能为空", trigger: "change" }]
  }
})

const { queryParams, assignForm, assignRules } = toRefs(data)

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

function statusText(status) {
  if (status === "partial") return "部分运行"
  return status || "-"
}

function parseIndexNums(text) {
  return String(text || "")
    .split(/[,，\s]+/)
    .map(item => Number(item))
    .filter(item => Number.isInteger(item) && item > 0)
}

function handleAssign(row) {
  assignForm.value = {
    hostId: row?.hostId || queryParams.value.hostId,
    indexText: row?.indexNum ? String(row.indexNum) : undefined,
    userId: row?.assignedCount === 1 ? row.assignedUserId : undefined
  }
  if (row?.assignedCount === 1 && row.assignedUserId) {
    userOptions.value = [{ userId: row.assignedUserId, userName: row.assignedUserName || row.assignedUserId, nickName: "" }]
  }
  assignOpen.value = true
}

function searchUsers(keyword) {
  if (!keyword) return
  userLoading.value = true
  listUser({ pageNum: 1, pageSize: 20, userName: keyword }).then(response => {
    userOptions.value = response.rows
    userLoading.value = false
  })
}

function submitAssign() {
  proxy.$refs["assignRef"].validate(valid => {
    if (!valid) return
    const indexNums = parseIndexNums(assignForm.value.indexText)
    if (!indexNums.length) {
      proxy.$modal.msgError("请填写有效的实例位")
      return
    }
    assignContainer({
      hostId: assignForm.value.hostId,
      indexNums,
      userId: assignForm.value.userId
    }).then(() => {
      proxy.$modal.msgSuccess("分配成功")
      assignOpen.value = false
      getList()
    })
  })
}

function handleUnassign(row) {
  proxy.$modal.confirm("确认取消主机 " + row.hostId + " 实例位 " + row.indexNum + " 的用户分配？").then(() => {
    return unassignContainerSlot({ hostId: row.hostId, indexNums: [row.indexNum] })
  }).then(() => {
    proxy.$modal.msgSuccess("操作成功")
    getList()
  }).catch(() => {})
}

getList()
</script>
