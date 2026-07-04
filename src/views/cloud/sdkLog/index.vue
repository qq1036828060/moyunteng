<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="主机ID" prop="hostId">
        <el-input v-model="queryParams.hostId" placeholder="请输入主机ID" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="接口路径" prop="apiPath">
        <el-input v-model="queryParams.apiPath" placeholder="请输入接口路径" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="结果" prop="success">
        <el-select v-model="queryParams.success" placeholder="请选择" clearable style="width: 140px">
          <el-option label="成功" value="1" />
          <el-option label="失败" value="0" />
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

    <el-table v-loading="loading" :data="logList">
      <el-table-column label="日志ID" align="center" prop="logId" width="90" />
      <el-table-column label="主机ID" align="center" prop="hostId" width="90" />
      <el-table-column label="方法" align="center" prop="httpMethod" width="90" />
      <el-table-column label="接口路径" align="center" prop="apiPath" width="180" />
      <el-table-column label="结果" align="center" prop="success" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.success === '1' ? 'success' : 'danger'">{{ scope.row.success === '1' ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="SDK code" align="center" prop="resultCode" width="100" />
      <el-table-column label="耗时(ms)" align="center" prop="costMs" width="100" />
      <el-table-column label="错误信息" align="center" prop="errorMsg" min-width="220" show-overflow-tooltip />
      <el-table-column label="请求摘要" align="center" prop="requestBody" min-width="220" show-overflow-tooltip />
      <el-table-column label="响应摘要" align="center" prop="responseBody" min-width="260" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="CloudSdkLog">
import { listSdkLog } from "@/api/cloud/sdkLog"

const { proxy } = getCurrentInstance()
const logList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    hostId: undefined,
    apiPath: undefined,
    success: undefined
  }
})

const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listSdkLog(queryParams.value).then(response => {
    logList.value = response.rows
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

getList()
</script>

