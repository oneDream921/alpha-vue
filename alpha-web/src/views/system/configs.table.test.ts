import { readFileSync } from 'node:fs'

const configsPage = readFileSync('src/views/system/configs.vue', 'utf8')

describe('parameter configuration VXE pilot', () => {
    it('keeps the pilot scoped to the main parameter table', () => {
        expect(configsPage).toContain("import VxeUITable from 'vxe-table'")
        expect(configsPage).toContain('<VxeTable')
        expect(configsPage).toContain('列设置')
        expect(configsPage).toContain('columnSettingsOpen')
        expect(configsPage).toContain('handleColumnDragStart')
        expect(configsPage).toContain('cycleColumnFixed')
        expect(configsPage).toContain('alpha-vue:table:system-configs:v1')
        expect(configsPage).toContain('version: CONFIG_TABLE_SETTINGS_VERSION')
        expect(configsPage).toContain('<a-table')
        expect(configsPage).toContain('v-permission="\'system:config:update\'"')
        expect(configsPage).toContain('v-permission="\'system:config:delete\'"')
    })

    it('keeps selection and pagination in page-owned state', () => {
        expect(configsPage).toContain('@checkbox-change="handleCheckboxChange"')
        expect(configsPage).toContain(
            ':checkbox-config="{ checkRowKeys: selectedRowKeys }"',
        )
        expect(configsPage).toContain('@change=')
        expect(configsPage).toContain('changePage({ current, pageSize: size })')
        expect(configsPage).toContain(':disabled="!selectedRowKeys.length"')
        expect(configsPage).toContain('@click="removeSelected"')
        expect(configsPage).toContain(
            'selectedRowKeys.value.map((id) => configApi.delete(id))',
        )
    })
})
