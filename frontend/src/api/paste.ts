import apiClient from './index'
import type { CreatePasteRequest, CreatePasteResponse, PasteData } from '@/types/paste'

/**
 * 创建 Paste
 */
export const createPaste = (data: CreatePasteRequest): Promise<CreatePasteResponse> => {
  return apiClient.post('/pastes', data)
}

/**
 * 获取 Paste
 */
export const getPaste = (key: string): Promise<PasteData> => {
  return apiClient.get(`/pastes/${key}`)
}
