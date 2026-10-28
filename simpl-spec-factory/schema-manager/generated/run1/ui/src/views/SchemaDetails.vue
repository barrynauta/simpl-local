<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ApiError, downloadVersion, listAllVersions, type SchemaType, type VersionItem } from '../api';
import { isAdmin, isViewer } from '../auth';
import { notifyError, notifySuccess } from '../notify';
import { DOWNLOAD_ERROR, NO_PERMISSION, formatDate, saveFile } from '../download';

// Details page with the versions table (SIMPL-13346, SIMPL-32787, D54, D57).
const props = defineProps<{ name: string }>();
const route = useRoute();
const router = useRouter();
const admin = isAdmin();
const schemaType = computed<SchemaType>(() => (route.query.schemaType === 'SEARCH' ? 'SEARCH' : 'RESOURCE'));

const versions = ref<VersionItem[]>([]);
const state = ref<'loading' | 'ok' | 'error' | 'forbidden' | 'notfound'>('loading');
const errorText = ref('');
const sortKey = ref<'version' | 'created'>('version');
const sortAsc = ref(false);

const latest = computed(() =>
  versions.value.reduce<VersionItem | null>((a, b) => (!a || Number(b.version) > Number(a.version) ? b : a), null),
);

const rows = computed(() => {
  const list = [...versions.value];
  list.sort((a, b) => {
    const c = sortKey.value === 'version'
      ? Number(a.version) - Number(b.version)
      : (a.created ?? '').localeCompare(b.created ?? '');
    return sortAsc.value ? c : -c;
  });
  return list;
});

function sortBy(key: 'version' | 'created'): void {
  if (sortKey.value === key) {
    sortAsc.value = !sortAsc.value;
  } else {
    sortKey.value = key;
    sortAsc.value = key !== 'version';
  }
}

function sortClass(key: 'version' | 'created'): string {
  if (sortKey.value !== key) {
    return 'sortable';
  }
  return sortAsc.value ? 'sortable sorted-asc' : 'sortable sorted-desc';
}

async function load(): Promise<void> {
  if (!isViewer()) {
    state.value = 'forbidden';
    return;
  }
  state.value = 'loading';
  try {
    versions.value = await listAllVersions(props.name, schemaType.value);
    state.value = 'ok';
  } catch (e) {
    if (e instanceof ApiError && e.status === 403) {
      state.value = 'forbidden';
    } else {
      state.value = e instanceof ApiError && e.status === 404 ? 'notfound' : 'error';
      errorText.value = e instanceof ApiError ? e.detail : 'Could not load the schema versions.';
    }
  }
}

async function download(v: VersionItem): Promise<void> {
  try {
    const blob = await downloadVersion(props.name, v.version, schemaType.value);
    saveFile(blob, `${props.name}-${v.version}.ttl`);
    notifySuccess(`Schema version ${v.version} downloaded successfully.`);
  } catch {
    notifyError(DOWNLOAD_ERROR);
  }
}

function newVersion(): void {
  router.push({
    path: `/schemas/${encodeURIComponent(props.name)}/versions/new`,
    query: { schemaType: schemaType.value },
  });
}

onMounted(load);
</script>

<template>
  <section>
    <p><router-link to="/schemas" data-testid="back-to-list">Back to schemas</router-link></p>
    <div v-if="state === 'forbidden'" class="state-box" data-testid="error-state">{{ NO_PERMISSION }}</div>
    <div v-else-if="state === 'error' || state === 'notfound'" class="state-box" data-testid="error-state">
      <p>{{ errorText }}</p>
      <button type="button" data-testid="retry-button" @click="load">Retry</button>
    </div>
    <template v-else>
      <div class="page-head">
        <h1 data-testid="detail-name">{{ name }}</h1>
        <button v-if="admin" type="button" class="primary" :data-testid="`action-new-version-${name}`" @click="newVersion">
          New Version
        </button>
      </div>
      <dl v-if="latest" class="summary">
        <dt>Title</dt>
        <dd data-testid="detail-title">{{ latest.title }}</dd>
        <dt>Schema type</dt>
        <dd data-testid="detail-schema-type">{{ latest.schemaType }}</dd>
        <dt>Resource type</dt>
        <dd data-testid="detail-resource-type">{{ latest.resourceType ?? '' }}</dd>
        <dt>Status</dt>
        <dd data-testid="detail-status">{{ latest.status }}</dd>
      </dl>
      <table class="grid" data-testid="versions-table">
        <thead>
          <tr>
            <th :class="sortClass('version')" @click="sortBy('version')">Version</th>
            <th>Status</th>
            <th>Schema Type</th>
            <th>Resource Type</th>
            <th :class="sortClass('created')" @click="sortBy('created')">Creation Date</th>
            <th>Creator</th>
            <th>Latest Change</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="v in rows" :key="v.version" :data-testid="`version-row-${v.version}`">
            <td>{{ v.version }}</td>
            <td><span :class="['badge', v.status.toLowerCase()]">{{ v.status }}</span></td>
            <td>{{ v.schemaType }}</td>
            <td>{{ v.resourceType ?? '' }}</td>
            <td>{{ formatDate(v.created) }}</td>
            <td>{{ v.createdBy ?? '' }}</td>
            <td>{{ v.changelog }}</td>
            <td class="actions">
              <button type="button" :data-testid="`action-download-version-${v.version}`" @click="download(v)">Download</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="state === 'loading'" class="muted">Loading...</p>
    </template>
  </section>
</template>
