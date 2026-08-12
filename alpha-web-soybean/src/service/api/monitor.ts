import { request } from '../request';
import type { PageResponse } from './system';

export interface OnlineSession {
  userId: number;
  terminalIndex: number;
  tokenSummary: string;
  clientId: string;
  deviceId?: string;
  deviceName?: string;
  ipAddress?: string;
  browser: string;
  operatingSystem: string;
  loginTime: string;
  lastActiveTime: string;
  timeoutSeconds: number;
  user?: { id: number; username: string; nickname: string; deptName?: string };
}

export const onlineApi = {
  page: (page = 1, size = 10) =>
    request<PageResponse<OnlineSession>>({ url: '/monitor/online-users', method: 'get', params: { page, size } }),
  kickout: (userId: number, terminalIndex: number) =>
    request<null>({ url: `/monitor/online-users/${userId}/sessions/${terminalIndex}`, method: 'delete' })
};

export interface RedisOverview {
  redisVersion?: string;
  uptimeSeconds?: number;
  usedMemoryBytes?: number;
  connectedClients?: number;
  managedKeyCounts: Record<string, number>;
}
export interface RedisKeyMetadata {
  key: string;
  category: string;
  type: string;
  ttlSeconds?: number;
  sizeBytes?: number;
  value?: string;
  valueTruncated: boolean;
  displayLevel: 'HIDDEN' | 'MASKED' | 'PLAIN';
}
export interface RedisKeyPage {
  records: RedisKeyMetadata[];
  nextCursor: string;
  hasMore: boolean;
}
export interface RedisKeyQuery {
  prefix: string;
  cursor: string;
  count: number;
  keyword?: string;
}
export type RedisMetricsStatus = 'DISABLED' | 'COLLECTING' | 'HEALTHY' | 'DEGRADED' | 'STALE';
export type RedisMetricValue = number | string;
export interface RedisMetricsSnapshot {
  redisVersion?: string;
  uptimeSeconds?: RedisMetricValue;
  usedMemoryBytes?: RedisMetricValue;
  usedMemoryRssBytes?: RedisMetricValue;
  usedMemoryPeakBytes?: RedisMetricValue;
  maxMemoryBytes?: RedisMetricValue;
  totalSystemMemoryBytes?: RedisMetricValue;
  memoryFragmentationRatio?: number;
  connectedClients?: RedisMetricValue;
  blockedClients?: RedisMetricValue;
  totalConnectionsReceived?: RedisMetricValue;
  rejectedConnections?: RedisMetricValue;
  totalCommandsProcessed?: RedisMetricValue;
  instantaneousOpsPerSecond?: RedisMetricValue;
  keyspaceHits?: RedisMetricValue;
  keyspaceMisses?: RedisMetricValue;
}
export interface RedisCommandMetric {
  command: string;
  calls?: RedisMetricValue;
  intervalCalls?: RedisMetricValue;
  callsPerSecond?: number;
  rejectedCalls?: RedisMetricValue;
  failedCalls?: RedisMetricValue;
  usecPerCall?: number;
}
export interface RedisMetricsTrendPoint {
  capturedAt: string;
  usedMemoryBytes?: RedisMetricValue | null;
  usedMemoryRssBytes?: RedisMetricValue | null;
  connectedClients?: RedisMetricValue | null;
  blockedClients?: RedisMetricValue | null;
  instantaneousOpsPerSecond?: RedisMetricValue | null;
  totalCommandsProcessed?: RedisMetricValue | null;
  rejectedConnections?: RedisMetricValue | null;
}
export interface RedisMetrics {
  enabled: boolean;
  status: RedisMetricsStatus;
  lastAttemptAt?: string;
  lastSuccessAt?: string;
  consecutiveFailures: number;
  sampleIntervalSeconds: number;
  retentionHours: number;
  maxSamples: number;
  current?: RedisMetricsSnapshot;
  commands: RedisCommandMetric[];
  trend: RedisMetricsTrendPoint[];
}

export const redisApi = {
  overview: () => request<RedisOverview>({ url: '/monitor/redis/overview', method: 'get' }),
  metrics: () => request<RedisMetrics>({ url: '/monitor/redis/metrics', method: 'get' }),
  keys: (params: RedisKeyQuery) => request<RedisKeyPage>({ url: '/monitor/redis/keys', method: 'get', params }),
  key: (key: string) => request<RedisKeyMetadata>({ url: '/monitor/redis/key', method: 'get', params: { key } }),
  delete: (key: string) => request<string>({ url: '/monitor/redis/key', method: 'delete', params: { key } })
};

export interface SqlLogEntry {
  id: number;
  createdAt: string;
  traceId?: string;
  statementId: string;
  sqlCommandType: string;
  tableName?: string;
  sql: string;
  elapsedMs: number;
  slow: boolean;
  resultSize?: number;
}
export interface SqlLogQuery {
  limit: number;
  type?: string;
  keyword?: string;
  slowOnly: boolean;
}
export interface SqlLogStatement {
  statementId: string;
  mapperName: string;
  methodName: string;
}
export interface SqlLogSettings {
  enabled: boolean;
  statements: SqlLogStatement[];
  excludedStatementIds: string[];
}
export interface SqlLogSettingsUpdate {
  enabled: boolean;
  excludedStatementIds: string[];
}
export const sqlMonitorApi = {
  logs: (params: SqlLogQuery) => request<SqlLogEntry[]>({ url: '/monitor/sql/logs', method: 'get', params }),
  clear: () => request<string>({ url: '/monitor/sql/logs', method: 'delete' }),
  settings: () => request<SqlLogSettings>({ url: '/monitor/sql/settings', method: 'get' }),
  updateSettings: (data: SqlLogSettingsUpdate) =>
    request<SqlLogSettings>({ url: '/monitor/sql/settings', method: 'put', data })
};

export interface OperationLog {
  id: number;
  userId?: number;
  username?: string;
  module: string;
  operation: string;
  method?: string;
  requestUri?: string;
  responseCode?: number;
  status: number;
  ipAddress?: string;
  location?: string;
  clientId?: string;
  deviceId?: string;
  deviceName?: string;
  browser?: string;
  operatingSystem?: string;
  errorCode?: number;
  durationMs?: number;
  traceId?: string;
  handlingStatus: 0 | 1 | 2;
  handledBy?: number;
  handledAt?: string;
  createdAt: string;
}
export interface OperationLogDetail {
  summary: OperationLog;
  exceptionStack?: string;
  requestSummary?: string;
  responseSummary?: string;
}
export interface LoginLog {
  id: number;
  username?: string;
  userId?: number;
  loginType: string;
  status: number;
  ipAddress?: string;
  location?: string;
  clientId?: string;
  deviceId?: string;
  deviceName?: string;
  browser?: string;
  operatingSystem?: string;
  traceId?: string;
  errorMessage?: string;
  userAgent?: string;
  message?: string;
  createdAt: string;
}
export interface OperationLogQuery {
  keyword?: string;
  status?: 0 | 1;
  handlingStatus?: 0 | 1 | 2;
}
export const logApi = {
  operations: (page = 1, size = 10, query: OperationLogQuery = {}) =>
    request<PageResponse<OperationLog>>({ url: '/logs/operations', method: 'get', params: { page, size, ...query } }),
  operationDetail: (id: number) => request<OperationLogDetail>({ url: `/logs/operations/${id}`, method: 'get' }),
  logins: (page = 1, size = 10) =>
    request<PageResponse<LoginLog>>({ url: '/logs/logins', method: 'get', params: { page, size } }),
  updateHandlingStatus: (id: number, handlingStatus: 0 | 1 | 2) =>
    request<null>({ url: `/logs/operations/${id}/handled`, method: 'put', params: { handlingStatus } })
};
