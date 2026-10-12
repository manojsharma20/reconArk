// The definition editor shows option maps as simple "key=value" lines. These helpers convert both ways.

/** "delimiter=;\nheader=true" -> { delimiter: ';', header: 'true' }. Blank lines and lines without '=' are skipped. */
export function parseKeyValues(text: string): Record<string, string> {
  const result: Record<string, string> = {};
  for (const line of text.split('\n')) {
    const index = line.indexOf('=');
    if (index <= 0) continue;
    const key = line.slice(0, index).trim();
    if (key) result[key] = line.slice(index + 1).trim();
  }
  return result;
}

/** { delimiter: ';' } -> "delimiter=;" */
export function formatKeyValues(values: Record<string, string>): string {
  return Object.entries(values)
    .map(([key, value]) => `${key}=${value}`)
    .join('\n');
}

/** "reference, currency" -> ['reference', 'currency'] */
export function parseList(text: string): string[] {
  return text
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean);
}
