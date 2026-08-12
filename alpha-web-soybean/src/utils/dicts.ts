import type { EnabledDictItem } from '@/service/api/system';

export type DictValue = string | number | boolean | Array<string | number | boolean> | null | undefined;

export interface DictLabelOptions {
  separator?: string;
  showValue?: boolean;
}

export function dictValueTokens(value: DictValue, separator = ',') {
  if (value === null || typeof value === 'undefined' || value === '') {
    return [];
  }
  if (Array.isArray(value)) {
    return value.map(item => String(item));
  }
  return String(value)
    .split(separator)
    .map(item => item.trim())
    .filter(Boolean);
}

export function dictLabels(
  options: EnabledDictItem[],
  value: DictValue,
  { separator = ',', showValue = true }: DictLabelOptions = {}
) {
  const labelsByValue = new Map(options.map(option => [option.value, option.label]));
  return dictValueTokens(value, separator)
    .map(token => labelsByValue.get(token) ?? (showValue ? token : ''))
    .filter(Boolean);
}

export function dictLabel(options: EnabledDictItem[], value: DictValue, config?: DictLabelOptions) {
  return dictLabels(options, value, config).join(config?.separator ?? ',');
}

export function defaultDictValue(options: EnabledDictItem[]) {
  return options.find(option => option.isDefault === 1)?.value;
}
