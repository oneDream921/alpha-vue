export function validateTypeCode(value: string): string | undefined {
  const typeCode = value.trim();
  if (!typeCode) return '请输入类型编码';
  if (!/^[A-Za-z][A-Za-z0-9._-]*$/.test(typeCode)) {
    return '类型编码需以字母开头，仅含字母、数字、点、下划线或连字符';
  }
  if (typeCode.length > 64) return '类型编码不能超过 64 个字符';
  return undefined;
}

export function validateItemValue(value: string): string | undefined {
  if (!value.trim()) return '请输入字典项值';
  if (value.length > 128) return '字典项值不能超过 128 个字符';
  return undefined;
}
