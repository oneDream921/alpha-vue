import type { Directive } from 'vue';
import { authStore } from '@/stores/auth';

export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(element, binding) {
    const required = Array.isArray(binding.value) ? binding.value : [binding.value];
    if (!required.some(item => authStore.hasPermission(item))) element.remove();
  }
};
