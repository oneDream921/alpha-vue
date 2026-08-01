import { readFileSync } from 'node:fs'

const configsPage = readFileSync('src/views/system/configs.vue', 'utf8')

describe('parameter configuration table', () => {
    it('uses the shared Ant Design table component for the main list', () => {
        expect(configsPage).not.toContain("from 'vxe-table'")
        expect(configsPage).toContain('<a-table')
        expect(configsPage).toContain('<TableColumnSetting')
        expect(configsPage).toContain(
            'storage-key="alpha-vue:table:system-configs:v1"',
        )
        expect(configsPage).toContain('configTableColumns')
        expect(configsPage).not.toContain('fixed: column.fixed')
        expect(configsPage).toContain('control="align"')
        expect(configsPage).toContain('configRowSelection')
        expect(configsPage).toContain('v-permission="\'system:config:update\'"')
        expect(configsPage).toContain('v-permission="\'system:config:delete\'"')
    })

    it('keeps selection and pagination in page-owned state', () => {
        expect(configsPage).toContain(':row-selection="configRowSelection"')
        expect(configsPage).toContain('@change=')
        expect(configsPage).toContain('changePage({ current, pageSize: size })')
        expect(configsPage).toContain(':disabled="!selectedRowKeys.length"')
        expect(configsPage).toContain('@click="removeSelected"')
        expect(configsPage).toContain(
            'selectedRowKeys.value.map((id) => configApi.delete(id))',
        )
    })
})
