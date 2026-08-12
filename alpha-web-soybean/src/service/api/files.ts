import type { FlatResponseData } from '@sa/axios';
import { request } from '../request';
import type { PageResponse } from './system';

type FileContentResponse<T> = FlatResponseData<T, App.Service.Response>;

export interface StoredFile {
  id: number;
  storageProvider: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  publicUrl: string;
  uploaderName?: string;
  createdAt: string;
}

function fetchContent(
  url: string,
  responseType: 'arraybuffer',
  _options?: { silent?: boolean }
): Promise<FileContentResponse<ArrayBuffer>>;
function fetchContent(
  url: string,
  responseType: 'text',
  _options?: { silent?: boolean }
): Promise<FileContentResponse<string>>;
function fetchContent(
  url: string,
  responseType?: undefined,
  _options?: { silent?: boolean }
): Promise<FileContentResponse<ArrayBuffer>>;
function fetchContent(
  url: string,
  responseType: 'arraybuffer' | 'text',
  _options?: { silent?: boolean }
): Promise<FileContentResponse<ArrayBuffer | string>>;
function fetchContent(
  url: string,
  responseType: 'arraybuffer' | 'text' = 'arraybuffer',
  _options: { silent?: boolean } = {}
) {
  if (responseType === 'arraybuffer') {
    return request<ArrayBuffer, 'arrayBuffer'>({
      url,
      method: 'get',
      responseType: 'arrayBuffer',
      headers: _options.silent ? { 'X-Client-Silent-Error': 'true' } : undefined
    });
  }
  return request<string, 'text'>({
    url,
    method: 'get',
    responseType: 'text',
    headers: _options.silent ? { 'X-Client-Silent-Error': 'true' } : undefined
  });
}

export const fileApi = {
  page: (page = 1, size = 10) =>
    request<PageResponse<StoredFile>>({ url: '/files', method: 'get', params: { page, size } }),
  upload: (file: File) => {
    const data = new FormData();
    data.append('file', file);
    return request<StoredFile>({ url: '/files/upload', method: 'post', data });
  },
  delete: (id: number) => request<null>({ url: `/files/${id}`, method: 'delete' }),
  accessUrl: (id: number) => request<string>({ url: `/files/${id}/access-url`, method: 'get' }),
  content: fetchContent
};
