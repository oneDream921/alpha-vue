import { http, type ApiResponse } from '../http'
import type { PageResponse } from '../system'

export interface StoredFile {
    id: number
    storageProvider: string
    originalName: string
    contentType: string
    sizeBytes: number
    publicUrl: string
    uploaderName?: string
    createdAt: string
}

export const fileApi = {
    page: (page = 1, size = 10) =>
        http.get<ApiResponse<PageResponse<StoredFile>>>('/files', {
            params: { page, size },
        }),
    upload: (file: File) => {
        const data = new FormData()
        data.append('file', file)
        return http.post<ApiResponse<StoredFile>>('/files/upload', data)
    },
    delete: (id: number) => http.delete<ApiResponse<null>>(`/files/${id}`),
    accessUrl: (id: number) =>
        http.get<ApiResponse<string>>(`/files/${id}/access-url`),
    content: (
        url: string,
        responseType: 'arraybuffer' | 'text' = 'arraybuffer',
        options: { silent?: boolean } = {},
    ) =>
        http.get<ArrayBuffer | string>(url, {
            baseURL: '',
            responseType,
            suppressGlobalErrorMessage: options.silent === true,
        }),
}
