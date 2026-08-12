import type { PluginOption } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import VueDevtools from 'vite-plugin-vue-devtools';
import progress from 'vite-plugin-progress';
import { setupElegantRouter } from './router';
import { setupUnocss } from './unocss';
import { setupUnplugin } from './unplugin';
import { setupHtmlPlugin } from './html';

interface VitePluginOptions {
  buildTime: string;
  mode: string;
  isTestRunner?: boolean;
}

export function setupVitePlugins(viteEnv: Env.ImportMeta, options: VitePluginOptions) {
  const { buildTime, mode, isTestRunner = false } = options;
  const plugins: PluginOption[] = [vue(), vueJsx()];

  if (isTestRunner) return plugins;

  plugins.push(
    setupElegantRouter(),
    setupUnocss(viteEnv),
    ...setupUnplugin(viteEnv),
    progress(),
    setupHtmlPlugin(buildTime)
  );

  if (mode !== 'test' && !isTestRunner) {
    plugins.splice(2, 0, VueDevtools());
  }

  return plugins;
}
