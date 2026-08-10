export function toggleDeptSelection(
    currentDeptId: number | undefined,
    selectedKeys: readonly (string | number)[],
) {
    const rawKey = selectedKeys[0]
    if (rawKey == null) return undefined

    const nextDeptId = Number(rawKey)
    if (!Number.isFinite(nextDeptId)) return undefined

    return nextDeptId === currentDeptId ? undefined : nextDeptId
}
