export function getCssColor(variable: string) {
  if (typeof globalThis.getComputedStyle !== 'function') return 'transparent';

  return (
    globalThis.getComputedStyle(globalThis.document.documentElement).getPropertyValue(variable).trim() || 'transparent'
  );
}

export function withAlpha(color: string, alpha: number) {
  const channels = color.match(/^rgb\(\s*([\d.]+)\s+([\d.]+)\s+([\d.]+)\s*\)$/);
  if (!channels) return color;

  return `rgba(${channels[1]}, ${channels[2]}, ${channels[3]}, ${alpha})`;
}
