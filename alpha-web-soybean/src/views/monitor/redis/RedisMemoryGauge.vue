<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { GaugeChart, type GaugeSeriesOption } from 'echarts/charts';
import { TooltipComponent, type TooltipComponentOption } from 'echarts/components';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import type { RedisMetricValue } from '@/service/api/monitor';
import { useThemeStore } from '@/store/modules/theme';
import { getCssColor } from '@/utils/theme';

type MemoryGaugeOption = echarts.ComposeOption<GaugeSeriesOption | TooltipComponentOption>;

const props = defineProps<{
  usedBytes?: RedisMetricValue | null;
  maxBytes?: RedisMetricValue | null;
  totalSystemBytes?: RedisMetricValue | null;
  fragmentationRatio?: number | null;
}>();

echarts.use([TooltipComponent, GaugeChart, CanvasRenderer]);

const themeStore = useThemeStore();
const chartColors = computed(() => {
  const themeColors = themeStore.themeColors;
  const darkMode = themeStore.darkMode;
  return {
    info: getCssColor('--alpha-chart-info'),
    success: getCssColor('--alpha-chart-success'),
    warning: getCssColor('--alpha-chart-warning'),
    axis: getCssColor('--alpha-chart-axis'),
    text: getCssColor('--alpha-text-secondary'),
    themeKey: `${themeColors.primary}-${darkMode}`
  };
});

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

function formatBytes(value: unknown) {
  const numericValue = toFiniteNumber(value);
  if (numericValue === undefined) return '-';
  if (numericValue === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const exponent = Math.min(Math.floor(Math.log(Math.abs(numericValue)) / Math.log(1024)), units.length - 1);
  const scaledValue = numericValue / 1024 ** exponent;
  return `${new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: exponent === 0 ? 0 : 2,
    maximumFractionDigits: exponent === 0 ? 0 : 2
  }).format(scaledValue)} ${units[exponent]}`;
}

function formatPercent(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
}

const usedMemory = computed(() => toFiniteNumber(props.usedBytes));
const maxMemory = computed(() => toFiniteNumber(props.maxBytes));
const totalSystemMemory = computed(() => toFiniteNumber(props.totalSystemBytes));
const usageBaseline = computed(() => {
  if (maxMemory.value && maxMemory.value > 0) {
    return {
      bytes: maxMemory.value,
      label: formatBytes(maxMemory.value),
      source: '最大内存'
    };
  }
  if (totalSystemMemory.value && totalSystemMemory.value > 0) {
    return {
      bytes: totalSystemMemory.value,
      label: formatBytes(totalSystemMemory.value),
      source: '系统内存'
    };
  }
  return undefined;
});
const utilization = computed(() => {
  if (usedMemory.value === undefined || !usageBaseline.value) return undefined;
  return Math.min(Math.max((usedMemory.value / usageBaseline.value.bytes) * 100, 0), 100);
});
const displayPercent = computed(() => utilization.value ?? 50);
const usageLabel = computed(() =>
  utilization.value === undefined ? '未配置上限' : `${formatPercent(utilization.value)}%`
);
const maxLabel = computed(() => (maxMemory.value && maxMemory.value > 0 ? formatBytes(maxMemory.value) : '未配置'));
const baselineLabel = computed(() => usageBaseline.value?.label || '无可用分母');
const baselineSourceLabel = computed(() => usageBaseline.value?.source || '未配置');
const fragmentationLabel = computed(() => {
  const value = toFiniteNumber(props.fragmentationRatio);
  return value === undefined ? '-' : formatPercent(value);
});
const detailLabel = computed(() =>
  utilization.value === undefined ? formatBytes(usedMemory.value) : `${formatPercent(utilization.value)}%`
);
const option = computed<MemoryGaugeOption>(() => {
  const colors = chartColors.value;

  return {
    tooltip: {
      formatter:
        usageBaseline.value === undefined
          ? `内存使用: ${formatBytes(usedMemory.value)}<br/>最大内存: 未设置限制<br/>系统内存: 未采集`
          : `内存使用: ${formatBytes(usedMemory.value)}<br/>${baselineSourceLabel.value}: ${baselineLabel.value}<br/>使用率: ${usageLabel.value}`
    },
    series: [
      {
        name: '内存',
        type: 'gauge',
        min: 0,
        max: 100,
        detail: {
          formatter: detailLabel.value,
          fontSize: 16,
          fontWeight: 'bold',
          offsetCenter: [0, '70%']
        },
        data: [
          {
            value: displayPercent.value,
            name: utilization.value === undefined ? '内存使用量' : '内存使用率'
          }
        ],
        axisLine: {
          lineStyle: {
            width: 8,
            color: [
              [0.3, colors.info],
              [0.7, colors.success],
              [1, colors.warning]
            ]
          }
        },
        pointer: {
          itemStyle: {
            color: 'auto'
          }
        },
        axisTick: {
          distance: -12,
          length: 4,
          lineStyle: {
            color: colors.axis,
            width: 1
          }
        },
        splitLine: {
          distance: -18,
          length: 12,
          lineStyle: {
            color: colors.axis,
            width: 1
          }
        },
        axisLabel: {
          color: colors.text,
          distance: 25,
          fontSize: 12
        },
        title: {
          offsetCenter: [0, '90%'],
          fontSize: 14
        },
        animationDuration: 1000
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
  <section class="redis-memory-gauge" aria-label="Redis 内存信息" data-testid="redis-memory-gauge">
    <div class="redis-memory-gauge-heading">
      <h3>内存信息</h3>
      <span>使用率按 maxmemory 计算</span>
    </div>
    <div
      ref="chartRef"
      class="redis-memory-echart"
      data-testid="redis-memory-echart"
      :aria-label="`Redis 内存使用量 ${formatBytes(usedMemory)}`"
    />
    <p class="redis-memory-assistive">
      内存使用量 {{ formatBytes(usedMemory) }}；使用率 {{ usageLabel }}；最大内存 {{ maxLabel }}；计算分母
      {{ baselineSourceLabel }} {{ baselineLabel }}；碎片率
      {{ fragmentationLabel }}
    </p>
  </section>
</template>

<style scoped>
.redis-memory-gauge {
  min-width: 0;
  height: 100%;
  min-height: 460px;
  padding: 18px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: var(--alpha-radius);
  background: var(--alpha-surface);
  box-shadow: var(--alpha-shadow);
}

.redis-memory-gauge-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.redis-memory-gauge-heading h3 {
  margin: 0;
  color: var(--alpha-text);
  font-size: 15px;
  line-height: 22px;
}

.redis-memory-gauge-heading span {
  color: var(--alpha-muted);
  font-size: 12px;
}

.redis-memory-echart {
  width: 100%;
  height: 370px;
  margin-top: 10px;
  overflow: hidden;
}

.redis-memory-assistive {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

@media (max-width: 767px) {
  .redis-memory-gauge {
    padding: 12px;
  }

  .redis-memory-gauge-heading {
    flex-direction: column;
  }

  .redis-memory-echart {
    height: 320px;
  }
}
</style>
