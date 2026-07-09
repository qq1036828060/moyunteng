<template>
  <div class="cloud-control-page" @mousemove="markActive" @click="markActive" @keydown="markActive" @touchstart="markActive" @wheel="markActive">
    <div class="control-toolbar">
      <div>
        <div class="control-title">{{ info.containerName || '云机控制' }}</div>
        <div class="control-meta">
          主机 {{ info.hostIp || '-' }} · 实例位 {{ info.indexNum || '-' }} · TCP {{ info.tcpPort || '-' }} / UDP {{ info.udpPort || '-' }}
        </div>
        <div v-if="playUrl" class="control-url">{{ playUrl }}</div>
      </div>
      <div class="control-actions">
        <el-button icon="Refresh" @click="loadWebrtc">刷新</el-button>
        <el-button icon="ChatDotRound" :loading="openingQq" @click="handleOpenQq">打开QQ</el-button>
        <el-button icon="Iphone" @click="toggleOrientation">{{ isPortrait ? '横屏' : '竖屏' }}</el-button>
        <el-button v-if="canStart" type="success" icon="VideoPlay" :loading="starting" @click="startAndReload">启动并刷新</el-button>
        <el-button type="primary" icon="FullScreen" @click="openNewWindow" :disabled="!playUrl">新窗口</el-button>
      </div>
    </div>

    <div v-loading="loading" element-loading-text="正在准备控制环境" class="player-frame">
      <el-result v-if="errorMessage" icon="warning" title="暂时无法进入控制" :sub-title="errorMessage" />
      <div v-if="playUrl" class="player-shell" :class="{ landscape: !isPortrait }">
        <iframe :src="playUrl" allow="autoplay; fullscreen; clipboard-read; clipboard-write" frameborder="0" />
      </div>
      <el-empty v-if="!playUrl && !errorMessage" description="暂无可用 WebRTC 地址" />
    </div>
  </div>
</template>

<script setup name="CloudMyContainerControl">
import { getMyContainerWebrtc, startMyContainer, stopMyContainer, openMyContainerQq } from '@/api/cloud/myContainer'

const route = useRoute()
const { proxy } = getCurrentInstance()
const loading = ref(false)
const starting = ref(false)
const openingQq = ref(false)
const info = ref({})
const playUrl = ref('')
const errorMessage = ref('')
const isPortrait = ref(true)
const autoOpenQqDone = ref(false)
const idleTimer = ref(null)
const IDLE_LIMIT = 2 * 60 * 1000
const canStart = computed(() => errorMessage.value.indexOf('未运行') !== -1)

function loadWebrtc() {
  markActive()
  loading.value = true
  errorMessage.value = ''
  playUrl.value = ''
  getMyContainerWebrtc(route.params.containerId).then(response => {
    info.value = response.data || {}
    playUrl.value = buildPlayUrl(info.value)
    if (!playUrl.value) {
      errorMessage.value = '播放器地址为空，请同步云机状态后重试'
    } else if (info.value.controlMessage) {
      proxy.$modal.msgSuccess(info.value.controlMessage)
    }
    loading.value = false
    runAutoOpenQq()
  }).catch(error => {
    errorMessage.value = error && error.message ? error.message : 'WebRTC 地址获取失败'
    loading.value = false
  })
}

function buildPlayUrl(data) {
  const path = data.playPath || data.playUrl || ''
  if (!path && !data.query) {
    return ''
  }
  const localPath = data.query ? '/webplayer/play-myt.html?' + data.query : normalizeLocalPlayerPath(path)
  const playerBase = (import.meta.env.VITE_APP_WEBRTC_PLAYER_BASE || '').replace(/\/$/, '')
  return playerBase + localPath
}

function normalizeLocalPlayerPath(path) {
  if (!path) {
    return ''
  }
  if (/^https?:\/\//i.test(path)) {
    const url = new URL(path)
    return normalizePlayerEntry(url.pathname) + url.search
  }
  const localPath = path.startsWith('/') ? path : '/' + path
  const queryIndex = localPath.indexOf('?')
  if (queryIndex === -1) {
    return normalizePlayerEntry(localPath)
  }
  return normalizePlayerEntry(localPath.slice(0, queryIndex)) + localPath.slice(queryIndex)
}

function normalizePlayerEntry(pathname) {
  if (pathname === '/webplayer/play.html' || pathname === '/webplayer/player.html') {
    return '/webplayer/play-myt.html'
  }
  return pathname
}

function runAutoOpenQq() {
  if (route.query.autoOpenQq !== '1' || autoOpenQqDone.value || !playUrl.value) {
    return
  }
  autoOpenQqDone.value = true
  setTimeout(handleOpenQq, 1200)
}

function handleOpenQq() {
  markActive()
  openingQq.value = true
  return openMyContainerQq(route.params.containerId).then(() => {
    proxy.$modal.msgSuccess('已打开 QQ，请在云机内手动完成登录')
  }).finally(() => {
    openingQq.value = false
  })
}

function startAndReload() {
  markActive()
  starting.value = true
  startMyContainer(route.params.containerId).then(() => {
    proxy.$modal.msgSuccess('启动指令已下发')
    setTimeout(loadWebrtc, 1800)
  }).finally(() => {
    starting.value = false
  })
}

function toggleOrientation() {
  markActive()
  isPortrait.value = !isPortrait.value
}

function openNewWindow() {
  markActive()
  if (!playUrl.value) {
    proxy.$modal.msgError('WebRTC 地址不存在')
    return
  }
  window.open(playUrl.value, '_blank')
}

function markActive() {
  resetIdleTimer()
}

function resetIdleTimer() {
  if (idleTimer.value) {
    window.clearTimeout(idleTimer.value)
  }
  idleTimer.value = window.setTimeout(closeIdleControl, IDLE_LIMIT)
}

function closeIdleControl() {
  playUrl.value = ''
  errorMessage.value = '长时间未操作，已自动关闭控制并释放云机资源'
  stopMyContainer(route.params.containerId).finally(() => {
    proxy.$modal.msgWarning('长时间未操作，已自动关闭控制')
  })
}

onMounted(() => {
  window.addEventListener('keydown', markActive)
  window.addEventListener('touchstart', markActive)
  window.addEventListener('mousemove', markActive)
  loadWebrtc()
})

onBeforeUnmount(() => {
  if (idleTimer.value) {
    window.clearTimeout(idleTimer.value)
  }
  window.removeEventListener('keydown', markActive)
  window.removeEventListener('touchstart', markActive)
  window.removeEventListener('mousemove', markActive)
})
</script>

<style scoped>
.cloud-control-page {
  min-height: calc(100vh - 84px);
  background: #0f172a;
}

.control-toolbar {
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #e5e7eb;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.control-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.control-meta {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 13px;
}

.control-url {
  margin-top: 2px;
  max-width: 760px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.control-actions {
  display: flex;
  gap: 8px;
}

.player-frame {
  height: calc(100vh - 148px);
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}

.player-shell {
  height: 100%;
  max-height: 100%;
  aspect-ratio: 9 / 16;
  background: #000;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.36);
}

.player-shell.landscape {
  width: 100%;
  max-width: 100%;
  height: auto;
  aspect-ratio: 16 / 9;
}

.player-shell iframe {
  display: block;
  width: 100%;
  height: 100%;
  background: #000;
}
</style>
