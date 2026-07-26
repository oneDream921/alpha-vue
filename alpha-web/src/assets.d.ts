declare module '*.svg' {
    const source: string
    export default source
}

declare module 'vue-cropper/dist/vue-cropper.es.js' {
    import type { DefineComponent } from 'vue'

    export const VueCropper: DefineComponent<
        Record<string, unknown>,
        Record<string, unknown>,
        unknown
    >
}
