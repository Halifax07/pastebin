import apiClient from './index'

export interface SummarizeRequest {
  content: string
}

export interface SummarizeResponse {
  summary: string
  tokens: number
}

/**
 * AI 总结接口
 */
export const summarizeContent = (data: SummarizeRequest): Promise<SummarizeResponse> => {
  return apiClient.post('/ai/summarize', data)
}
