/** Saves a blob as a file through a temporary link. */
export function saveFile(content: Blob, fileName: string): void {
  const url = URL.createObjectURL(content);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.setTimeout(() => URL.revokeObjectURL(url), 10000);
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return iso;
  }
  return d.toISOString().replace('T', ' ').substring(0, 19);
}

export const PASCAL = /^[A-Z][a-zA-Z0-9]*$/;
export const RESOURCE_TYPES = ['Data', 'Application', 'Infrastructure'];
export const DOWNLOAD_ERROR = 'An error occurred while downloading the schema. Please update the page and try again.';
export const NO_PERMISSION = 'You do not have permission to view this page.';
