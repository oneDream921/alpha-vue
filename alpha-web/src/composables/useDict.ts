import { computed, reactive, ref } from 'vue'

import type { EnabledDictItem } from '@/service/system/index'
import { dictStore } from '@/stores/dict'
import { defaultDictValue, dictLabel, type DictValue } from '@/utils/dicts'

export function useDict(...typeCodes: string[]) {
    const dicts = reactive<Record<string, EnabledDictItem[]>>({})
    const loading = ref(false)
    const error = ref<unknown>()

    async function load(force = false) {
        loading.value = true
        error.value = undefined
        try {
            await Promise.all(
                typeCodes.map(async (typeCode) => {
                    const normalized = typeCode.trim()
                    dicts[normalized] = await dictStore.load(normalized, force)
                }),
            )
        } catch (unknownError: unknown) {
            error.value = unknownError
            throw unknownError
        } finally {
            loading.value = false
        }
    }

    function options(typeCode: string) {
        return dicts[typeCode.trim()] ?? []
    }

    function label(typeCode: string, value: DictValue) {
        return dictLabel(options(typeCode), value)
    }

    function defaultValue(typeCode: string) {
        return defaultDictValue(options(typeCode))
    }

    void load().catch(() => undefined)

    return {
        dicts,
        loading: computed(() => loading.value),
        error: computed(() => error.value),
        load,
        options,
        label,
        defaultValue,
    }
}
