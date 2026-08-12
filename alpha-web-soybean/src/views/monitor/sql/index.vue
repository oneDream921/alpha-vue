<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Modal, message } from 'ant-design-vue';
import {
  ClearOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SettingOutlined
} from '@ant-design/icons-vue';
import { type SqlLogEntry, type SqlLogSettings, type SqlLogStatement, sqlMonitorApi } from '@/service/api/monitor';
import SqlLogTable from './SqlLogTable.vue';

const rows = ref<SqlLogEntry[]>([]);
const loading = ref(false);
const clearing = ref(false);
const queryError = ref('');
const sqlDetailOpen = ref(false);
const mapperDrawerOpen = ref(false);
const selectedSql = ref<SqlLogEntry>();
const settings = ref<SqlLogSettings>({
  enabled: true,
  statements: [],
  excludedStatementIds: []
});
const settingsSaving = ref(false);
const limit = ref(100);
const sqlType = ref('ALL');
const keyword = ref('');
const slowOnly = ref(false);

const typeOptions = [
  { value: 'ALL', label: '全部类型' },
  { value: 'SELECT', label: 'SELECT' },
  { value: 'INSERT', label: 'INSERT' },
  { value: 'UPDATE', label: 'UPDATE' },
  { value: 'DELETE', label: 'DELETE' },
  { value: 'UNKNOWN', label: 'UNKNOWN' }
];

const slowCount = computed(() => rows.value.filter(row => row.slow).length);
const enabledLabel = computed(() => (settings.value.enabled ? '采集中' : '已暂停'));
const enabledColor = computed(() => (settings.value.enabled ? 'green' : 'orange'));
const excludedStatementIds = computed(() => new Set(settings.value.excludedStatementIds));
const checkedStatementIds = computed(() =>
  settings.value.statements
    .map(statement => statement.statementId)
    .filter(statementId => !excludedStatementIds.value.has(statementId))
);
const mapperTreeData = computed(() => buildMapperTree(settings.value.statements));

async function loadSettings() {
  const response = await sqlMonitorApi.settings();
  if (response.error || !response.data) throw response.error;
  settings.value = response.data;
}

async function loadLogs() {
  queryError.value = '';
  loading.value = true;
  try {
    const search = keyword.value.trim();
    rows.value =
      (
        await sqlMonitorApi.logs({
          limit: limit.value,
          type: sqlType.value === 'ALL' ? undefined : sqlType.value,
          keyword: search || undefined,
          slowOnly: slowOnly.value
        })
      ).data ?? [];
  } catch {
    queryError.value = 'SQL 日志加载失败，请确认后端服务正常';
    message.error(queryError.value);
  } finally {
    loading.value = false;
  }
}

async function refresh() {
  await Promise.all([loadSettings(), loadLogs()]);
}

async function copySql(row: SqlLogEntry) {
  await window.navigator.clipboard.writeText(row.sql);
  message.success('SQL 已复制');
}

function openSqlDetail(row: SqlLogEntry) {
  selectedSql.value = row;
  sqlDetailOpen.value = true;
}

async function updateCollectionEnabled(enabled: boolean) {
  await saveSettings(enabled, settings.value.excludedStatementIds);
  message.success(enabled ? 'SQL 日志采集已开始' : 'SQL 日志采集已暂停');
}

async function updateCheckedStatements(checkedKeys: unknown) {
  const keys = checkedKeyArray(checkedKeys);
  const checked = new Set(keys.map(String));
  const excluded = settings.value.statements
    .map(statement => statement.statementId)
    .filter(statementId => !checked.has(statementId));
  await saveSettings(settings.value.enabled, excluded);
  message.success('Mapper 采集范围已更新');
}

async function saveSettings(enabled: boolean, nextExcludedStatementIds: string[]) {
  settingsSaving.value = true;
  try {
    const response = await sqlMonitorApi.updateSettings({ enabled, excludedStatementIds: nextExcludedStatementIds });
    if (response.error || !response.data) throw response.error;
    settings.value = response.data;
  } finally {
    settingsSaving.value = false;
  }
}

function clearLogs() {
  Modal.confirm({
    title: '清空 SQL 日志',
    content: '将清空当前内存中的最近 SQL 摘要，不影响数据库和审计日志。',
    okText: '清空',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      clearing.value = true;
      try {
        await sqlMonitorApi.clear();
        rows.value = [];
        message.success('SQL 日志已清空');
      } finally {
        clearing.value = false;
      }
    }
  });
}

onMounted(refresh);

function buildMapperTree(statements: SqlLogStatement[]) {
  const mapperGroups = new Map<string, SqlLogStatement[]>();
  statements.forEach(statement => {
    const group = mapperGroups.get(statement.mapperName) || [];
    group.push(statement);
    mapperGroups.set(statement.mapperName, group);
  });
  return [...mapperGroups.entries()].map(([mapperName, items]) => ({
    title: shortMapperName(mapperName),
    key: mapperName,
    children: items.map(item => ({
      title: item.methodName,
      key: item.statementId
    }))
  }));
}

function shortMapperName(mapperName: string) {
  const parts = mapperName.split('.');
  return parts.slice(-2).join('.');
}

function checkedKeyArray(value: unknown) {
  if (Array.isArray(value)) {
    return value;
  }
  if (value && typeof value === 'object' && 'checked' in value && Array.isArray(value.checked)) {
    return value.checked;
  }
  return [];
}
</script>

<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <h1>SQL 日志</h1>
        <p>
          最近 {{ rows.length }} 条，慢 SQL {{ slowCount }} 条
          <ATag :color="enabledColor">{{ enabledLabel }}</ATag>
        </p>
      </div>
      <ASpace wrap>
        <AButton
          v-permission="'monitor:sql:control'"
          :loading="settingsSaving"
          @click="updateCollectionEnabled(!settings.enabled)"
        >
          <PlayCircleOutlined v-if="!settings.enabled" />
          <PauseCircleOutlined v-else />
          {{ settings.enabled ? '暂停采集' : '开始采集' }}
        </AButton>
        <AButton v-permission="'monitor:sql:control'" @click="mapperDrawerOpen = true">
          <SettingOutlined />
          Mapper
        </AButton>
        <AButton @click="refresh">
          <ReloadOutlined />
          刷新
        </AButton>
        <AButton v-permission="'monitor:sql:clear'" danger :loading="clearing" @click="clearLogs">
          <ClearOutlined />
          清空
        </AButton>
      </ASpace>
    </div>
    <AAlert v-if="queryError" type="error" show-icon :message="queryError" class="mb-4" />
    <div class="query-bar sql-query-bar">
      <ASelect
        v-model:value="sqlType"
        class="sql-type-select"
        :options="typeOptions"
        :dropdown-match-select-width="false"
        @change="loadLogs"
      />
      <ASelect
        v-model:value="limit"
        class="sql-limit-select"
        :options="
          [50, 100, 200].map(value => ({
            value,
            label: `最近 ${value} 条`
          }))
        "
        :dropdown-match-select-width="false"
        @change="loadLogs"
      />
      <ACheckbox v-model:checked="slowOnly" @change="loadLogs">只看慢 SQL</ACheckbox>
      <AInputSearch
        v-model:value="keyword"
        class="sql-keyword-search"
        placeholder="SQL / 表名 / traceId"
        enter-button="查询"
        allow-clear
        @search="loadLogs"
      />
    </div>
    <SqlLogTable :rows="rows" :loading="loading" @inspect="openSqlDetail" @copy="copySql" />
    <AModal v-model:open="sqlDetailOpen" title="SQL 详情" :footer="null" width="820px">
      <ADescriptions v-if="selectedSql" size="small" :column="2" bordered>
        <ADescriptionsItem label="类型">{{ selectedSql.sqlCommandType }}</ADescriptionsItem>
        <ADescriptionsItem label="耗时">{{ selectedSql.elapsedMs }} ms</ADescriptionsItem>
        <ADescriptionsItem label="表名">{{ selectedSql.tableName || '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="结果">{{ selectedSql.resultSize ?? '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="traceId" :span="2">{{ selectedSql.traceId || '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="Mapper" :span="2">{{ selectedSql.statementId }}</ADescriptionsItem>
      </ADescriptions>
      <pre v-if="selectedSql" class="sql-detail-content">{{ selectedSql.sql }}</pre>
    </AModal>
    <ADrawer v-model:open="mapperDrawerOpen" title="Mapper 采集范围" placement="right" :width="420">
      <AEmpty v-if="mapperTreeData.length === 0" description="暂无已发现 Mapper" />
      <ASpin v-else :spinning="settingsSaving">
        <ATree
          checkable
          default-expand-all
          :tree-data="mapperTreeData"
          :checked-keys="checkedStatementIds"
          @check="updateCheckedStatements"
        />
      </ASpin>
    </ADrawer>
  </section>
</template>

<style scoped>
.sql-query-bar {
  flex-wrap: wrap;
  align-items: center;
}

.sql-type-select {
  width: 140px;
}

.sql-limit-select {
  width: 140px;
}

.sql-keyword-search {
  min-width: 260px;
  flex: 1;
}

.sql-preview-cell {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.sql-preview-text,
.sql-trace-cell,
.sql-statement-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sql-preview-text {
  min-width: 0;
  color: var(--alpha-text);
}

.sql-detail-content {
  margin-top: 16px;
  max-height: 360px;
  overflow: auto;
  padding: 12px;
  border-radius: var(--alpha-radius);
  background: var(--alpha-canvas);
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 767px) {
  .sql-query-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .sql-type-select,
  .sql-limit-select {
    width: min(100%, 280px);
  }

  .sql-keyword-search {
    width: 100%;
    min-width: 0;
  }
}
</style>
