<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ApiError, changeStatus, downloadLatest, listSchemas, type SchemaItem, type SchemaType } from '../api';
import { isAdmin, isViewer } from '../auth';
import { notifyError, notifySuccess } from '../notify';
import { DOWNLOAD_ERROR, NO_PERMISSION, RESOURCE_TYPES, formatDate, saveFile } from '../download';

// Schema list page (SIMPL-15837, SIMPL-32790, D53).
const PAGE_SIZE = 20;
const router = useRouter();
const admin = isAdmin();

const status = ref('');
const resourceType = ref('');
const schemaType = ref<SchemaType>('RESOURCE');
const page = ref(1);
const totalPages = ref(0);
const items = ref<SchemaItem[]>([]);
const state = ref<'loading' | 'ok' | 'error' | 'forbidden'>('loading');
const sortKey = ref<'name' | 'title' | null>(null);
const sortAsc = ref(true);
const confirmName = ref<string | null>(null);
const busy = ref(false);

const anyFilter = computed(() => status.value !== '' || resourceType.value !== '' || schemaType.value !== 'RESOURCE');

const rows = computed(() => {
  const list = [...items.value];
  if (sortKey.value) {
    const key = sortKey.value;
    list.sort((a, b) => {
      const c = (a[key] ?? '').localeCompare(b[key] ?? '');
      return sortAsc.value ? c : -c;
    });
  }
  return list;
});

async function load(): Promise<void> {
  if (!isViewer()) {
    state.value = 'forbidden';
    return;
  }
  state.value = 'loading';
  try {
    const result = await listSchemas({
      schemaType: schemaType.value,
      status: status.value,
      resourceType: schemaType.value === 'SEARCH' ? '' : resourceType.value,
      page: page.value,
      pageSize: PAGE_SIZE,
    });
    items.value = result.metadata;
    totalPages.value = result.totalPages;
    state.value = 'ok';
  } catch (e) {
    state.value = e instanceof ApiError && e.status === 403 ? 'forbidden' : 'error';
  }
}

function filterChanged(): void {
  if (schemaType.value === 'SEARCH') {
    resourceType.value = '';
  }
  page.value = 1;
  load();
}

function go(delta: number): void {
  page.value = Math.max(1, page.value + delta);
  load();
}

function sortBy(key: 'name' | 'title'): void {
  if (sortKey.value === key) {
    sortAsc.value = !sortAsc.value;
  } else {
    sortKey.value = key;
    sortAsc.value = true;
  }
}

function sortClass(key: 'name' | 'title'): string {
  if (sortKey.value !== key) {
    return 'sortable';
  }
  return sortAsc.value ? 'sortable sorted-asc' : 'sortable sorted-desc';
}

function openDetails(item: SchemaItem): void {
  router.push({ path: `/schemas/${encodeURIComponent(item.name)}`, query: { schemaType: item.schemaType } });
}

async function download(item: SchemaItem): Promise<void> {
  try {
    const { content, version } = await downloadLatest(item.name, item.schemaType);
    saveFile(content, `${item.name}-${version || item.version}.ttl`);
  } catch {
    notifyError(DOWNLOAD_ERROR);
  }
}

async function confirmRevoke(): Promise<void> {
  const item = items.value.find((i) => i.name === confirmName.value);
  confirmName.value = null;
  if (!item) {
    return;
  }
  busy.value = true;
  try {
    await changeStatus(item.name, item.schemaType, 'REVOKED');
    notifySuccess(`Schema "${item.name}" revoked successfully.`);
    await load();
  } catch (e) {
    notifyError(e instanceof ApiError ? e.detail : 'The schema could not be revoked.');
  } finally {
    busy.value = false;
  }
}

async function publish(item: SchemaItem): Promise<void> {
  busy.value = true;
  try {
    await changeStatus(item.name, item.schemaType, 'PUBLISHED');
    notifySuccess(`Schema ${item.name} published successfully!`);
    await load();
  } catch (e) {
    notifyError(e instanceof ApiError ? e.detail : 'The schema could not be published.');
  } finally {
    busy.value = false;
  }
}

function newVersion(item: SchemaItem): void {
  router.push({ path: `/schemas/${encodeURIComponent(item.name)}/versions/new`, query: { schemaType: item.schemaType } });
}

onMounted(load);
</script>

<template>
  <section>
    <div class="page-head">
      <h1>Schemas</h1>
      <button
        v-if="admin && state !== 'forbidden'"
        type="button"
        class="primary"
        data-testid="create-schema-button"
        @click="router.push('/schemas/new')"
      >
        Create Schema
      </button>
    </div>

    <div v-if="state === 'forbidden'" class="state-box" data-testid="error-state">{{ NO_PERMISSION }}</div>

    <div v-else-if="state === 'error'" class="state-box">
      <p data-testid="error-state">Could not load schemas. Please try again later.</p>
      <button type="button" data-testid="retry-button" @click="load">Retry</button>
    </div>

    <template v-else>
      <div class="filters">
        <label>
          Status
          <select v-model="status" data-testid="filter-status" @change="filterChanged">
            <option value="">All</option>
            <option value="PUBLISHED">PUBLISHED</option>
            <option value="REVOKED">REVOKED</option>
          </select>
        </label>
        <label>
          Schema type
          <select v-model="schemaType" data-testid="filter-schema-type" @change="filterChanged">
            <option value="RESOURCE">RESOURCE</option>
            <option value="SEARCH">SEARCH</option>
          </select>
        </label>
        <label>
          Resource type
          <select
            v-model="resourceType"
            data-testid="filter-resource-type"
            :disabled="schemaType === 'SEARCH'"
            @change="filterChanged"
          >
            <option value="">All</option>
            <option v-for="rt in RESOURCE_TYPES" :key="rt" :value="rt">{{ rt }}</option>
          </select>
        </label>
      </div>

      <table class="grid" data-testid="schema-list-table">
        <thead>
          <tr>
            <th :class="sortClass('name')" @click="sortBy('name')">Schema Name</th>
            <th :class="sortClass('title')" @click="sortBy('title')">Title</th>
            <th>Description</th>
            <th>Schema Type</th>
            <th>Resource Type</th>
            <th>Latest Version</th>
            <th>Status</th>
            <th>Creation Date</th>
            <th>Creator</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in rows" :key="item.schemaType + ':' + item.name" :data-testid="`schema-row-${item.name}`">
            <td>
              <a href="#" class="name-link" @click.prevent="openDetails(item)">{{ item.name }}</a>
            </td>
            <td>{{ item.title }}</td>
            <td>{{ item.description }}</td>
            <td>{{ item.schemaType }}</td>
            <td>{{ item.resourceType ?? '' }}</td>
            <td>{{ item.version }}</td>
            <td><span :class="['badge', item.status.toLowerCase()]">{{ item.status }}</span></td>
            <td>{{ formatDate(item.created) }}</td>
            <td>{{ item.createdBy ?? '' }}</td>
            <td class="actions">
              <button type="button" :data-testid="`action-download-${item.name}`" @click="download(item)">Download</button>
              <template v-if="admin">
                <button
                  v-if="item.status === 'PUBLISHED'"
                  type="button"
                  :disabled="busy"
                  :data-testid="`action-revoke-${item.name}`"
                  @click="confirmName = item.name"
                >
                  Revoke
                </button>
                <button
                  v-if="item.status === 'REVOKED'"
                  type="button"
                  :disabled="busy"
                  :data-testid="`action-publish-${item.name}`"
                  @click="publish(item)"
                >
                  Publish
                </button>
                <button type="button" :data-testid="`action-new-version-${item.name}`" @click="newVersion(item)">
                  New Version
                </button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="state === 'loading'" class="muted">Loading...</p>
      <div v-if="state === 'ok' && items.length === 0" class="empty" data-testid="empty-state">
        {{ anyFilter ? 'No schemas match the selected filters.' : 'No schemas found.' }}
      </div>

      <div v-if="state === 'ok' && items.length > 0" class="pagination">
        <button type="button" data-testid="pagination-prev" :disabled="page <= 1" @click="go(-1)">Previous</button>
        <span data-testid="pagination-info">Page {{ page }} of {{ Math.max(totalPages, 1) }}</span>
        <button type="button" data-testid="pagination-next" :disabled="page >= totalPages" @click="go(1)">Next</button>
      </div>
    </template>

    <div v-if="confirmName" class="modal-backdrop">
      <div class="modal" role="dialog" aria-modal="true" data-testid="confirm-dialog">
        <h2>Revoke schema</h2>
        <p>Are you sure you want to revoke schema "{{ confirmName }}"?</p>
        <div class="modal-actions">
          <button type="button" data-testid="cancel-button" @click="confirmName = null">Cancel</button>
          <button type="button" class="danger" data-testid="confirm-button" @click="confirmRevoke">Confirm</button>
        </div>
      </div>
    </div>
  </section>
</template>
