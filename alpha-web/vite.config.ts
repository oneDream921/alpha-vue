import { fileURLToPath, URL } from 'node:url'

import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig } from 'vite'

export default defineConfig({
    plugins: [
        vue(),
        tailwindcss(),
        Components({
            dts: 'src/components.d.ts',
            resolvers: [AntDesignVueResolver({ importStyle: false })],
        }),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/uploads': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (!id.includes('node_modules')) return
                    if (id.includes('vue-router')) return 'router-vendor'
                    if (id.includes('pinia')) return 'state-vendor'
                    // Ant Design Vue stays route-aware: splitting it here
                    // creates an icons <-> UI circular chunk dependency.
                    if (id.includes('/node_modules/.pnpm/vue@')) {
                        return 'vue-vendor'
                    }
                },
            },
        },
    },
    test: {
        environment: 'jsdom',
        environmentOptions: {
            jsdom: { url: 'http://localhost/' },
        },
        globals: true,
        setupFiles: './src/test/setup.ts',
    },
})
