import { defineConfig, presetWind4 } from 'unocss'

export default defineConfig({
    presets: [
        presetWind4({
            preflights: {
                // Ant Design Vue owns the global component reset. Enabling a
                // second reset here would change button and form control defaults.
                reset: false,
            },
        }),
    ],
    shortcuts: {
        'flex-center': 'flex items-center justify-center',
        'flex-between': 'flex items-center justify-between',
    },
})
