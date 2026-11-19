<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ApiError, changeStatus, downloadContent, listVocabularies, type Vocabulary } from "../api";
import { isAdmin } from "../auth";
import { describeError } from "../errors";
import { formatUtc } from "../format";
import { clearNotice, notice, setNotice } from "../notifications";
import { STATUS_LABEL, TEXTS } from "../texts";
import NoticePanel from "../components/NoticePanel.vue";

// Vocabulary list (SIMPL-18161, 18163, 18165, 18174, 18178; decisions D23, D34, D36, D50, D60).
const router = useRouter();
const admin = isAdmin();
const PAGE_SIZE = 20;

type SortKey = "name" | "status" | "creationDate" | "lastUpdated";
const filters = reactive({ name: "", status: "", creationDate: "", lastUpdated: "" });
const sort = reactive<{ key: SortKey; descending: boolean }>({ key: "name", descending: false });
const page = ref(1);
const totalPages = ref(0);
const total = ref(0);
const items = ref<Vocabulary[]>([]);
const loading = ref(false);
const loadError = ref<{ message: string; detail: string } | null>(null);
const actionError = ref<{ message: string; detail: string } | null>(null);
const openMenu = ref<string | null>(null);

async function load(): Promise<void> {
  loading.value = true;
  loadError.value = null;
  const query = new URLSearchParams();
  query.set("page", String(page.value));
  query.set("pageSize", String(PAGE_SIZE));
  query.set("sort", (sort.descending ? "-" : "") + sort.key);
  if (filters.name) query.set("name", filters.name);
  if (filters.status) query.set("status", filters.status);
  if (filters.creationDate) query.set("creationDate", filters.creationDate);
  if (filters.lastUpdated) query.set("lastUpdated", filters.lastUpdated);
  try {
    const result = await listVocabularies(query);
    items.value = result.items;
    totalPages.value = result.totalPages;
    total.value = result.total;
  } catch (e) {
    items.value = [];
    const described = describeError(e);
    const detail = e instanceof ApiError ? (e.problem.detail ?? "") : described.detail;
    loadError.value = { message: described.message, detail };
  } finally {
    loading.value = false;
  }
}

function applyFilters(): void {
  page.value = 1;
  void load();
}

/** Decision D50: a click on another column sorts it ascending; a click on the active column reverses it. */
function sortBy(key: SortKey): void {
  if (sort.key === key) {
    sort.descending = !sort.descending;
  } else {
    sort.key = key;
    sort.descending = false;
  }
  page.value = 1;
  void load();
}

function ariaSort(key: SortKey): "ascending" | "descending" | "none" {
  if (sort.key !== key) return "none";
  return sort.descending ? "descending" : "ascending";
}

function goTo(newPage: number): void {
  page.value = newPage;
  void load();
}

function toggleMenu(name: string): void {
  openMenu.value = openMenu.value === name ? null : name;
}

async function download(v: Vocabulary): Promise<void> {
  openMenu.value = null;
  actionError.value = null;
  try {
    await downloadContent(v.name);
  } catch (e) {
    actionError.value = describeError(e);
  }
}

async function toggleStatus(v: Vocabulary): Promise<void> {
  openMenu.value = null;
  actionError.value = null;
  clearNotice();
  const target = v.status === "ACTIVE" ? "DEPRECATED" : "ACTIVE";
  try {
    const changed = await changeStatus(v.name, target);
    setNotice(TEXTS.statusChanged(v.name, STATUS_LABEL[changed.status]));
    await load();
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      actionError.value = { message: TEXTS.vocabularyNotFound, detail: e.problem.detail ?? "" };
    } else if (e instanceof ApiError && e.status === 403) {
      actionError.value = { message: TEXTS.notAuthorized, detail: "" };
    } else {
      const described = describeError(e);
      actionError.value = {
        message: `${TEXTS.statusUpdateError} ${described.message}`,
        detail: described.detail,
      };
    }
  }
}

onMounted(load);
onBeforeUnmount(clearNotice);
</script>

<template>
  <h1>Vocabularies</h1>
  <NoticePanel :success="notice.success" :warnings="notice.warnings" />
  <div v-if="actionError" class="message error" role="alert">
    <p>{{ actionError.message }}</p>
    <p v-if="actionError.detail" class="detail">{{ actionError.detail }}</p>
  </div>

  <div class="filters" role="search">
    <label>
      Name contains
      <input
        v-model="filters.name"
        name="name"
        type="text"
        @keyup.enter="applyFilters"
        @change="applyFilters"
      />
    </label>
    <label>
      Vocabulary status
      <select v-model="filters.status" name="status" @change="applyFilters">
        <option value="">All</option>
        <option value="ACTIVE">Active</option>
        <option value="DEPRECATED">Deprecated</option>
      </select>
    </label>
    <label>
      Date created
      <input v-model="filters.creationDate" name="creationDate" type="date" @change="applyFilters" />
    </label>
    <label>
      Last updated
      <input v-model="filters.lastUpdated" name="lastUpdated" type="date" @change="applyFilters" />
    </label>
    <button v-if="admin" type="button" @click="router.push('/vocabularies/new')">
      {{ TEXTS.newVocabulary }}
    </button>
  </div>

  <div v-if="loadError" class="message error" role="alert">
    <p>{{ TEXTS.listLoadError }}</p>
    <p v-if="loadError.message !== TEXTS.listLoadError && loadError.message !== loadError.detail">
      {{ loadError.message }}
    </p>
    <p v-if="loadError.detail" class="detail">{{ loadError.detail }}</p>
  </div>

  <p v-if="!loading && !loadError && items.length === 0">{{ TEXTS.noVocabularies }}</p>

  <table v-if="items.length > 0" aria-label="Vocabulary list">
    <thead>
      <tr>
        <th>Actions</th>
        <th :aria-sort="ariaSort('name')">
          <button class="sort" type="button" @click="sortBy('name')">Name</button>
        </th>
        <th>Description</th>
        <th>Latest version</th>
        <th :aria-sort="ariaSort('status')">
          <button class="sort" type="button" @click="sortBy('status')">Status</button>
        </th>
        <th :aria-sort="ariaSort('creationDate')">
          <button class="sort" type="button" @click="sortBy('creationDate')">Date created</button>
        </th>
        <th :aria-sort="ariaSort('lastUpdated')">
          <button class="sort" type="button" @click="sortBy('lastUpdated')">Last updated</button>
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
              class="secondary"
              aria-label="Actions"
              :aria-expanded="openMenu === v.name"
              @click="toggleMenu(v.name)"
            >
              &#8942;
            </button>
            <ul v-if="openMenu === v.name" role="menu">
              <li role="none">
                <button role="menuitem" type="button" @click="router.push(`/vocabularies/${v.name}`)">
                  {{ TEXTS.viewDetails }}
                </button>
              </li>
              <li role="none">
                <button role="menuitem" type="button" @click="download(v)">{{ TEXTS.download }}</button>
              </li>
              <li v-if="admin" role="none">
                <button role="menuitem" type="button" @click="router.push(`/vocabularies/${v.name}/update`)">
                  {{ TEXTS.update }}
                </button>
              </li>
              <li v-if="admin" role="none">
                <button role="menuitem" type="button" @click="toggleStatus(v)">
                  {{ v.status === "ACTIVE" ? TEXTS.deprecate : TEXTS.activate }}
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
          {{ STATUS_LABEL[v.status] }}
          <span v-if="v.status === 'DEPRECATED'" class="deprecation">
            <span class="warning-icon" aria-hidden="true">&#9888;</span>
            <span class="warning-text">{{ TEXTS.vocabularyDeprecated }}</span>
          </span>
        </td>
        <td>{{ formatUtc(v.creationDate) }}</td>
        <td>{{ formatUtc(v.lastUpdateDate) }}</td>
        <td>{{ v.createdBy }}</td>
      </tr>
    </tbody>
  </table>

  <div v-if="totalPages > 1" class="pager" aria-label="Pages">
    <button type="button" class="secondary" :disabled="page <= 1" @click="goTo(page - 1)">Previous</button>
    <span>Page {{ page }} of {{ totalPages }} ({{ total }} vocabularies)</span>
    <button type="button" class="secondary" :disabled="page >= totalPages" @click="goTo(page + 1)">
      Next
    </button>
  </div>
</template>
