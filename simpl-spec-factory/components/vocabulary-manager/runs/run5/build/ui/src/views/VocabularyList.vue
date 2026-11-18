<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { download, listVocabularies, setVocabularyStatus, type Vocabulary } from '../lib/api';
import { isAdmin } from '../lib/auth';
import { errorLines } from '../lib/errors';
import { formatDate } from '../lib/format';
import { ApiError } from '../lib/api';
import {
  LIST_NOT_LOADED,
  NO_VOCABULARIES,
  STATUS_NOT_UPDATED,
  VOCABULARY_DEPRECATED,
  VOCABULARY_NOT_FOUND,
  statusChanged,
  statusLabel,
} from '../lib/messages';

/** Vocabulary list (SIMPL-18161, SIMPL-18163, SIMPL-18178; D23, D34, D36, D50, D60). */
type SortField = 'name' | 'status' | 'creationDate' | 'lastUpdated';

const router = useRouter();
const admin = isAdmin();
const items = ref<Vocabulary[]>([]);
const total = ref(0);
const totalPages = ref(0);
const page = ref(1);
const pageSize = 20;
const loading = ref(false);
const loadError = ref<string[] | null>(null);
const sortField = ref<SortField>('name');
const sortAscending = ref(true);
const nameFilter = ref('');
const statusFilter = ref('');
const createdFilter = ref('');
const openMenu = ref<string | null>(null);
const notice = ref<{ kind: 'success' | 'error'; lines: string[] } | null>(null);

const sortParam = computed(() => (sortAscending.value ? '' : '-') + sortField.value);

async function load(): Promise<void> {
  loading.value = true;
  loadError.value = null;
  const query = new URLSearchParams({
    page: String(page.value),
    pageSize: String(pageSize),
    sort: sortParam.value,
  });
  if (nameFilter.value) query.set('name', nameFilter.value);
  if (statusFilter.value) query.set('status', statusFilter.value);
  if (createdFilter.value) query.set('creationDate', createdFilter.value);
  try {
    const result = await listVocabularies(query);
    items.value = result.items;
    total.value = result.total;
    totalPages.value = result.totalPages;
  } catch (error) {
    items.value = [];
    loadError.value = errorLines(error, LIST_NOT_LOADED);
  } finally {
    loading.value = false;
  }
}

/** D50: a click on another column sorts it ascending; a click on the active column reverses it. */
function sortBy(field: SortField): void {
  if (sortField.value === field) {
    sortAscending.value = !sortAscending.value;
  } else {
    sortField.value = field;
    sortAscending.value = true;
  }
  page.value = 1;
  void load();
}

function ariaSort(field: SortField): 'ascending' | 'descending' | 'none' {
  if (sortField.value !== field) return 'none';
  return sortAscending.value ? 'ascending' : 'descending';
}

let nameTimer: ReturnType<typeof setTimeout> | undefined;
watch(nameFilter, () => {
  clearTimeout(nameTimer);
  nameTimer = setTimeout(() => {
    page.value = 1;
    void load();
  }, 300);
});
watch([statusFilter, createdFilter], () => {
  page.value = 1;
  void load();
});

function goTo(newPage: number): void {
  page.value = newPage;
  void load();
}

function toggleMenu(name: string): void {
  openMenu.value = openMenu.value === name ? null : name;
}

async function changeStatus(vocabulary: Vocabulary): Promise<void> {
  openMenu.value = null;
  notice.value = null;
  const target = vocabulary.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  try {
    const updated = await setVocabularyStatus(vocabulary.name, target);
    items.value = items.value.map((v) => (v.name === updated.name ? updated : v));
    notice.value = { kind: 'success', lines: [statusChanged(updated.name, updated.status)] };
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notice.value = {
        kind: 'error',
        lines: [
          VOCABULARY_NOT_FOUND,
          `The vocabulary ${vocabulary.name} could not be found. No status change was applied.`,
        ],
      };
    } else if (error instanceof ApiError && error.status === 403) {
      notice.value = { kind: 'error', lines: errorLines(error) };
    } else {
      notice.value = { kind: 'error', lines: errorLines(error, STATUS_NOT_UPDATED) };
    }
  }
}

async function downloadLatest(vocabulary: Vocabulary): Promise<void> {
  openMenu.value = null;
  notice.value = null;
  try {
    await download(vocabulary.name);
  } catch (error) {
    notice.value = { kind: 'error', lines: errorLines(error, 'The vocabulary could not be downloaded.') };
  }
}

function openDetails(vocabulary: Vocabulary): void {
  openMenu.value = null;
  void router.push(`/vocabularies/${vocabulary.name}`);
}

function openUpdate(vocabulary: Vocabulary): void {
  openMenu.value = null;
  void router.push(`/vocabularies/${vocabulary.name}/update`);
}

onMounted(load);
</script>

<template>
  <section>
    <h1>Vocabularies</h1>
    <div class="toolbar">
      <label>
        Name contains
        <input v-model.trim="nameFilter" type="text" name="nameFilter" aria-label="Name contains" />
      </label>
      <label>
        Vocabulary status
        <select v-model="statusFilter" name="statusFilter" aria-label="Vocabulary status">
          <option value="">All</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
        </select>
      </label>
      <label>
        Created on
        <input v-model="createdFilter" type="date" name="createdFilter" aria-label="Created on" />
      </label>
      <button v-if="admin" type="button" class="primary" @click="router.push('/vocabularies/new')">
        New Vocabulary +
      </button>
    </div>

    <div v-if="notice" :class="['message', notice.kind]" role="status">
      <p v-for="line in notice.lines" :key="line">{{ line }}</p>
    </div>

    <div v-if="loadError" class="message error" role="alert">
      <p v-for="line in loadError" :key="line">{{ line }}</p>
    </div>

    <table v-else aria-label="Vocabulary list">
      <thead>
        <tr>
          <th>Actions</th>
          <th :aria-sort="ariaSort('name')">
            <button type="button" class="sort" @click="sortBy('name')">Name</button>
          </th>
          <th>Description</th>
          <th>Latest version</th>
          <th :aria-sort="ariaSort('status')">
            <button type="button" class="sort" @click="sortBy('status')">Status</button>
          </th>
          <th :aria-sort="ariaSort('creationDate')">
            <button type="button" class="sort" @click="sortBy('creationDate')">Date created</button>
          </th>
          <th :aria-sort="ariaSort('lastUpdated')">
            <button type="button" class="sort" @click="sortBy('lastUpdated')">Last updated</button>
          </th>
          <th>Creator</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="v in items" :key="v.name" :data-name="v.name">
          <td>
            <div class="menu">
              <button
                type="button"
                aria-label="Actions"
                :aria-expanded="openMenu === v.name"
                @click="toggleMenu(v.name)"
              >
                &#8942;
              </button>
              <ul v-if="openMenu === v.name" role="menu">
                <li>
                  <button type="button" role="menuitem" @click="openDetails(v)">
                    View vocabulary details
                  </button>
                </li>
                <li><button type="button" role="menuitem" @click="openDetails(v)">History</button></li>
                <li>
                  <button type="button" role="menuitem" @click="downloadLatest(v)">
                    Download vocabulary
                  </button>
                </li>
                <li v-if="admin">
                  <button type="button" role="menuitem" @click="openUpdate(v)">Update</button>
                </li>
                <li v-if="admin">
                  <button type="button" role="menuitem" @click="changeStatus(v)">
                    {{ v.status === 'ACTIVE' ? 'Deprecate' : 'Activate' }}
                  </button>
                </li>
              </ul>
            </div>
          </td>
          <td>
            <RouterLink :to="`/vocabularies/${v.name}`">{{ v.name }}</RouterLink>
          </td>
          <td>{{ v.description }}</td>
          <td>{{ v.version }}</td>
          <td>
            <span class="status">{{ statusLabel(v.status) }}</span>
            <template v-if="v.status === 'DEPRECATED'">
              <span
                class="warning-icon"
                role="img"
                :aria-label="VOCABULARY_DEPRECATED"
                :title="VOCABULARY_DEPRECATED"
              >
                &#9888;
              </span>
              <span class="deprecation">{{ VOCABULARY_DEPRECATED }}</span>
            </template>
          </td>
          <td>{{ formatDate(v.creationDate) }}</td>
          <td>{{ formatDate(v.lastUpdateDate) }}</td>
          <td>{{ v.createdBy }}</td>
        </tr>
      </tbody>
    </table>

    <p v-if="!loading && !loadError && items.length === 0" class="empty">{{ NO_VOCABULARIES }}</p>

    <div v-if="!loadError && totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1" @click="goTo(page - 1)">Previous</button>
      <span>Page {{ page }} of {{ totalPages }} ({{ total }} vocabularies)</span>
      <button type="button" :disabled="page >= totalPages" @click="goTo(page + 1)">Next</button>
    </div>
  </section>
</template>
