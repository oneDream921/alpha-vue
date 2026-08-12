<script setup lang="ts">
/* eslint-disable complexity */
import { computed, onMounted, ref } from 'vue';
import { message } from 'ant-design-vue';
import { ReloadOutlined } from '@ant-design/icons-vue';
import { type RedisKeyMetadata, type RedisMetrics, type RedisOverview, redisApi } from '@/service/api/monitor';
import AlphaTableCard from '@/components/AlphaTableCard.vue';
import RedisCommandChart from './RedisCommandChart.vue';
import RedisMemoryGauge from './RedisMemoryGauge.vue';
import RedisTrendChart from './RedisTrendChart.vue';
import RedisKeyTable from './RedisKeyTable.vue';

const prefix = ref('');
const pageSize = ref(50);
const keyword = ref('');
const cursor = ref('0');
const exhausted = ref(false);
const rows = ref<RedisKeyMetadata[]>([]);
const overview = ref<RedisOverview>();
const metrics = ref<RedisMetrics>();
const overviewLoading = ref(false);
const metricsLoading = ref(false);
const loading = ref(false);
const overviewError = ref('');
const metricsError = ref('');
const queryError = ref('');
const metadataOpen = ref(false);
const selected = ref<RedisKeyMetadata>();
const deleting = ref<RedisKeyMetadata>();
const deleteOpen = ref(false);
const deleteConfirmation = ref('');

const canDelete = computed(() => deleteConfirmation.value === '删除');
function displayLevelLabel(level: RedisKeyMetadata['displayLevel']) {
  return { HIDDEN: '完全隐藏', MASKED: '已脱敏', PLAIN: '明文' }[level];
}

function displayLevelColor(level: RedisKeyMetadata['displayLevel']) {
  return { HIDDEN: 'red', MASKED: 'orange', PLAIN: 'green' }[level];
}

const discoveredKeyCount = computed(() => overview.value?.managedKeyCounts?.['全部 Redis 键'] || 0);
const hasMore = computed(() => !queryError.value && !exhausted.value);
const queryScope = computed(() => (prefix.value.trim() ? `前缀 ${prefix.value.trim()}*` : '全库'));
const querySummary = computed(() => {
  const discovered = prefix.value.trim() ? '' : `，概览已发现 ${discoveredKeyCount.value} 个键`;
  return `已显示 ${rows.value.length} 条 / ${queryScope.value}${discovered}`;
});
const metricsStatus = computed(() => metrics.value?.status || 'COLLECTING');
const metricsStatusLabel = computed(() => {
  const labels: Record<string, string> = {
    DISABLED: '已关闭',
    COLLECTING: '首次采集中',
    HEALTHY: '采集中',
    DEGRADED: '采样异常',
    STALE: '数据过期'
  };
  return labels[metricsStatus.value] || '状态未知';
});
const metricsStatusColor = computed(() => {
  const colors: Record<string, string> = {
    DISABLED: 'default',
    COLLECTING: 'blue',
    HEALTHY: 'green',
    DEGRADED: 'orange',
    STALE: 'red'
  };
  return colors[metricsStatus.value] || 'default';
});
const metricsStatusDescription = computed(() => {
  if (metricsStatus.value === 'DISABLED') {
    return '增强采样已关闭，概览、受限键查询和单键删除仍可使用。';
  }
  if (metricsStatus.value === 'COLLECTING') {
    return '正在等待 Redis 指标的首次有效采样。';
  }
  if (metricsStatus.value === 'DEGRADED') {
    return '最近采样失败，页面保留最后一次成功数据。';
  }
  if (metricsStatus.value === 'STALE') {
    return '指标超过采样有效期，请检查 Redis 连接或刷新页面。';
  }
  return '每分钟采样当前实例，趋势仅保留在本进程内。';
});
const snapshot = computed(() => metrics.value?.current);
const topCommands = computed(() => metrics.value?.commands || []);
const trends = computed(() => metrics.value?.trend || []);
const redisMetricCards = computed(() => [
  {
    label: '使用内存',
    value: formatBytes(snapshot.value?.usedMemoryBytes ?? overview.value?.usedMemoryBytes),
    hint: 'Redis used_memory'
  },
  {
    label: 'RSS 内存',
    value: formatBytes(snapshot.value?.usedMemoryRssBytes),
    hint: '进程实际驻留内存'
  },
  {
    label: '连接客户端',
    value: formatCount(snapshot.value?.connectedClients ?? overview.value?.connectedClients),
    hint: '当前连接数'
  },
  {
    label: '每秒命令',
    value: formatDecimal(snapshot.value?.instantaneousOpsPerSecond),
    hint: 'Redis 即时吞吐'
  },
  {
    label: '内存碎片率',
    value: formatDecimal(snapshot.value?.memoryFragmentationRatio),
    hint: 'mem_fragmentation_ratio'
  },
  {
    label: 'Key 数量',
    value: formatCount(discoveredKeyCount.value),
    hint: queryScope.value
  }
]);
const redisBasicInfoRows = computed(() => [
  [
    {
      label: 'Redis 版本',
      value: snapshot.value?.redisVersion || overview.value?.redisVersion || '-'
    },
    {
      label: '运行时长',
      value: formatDuration(snapshot.value?.uptimeSeconds ?? overview.value?.uptimeSeconds)
    },
    { label: '采样状态', value: metricsStatusLabel.value },
    {
      label: '采样间隔',
      value: `${formatCount(metrics.value?.sampleIntervalSeconds)} 秒`
    }
  ],
  [
    {
      label: '使用内存',
      value: formatBytes(snapshot.value?.usedMemoryBytes ?? overview.value?.usedMemoryBytes)
    },
    {
      label: 'RSS 内存',
      value: formatBytes(snapshot.value?.usedMemoryRssBytes)
    },
    {
      label: '峰值内存',
      value: formatBytes(snapshot.value?.usedMemoryPeakBytes)
    },
    {
      label: '内存上限',
      value: formatMaxMemory(snapshot.value?.maxMemoryBytes)
    }
  ],
  [
    {
      label: '客户端数',
      value: formatCount(snapshot.value?.connectedClients ?? overview.value?.connectedClients)
    },
    {
      label: '阻塞连接',
      value: formatCount(snapshot.value?.blockedClients)
    },
    { label: 'Key 数量', value: formatCount(discoveredKeyCount.value) },
    {
      label: '命令总数',
      value: formatCount(snapshot.value?.totalCommandsProcessed)
    }
  ],
  [
    {
      label: '每秒命令',
      value: formatDecimal(snapshot.value?.instantaneousOpsPerSecond)
    },
    {
      label: '命中/未命中',
      value: `${formatCount(snapshot.value?.keyspaceHits)} / ${formatCount(snapshot.value?.keyspaceMisses)}`
    },
    {
      label: '最后采样',
      value: formatDateTime(metrics.value?.lastSuccessAt)
    },
    {
      label: '保留窗口',
      value: `${formatCount(metrics.value?.retentionHours)} 小时 / ${formatCount(metrics.value?.maxSamples)} 点`
    }
  ]
]);
const redisBasicInfoItems = computed(() => redisBasicInfoRows.value.flat());
const deletingImpact = computed(() => {
  const row = deleting.value;
  if (!row) return '';
  if (row.category === '验证码') {
    return '删除后对应验证码会立即失效，用户需要重新获取验证码。';
  }
  if (row.category === '登录失败窗口') {
    return '删除后会清除该账号/IP 的登录失败计数，可能解除当前登录限制。';
  }
  if (row.category === 'Sa-Token 会话') {
    return '删除后对应登录会话会立即失效，相关用户会被下线。';
  }
  return '删除后可能影响业务缓存、临时状态、队列进度或第三方集成，请确认该键可以安全移除。';
});

function toFiniteNumber(value: unknown): number | undefined {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : undefined;
  }
  if (typeof value !== 'string' || !value.trim()) return undefined;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function formatCount(value: unknown) {
  const numericValue = toFiniteNumber(value);
  if (numericValue === undefined) return '-';
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(numericValue);
}

function formatDecimal(value: unknown) {
  const numericValue = toFiniteNumber(value);
  if (numericValue === undefined) return '-';
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(numericValue);
}

function formatBytes(value: unknown) {
  const numericValue = toFiniteNumber(value);
  if (numericValue === undefined) return '-';
  if (numericValue === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const exponent = Math.min(Math.floor(Math.log(Math.abs(numericValue)) / Math.log(1024)), units.length - 1);
  const scaledValue = numericValue / 1024 ** exponent;
  return `${new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: exponent === 0 ? 0 : 2
  }).format(scaledValue)} ${units[exponent]}`;
}

function formatMaxMemory(value: unknown) {
  const numericValue = toFiniteNumber(value);
  if (numericValue === undefined) return '-';
  if (numericValue === 0) return '未配置';
  return formatBytes(numericValue);
}

function formatDuration(value: unknown) {
  const numericValue = toFiniteNumber(value);
  if (numericValue === undefined) return '-';
  const totalSeconds = Math.max(0, Math.floor(numericValue));
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  if (days > 0) return `${days} 天 ${hours} 小时`;
  if (hours > 0) return `${hours} 小时 ${minutes} 分钟`;
  return `${minutes} 分钟`;
}

function formatDateTime(value: string | undefined) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

async function loadOverview() {
  overviewLoading.value = true;
  overviewError.value = '';
  try {
    const response = await redisApi.overview();
    if (response.error || !response.data) throw response.error;
    overview.value = response.data;
  } catch {
    overviewError.value = 'Redis 概览加载失败，请确认后端服务和 Redis 连接正常';
    message.error(overviewError.value);
  } finally {
    overviewLoading.value = false;
  }
}

async function loadMetrics() {
  metricsLoading.value = true;
  metricsError.value = '';
  try {
    const response = await redisApi.metrics();
    if (response.error || !response.data) throw response.error;
    metrics.value = response.data;
  } catch {
    metricsError.value = 'Redis 指标加载失败，现有概览和键管理仍可使用';
  } finally {
    metricsLoading.value = false;
  }
}

function resetScanState() {
  rows.value = [];
  cursor.value = '0';
  exhausted.value = false;
}

async function loadKeys(append = false) {
  if (!append) {
    resetScanState();
  }
  queryError.value = '';
  loading.value = true;
  try {
    const search = keyword.value.trim();
    const response = await redisApi.keys({
      prefix: prefix.value.trim(),
      cursor: cursor.value,
      count: pageSize.value,
      keyword: search || undefined
    });
    if (response.error || !response.data) throw response.error;
    const page = response.data;
    cursor.value = page.nextCursor;
    exhausted.value = !page.hasMore;
    rows.value = append ? [...rows.value, ...page.records] : page.records;
  } catch {
    queryError.value = 'Redis 键列表加载失败，请确认后端服务和 Redis 连接正常';
    if (!append) {
      rows.value = [];
    }
    exhausted.value = true;
    message.error(queryError.value);
  } finally {
    loading.value = false;
  }
}

function changeQuery() {
  loadKeys();
}

function loadMore() {
  if (hasMore.value) {
    loadKeys(true);
  }
}

async function inspect(row: RedisKeyMetadata) {
  const response = await redisApi.key(row.key);
  if (response.error || !response.data) throw response.error;
  selected.value = response.data;
  metadataOpen.value = true;
}

function openDelete(row: RedisKeyMetadata) {
  deleting.value = row;
  deleteConfirmation.value = '';
  deleteOpen.value = true;
}

async function confirmDelete() {
  if (!deleting.value || !canDelete.value) return;
  await redisApi.delete(deleting.value.key);
  message.success('Redis 键已删除；相关会话或验证码将立即失效');
  deleting.value = undefined;
  deleteOpen.value = false;
  await loadOverview();
  await loadKeys();
}

async function refresh() {
  queryError.value = '';
  await Promise.all([loadOverview(), loadMetrics(), loadKeys()]);
}

onMounted(refresh);
</script>

<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <h1>Redis 管理</h1>
        <p>可查询全库键和值内容；删除前请确认影响</p>
      </div>
      <AButton :loading="overviewLoading || metricsLoading || loading" @click="refresh">
        <ReloadOutlined />
        刷新
      </AButton>
    </div>
    <AAlert v-if="overviewError" type="error" show-icon :message="overviewError" class="mb-4" />
    <AAlert v-if="queryError" type="error" show-icon :message="queryError" class="mb-4" />
    <AAlert v-if="metricsError" type="warning" show-icon :message="metricsError" class="mb-4" />
    <ATabs class="redis-monitor-tabs" default-active-key="overview">
      <ATabPane key="overview" tab="运行概览">
        <section class="redis-panel" aria-labelledby="redis-overview-title">
          <div class="redis-panel-heading">
            <div>
              <h2 id="redis-overview-title">Redis 运行概览</h2>
              <p>{{ metricsStatusDescription }}</p>
            </div>
            <ATag :color="metricsStatusColor" data-testid="redis-metrics-status">
              {{ metricsStatusLabel }}
            </ATag>
          </div>
          <ASpin :spinning="metricsLoading">
            <div class="redis-kpi-grid">
              <div v-for="card in redisMetricCards" :key="card.label" class="redis-kpi-card">
                <span class="redis-kpi-label" :title="card.label">{{ card.label }}</span>
                <strong class="redis-kpi-value" :title="card.value">{{ card.value }}</strong>
                <span class="redis-kpi-hint" :title="card.hint">{{ card.hint }}</span>
              </div>
            </div>
            <div class="redis-basic-info" aria-label="Redis 基本信息">
              <div class="redis-subheading">
                <h3>Redis 基本信息</h3>
                <span>仅展示白名单运行字段</span>
              </div>
              <div class="redis-basic-info-grid">
                <div v-for="item in redisBasicInfoItems" :key="item.label" class="redis-basic-info-item">
                  <span class="redis-basic-info-label" :title="item.label">{{ item.label }}</span>
                  <strong class="redis-basic-info-value" :title="item.value">{{ item.value }}</strong>
                </div>
              </div>
            </div>
            <template v-if="metricsStatus !== 'DISABLED'">
              <div class="redis-chart-grid">
                <RedisCommandChart :commands="topCommands" :total-calls="snapshot?.totalCommandsProcessed" />
                <RedisMemoryGauge
                  :used-bytes="snapshot?.usedMemoryBytes"
                  :max-bytes="snapshot?.maxMemoryBytes"
                  :total-system-bytes="snapshot?.totalSystemMemoryBytes"
                  :fragmentation-ratio="snapshot?.memoryFragmentationRatio"
                />
              </div>
              <div class="redis-command-section">
                <div class="redis-subheading">
                  <h3>Top 10 命令</h3>
                  <span>仅展示安全命令名与统计摘要</span>
                </div>
                <AlphaTableCard :loading="metricsLoading">
                  <ATable
                    row-key="command"
                    :data-source="topCommands"
                    :pagination="false"
                    :scroll="{ x: 760 }"
                    size="small"
                  >
                    <ATableColumn title="命令" data-index="command" width="180" />
                    <ATableColumn title="调用次数" width="130">
                      <template #default="{ record }">
                        {{ formatCount(record.calls) }}
                      </template>
                    </ATableColumn>
                    <ATableColumn title="每秒调用" width="130">
                      <template #default="{ record }">
                        {{ formatDecimal(record.callsPerSecond) }}
                      </template>
                    </ATableColumn>
                    <ATableColumn title="失败次数" width="130">
                      <template #default="{ record }">
                        {{ formatCount(record.failedCalls) }}
                      </template>
                    </ATableColumn>
                    <ATableColumn title="平均耗时（微秒）" width="180">
                      <template #default="{ record }">
                        {{ formatDecimal(record.usecPerCall) }}
                      </template>
                    </ATableColumn>
                  </ATable>
                </AlphaTableCard>
              </div>
            </template>
            <AEmpty v-else description="增强指标已关闭，命令统计和内存图表不可用" />
          </ASpin>
        </section>
      </ATabPane>
      <ATabPane key="trends" tab="趋势分析">
        <section class="redis-panel" aria-labelledby="redis-trend-title">
          <div class="redis-panel-heading">
            <div>
              <h2 id="redis-trend-title">Redis 趋势分析</h2>
              <p>按采样时间展示内存、吞吐与连接变化</p>
            </div>
          </div>
          <ASpin :spinning="metricsLoading">
            <AEmpty v-if="metricsStatus === 'DISABLED'" description="增强指标已关闭，暂无趋势数据" />
            <div v-else-if="trends.length" class="redis-trend-grid">
              <RedisTrendChart title="内存趋势" metric="usedMemoryBytes" unit=" 字节" :points="trends" />
              <RedisTrendChart title="命令吞吐趋势" metric="instantaneousOpsPerSecond" unit=" 次/秒" :points="trends" />
              <RedisTrendChart title="连接趋势" metric="connectedClients" unit=" 个" :points="trends" />
            </div>
            <AEmpty v-else description="暂无趋势数据，等待至少一次成功采样" />
          </ASpin>
        </section>
      </ATabPane>
      <ATabPane key="keys" tab="键管理">
        <section class="redis-panel" aria-labelledby="redis-keys-title">
          <div class="redis-panel-heading">
            <div>
              <h2 id="redis-keys-title">Redis 键管理</h2>
              <p>{{ querySummary }}</p>
            </div>
          </div>
          <div class="query-bar redis-query-bar">
            <AInputSearch
              v-model:value="prefix"
              class="redis-prefix-search"
              placeholder="前缀筛选（可选，例如 satoken:）"
              enter-button="筛选"
              allow-clear
              @search="changeQuery"
            />
            <AInputSearch
              v-model:value="keyword"
              class="redis-keyword-search"
              placeholder="键名关键词"
              enter-button="查询"
              allow-clear
              @search="changeQuery"
            />
            <ASelect
              v-model:value="pageSize"
              class="redis-page-size-select"
              :options="
                [20, 50, 100].map(value => ({
                  value,
                  label: `每次加载 ${value} 条`
                }))
              "
              :dropdown-match-select-width="false"
              popup-class-name="redis-select-popup"
              @change="changeQuery"
            />
          </div>
          <AlphaTableCard :loading="loading">
            <RedisKeyTable :rows="rows" :loading="loading" @inspect="inspect" @remove="openDelete" />
          </AlphaTableCard>
          <div class="redis-result-bar">
            <span>{{ querySummary }}</span>
            <AButton :loading="loading" :disabled="!hasMore" @click="loadMore">加载更多</AButton>
          </div>
        </section>
      </ATabPane>
    </ATabs>
    <ADrawer v-model:open="metadataOpen" title="Redis 键元数据" placement="right">
      <ADescriptions v-if="selected" :column="1" bordered>
        <ADescriptionsItem label="键名">{{ selected.key }}</ADescriptionsItem>
        <ADescriptionsItem label="分类">{{ selected.category }}</ADescriptionsItem>
        <ADescriptionsItem label="类型">{{ selected.type }}</ADescriptionsItem>
        <ADescriptionsItem label="TTL">{{ selected.ttlSeconds }}</ADescriptionsItem>
        <ADescriptionsItem label="值">
          <ATooltip :title="selected.displayLevel === 'HIDDEN' ? undefined : selected.value || ''">
            <pre class="redis-value-preview">{{
              selected.displayLevel === 'HIDDEN' ? '敏感数据已隐藏' : selected.value || '-'
            }}</pre>
          </ATooltip>
          <ATag :color="displayLevelColor(selected.displayLevel)">
            {{ displayLevelLabel(selected.displayLevel) }}
          </ATag>
        </ADescriptionsItem>
      </ADescriptions>
    </ADrawer>
    <AModal
      v-model:open="deleteOpen"
      title="确认删除 Redis 键"
      ok-text="确认删除"
      ok-type="danger"
      :ok-button-props="{ disabled: !canDelete }"
      @ok="confirmDelete"
    >
      <p>
        {{ deletingImpact }}
      </p>
      <p>请输入“删除”确认：</p>
      <AInput v-model:value="deleteConfirmation" autocomplete="off" />
    </AModal>
  </section>
</template>

<style scoped>
.redis-monitor-tabs {
  margin-top: 8px;
}

.redis-panel {
  margin-bottom: 24px;
  padding: 20px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: var(--alpha-surface);
  box-shadow: var(--alpha-shadow);
}

.redis-panel-heading,
.redis-subheading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.redis-panel-heading {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--alpha-border-soft);
}

.redis-panel-heading h2,
.redis-subheading h3 {
  margin: 0;
  color: var(--alpha-text);
  font-size: 18px;
  line-height: 26px;
}

.redis-panel-heading p,
.redis-subheading span {
  margin: 4px 0 0;
  color: var(--alpha-muted);
  font-size: 13px;
  line-height: 20px;
}

.redis-kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.redis-kpi-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: linear-gradient(180deg, var(--alpha-surface) 0%, var(--alpha-canvas) 100%);
}

.redis-kpi-label,
.redis-kpi-hint {
  display: block;
  overflow: hidden;
  color: var(--alpha-muted);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.redis-kpi-value {
  display: block;
  margin: 6px 0 2px;
  overflow: hidden;
  color: var(--alpha-text);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
  line-height: 28px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.redis-basic-info {
  min-width: 0;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: var(--alpha-canvas);
}

.redis-basic-info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.redis-basic-info-item {
  display: flex;
  min-width: 0;
  min-height: 68px;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  padding: 11px 13px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: 8px;
  background: var(--alpha-surface);
}

.redis-basic-info-label,
.redis-basic-info-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.redis-basic-info-label {
  color: var(--alpha-muted);
  font-size: 12px;
  line-height: 18px;
}

.redis-basic-info-value {
  color: var(--alpha-text);
  font-size: 15px;
  line-height: 22px;
  font-variant-numeric: tabular-nums;
}

.redis-chart-grid {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
}

.redis-trend-grid {
  display: grid;
  gap: 16px;
}

.redis-command-section {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: var(--alpha-canvas);
}

.redis-subheading {
  align-items: baseline;
  margin-bottom: 10px;
}

.redis-result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  color: var(--alpha-muted);
}

.redis-key-cell,
.redis-value-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.redis-value-preview {
  max-width: 100%;
  max-height: 360px;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 1023px) {
  .redis-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .redis-chart-grid,
  .redis-trend-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .redis-panel {
    padding: 14px;
  }

  .redis-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .redis-kpi-card,
  .redis-basic-info,
  .redis-command-section {
    padding: 12px;
  }

  .redis-panel-heading,
  .redis-subheading,
  .redis-result-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .redis-basic-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
