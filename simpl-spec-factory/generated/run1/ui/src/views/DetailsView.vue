<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '../api'
import { session } from '../auth'
import { formatDate } from '../format'
import { notify } from '../notify'
import type { VocabularyCategory, VocabularyMetadata } from '../types'
import DeprecationWarning from '../components/DeprecationWarning.vue'
import ProblemReport from '../components/ProblemReport.vue'
import StatusBadge from '../components/StatusBadge.vue'
import StatusChange from '../components/StatusChange.vue'

/** Vocabulary details and version history (SIMPL-18167, 18176, 18452, 18559). */
const props = defineProps<{ name: string }>()
const router = useRouter()
const vocabulary = ref<VocabularyCategory | null>(null)
const versions = ref<VocabularyMetadata[]>([])
const error = ref<ApiError | null>(null)
const loading = ref(false)
const showStatus = ref(false)
const statusFilter = ref('')

const latestActive = computed(() =>
  versions.value.filter((v) => v.status === 'ACTIVE').reduce<number | null>((m, v) => (m === null || v.version > m ? v.version : m), null))
const shown = computed(() => versions.value.filter((v) => !statusFilter.value || v.status === statusFilter.value))

async function load() {
  loading.value = true
  error.value = null
  try {
    vocabulary.value = await api.get(props.name)
    const all: VocabularyMetadata[] = []
    let page = 1
    for (;;) {
      const res = await api.versions(props.name, page, 100)
      all.push(...res.items)
      if (page >= res.totalPages) break
      page++
    }
    versions.value = all // newest first (chronological history)
  } catch (e) {
    error.value = e as ApiError
  } finally {
    loading.value = false
  }
}

async function download(version?: number) {
  try {
    const file = await api.download(props.name, version)
    notify('success', `Downloaded ${file}.`)
  } catch (e) {
    notify('error', `The vocabulary could not be downloaded: ${(e as ApiError).message}`)
  }
}

function changed() {
  showStatus.value = false
  load()
}

onMounted(load)
watch(() => props.name, load)
</script>

<template>
  <p><RouterLink to="/">Back to all vocabularies</RouterLink></p>
  <p v-if="loading" aria-busy="true">Loading...</p>
  <ProblemReport v-if="error" :error="error"
                 :title="error.status === 404 ? 'The vocabulary could not be found.' : 'The vocabulary could not be loaded.'" />

  <template v-if="vocabulary">
    <h2 data-testid="vocabulary-name">{{ vocabulary.name }}</h2>
    <DeprecationWarning v-if="vocabulary.status === 'DEPRECATED'" :name="vocabulary.name"
                        :latest-active="latestActive" subject="vocabulary" />
    <dl class="meta">
      <dt>Description</dt><dd>{{ vocabulary.description }}</dd>
      <dt>Current version</dt><dd>{{ vocabulary.version }}</dd>
      <dt>Status</dt><dd><StatusBadge :status="vocabulary.status" /></dd>
      <dt>Date created</dt><dd>{{ formatDate(vocabulary.creationDate) }}</dd>
      <dt>Last updated</dt><dd>{{ formatDate(vocabulary.lastUpdateDate) }}</dd>
      <dt>Creator</dt><dd>{{ vocabulary.createdBy }}</dd>
    </dl>
    <div class="row-actions" style="margin:1rem 0">
      <button @click="download()">Download latest version</button>
      <button v-if="session.isAdmin && vocabulary.status === 'ACTIVE'" data-testid="update-button"
              @click="router.push(`/vocabularies/${vocabulary.name}/new-version`)">Update (new version)</button>
      <button v-if="session.isAdmin" class="secondary" data-testid="status-button"
              @click="showStatus = !showStatus">Change status</button>
    </div>
    <p v-if="session.isAdmin && vocabulary.status === 'DEPRECATED'"><small class="hint">
      A deprecated vocabulary cannot get a new version. Set its status to Active first.</small></p>
    <StatusChange v-if="showStatus" :vocabulary="vocabulary" @changed="changed" @cancel="showStatus = false" />

    <h3>Version history</h3>
    <label style="max-width:16rem">Show
      <select v-model="statusFilter">
        <option value="">All versions</option>
        <option value="ACTIVE">Active versions</option>
        <option value="DEPRECATED">Deprecated versions</option>
      </select>
    </label>
    <table data-testid="version-history">
      <thead>
        <tr><th>Actions</th><th>Version</th><th>Status</th><th>Date created</th><th>Description</th><th>Change log</th><th>Creator</th></tr>
      </thead>
      <tbody>
        <template v-for="v in shown" :key="v.version">
          <tr :data-version="v.version">
            <td>
              <div class="row-actions">
                <button class="secondary outline" style="padding:0.2rem 0.5rem" @click="download(v.version)">Download</button>
                <RouterLink :to="`/vocabularies/${v.name}/versions/${v.version}`">View</RouterLink>
              </div>
            </td>
            <td>{{ v.version }}</td>
            <td><StatusBadge :status="v.status" /></td>
            <td>{{ formatDate(v.creationDate) }}</td>
            <td>{{ v.description }}</td>
            <td>{{ v.changelog }}</td>
            <td>{{ v.createdBy }}</td>
          </tr>
          <tr v-if="v.status === 'DEPRECATED'">
            <td colspan="7" style="padding-top:0">
              <DeprecationWarning :name="v.name" :version="v.version" :latest-active="latestActive" subject="version" />
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </template>
</template>
