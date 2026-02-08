export enum AppGenStatusEnum {
  NOT_GENERATED = 'not_generated',
  GENERATING = 'generating',
  READY = 'ready',
  FAILED = 'failed',
}

export type AppGenStatusMeta = {
  value: AppGenStatusEnum
  label: string
  color: string
}

export const APP_GEN_STATUS_OPTIONS: AppGenStatusMeta[] = [
  { value: AppGenStatusEnum.NOT_GENERATED, label: '未生成', color: 'default' },
  { value: AppGenStatusEnum.GENERATING, label: '生成中', color: 'blue' },
  { value: AppGenStatusEnum.READY, label: '已完成', color: 'green' },
  { value: AppGenStatusEnum.FAILED, label: '失败', color: 'red' },
]

export const getAppGenStatusMeta = (status?: string): AppGenStatusMeta => {
  const matched = APP_GEN_STATUS_OPTIONS.find((item) => item.value === status)
  return matched ?? APP_GEN_STATUS_OPTIONS[0]
}

export const formatAppGenStatus = (status?: string) => {
  return getAppGenStatusMeta(status).label
}
