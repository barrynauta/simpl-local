import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  ApiError,
  buildQuery,
  fetchContent,
  filenameFrom,
  listAllVersions,
  patchVocabularyStatus,
} from '../src/api';
import { asUser, jsonResponse, mockFetch, page, version } from './helpers';
import { formatDate, statusLabel, countText } from '../src/format';
import { loadErrorText, statusErrorText, submitErrorText } from '../src/errors';
import { validateName, validateText } from '../src/validation';

afterEach(() => vi.unstubAllGlobals());

describe('api client', () => {
  it('builds queries without empty values', () => {
    expect(buildQuery({})).toBe('');
    expect(buildQuery({ name: 'Ab', status: 'ACTIVE', sort: '-name', page: 2, pageSize: 20 })).toBe(
      '?name=Ab&status=ACTIVE&sort=-name&page=2&pageSize=20',
    );
  });

  it('sends the bearer token and Accept header and reads every page of versions', async () => {
    asUser(true);
    const calls = mockFetch((url) =>
      url.includes('page=1')
        ? jsonResponse({ ...page([version('Abc', 2)], 101, 1, 100) })
        : jsonResponse({ ...page([version('Abc', 1)], 101, 2, 100) }),
    );
    const versions = await listAllVersions('Abc');
    expect(versions.map((v) => v.version)).toEqual([2, 1]);
    expect(calls[0].url).toBe('/v1/vocabularies/Abc/versions?page=1&pageSize=100');
    const headers = calls[0].init.headers as Record<string, string>;
    expect(headers.Authorization).toBe('Bearer test-token');
    expect(headers.Accept).toBe('application/json');
  });

  it('turns a problem response into an ApiError and logs in again on 401', async () => {
    const { login } = asUser(false);
    mockFetch(() => jsonResponse({ status: 401, title: 'Unauthorized' }, 401));
    await expect(patchVocabularyStatus('Abc', 'DEPRECATED')).rejects.toBeInstanceOf(ApiError);
    expect(login).toHaveBeenCalled();
  });

  it('maps a network failure to 503', async () => {
    asUser(true);
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('down')));
    await expect(patchVocabularyStatus('Abc', 'ACTIVE')).rejects.toMatchObject({ status: 503 });
  });

  it('reads the download filename from Content-Disposition', async () => {
    asUser(true);
    const calls = mockFetch(
      () =>
        new Response('x', {
          headers: { 'Content-Disposition': 'attachment; filename="Abc-v2.ttl"' },
        }),
    );
    const result = await fetchContent('Abc', 2);
    expect(result.filename).toBe('Abc-v2.ttl');
    expect(calls[0].url).toBe('/v1/content/Abc/versions/2');
    expect((calls[0].init.headers as Record<string, string>).Accept).toBe('text/turtle');
    expect(filenameFrom(null, 'Abc.ttl')).toBe('Abc.ttl');
  });
});

describe('format, errors and validation', () => {
  it('formats dates in UTC', () => {
    expect(formatDate('2026-11-18T09:27:00Z')).toBe('2026-11-18 09:27 UTC');
    expect(formatDate(undefined)).toBe('');
    expect(formatDate('nonsense')).toBe('nonsense');
  });

  it('labels statuses and counts', () => {
    expect(statusLabel('ACTIVE')).toBe('Active');
    expect(statusLabel('DEPRECATED')).toBe('Deprecated');
    expect(countText(1, 'error', 'errors')).toBe('1 error found');
    expect(countText(3, 'error', 'errors')).toBe('3 errors found');
  });

  it('chooses the decided error texts', () => {
    const forbidden = new ApiError(403, { detail: 'no' });
    const missing = new ApiError(404, { detail: 'gone' });
    const broken = new ApiError(503, { detail: 'store down' });
    const conflict = new ApiError(409, { detail: 'already exists' });
    expect(submitErrorText(forbidden)).toBe('You are not authorized to perform this action.');
    expect(submitErrorText(broken)).toBe(
      'Something went wrong. Retry later or contact support. store down',
    );
    expect(submitErrorText(conflict)).toBe('already exists');
    expect(statusErrorText(missing)).toBe('Vocabulary not found. The status was not changed.');
    expect(statusErrorText(broken)).toBe(
      'The vocabulary status could not be updated. Something went wrong. Retry later or contact support. store down',
    );
    expect(statusErrorText(forbidden)).toBe('You are not authorized to perform this action.');
    expect(loadErrorText(broken, 'The vocabulary list could not be loaded.')).toBe(
      'The vocabulary list could not be loaded. store down',
    );
  });

  it('validates the form fields of D37', () => {
    expect(validateName('')).not.toBe('');
    expect(validateName('Ab')).not.toBe('');
    expect(validateName('abc')).not.toBe('');
    expect(validateName('Abc1')).toBe('');
    expect(validateText('   ', 'Description')).toBe('Description is required.');
    expect(validateText('x'.repeat(1001), 'Description')).not.toBe('');
    expect(validateText(' ok ', 'Description')).toBe('');
  });
});
