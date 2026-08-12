import { afterEach, describe, expect, it } from 'vitest';
import { getCssColor, withAlpha } from './theme';

describe('getCssColor', () => {
  afterEach(() => {
    document.documentElement.style.removeProperty('--alpha-test-color');
  });

  it('reads a color from the active document theme', () => {
    document.documentElement.style.setProperty('--alpha-test-color', 'rgb(1 2 3)');

    expect(getCssColor('--alpha-test-color')).toBe('rgb(1 2 3)');
  });

  it('returns a transparent fallback for an unset token', () => {
    expect(getCssColor('--alpha-missing-color')).toBe('transparent');
  });

  it('converts a theme rgb color to rgba for chart gradients', () => {
    expect(withAlpha('rgb(1 2 3)', 0.2)).toBe('rgba(1, 2, 3, 0.2)');
    expect(withAlpha('transparent', 0.2)).toBe('transparent');
  });
});
