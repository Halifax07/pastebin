export interface PasteSettings {
  language: string
  isBurnAfterReading: boolean
  expireMinutes: number | null
  password: string
}

export interface PasteData {
  key: string
  content: string
  syntax: string
  isBurnAfterReading: boolean
  expireAt?: string
  isEncrypted?: boolean
}

export interface CreatePasteRequest {
  content: string
  syntax: string
  isBurnAfterReading: boolean
  expireMinutes?: number | null
}

export interface CreatePasteResponse {
  key: string
  url: string
}
