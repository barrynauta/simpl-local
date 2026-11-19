<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ApiError, saveBlob, type Vocabulary } from '../api';
import ActionsMenu, { type MenuItem } from '../components/ActionsMenu.vue';
import ProblemMessage from '../components/ProblemMessage.vue';
import { useApp } from '../context';
import { formatDate, statusLabel } from '../format';
import { TEXT, statusChanged } from '../texts';

/** Vocabulary list (route /vocabularies): SIMPL-18161, 18163, 18165, 18174, 18178; D23, D34, D36, D50, D72. */
type SortField = 'name' | 'status' | 'creationDate' | 'lastUpdated';

const app = useApp();
const router = useRouter();
const PAGE_SIZE = 20;

const items = ref<Vocabulary[]>([]);
const total = ref(0);
const totalPages = ref(0);
const page = ref(1);
const loaded = ref(false);
const loadError = ref<string | null>(null);
const success = ref<string | null>(null);
const failure = ref<{ text: string; detail?: string | null } | null>(null);
const sort = reactive<{ field: SortField; descending: boolean }>({ field: 'name', descending: false });
const filters = reactive({ name: '', status: '', creationDate: '' });

const sortValue = computed(() => (sort.descending ? '-' : '') + sort.field);

async function load() {
  loadError.value = null;
  try {
    const result = await app.api.listVocabularies({
      name: filters.name || undefined,
      status: filters.status || undefined,
      creationDate: filters.creationDate || undefined,
      sort: sortValue.value,
      page: page.value,
      pageSize: PAGE_SIZE,
    });
    items.value = result.items;
    total.value = result.total;
    totalPages.value = result.totalPages;
  } catch (error) {
    items.value = [];
    loadError.value = error instanceof ApiError ? (error.problem.detail ?? null) : null;
    loadError.value = loadError.value ?? '';
  } finally {
    loaded.value = true;
  }
}

/** D50: a click on another column sorts it ascending; a click on the active column reverses it. */
function sortBy(field: SortField) {
  if (sort.field === field) {
    sort.descending = !sort.descending;
  } else {
    sort.field = field;
    sort.descending = false;
  }
  page.value = 1;
  void load();
}

function ariaSort(field: SortField): 'ascending' | 'descending' | 'none' {
  if (sort.field !== field) {
    return 'none';
  }
  return sort.descending ? 'descending' : 'ascending';
}

let filterTimer: ReturnType<typeof setTimeout> | undefined;
watch(filters, () => {
  clearTimeout(filterTimer);
  filterTimer = setTimeout(() => {
    page.value = 1;
    void load();
  }, 300);
});

function goToPage(target: number) {
  page.value = target;
  void load();
}

async function download(vocabulary: Vocabulary) {
  failure.value = null;
  try {
    saveBlob(await app.api.content(vocabulary.name), `${vocabulary.name}.ttl`);
  } catch (error) {
    failure.value = describe(error);
  }
}

async function changeStatus(vocabulary: Vocabulary) {
  success.value = null;
  failure.value = null;
  const target = vocabulary.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  try {
    const updated = await app.api.changeStatus(vocabulary.name, target);
    items.value = items.value.map((item) => (item.name === updated.name ? updated : item));
    success.value = statusChanged(updated.name, updated.status);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      failure.value = { text: TEXT.statusNotFound };
    } else if (error instanceof ApiError && error.status === 403) {
      failure.value = { text: TEXT.notAuthorized };
    } else {
      const detail = error instanceof ApiError ? error.problem.detail : null;
      failure.value = { text: `${TEXT.statusFailed} ${TEXT.genericError}`, detail };
    }
  }
}

function describe(error: unknown): { text: string; detail?: string | null } {
  if (error instanceof ApiError && error.status === 403) {
    return { text: TEXT.notAuthorized };
  }
  return { text: TEXT.genericError, detail: error instanceof ApiError ? error.problem.detail : null };
}

function actions(vocabulary: Vocabulary): MenuItem[] {
  const list: MenuItem[] = [
    { label: 'View vocabulary details', run: () => void router.push(`/vocabularies/${vocabulary.name}`) },
    { label: 'History', run: () => void router.push(`/vocabularies/${vocabulary.name}`) },
    { label: 'Download vocabulary', run: () => void download(vocabulary) },
  ];
  if (app.isAdmin()) {
    list.push({ label: 'Update', run: () => void router.push(`/vocabularies/${vocabulary.name}/versions/new`) });
    list.push({
      label: vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate',
      run: () => void changeStatus(vocabulary),
    });
  }
  return list;
}

onMounted(load);
</script>

<template>
  <section aria-labelledby="list-title">
    <div class="title-row">
      <h1 id="list-title">Vocabularies</h1>
      <RouterLink v-if="app.isAdmin()" class="button primary" to="/vocabularies/new">New Vocabulary +</RouterLink>
    </div>

    <form class="filters" role="search" aria-label="Filter vocabularies" @submit.prevent="load">
      <div class="field">
        <label for="filter-name">Name contains</label>
        <input id="filter-name" v-model="filters.name" type="text" maxlength="64" />
      </div>
      <div class="field">
        <label for="filter-status">Vocabulary status</label>
        <select id="filter-status" v-model="filters.status">
          <option value="">All</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
        </select>
      </div>
      <div class="field">
        <label for="filter-created">Created on</label>
        <input id="filter-created" v-model="filters.creationDate" type="date" />
      </div>
    </form>

    <div role="status" class="message success" :class="{ hidden: !success }">{{ success }}</div>
    <ProblemMessage v-if="failure" :text="failure.text" :detail="failure.detail" />
    <ProblemMessage v-if="loadError !== null" :text="TEXT.listLoadError" :detail="loadError" />

    <p v-if="loaded && loadError === null && items.length === 0">{{ TEXT.noVocabularies }}</p>

    <table v-if="items.length > 0" class="table">
      <caption class="visually-hidden">
        Vocabularies, {{ total }} in total
      </caption>
      <thead>
        <tr>
          <th scope="col">Actions</th>
          <th scope="col" :aria-sort="ariaSort('name')">
            <button type="button" class="sort" @click="sortBy('name')">Name</button>
          </th>
          <th scope="col">Description</th>
          <th scope="col">Latest version</th>
          <th scope="col" :aria-sort="ariaSort('status')">
            <button type="button" class="sort" @click="sortBy('status')">Status</button>
          </th>
          <th scope="col" :aria-sort="ariaSort('creationDate')">
            <button type="button" class="sort" @click="sortBy('creationDate')">Date created</button>
          </th>
          <th scope="col" :aria-sort="ariaSort('lastUpdated')">
            <button type="button" class="sort" @click="sortBy('lastUpdated')">Last updated</button>
          </th>
          <th scope="col">Creator</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="vocabulary in items" :key="vocabulary.name">
          <td><ActionsMenu :label="`Actions for ${vocabulary.name}`" :items="actions(vocabulary)" /></td>
          <td>
            <RouterLink :to="`/vocabularies/${vocabulary.name}`">{{ vocabulary.name }}</RouterLink>
          </td>
          <td>{{ vocabulary.description }}</td>
          <td>{{ vocabulary.version }}</td>
          <td>
            {{ statusLabel(vocabulary.status) }}
            <span
              v-if="vocabulary.status === 'DEPRECATED'"
              class="warning-icon"
              role="img"
              :aria-label="TEXT.vocabularyDeprecated"
              :title="TEXT.vocabularyDeprecated"
              >&#9888;</span
            >
          </td>
          <td>{{ formatDate(vocabulary.creationDate) }}</td>
          <td>{{ formatDate(vocabulary.lastUpdateDate) }}</td>
          <td>{{ vocabulary.createdBy }}</td>
        </tr>
      </tbody>
    </table>

    <nav v-if="totalPages > 1" class="pager" aria-label="Pages">
      <button type="button" :disabled="page <= 1" @click="goToPage(page - 1)">Previous page</button>
      <span>Page {{ page }} of {{ totalPages }}</span>
      <button type="button" :disabled="page >= totalPages" @click="goToPage(page + 1)">Next page</button>
    </nav>
  </section>
</template>
