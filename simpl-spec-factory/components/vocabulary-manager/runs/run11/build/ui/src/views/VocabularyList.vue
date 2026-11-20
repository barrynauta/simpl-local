<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ApiError, download, listVocabularies, setStatus, type Vocabulary } from "../api";
import { isAdmin } from "../auth";
import ActionsMenu, { type MenuItem } from "../components/ActionsMenu.vue";
import { TEXT, formatDate, statusLabel } from "../texts";

/** Vocabulary list (SIMPL-18161, 18163, 18165, 18174, 18178; D23, D34, D36, D50, D72, D100, D115). */
type SortField = "name" | "status" | "creationDate" | "lastUpdated";

const router = useRouter();
const admin = isAdmin();
const rows = ref<Vocabulary[]>([]);
const page = ref(1);
const totalPages = ref(0);
const loaded = ref(false);
const loadError = ref("");
const notice = ref("");
const actionError = ref("");
const nameFilter = ref("");
const statusFilter = ref("");
const createdFilter = ref("");
const sortField = ref<SortField>("name");
const ascending = ref(true);

const columns: { field: SortField | null; label: string }[] = [
  { field: "name", label: "Name" },
  { field: "status", label: "Status" },
  { field: null, label: "Description" },
  { field: null, label: "Creator" },
  { field: "creationDate", label: "Date created" },
  { field: "lastUpdated", label: "Last updated" },
  { field: null, label: "Actions" },
];

const pageText = computed(() => `Page ${totalPages.value === 0 ? 0 : page.value} of ${totalPages.value}`);

function problemDetail(error: unknown): string {
  return error instanceof ApiError ? (error.problem.detail ?? "") : String(error);
}

async function load(): Promise<void> {
  const query = new URLSearchParams({ page: String(page.value), pageSize: "20" });
  if (nameFilter.value) query.set("name", nameFilter.value);
  if (statusFilter.value) query.set("status", statusFilter.value);
  if (createdFilter.value) query.set("creationDate", createdFilter.value);
  query.set("sort", (ascending.value ? "" : "-") + sortField.value);
  try {
    const result = await listVocabularies(query);
    rows.value = result.items;
    totalPages.value = result.totalPages;
    loadError.value = "";
  } catch (error) {
    rows.value = [];
    loadError.value = `${TEXT.listLoadFailed} ${problemDetail(error)}`.trim();
  }
  loaded.value = true;
}

function sortBy(field: SortField): void {
  if (sortField.value === field) {
    ascending.value = !ascending.value;
  } else {
    sortField.value = field;
    ascending.value = true;
  }
  page.value = 1;
  void load();
}

function ariaSort(field: SortField | null): "ascending" | "descending" | "none" | undefined {
  if (field === null) return undefined;
  if (field !== sortField.value) return "none";
  return ascending.value ? "ascending" : "descending";
}

let timer: ReturnType<typeof setTimeout> | undefined;
watch([nameFilter, statusFilter, createdFilter], () => {
  clearTimeout(timer);
  timer = setTimeout(() => {
    page.value = 1;
    void load();
  }, 300);
});

function go(delta: number): void {
  page.value += delta;
  void load();
}

async function changeStatus(row: Vocabulary): Promise<void> {
  const target = row.status === "ACTIVE" ? "DEPRECATED" : "ACTIVE";
  notice.value = "";
  actionError.value = "";
  try {
    const updated = await setStatus(row.name, target);
    row.status = updated.status;
    row.lastUpdateDate = updated.lastUpdateDate;
    notice.value = `Status of ${row.name} changed to ${statusLabel(updated.status)}.`;
  } catch (error) {
    actionError.value = statusFailure(error);
  }
}

function statusFailure(error: unknown): string {
  if (error instanceof ApiError && error.status === 404)
    return `${TEXT.statusNotFound} ${problemDetail(error)}`.trim();
  if (error instanceof ApiError && error.status === 403) return TEXT.notAuthorized;
  if (error instanceof ApiError && error.status < 500) {
    return `${TEXT.statusFailed} ${problemDetail(error)}`.trim();
  }
  return `${TEXT.statusFailed} ${TEXT.genericError} ${problemDetail(error)}`.trim();
}

async function downloadLatest(name: string): Promise<void> {
  actionError.value = "";
  try {
    await download(name);
  } catch (error) {
    actionError.value = `${TEXT.genericError} ${problemDetail(error)}`.trim();
  }
}

function itemsFor(row: Vocabulary): MenuItem[] {
  const items: MenuItem[] = [
    { label: "View vocabulary details", run: () => void router.push(`/vocabularies/${row.name}`) },
    { label: "History", run: () => void router.push(`/vocabularies/${row.name}`) },
    { label: "Download vocabulary", run: () => void downloadLatest(row.name) },
  ];
  if (admin) {
    items.push({ label: "Update", run: () => void router.push(`/vocabularies/${row.name}/versions/new`) });
    items.push({
      label: row.status === "ACTIVE" ? "Deprecate" : "Activate",
      run: () => void changeStatus(row),
    });
  }
  return items;
}

onMounted(load);
</script>

<template>
  <section>
    <div class="title-row">
      <h1>Vocabularies</h1>
      <RouterLink v-if="admin" class="button primary" to="/vocabularies/new">New Vocabulary +</RouterLink>
    </div>

    <form class="filters" role="search" @submit.prevent>
      <div>
        <label for="filter-name">Name contains</label>
        <input id="filter-name" v-model="nameFilter" type="search" maxlength="64" />
      </div>
      <div>
        <label for="filter-status">Vocabulary status</label>
        <select id="filter-status" v-model="statusFilter">
          <option value="">All</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
        </select>
      </div>
      <div>
        <label for="filter-created">Created on</label>
        <input id="filter-created" v-model="createdFilter" type="date" />
      </div>
    </form>

    <p v-if="notice" role="status" class="notice">{{ notice }}</p>
    <p v-if="actionError" role="alert" class="error">{{ actionError }}</p>
    <p v-if="loadError" role="alert" class="error">{{ loadError }}</p>

    <table v-if="!loadError">
      <thead>
        <tr>
          <th v-for="column in columns" :key="column.label" scope="col" :aria-sort="ariaSort(column.field)">
            <button v-if="column.field" type="button" class="sort" @click="sortBy(column.field)">
              {{ column.label }}
              <span aria-hidden="true">{{ sortField === column.field ? (ascending ? "▲" : "▼") : "" }}</span>
            </button>
            <template v-else>{{ column.label }}</template>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.name">
          <td>
            <RouterLink :to="`/vocabularies/${row.name}`">{{ row.name }}</RouterLink>
          </td>
          <td>
            {{ statusLabel(row.status) }}
            <span
              v-if="row.status === 'DEPRECATED'"
              class="warning-icon"
              role="img"
              :aria-label="TEXT.vocabularyDeprecated"
              :title="TEXT.vocabularyDeprecated"
              >&#9888;</span
            >
          </td>
          <td>{{ row.description }}</td>
          <td>{{ row.createdBy }}</td>
          <td>{{ formatDate(row.creationDate) }}</td>
          <td>{{ formatDate(row.lastUpdateDate) }}</td>
          <td><ActionsMenu :label="`Actions for ${row.name}`" :items="itemsFor(row)" /></td>
        </tr>
      </tbody>
    </table>
    <p v-if="loaded && !loadError && rows.length === 0">{{ TEXT.noVocabularies }}</p>

    <div class="pager">
      <button type="button" :disabled="page <= 1" @click="go(-1)">Previous page</button>
      <span>{{ pageText }}</span>
      <button type="button" :disabled="page >= totalPages" @click="go(1)">Next page</button>
    </div>
  </section>
</template>
