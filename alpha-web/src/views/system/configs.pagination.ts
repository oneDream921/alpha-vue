interface TablePagination {
    current?: number
    pageSize?: number
}

export function configPageFromTableChange(
    pagination: TablePagination,
    currentPage: number,
    currentPageSize: number,
) {
    return {
        page: pagination.current ?? currentPage,
        pageSize: pagination.pageSize ?? currentPageSize,
    }
}
