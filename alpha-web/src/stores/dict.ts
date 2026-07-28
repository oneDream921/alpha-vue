import { defineStore } from 'pinia'

import { dictApi, type EnabledDictItem } from '@/service/system'

import { pinia } from './pinia'

interface DictState {
    itemsByType: Record<string, EnabledDictItem[]>
}

const pendingLoads = new Map<string, Promise<EnabledDictItem[]>>()

const usePiniaDictStore = defineStore('dict', {
    state: (): DictState => ({
        itemsByType: {},
    }),
})

const store = usePiniaDictStore(pinia)

function normalizeTypeCode(typeCode: string) {
    return typeCode.trim()
}

export const dictStore = {
    get(typeCode: string) {
        return store.itemsByType[normalizeTypeCode(typeCode)]
    },
    set(typeCode: string, items: EnabledDictItem[]) {
        store.itemsByType[normalizeTypeCode(typeCode)] = items
    },
    async load(typeCode: string, force = false) {
        const normalized = normalizeTypeCode(typeCode)
        if (!force && store.itemsByType[normalized]) {
            return store.itemsByType[normalized]
        }
        const pending = pendingLoads.get(normalized)
        if (!force && pending) {
            return pending
        }
        const request = dictApi.enabledItems(normalized).then((response) => {
            const items = response.data.data
            store.itemsByType[normalized] = items
            return items
        })
        pendingLoads.set(normalized, request)
        try {
            return await request
        } finally {
            pendingLoads.delete(normalized)
        }
    },
    remove(typeCode: string) {
        delete store.itemsByType[normalizeTypeCode(typeCode)]
        pendingLoads.delete(normalizeTypeCode(typeCode))
    },
    clear() {
        store.$reset()
        pendingLoads.clear()
    },
}
