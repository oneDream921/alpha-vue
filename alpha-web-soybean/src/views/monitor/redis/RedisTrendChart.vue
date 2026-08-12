<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { LineChart, type LineSeriesOption } from 'echarts/charts';
import {
  GridComponent,
  type GridComponentOption,
  TooltipComponent,
  type TooltipComponentOption
} from 'echarts/components';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import type { RedisMetricsTrendPoint } from '@/service/api/monitor';
import { useThemeStore } from '@/store/modules/theme';
import { getCssColor, withAlpha } from '@/utils/theme';

type TrendMetric =
  | 'usedMemoryBytes'
  | 'usedMemoryRssBytes'
  | 'instantaneousOpsPerSecond'
  | 'connectedClients'
  | 'blockedClients';

type TrendChartOption = echarts.ComposeOption<LineSeriesOption | GridComponentOption | TooltipComponentOption>;

interface TrendValuePoint {
  capturedAt: string;
  label: string;
  value: number;
}

function toFiniteNumber(value: unknown): number | undefined {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : undefined;
  }
  if (typeof value !== 'string' || !value.trim()) return undefined;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

const props = withDefaults(
  defineProps<{
    title: string;
    points: RedisMetricsTrendPoint[];
    metric: TrendMetric;
    color?: string;
    unit?: string;
  }>(),
  {
    color: '',
    unit: ''
  }
);

const themeStore = useThemeStore();
const chartColors = computed(() => {
  const themeColors = themeStore.themeColors;
  const darkMode = themeStore.darkMode;
  return {
    surface: getCssColor('--alpha-surface'),
    border: getCssColor('--alpha-border'),
    text: getCssColor('--alpha-text'),
    axis: getCssColor('--alpha-chart-axis'),
    grid: getCssColor('--alpha-chart-grid'),
    shadow: getCssColor('--alpha-shadow-color'),
    line: props.color || getCssColor('--alpha-chart-primary'),
    themeKey: `${themeColors.primary}-${darkMode}`
  };
});

echarts.use([TooltipComponent, GridComponent, LineChart, CanvasRenderer]);

const chartRef = ref<globalThis.HTMLElement>();
let chart: echarts.ECharts | undefined;
let resizeObserver: InstanceType<typeof globalThis.ResizeObserver> | undefined;

function formatValue(value: number) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 }).format(value);
}

function formatTimestamp(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

const validPoints = computed<TrendValuePoint[]>(() =>
  props.points
    .map(point => {
      const value = toFiniteNumber(point[props.metric]);
      if (value === undefined) return undefined;
      return {
        capturedAt: point.capturedAt,
        label: formatTimestamp(point.capturedAt),
        value
      };
    })
    .filter((item): item is TrendValuePoint => Boolean(item))
);

const summary = computed(() => {
  const last = validPoints.value.at(-1);
  if (!last) return '暂无可用采样数据';
  return `当前 ${formatValue(last.value)}${props.unit}`;
});

const chartId = computed(() => `redis-trend-${props.metric.replace(/([A-Z])/g, '-$1').toLowerCase()}`);

const option = computed<TrendChartOption>(() => {
  const colors = chartColors.value;

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: colors.surface,
      borderColor: colors.border,
      borderWidth: 1,
      textStyle: {
        color: colors.text
      },
      extraCssText: `box-shadow: 0 0 10px ${colors.shadow};`,
      formatter(params) {
        const rows = Array.isArray(params) ? params : [params];
        return rows
          .map(item => {
            const point = validPoints.value[item.dataIndex || 0];
            const value = typeof item.value === 'number' ? item.value : toFiniteNumber(item.value);
            return [
              `<div style="font-weight:600;margin-bottom:5px;">${point?.label || ''}</div>`,
              `<div>${props.title}：<span style="font-weight:600;">${value === undefined ? '-' : formatValue(value)}${props.unit}</span></div>`
            ].join('');
          })
          .join('');
      }
    },
    grid: {
      top: 28,
      right: 24,
      bottom: 42,
      left: 72,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: validPoints.value.map(point => point.label),
      axisLabel: {
        color: colors.axis
      },
      axisLine: {
        lineStyle: {
          color: colors.grid
        }
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: {
        color: colors.axis,
        formatter(value: number) {
          return formatValue(value);
        }
      },
      splitLine: {
        lineStyle: {
          color: colors.grid,
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: props.title,
        type: 'line',
        data: validPoints.value.map(point => point.value),
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        showSymbol: validPoints.value.length <= 36,
        connectNulls: false,
        lineStyle: {
          width: 3,
          color: colors.line
        },
        itemStyle: {
          color: colors.line,
          borderColor: colors.surface,
          borderWidth: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: withAlpha(colors.line, 0.2) },
              { offset: 1, color: withAlpha(colors.line, 0) }
            ]
          }
        },
        animationDuration: 800
      }
    ]
  };
});

function updateChart() {
  if (!chart) return;
  chart.setOption(option.value, true);
}

function hasChartSize(element: globalThis.HTMLElement) {
  return element.clientWidth > 0 && element.clientHeight > 0;
}

async function renderChart() {
  await nextTick();
  if (!chartRef.value) return;
  if (!resizeObserver && 'ResizeObserver' in globalThis) {
    resizeObserver = new globalThis.ResizeObserver(() => {
      if (!chartRef.value || !hasChartSize(chartRef.value)) return;
      if (!chart) {
        renderChart();
        return;
      }
      chart.resize();
    });
    resizeObserver.observe(chartRef.value);
  }
  if (chart || !hasChartSize(chartRef.value)) return;
  chart = echarts.init(chartRef.value);
  updateChart();
}

watch(option, () => {
  if (!chart) {
    renderChart();
    return;
  }
  updateChart();
});

onMounted(renderChart);

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chart?.dispose();
  chart = undefined;
});
</script>

<template>
  <section class="redis-trend-chart" :aria-labelledby="chartId">
    <div class="redis-trend-heading">
      <div>
        <h3 :id="chartId">{{ title }}</h3>
        <p>{{ summary }}</p>
      </div>
      <span class="redis-trend-count">{{ validPoints.length }} 个有效点</span>
    </div>
    <div v-if="!validPoints.length" class="redis-trend-empty">
      <span>{{ summary }}</span>
    </div>
    <template v-else>
      <p class="redis-trend-assistive">
        {{ validPoints.map(point => `${point.label} ${formatValue(point.value)}${unit}`).join('；') }}
      </p>
      <div
        ref="chartRef"
        class="redis-trend-echart"
        data-testid="redis-trend-echart"
        :aria-label="`${title}，${summary}`"
      />
    </template>
  </section>
</template>

<style scoped>
.redis-trend-chart {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: var(--alpha-surface);
}

.redis-trend-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 48px;
}

.redis-trend-heading h3 {
  margin: 0;
  color: var(--alpha-text);
  font-size: 15px;
  line-height: 22px;
}

.redis-trend-heading p {
  margin: 4px 0 0;
  color: var(--alpha-muted);
  font-size: 12px;
}

.redis-trend-count {
  flex: none;
  color: var(--alpha-muted);
  font-size: 12px;
  white-space: nowrap;
}

.redis-trend-echart {
  width: 100%;
  height: 320px;
  margin-top: 12px;
}

.redis-trend-empty {
  display: grid;
  min-height: 300px;
  place-items: center;
  color: var(--alpha-muted);
  font-size: 13px;
}

.redis-trend-assistive {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

@media (max-width: 767px) {
  .redis-trend-chart {
    padding: 12px;
  }

  .redis-trend-echart {
    height: 260px;
  }
}
</style>
