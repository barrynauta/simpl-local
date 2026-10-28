export interface PropertyRow { path: string; value: string }

/** D75: every property of credentialSubject as a path and value pair; nested objects joined with '/'. */
export function flatten(subject: unknown, prefix = ''): PropertyRow[] {
  const rows: PropertyRow[] = [];
  if (Array.isArray(subject)) {
    subject.forEach((v, i) => rows.push(...flatten(v, prefix ? `${prefix}/${i}` : String(i))));
    return rows;
  }
  if (subject && typeof subject === 'object') {
    for (const [k, v] of Object.entries(subject as Record<string, unknown>)) {
      if (k === '@context') continue;
      rows.push(...flatten(v, prefix ? `${prefix}/${k}` : k));
    }
    return rows;
  }
  rows.push({ path: prefix, value: subject === null || subject === undefined ? '' : String(subject) });
  return rows;
}
