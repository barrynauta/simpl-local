<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '../api'
import { session } from '../auth'
import { formatDate } from '../format'
import { notify } from '../notify'
import type { Page, VocabularyCategory } from '../types'
import ProblemReport from '../components/ProblemReport.vue'
import StatusBadge from '../components/StatusBadge.vue'
import StatusChange from '../components/StatusChange.vue'

/** Vocabulary list with filters, sorting and paging (SIMPL-18161, 18163, 18178). */
const router = useRouter()
const filters = reactive({ name: '', status: '', creationDate: '', lastUpdated: '' })
const sort = ref('name')
const page = ref(1)
const pageSize = ref(20)
const result = ref<Page<VocabularyCategory> | null>(null)
const loading = ref(false)
const error = ref<ApiError | null>(null)
const nameHint = ref('')
const openMenu = ref<string | null>(null)
const statusTarget = ref<VocabularyCategory | null>(null)

const deprecatedCount = computed(() => result.value?.items.filter((v) => v.status === 'DEPRECATED').length ?? 0)

/** D23: the API expects PascalCase; the UI capitalises the first letter and the API matches case-insensitively. */
function nameParam(): string | undefined {
  const raw = filters.name.trim()
  nameHint.value = ''
  if (!raw) return undefined
  const value = raw.charAt(0).toUpperCase() + raw.slice(1)
  if (!/^[A-Z][a-zA-Z0-9]*$/.test(value) || value.length < 3 || value.length > 64) {
    nameHint.value = 'Enter 3 to 64 letters or digits to filter by name.'
    return undefined
  }
  return value
}

async function load() {
  loading.value = true
  error.value = null
  try {
    result.value = await api.list({
      name: nameParam(),
      status: filters.status as '' | 'ACTIVE' | 'DEPRECATED',
      creationDate: filters.creationDate || undefined,
      lastUpdated: filters.lastUpdated || undefined,
      sort: sort.value,
      page: page.value,
      pageSize: pageSize.value,
    })
  } catch (e) {
    error.value = e as ApiError
    result.value = null
  } finally {
    loading.value = false
  }
}

let timer: number | undefined
watch(filters, () => {
  window.clearTimeout(timer)
  timer = window.setTimeout(() => {
    page.value = 1
    load()
  }, 300)
})
watch([page, pageSize], load)

function sortBy(field: string) {
  sort.value = sort.value === field ? `-${field}` : field
  page.value = 1
  load()
}

function arrow(field: string) {
  if (sort.value === field) return ' (ascending)'
  if (sort.value === `-${field}`) return ' (descending)'
  return ''
}

async function download(v: VocabularyCategory) {
  openMenu.value = null
  try {
    const file = await api.download(v.name)
    notify('success', `Downloaded ${file} (version ${v.version}).`)
  } catch (e) {
    notify('error', `The vocabulary could not be downloaded: ${(e as ApiError).message}`)
  }
}

function changed() {
  statusTarget.value = null
  load()
}

function toggleMenu(name: string) {
  openMenu.value = openMenu.value === name ? null : name
}

onMounted(() => {
  if (session.canRead) load()
  document.addEventListener('click', (ev) => {
    if (!(ev.target as HTMLElement).closest('td.actions')) openMenu.value = null
  })
})
</script>

<template>
  <div class="topbar">
    <h2 style="margin:0">Vocabularies</h2>
    <button v-if="session.isAdmin" style="width:auto" data-testid="new-vocabulary"
            @click="router.push('/vocabularies/new')">New Vocabulary +</button>
  </div>

  <form class="filters" role="search" @submit.prevent="load">
    <label>Name contains
      <input v-model="filters.name" name="name" placeholder="e.g. shacl" autocomplete="off" />
    </label>
    <label>Vocabulary status
      <select v-model="filters.status" name="status">
        <option value="">All</option>
        <option value="ACTIVE">Active</option>
        <option value="DEPRECATED">Deprecated</option>
      </select>
    </label>
    <label>Date created
      <input v-model="filters.creationDate" type="date" name="creationDate" />
    </label>
    <label>Last updated
      <input v-model="filters.lastUpdated" type="date" name="lastUpdated" />
    </label>
  </form>
  <small v-if="nameHint" class="field-error">{{ nameHint }}</small>

  <StatusChange v-if="statusTarget" :vocabulary="statusTarget" @changed="changed" @cancel="statusTarget = null" />

  <ProblemReport v-if="error" :error="error" title="The vocabulary list could not be loaded." />

  <div v-if="deprecatedCount > 0" class="warning" role="alert" data-testid="list-deprecation-warning">
    <strong>Warning:</strong> {{ deprecatedCount }} deprecated
    {{ deprecatedCount === 1 ? 'vocabulary is' : 'vocabularies are' }} shown. Deprecated vocabularies should not be
    used in new resources.
  </div>

  <p v-if="loading" aria-busy="true">Loading vocabularies...</p>

  <figure v-if="result" style="overflow:visible">
    <table data-testid="vocabulary-table">
      <thead>
        <tr>
          <th>Actions</th>
          <th class="sortable" @click="sortBy('name')">Name{{ arrow('name') }}</th>
          <th>Description</th>
          <th>Version</th>
          <th class="sortable" @click="sortBy('status')">Status{{ arrow('status') }}</th>
          <th class="sortable" @click="sortBy('creationDate')">Date created{{ arrow('creationDate') }}</th>
          <th class="sortable" @click="sortBy('lastUpdated')">Last updated{{ arrow('lastUpdated') }}</th>
          <th>Creator</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="v in result.items" :key="v.name" :data-name="v.name">
          <td class="actions">
            <button class="secondary outline dots" aria-label="Actions" @click.stop="toggleMenu(v.name)">...</button>
            <div v-if="openMenu === v.name" class="menu" role="menu">
              <button role="menuitem" @click="router.push(`/vocabularies/${v.name}`)">View vocabulary details</button>
              <button role="menuitem" @click="router.push(`/vocabularies/${v.name}`)">View version history</button>
              <button role="menuitem" @click="download(v)">Download vocabulary</button>
              <button v-if="session.isAdmin && v.status === 'ACTIVE'" role="menuitem"
                      @click="router.push(`/vocabularies/${v.name}/new-version`)">Update (new version)</button>
              <button v-if="session.isAdmin" role="menuitem"
                      @click="statusTarget = v; openMenu = null">Change status</button>
            </div>
          </td>
          <td><RouterLink :to="`/vocabularies/${v.name}`">{{ v.name }}</RouterLink></td>
          <td>{{ v.description }}</td>
          <td>{{ v.version }}</td>
          <td>
            <StatusBadge :status="v.status" />
            <div v-if="v.status === 'DEPRECATED'"><small>Do not use in new resources.</small></div>
          </td>
          <td>{{ formatDate(v.creationDate) }}</td>
          <td>{{ formatDate(v.lastUpdateDate) }}</td>
          <td>{{ v.createdBy }}</td>
        </tr>
        <tr v-if="result.items.length === 0"><td colspan="8">No vocabularies match the filters.</td></tr>
      </tbody>
    </table>
  </figure>

  <div v-if="result" class="pager">
    <button class="secondary outline" :disabled="page <= 1" @click="page--">Previous</button>
    <span>Page {{ result.page }} of {{ Math.max(result.totalPages, 1) }} ({{ result.total }} vocabularies)</span>
    <button class="secondary outline" :disabled="page >= result.totalPages" @click="page++">Next</button>
    <label style="margin:0">Per page
      <select v-model.number="pageSize" style="width:auto">
        <option :value="10">10</option><option :value="20">20</option>
        <option :value="50">50</option><option :value="100">100</option>
      </select>
    </label>
  </div>
</template>
