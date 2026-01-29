<template>
  <div class="home-container">
    <!-- Header -->
    <div class="header">
      <div class="header-left">
        <div class="logo-wrapper">
          <span class="logo-icon">🔐</span>
          <h1 class="logo">Secure Pastebin</h1>
        </div>
        <span class="tagline">安全 · 加密 · 私密分享</span>
      </div>
      <div class="header-right">
        <el-button type="primary" size="default" @click="handleSave" :loading="saving" class="save-btn">
          <el-icon><Share /></el-icon>
          <span>保存并分享</span>
        </el-button>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar-panel">
      <div class="toolbar-scroll">
        <div class="setting-item">
          <el-icon class="setting-icon"><Document /></el-icon>
          <label>语言</label>
          <el-select v-model="settings.language" placeholder="选择语言" class="setting-select" size="default">
            <el-option
              v-for="lang in LANGUAGE_OPTIONS"
              :key="lang.value"
              :label="lang.label"
              :value="lang.value"
            />
          </el-select>
        </div>

        <div class="divider"></div>

        <div class="setting-item">
          <el-icon class="setting-icon"><Clock /></el-icon>
          <label>过期时间</label>
          <el-select v-model="settings.expireMinutes" placeholder="选择过期时间" class="setting-select" size="default">
            <el-option
              v-for="option in EXPIRE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <div class="divider"></div>

        <div class="setting-item">
          <el-icon class="setting-icon"><Lock /></el-icon>
          <label>密码保护</label>
          <el-input
            v-model="settings.password"
            placeholder="可选，留空公开"
            class="setting-input"
            type="password"
            show-password
            clearable
            size="default"
          />
        </div>

        <div class="burn-switch-wrapper">
          <el-tooltip content="内容被查看一次后自动销毁" placement="bottom">
            <el-switch
              v-model="settings.isBurnAfterReading"
              active-text="阅后即焚"
              inactive-text=""
              inline-prompt
              style="--el-switch-on-color: #f56c6c;"
            />
          </el-tooltip>
        </div>
      </div>
    </div>

    <!-- Editor -->
    <div class="editor-wrapper">
      <div class="editor-container">
        <vue-monaco-editor
          v-model:value="code"
          :language="settings.language"
          :options="editorOptions"
          theme="vs-dark"
          @mount="handleEditorMount"
        />
      </div>
    </div>

    <!-- Share Dialog -->
    <el-dialog
      v-model="showShareDialog"
      title="🎉 分享链接已生成"
      width="560px"
      :close-on-click-modal="false"
      class="share-dialog"
      align-center
    >
      <div class="share-content">
        <p class="share-tip">将以下链接发送给任何人，即可查看内容：</p>
        
        <div class="share-link-box">
          <el-input
            v-model="shareUrl"
            readonly
            size="large"
            class="share-input"
          >
            <template #append>
              <el-button @click="copyShareLink" type="primary">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </template>
          </el-input>
        </div>

        <div class="share-info">
          <div v-if="settings.password" class="info-item warning">
            <el-icon><Lock /></el-icon>
            <span>内容已加密，查看密码：<strong class="highlight-text">{{ settings.password }}</strong></span>
          </div>
          <div v-if="settings.isBurnAfterReading" class="info-item danger">
            <span>🔥</span>
            <span>阅后即焚已开启，被查看后将自动销毁</span>
          </div>
          <div v-if="settings.expireMinutes" class="info-item">
            <el-icon><Clock /></el-icon>
            <span>链接将在 {{ getExpireLabel(settings.expireMinutes) }} 后过期</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="handleCreateNew" size="large">
          继续创建
        </el-button>
        <el-button type="primary" @click="handleViewPaste" size="large">
          查看内容
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Share, Document, Clock, Lock, CopyDocument } from '@element-plus/icons-vue'
import { LANGUAGE_OPTIONS, EXPIRE_OPTIONS } from '@/utils/constants'
import { encrypt } from '@/utils/crypto'
import { generateShareUrl } from '@/utils/config'
import { createPaste } from '@/api/paste'
import type { PasteSettings } from '@/types/paste'

const router = useRouter()

// Monaco Editor
const code = ref('')
const saving = ref(false)
const showShareDialog = ref(false)
const shareUrl = ref('')
const savedKey = ref('')

const settings = reactive<PasteSettings>({
  language: 'plaintext',
  isBurnAfterReading: false,
  expireMinutes: null,
  password: ''
})

const editorOptions = {
  fontSize: 14,
  minimap: { enabled: true },
  automaticLayout: true,
  scrollBeyondLastLine: false,
  tabSize: 2,
  wordWrap: 'on',
  padding: { top: 16, bottom: 16 },
  lineNumbers: 'on',
  renderLineHighlight: 'all',
  cursorBlinking: 'smooth',
  smoothScrolling: true,
}

const handleEditorMount = (editor: any) => {
  // 编辑器挂载后可以进行额外配置
  editor.focus()
}

const handleSave = async () => {
  if (!code.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  saving.value = true
  
  try {
    let contentToSave = code.value

    // 如果设置了密码，进行客户端加密
    if (settings.password.trim()) {
      try {
        contentToSave = encrypt(code.value, settings.password)
      } catch (error: any) {
        ElMessage.error(error.message || '加密失败')
        saving.value = false
        return
      }
    }

    // 调用后端 API 保存
    const response = await createPaste({
      content: contentToSave,
      syntax: settings.language,
      isBurnAfterReading: settings.isBurnAfterReading,
      expireMinutes: settings.expireMinutes
    })

    // 生成完整的分享链接
    savedKey.value = response.key
    shareUrl.value = generateShareUrl(response.key)
    
    // 显示分享弹窗
    showShareDialog.value = true
    ElMessage.success('🎉 保存成功！')
    
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 复制分享链接
const copyShareLink = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('链接已复制到剪贴板！')
  } catch (err) {
    // 降级方案
    const input = document.createElement('input')
    input.value = shareUrl.value
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('链接已复制到剪贴板！')
  }
}

// 查看内容
const handleViewPaste = () => {
  showShareDialog.value = false
  router.push(`/${savedKey.value}`)
}

// 继续创建新内容
const handleCreateNew = () => {
  showShareDialog.value = false
  code.value = ''
  settings.language = 'plaintext'
  settings.isBurnAfterReading = false
  settings.expireMinutes = null
  settings.password = ''
}

// 获取过期时间标签
const getExpireLabel = (minutes: number) => {
  const option = EXPIRE_OPTIONS.find(opt => opt.value === minutes)
  return option ? option.label : `${minutes} 分钟`
}
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #0d1117; /* GitHub Dark Dimmed 背景色 */
  color: #c9d1d9;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background-color: #161b22;
  border-bottom: 1px solid #30363d;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  font-size: 24px;
}

.logo {
  font-size: 20px;
  font-weight: 600;
  color: #c9d1d9;
  margin: 0;
  letter-spacing: 0.5px;
}

.tagline {
  color: #8b949e;
  font-size: 13px;
  padding-left: 16px;
  border-left: 1px solid #30363d;
  display: none; /* 小屏幕隐藏 */
}

@media (min-width: 768px) {
  .tagline {
    display: inline-block;
  }
}

.save-btn {
  background-color: #238636;
  border: 1px solid rgba(240, 246, 252, 0.1);
  color: #ffffff;
  font-weight: 500;
  transition: 0.2s;
}

.save-btn:hover {
  background-color: #2ea043;
}

/* Toolbar Panel */
.toolbar-panel {
  background-color: #0d1117;
  border-bottom: 1px solid #30363d;
  padding: 8px 16px; /* 减小内边距 */
}

.toolbar-scroll {
  display: flex;
  align-items: center;
  gap: 16px; /* 减小间距 */
  overflow-x: auto;
  white-space: nowrap;
  padding-bottom: 4px; /* 为滚动条预留一点空间 */
}

/* 隐藏滚动条但保留功能 */
.toolbar-scroll::-webkit-scrollbar {
  height: 4px;
}
.toolbar-scroll::-webkit-scrollbar-thumb {
  background-color: #30363d;
  border-radius: 4px;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.setting-icon {
  color: #8b949e;
  font-size: 16px;
}

.setting-item label {
  color: #8b949e;
  font-size: 13px; /* 字体改小 */
  font-weight: 500;
}

.divider {
  width: 1px;
  height: 20px;
  background-color: #30363d;
  margin: 0 4px;
}

.setting-select {
  width: 140px; /* 稍微窄一点 */
}

.setting-input {
  width: 160px;
}

.burn-switch-wrapper {
  margin-left: auto; /* 靠右 */
  display: flex;
  align-items: center;
}

/* Element Plus Overrides for Toolbar */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) { /* Element Plus 2.5+ uses select__wrapper */
  background-color: #0d1117 !important;
  box-shadow: 0 0 0 1px #30363d inset !important;
  transition: box-shadow 0.2s;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #58a6ff inset !important;
}

:deep(.el-input__inner) {
  color: #c9d1d9 !important;
  font-size: 13px;
}

/* Editor Area */
.editor-wrapper {
  flex: 1;
  padding: 0; /* 移除 padding，让编辑器全屏 */
  overflow: hidden;
  position: relative;
}

.editor-container {
  height: 100%;
  width: 100%;
}

/* Share Dialog */
:deep(.share-dialog .el-dialog) {
  background-color: #161b22;
  border: 1px solid #30363d;
  border-radius: 6px;
}

:deep(.share-dialog .el-dialog__header) {
  border-bottom: 1px solid #30363d;
  margin-right: 0;
  padding: 16px;
}

:deep(.share-dialog .el-dialog__title) {
  color: #c9d1d9;
  font-size: 16px;
}

:deep(.share-dialog .el-dialog__body) {
  padding: 24px;
  color: #c9d1d9;
}

:deep(.share-dialog .el-dialog__footer) {
  border-top: 1px solid #30363d;
  padding: 16px;
}

.info-item {
  background-color: rgba(65, 132, 228, 0.1);
  border: 1px solid rgba(65, 132, 228, 0.2);
  color: #c9d1d9;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item.warning {
  background-color: rgba(187, 128, 9, 0.15);
  border-color: rgba(187, 128, 9, 0.4);
  color: #e3b341;
}

.info-item.danger {
  background-color: rgba(248, 81, 73, 0.15);
  border-color: rgba(248, 81, 73, 0.4);
  color: #f85149;
}

.highlight-text {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
