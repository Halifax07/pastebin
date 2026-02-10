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
      width="520px"
      :close-on-click-modal="false"
      :show-close="true"
      class="share-dialog"
      align-center
    >
      <template #header>
        <div class="share-dialog-header">
          <div class="success-icon">
            <el-icon :size="28"><SuccessFilled /></el-icon>
          </div>
          <div class="header-text">
            <h3>分享链接已生成</h3>
            <p>将链接发送给任何人即可查看内容</p>
          </div>
        </div>
      </template>

      <div class="share-content">
        <!-- 链接复制区域 -->
        <div class="link-section">
          <label class="section-label">
            <el-icon><Link /></el-icon>
            分享链接
          </label>
          <div class="link-copy-box">
            <div class="link-text">{{ shareUrl }}</div>
            <el-button 
              type="primary" 
              @click="copyShareLink" 
              class="copy-btn"
              :icon="CopyDocument"
            >
              复制链接
            </el-button>
          </div>
        </div>

        <!-- 信息卡片区域 -->
        <div class="info-cards">
          <!-- 密码卡片 -->
          <div v-if="settings.password" class="info-card password-card">
            <div class="card-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-label">访问密码</div>
              <div class="card-value password-value">
                <span class="password-text">{{ settings.password }}</span>
                <el-button 
                  text 
                  size="small" 
                  @click="copyPassword"
                  class="copy-password-btn"
                >
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </div>
            </div>
          </div>

          <!-- 过期时间卡片 -->
          <div v-if="settings.expireMinutes" class="info-card expire-card">
            <div class="card-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-label">有效期</div>
              <div class="card-value">{{ getExpireLabel(settings.expireMinutes) }}</div>
            </div>
          </div>

          <!-- 阅后即焚提示 -->
          <div v-if="settings.isBurnAfterReading" class="info-card burn-card">
            <div class="card-icon burn-icon">
              <span>🔥</span>
            </div>
            <div class="card-content">
              <div class="card-label">阅后即焚</div>
              <div class="card-value">查看后自动销毁</div>
            </div>
          </div>
        </div>

        <!-- 安全提示 -->
        <div class="security-tip" v-if="settings.password">
          <el-icon><InfoFilled /></el-icon>
          <span>请将密码与链接一同发送给接收者</span>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCreateNew" size="large" class="footer-btn">
            <el-icon><Plus /></el-icon>
            继续创建
          </el-button>
          <el-button type="primary" @click="handleViewPaste" size="large" class="footer-btn primary-btn">
            <el-icon><View /></el-icon>
            查看内容
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Share, Document, Clock, Lock, CopyDocument, SuccessFilled, Link, InfoFilled, Plus, View } from '@element-plus/icons-vue'
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

// 复制密码
const copyPassword = async () => {
  try {
    await navigator.clipboard.writeText(settings.password)
    ElMessage.success('密码已复制到剪贴板！')
  } catch (err) {
    const input = document.createElement('input')
    input.value = settings.password
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('密码已复制到剪贴板！')
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

/* Share Dialog - 全新设计 */
:deep(.share-dialog) {
  --el-dialog-bg-color: #2a2a2a;
  --el-border-color-lighter: #444;
  border-radius: 12px;
}

:deep(.share-dialog .el-dialog__header) {
  padding: 24px 24px 16px;
  margin-right: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

:deep(.share-dialog .el-dialog__body) {
  padding: 20px 24px;
}

:deep(.share-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* Dialog Header */
.share-dialog-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 28px 20px;
}

.success-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #67c23a;
}

.header-text h3 {
  font-size: 18px;
  font-weight: 600;
  color: #e0e0e0;
  margin: 0;
}

.header-text p {
  font-size: 14px;
  color: #a0a0a0;
  margin: 4px 0 0;
}

/* Link Section */
.link-section {
  margin-bottom: 20px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #c0c0c0;
  margin-bottom: 12px;
}

.link-copy-box {
  display: flex;
  align-items: stretch;
  background-color: #1e1e1e;
  border-radius: 6px;
  border: 1px solid #444;
  overflow: hidden;
}

.link-text {
  flex-grow: 1;
  padding: 0 12px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  color: #d4d4d4;
  display: flex;
  align-items: center;
  white-space: nowrap;
  overflow-x: auto;
}

.copy-btn {
  flex-shrink: 0;
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

/* Info Cards */
.info-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.info-card {
  background-color: #333;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-icon {
  font-size: 20px;
  color: #888;
}

.burn-icon span {
  font-size: 20px;
}

.card-content {
  line-height: 1.4;
}

.card-label {
  font-size: 13px;
  color: #a0a0a0;
  margin-bottom: 4px;
}

.card-value {
  font-size: 14px;
  font-weight: 500;
  color: #e0e0e0;
}

.password-value {
  display: flex;
  align-items: center;
  gap: 4px;
}

.copy-password-btn {
  color: #a0a0a0;
}

.security-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background-color: rgba(230, 162, 60, 0.1);
  border-radius: 6px;
  font-size: 13px;
  color: #e6a23c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.footer-btn {
  font-weight: 500;
}
</style>
