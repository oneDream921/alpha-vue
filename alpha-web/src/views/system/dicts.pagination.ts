interface TablePagination {
    current?: number
    pageSize?: number
}

export function dictPageFromTableChange(
    pagination: TablePagination,
    currentPage: number,
    currentPageSize: number,
) {
    return {
        page: pagination.current ?? currentPage,
        pageSize: pagination.pageSize ?? currentPageSize,
    }
}

export function itemPageForTypeSelection(
    selectedTypeId: number | undefined,
    currentPageSize: number,
) {
    return {
        selectedTypeId,
        page: 1,
        pageSize: currentPageSize,
        shouldLoad: selectedTypeId !== undefined,
    }
}

export function dictionaryTypeRow<T>(type: T, selectType: (type: T) => void) {
    return {
        onClick: () => selectType(type),
    }
}
