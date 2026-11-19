<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import ActionsMenu, { type MenuAction } from '../components/ActionsMenu.vue'
import StatusCell from '../components/StatusCell.vue'
import { downloadContent, listVocabularies, updateVocabularyStatus, type Vocabulary } from '../lib/api'
import { isAdmin } from '../lib/auth'
import { formatDate, statusLabel } from '../lib/format'
import { problemDetail, status, TEXT } from '../lib/messages'

/** Vocabulary list (SIMPL-18161, 18163, 18165, 18174, 18178; D23, D34, D36, D50, D60, D72). */
const router = useRouter()
const PAGE_SIZE = 20

type SortField = 'name' | 'status' | 'creationDate' | 'lastUpdated'
const sortField = ref<SortField>('name')
const sortDescending = ref(false)
const nameFilter = ref('')
const statusFilter = ref('')
const createdFilter = ref('')
const page = ref(1)

const rows = ref<Vocabulary[]>([])
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const loadError = ref('')
const success = ref('')
const actionError = ref('')

const admin = isAdmin()

const columns: { field: SortField | null; label: string }[] = [
  { field: null, label: 'Actions' },
  { field: 'name', label: 'Name' },
  { field: null, label: 'Description' },
  { field: null, label: 'Latest version' },
  { field: 'status', label: 'Status' },
  { field: 'creationDate', label: 'Date created' },
  { field: 'lastUpdated', label: 'Last updated' },
  { field: null, label: 'Creator' },
]

const sortParam = computed(() => (sortDescending.value ? '-' : '') + sortField.value)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const result = await listVocabularies({
      name: nameFilter.value || undefined,
      status: statusFilter.value || undefined,
      creationDate: createdFilter.value || undefined,
      sort: sortParam.value,
      page: page.value,
      pageSize: PAGE_SIZE,
    })
    rows.value = result.items
    total.value = result.total
    totalPages.value = result.totalPages
  } catch (error) {
    rows.value = []
    loadError.value = problemDetail(error)
  } finally {
    loading.value = false
  }
}

/** D50: a click on another column sorts it ascending; a click on the active column reverses it. */
function sortBy(field: SortField) {
  if (sortField.value === field) {
    sortDescending.value = !sortDescending.value
  } else {
    sortField.value = field
    sortDescending.value = false
  }
  page.value = 1
  void load()
}

function ariaSort(field: SortField | null): 'ascending' | 'descending' | 'none' | undefined {
  if (!field) return undefined
  if (sortField.value !== field) return 'none'
  return sortDescending.value ? 'descending' : 'ascending'
}

let filterTimer: ReturnType<typeof setTimeout> | undefined
watch([nameFilter, statusFilter, createdFilter], () => {
  clearTimeout(filterTimer)
  filterTimer = setTimeout(() => {
    page.value = 1
    void load()
  }, 250)
})

function goTo(target: number) {
  page.value = target
  void load()
}

async function changeStatus(vocabulary: Vocabulary) {
  success.value = ''
  actionError.value = ''
  const target = vocabulary.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE'
  try {
    const updated = await updateVocabularyStatus(vocabulary.name, target)
    rows.value = rows.value.map((row) => (row.name === updated.name ? updated : row))
    success.value = `Status of ${updated.name} changed to ${statusLabel(updated.status)}.`
  } catch (error) {
    const code = status(error)
    if (code === 404) {
      actionError.value = TEXT.statusNotFound
    } else if (code === 403) {
      actionError.value = TEXT.notAuthorized
    } else {
      actionError.value = `${TEXT.statusError} ${TEXT.genericError} ${problemDetail(error)}`.trim()
    }
  }
}

async function download(vocabulary: Vocabulary) {
  actionError.value = ''
  try {
    await downloadContent(vocabulary.name)
  } catch (error) {
    actionError.value =
      status(error) === 403 ? TEXT.notAuthorized : `${TEXT.genericError} ${problemDetail(error)}`
  }
}

function actions(vocabulary: Vocabulary): MenuAction[] {
  const list: MenuAction[] = [
    { label: 'View vocabulary details', run: () => router.push(`/vocabularies/${vocabulary.name}`) },
    { label: 'History', run: () => router.push(`/vocabularies/${vocabulary.name}#versions`) },
    { label: 'Download vocabulary', run: () => download(vocabulary) },
  ]
  if (admin) {
    list.push({ label: 'Update', run: () => router.push(`/vocabularies/${vocabulary.name}/versions/new`) })
    list.push({
      label: vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate',
      run: () => changeStatus(vocabulary),
    })
  }
  return list
}

onMounted(load)
</script>

<template>
  <h1>Vocabularies</h1>
  <div class="toolbar">
    <RouterLink v-if="admin" class="button primary" to="/vocabularies/new">New Vocabulary +</RouterLink>
  </div>

  <form class="filters" role="search" aria-label="Filter vocabularies" @submit.prevent="load">
    <div class="field">
      <label for="filter-name">Name contains</label>
      <input id="filter-name" v-model="nameFilter" type="search" maxlength="64" autocomplete="off" />
    </div>
    <div class="field">
      <label for="filter-status">Vocabulary status</label>
      <select id="filter-status" v-model="statusFilter">
        <option value="">All</option>
        <option value="ACTIVE">Active</option>
        <option value="DEPRECATED">Deprecated</option>
      </select>
    </div>
    <div class="field">
      <label for="filter-created">Created on</label>
      <input id="filter-created" v-model="createdFilter" type="date" />
    </div>
  </form>

  <div v-if="success" role="status" class="message success">{{ success }}</div>
  <div v-if="actionError" role="alert" class="message error">{{ actionError }}</div>
  <div v-if="loadError" role="alert" class="message error">
    {{ TEXT.listError }} <span class="detail">{{ loadError }}</span>
  </div>

  <table v-if="!loadError" class="data-table">
    <caption class="visually-hidden">
      Vocabularies
    </caption>
    <thead>
      <tr>
        <th v-for="column in columns" :key="column.label" scope="col" :aria-sort="ariaSort(column.field)">
          <button v-if="column.field" type="button" class="sort-button" @click="sortBy(column.field)">
            {{ column.label }}
            <span v-if="sortField === column.field" aria-hidden="true">{{ sortDescending ? '▼' : '▲' }}</span>
          </button>
          <template v-else>{{ column.label }}</template>
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="vocabulary in rows" :key="vocabulary.name">
        <td><ActionsMenu :label="`Actions for ${vocabulary.name}`" :actions="actions(vocabulary)" /></td>
        <td>
          <RouterLink :to="`/vocabularies/${vocabulary.name}`">{{ vocabulary.name }}</RouterLink>
        </td>
        <td>{{ vocabulary.description }}</td>
        <td>{{ vocabulary.version }}</td>
        <td><StatusCell :status="vocabulary.status" /></td>
        <td>{{ formatDate(vocabulary.creationDate) }}</td>
        <td>{{ formatDate(vocabulary.lastUpdateDate) }}</td>
        <td>{{ vocabulary.createdBy }}</td>
      </tr>
      <tr v-if="!loading && rows.length === 0">
        <td :colspan="columns.length">{{ TEXT.noVocabularies }}</td>
      </tr>
    </tbody>
  </table>

  <nav v-if="totalPages > 1" class="pager" aria-label="Pages">
    <button type="button" :disabled="page <= 1" @click="goTo(page - 1)">Previous page</button>
    <span>Page {{ page }} of {{ totalPages }} ({{ total }} vocabularies)</span>
    <button type="button" :disabled="page >= totalPages" @click="goTo(page + 1)">Next page</button>
  </nav>
</template>
