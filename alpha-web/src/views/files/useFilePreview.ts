import { computed, ref } from 'vue'
import * as XLSX from 'xlsx'

import { fileApi, type StoredFile } from '@/service/files/index'

export type PreviewKind =
    'image' | 'pdf' | 'text' | 'spreadsheet' | 'unsupported'

export type PreviewColumn = {
    title: string
    dataIndex: string
    key: string
}

export function useFilePreview() {
    const open = ref(false)
    const loading = ref(false)
    const file = ref<StoredFile | null>(null)
    const kind = ref<PreviewKind>('unsupported')
    const text = ref('')
    const url = ref('')
    const error = ref('')
    const columns = ref<PreviewColumn[]>([])
    const rows = ref<Record<string, string>[]>([])

    function isContentType(target: StoredFile, type: string) {
        return target.contentType.toLowerCase() === type
    }

    function isSpreadsheet(target: StoredFile) {
        return (
            isContentType(target, 'application/vnd.ms-excel') ||
            isContentType(
                target,
                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            )
        )
    }

    function isText(target: StoredFile) {
        return isContentType(target, 'text/plain')
    }

    function clearUrl() {
        if (url.value.startsWith('blob:')) URL.revokeObjectURL(url.value)
        url.value = ''
    }

    function close() {
        open.value = false
        clearUrl()
        file.value = null
        text.value = ''
        error.value = ''
        columns.value = []
        rows.value = []
    }

    function parseSpreadsheet(data: unknown[][]) {
        const source = data.slice(0, 200)
        const width = Math.min(
            50,
            source.reduce((maximum, row) => Math.max(maximum, row.length), 0),
        )
        const parsedColumns = Array.from({ length: width }, (_, index) => ({
            title: String(source[0]?.[index] || `列 ${index + 1}`),
            dataIndex: `column_${index}`,
            key: `column_${index}`,
        }))
        const parsedRows = source
            .slice(1)
            .map((row) =>
                Object.fromEntries(
                    parsedColumns.map((column, columnIndex) => [
                        column.dataIndex,
                        String(row[columnIndex] ?? ''),
                    ]),
                ),
            )
        return {
            columns: parsedColumns,
            rows: parsedRows.map((row, index) => ({ ...row, key: `${index}` })),
        }
    }

    async function preview(target: StoredFile) {
        close()
        file.value = target
        open.value = true
        loading.value = true
        kind.value = target.contentType.startsWith('image/')
            ? 'image'
            : isContentType(target, 'application/pdf')
              ? 'pdf'
              : isText(target)
                ? 'text'
                : isSpreadsheet(target)
                  ? 'spreadsheet'
                  : 'unsupported'
        if (kind.value === 'image') {
            url.value = target.publicUrl
            loading.value = false
            return
        }
        if (kind.value === 'unsupported') {
            loading.value = false
            return
        }
        try {
            const response = await fileApi.content(
                target.publicUrl,
                kind.value === 'text' ? 'text' : 'arraybuffer',
            )
            if (kind.value === 'pdf') {
                const blob = new Blob([response.data as ArrayBuffer], {
                    type: target.contentType,
                })
                url.value = URL.createObjectURL(blob)
            } else if (kind.value === 'text') {
                text.value = response.data as string
            } else {
                const workbook = XLSX.read(response.data as ArrayBuffer, {
                    type: 'array',
                    cellDates: true,
                })
                const firstSheet = workbook.Sheets[workbook.SheetNames[0] ?? '']
                if (!firstSheet) throw new Error('工作簿没有可预览的工作表')
                const data = XLSX.utils.sheet_to_json<unknown[]>(firstSheet, {
                    header: 1,
                    raw: false,
                    defval: '',
                })
                const parsed = parseSpreadsheet(data)
                columns.value = parsed.columns
                rows.value = parsed.rows
            }
        } catch {
            error.value = '文件内容读取失败，请下载文件后查看。'
        } finally {
            loading.value = false
        }
    }

    return {
        open,
        loading,
        file,
        kind,
        text,
        url,
        error,
        columns,
        rows,
        title: computed(() =>
            file.value ? `预览：${file.value.originalName}` : '文件预览',
        ),
        preview,
        close,
    }
}
