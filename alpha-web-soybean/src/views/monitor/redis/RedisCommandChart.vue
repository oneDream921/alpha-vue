<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { PieChart, type PieSeriesOption } from 'echarts/charts';
import {
  LegendComponent,
  type LegendComponentOption,
  TooltipComponent,
  type TooltipComponentOption
} from 'echarts/components';
import * as echarts from 'echarts/core';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';
import type { RedisCommandMetric, RedisMetricValue } from '@/service/api/monitor';
import { useThemeStore } from '@/store/modules/theme';
import { getCssColor } from '@/utils/theme';

type CommandChartOption = echarts.ComposeOption<PieSeriesOption | LegendComponentOption | TooltipComponentOption>;

interface CommandRank {
  name: string;
  value: number;
  percent: number;
  color: string;
}

const props = defineProps<{
  commands: RedisCommandMetric[];
  totalCalls?: RedisMetricValue | null;
}>();

echarts.use([TooltipComponent, LegendComponent, PieChart, LabelLayout, CanvasRenderer]);

const themeStore = useThemeStore();
const palette = computed(() => ({
  key: `${themeStore.themeColors.primary}-${themeStore.darkMode}`,
  values: [
    getCssColor('--alpha-chart-primary'),
    getCssColor('--alpha-chart-info'),
    getCssColor('--alpha-chart-success'),
    getCssColor('--alpha-chart-warning'),
    getCssColor('--alpha-chart-error'),
    getCssColor('--alpha-primary-strong'),
    getCssColor('--alpha-chart-info'),
    getCssColor('--alpha-chart-success'),
    getCssColor('--alpha-chart-warning'),
    getCssColor('--alpha-chart-error'),
    getCssColor('--alpha-primary')
  ]
}));
const chartRef = ref<globalThis.HTMLElement>();
let chart: echarts.ECharts | undefined;
let resizeObserver: InstanceType<typeof globalThis.ResizeObserver> | undefined;

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

function formatPercent(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
}

const commandCalls = computed(() =>
  props.commands
    .map(item => ({
      name: item.command,
      value: toFiniteNumber(item.calls) ?? 0
    }))
    .filter(item => item.value > 0)
    .sort((left, right) => right.value - left.value)
);
const total = computed(() => {
  const totalCalls = toFiniteNumber(props.totalCalls);
  const visibleTotal = commandCalls.value.reduce((sum, item) => sum + item.value, 0);
  return Math.max(totalCalls ?? visibleTotal, visibleTotal);
});
const ranks = computed<CommandRank[]>(() => {
  if (total.value <= 0) return [];
  const rows = commandCalls.value.slice(0, 10);
  const topTotal = rows.reduce((sum, item) => sum + item.value, 0);
  const others = Math.max(total.value - topTotal, 0);
  if (others > 0) {
    rows.push({ name: '其他命令', value: others });
  }
  return rows.map((item, index) => ({
    ...item,
    percent: (item.value / total.value) * 100,
    color: palette.value.values[index % palette.value.values.length]
  }));
});
const option = computed<CommandChartOption>(() => ({
  color: palette.value.values,
  tooltip: {
    trigger: 'item',
    formatter(params) {
      const data = params as {
        name?: string;
        value?: number;
        percent?: number;
      };
      return [
        `<div style="font-weight:600;margin-bottom:5px;">${data.name || '未知命令'}</div>`,
        `<div>执行次数：<span style="font-weight:600;">${formatCount(data.value)}</span></div>`,
        `<div>占比：<span style="font-weight:600;">${formatPercent(data.percent || 0)}%</span></div>`
      ].join('');
    },
    backgroundColor: getCssColor('--alpha-surface'),
    borderColor: getCssColor('--alpha-border'),
    borderWidth: 1,
    textStyle: {
      color: getCssColor('--alpha-text')
    },
    extraCssText: `box-shadow: 0 0 10px ${getCssColor('--alpha-shadow-color')};`
  },
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 10,
    top: 20,
    bottom: 20,
    textStyle: {
      color: getCssColor('--alpha-text-secondary')
    }
  },
  series: [
    {
      name: '命令',
      type: 'pie',
      roseType: 'radius',
      radius: [15, 95],
      center: ['40%', '52%'],
      data: ranks.value.map(rank => ({
        name: rank.name,
        value: rank.value
      })),
      animationEasing: 'cubicInOut',
      animationDuration: 1000,
      itemStyle: {
        borderRadius: 10,
        borderColor: getCssColor('--alpha-surface'),
        borderWidth: 2
      },
      label: {
        formatter: '{b}: {d}%',
        color: getCssColor('--alpha-text-secondary'),
        overflow: 'truncate',
        width: 90
      },
      labelLine: {
        length: 14,
        length2: 36
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: getCssColor('--alpha-shadow-color')
        }
      }
    }
  ]
}));

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
  <section class="redis-command-chart" aria-label="Redis 命令统计" data-testid="redis-command-chart">
    <div class="redis-command-chart-heading">
      <h3>命令统计</h3>
      <span>按累计调用次数占比</span>
    </div>
    <div v-if="!ranks.length" class="redis-command-chart-empty">暂无命令统计</div>
    <div v-else class="redis-command-chart-body">
      <p class="redis-command-assistive">
        总调用 {{ formatCount(total) }}；{{
          ranks
            .map(rank => `${rank.name} ${formatCount(rank.value)} 次，占比 ${formatPercent(rank.percent)}%`)
            .join('；')
        }}
      </p>
      <div ref="chartRef" class="redis-command-echart" data-testid="redis-command-echart" />
    </div>
  </section>
</template>

<style scoped>
.redis-command-chart {
  min-width: 0;
  height: 100%;
  min-height: 460px;
  padding: 18px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: var(--alpha-surface);
  box-shadow: var(--alpha-shadow);
}

.redis-command-chart-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.redis-command-chart-heading h3 {
  margin: 0;
  color: var(--alpha-text);
  font-size: 15px;
  line-height: 22px;
}

.redis-command-chart-heading span,
.redis-command-chart-empty {
  color: var(--alpha-muted);
  font-size: 12px;
}

.redis-command-chart-body {
  position: relative;
  min-height: 390px;
  margin-top: 10px;
}

.redis-command-echart {
  min-width: 0;
  height: 390px;
}

.redis-command-chart-empty {
  display: grid;
  min-height: 360px;
  place-items: center;
}

.redis-command-assistive {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

@media (max-width: 767px) {
  .redis-command-chart {
    padding: 12px;
  }

  .redis-command-chart-heading {
    flex-direction: column;
  }

  .redis-command-echart {
    height: 320px;
  }
}
</style>
