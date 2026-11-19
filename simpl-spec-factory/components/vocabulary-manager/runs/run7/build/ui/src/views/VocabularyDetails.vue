<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import ActionsMenu, { type MenuAction } from '../components/ActionsMenu.vue'
import {
  downloadContent,
  getVocabulary,
  listAllVersions,
  updateVocabularyStatus,
  type ValidationWarning,
  type Vocabulary,
  type VocabularyVersion,
} from '../lib/api'
import { isAdmin } from '../lib/auth'
import { takeFlash } from '../lib/flash'
import { formatDate, statusLabel } from '../lib/format'
import { problemDetail, status, TEXT } from '../lib/messages'

/**
 * Vocabulary details with the version history (SIMPL-18167, 18176, 18452, 18559; D35, D36, D64, D72).
 */
const route = useRoute()
const router = useRouter()
const name = computed(() => String(route.params.name))
const admin = isAdmin()

const vocabulary = ref<Vocabulary | null>(null)
const versions = ref<VocabularyVersion[]>([])
const notFound = ref(false)
const loadError = ref('')
const notice = ref('')
const warnings = ref<ValidationWarning[]>([])
const actionError = ref('')

const selectedNumber = computed(() => {
  const value = Number(route.query.version)
  return Number.isInteger(value) && value > 0 ? value : null
})
const selected = computed(() => versions.value.find((v) => v.version === selectedNumber.value) ?? null)

/** Latest ACTIVE version: highest-numbered version with status ACTIVE (D5). */
const latestActive = computed(() => {
  const active = versions.value.filter((v) => v.status === 'ACTIVE').map((v) => v.version)
  return active.length ? Math.max(...active) : null
})

function effectivelyDeprecated(version: VocabularyVersion): boolean {
  return version.status === 'DEPRECATED' || vocabulary.value?.status === 'DEPRECATED'
}

/** D36 with D64: a link to the latest ACTIVE version only for an ACTIVE vocabulary, never to itself. */
function activeLink(version: VocabularyVersion): number | null {
  if (vocabulary.value?.status !== 'ACTIVE') return null
  const target = latestActive.value
  return target !== null && target !== version.version ? target : null
}

async function load() {
  notFound.value = false
  loadError.value = ''
  try {
    vocabulary.value = await getVocabulary(name.value)
    versions.value = await listAllVersions(name.value)
  } catch (error) {
    vocabulary.value = null
    versions.value = []
    if (status(error) === 404) {
      notFound.value = true
    } else {
      loadError.value = problemDetail(error)
    }
  }
}

function openVersion(version: number) {
  void router.replace({ query: { ...route.query, version: String(version) }, hash: '#version-panel' })
}

function closeVersion() {
  const query = { ...route.query }
  delete query.version
  void router.replace({ query })
}

async function download(version?: number) {
  actionError.value = ''
  try {
    await downloadContent(name.value, version)
  } catch (error) {
    actionError.value =
      status(error) === 403 ? TEXT.notAuthorized : `${TEXT.genericError} ${problemDetail(error)}`
  }
}

async function changeStatus() {
  if (!vocabulary.value) return
  notice.value = ''
  actionError.value = ''
  const target = vocabulary.value.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE'
  try {
    const updated = await updateVocabularyStatus(vocabulary.value.name, target)
    vocabulary.value = updated
    notice.value = `Status of ${updated.name} changed to ${statusLabel(updated.status)}.`
  } catch (error) {
    const code = status(error)
    if (code === 404) actionError.value = TEXT.statusNotFound
    else if (code === 403) actionError.value = TEXT.notAuthorized
    else actionError.value = `${TEXT.statusError} ${TEXT.genericError} ${problemDetail(error)}`.trim()
  }
}

function versionActions(version: VocabularyVersion): MenuAction[] {
  return [
    { label: 'View version details', run: () => openVersion(version.version) },
    { label: 'Download', run: () => download(version.version) },
  ]
}

watch(name, load)
onMounted(async () => {
  const flashed = takeFlash()
  notice.value = flashed.message
  warnings.value = flashed.warnings
  await load()
})
</script>

<template>
  <p><RouterLink to="/vocabularies">Back to vocabulary list</RouterLink></p>

  <div v-if="notFound" role="alert" class="message error">{{ TEXT.vocabularyNotFound }}</div>
  <div v-else-if="loadError" role="alert" class="message error">
    {{ TEXT.historyError }} <span class="detail">{{ loadError }}</span>
  </div>

  <div v-if="notice" role="status" class="message success">{{ notice }}</div>
  <section v-if="warnings.length" aria-label="Validation warnings" class="panel warnings">
    <h2>Validation warnings</h2>
    <ul>
      <li v-for="(warning, index) in warnings" :key="index">
        {{ warning.description }} <span class="location">({{ warning.location }})</span>
      </li>
    </ul>
  </section>
  <div v-if="actionError" role="alert" class="message error">{{ actionError }}</div>

  <template v-if="vocabulary">
    <h1>{{ vocabulary.name }}</h1>
    <div v-if="vocabulary.status === 'DEPRECATED'" class="banner warning">
      {{ TEXT.vocabularyDeprecated }}
    </div>
    <dl class="summary">
      <dt>Status</dt>
      <dd>{{ statusLabel(vocabulary.status) }}</dd>
      <dt>Description</dt>
      <dd>{{ vocabulary.description }}</dd>
      <dt>Latest version</dt>
      <dd>{{ vocabulary.version }}</dd>
    </dl>
    <div class="toolbar">
      <button type="button" @click="download()">Download vocabulary</button>
      <RouterLink v-if="admin" class="button" :to="`/vocabularies/${vocabulary.name}/versions/new`"
        >Update</RouterLink
      >
      <button v-if="admin" type="button" @click="changeStatus">
        {{ vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate' }}
      </button>
    </div>

    <h2 id="versions">Version history</h2>
    <table class="data-table">
      <caption class="visually-hidden">
        Versions of
        {{
          vocabulary.name
        }}
      </caption>
      <thead>
        <tr>
          <th scope="col">Version</th>
          <th scope="col">Status</th>
          <th scope="col">Date created</th>
          <th scope="col">Description</th>
          <th scope="col">Change log</th>
          <th scope="col">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="version in versions"
          :key="version.version"
          :class="{ selected: version.version === selectedNumber }"
        >
          <td>
            <button type="button" class="link-button" @click="openVersion(version.version)">
              {{ version.version }}
            </button>
          </td>
          <td>
            {{ statusLabel(version.status) }}
            <div v-if="effectivelyDeprecated(version)" class="version-warning">
              <span>{{ TEXT.versionDeprecated }}</span>
              <RouterLink
                v-if="activeLink(version) !== null"
                :to="{ query: { version: String(activeLink(version)) } }"
                >Latest ACTIVE version: {{ activeLink(version) }}</RouterLink
              >
            </div>
          </td>
          <td>{{ formatDate(version.creationDate) }}</td>
          <td>{{ version.description }}</td>
          <td>{{ version.changelog }}</td>
          <td>
            <ActionsMenu
              :label="`Actions for version ${version.version}`"
              :actions="versionActions(version)"
            />
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="selected" id="version-panel" :aria-label="`Version ${selected.version}`" class="panel">
      <h2>Version {{ selected.version }}</h2>
      <div v-if="effectivelyDeprecated(selected)" class="banner warning">
        <span>{{ TEXT.versionDeprecated }}</span>
        <RouterLink
          v-if="activeLink(selected) !== null"
          :to="{ query: { version: String(activeLink(selected)) } }"
          >Latest ACTIVE version: {{ activeLink(selected) }}</RouterLink
        >
      </div>
      <dl class="summary">
        <dt>Version</dt>
        <dd>{{ selected.version }}</dd>
        <dt>Status</dt>
        <dd>{{ statusLabel(selected.status) }}</dd>
        <dt>Date created</dt>
        <dd>{{ formatDate(selected.creationDate) }}</dd>
        <dt>Creator</dt>
        <dd>{{ selected.createdBy }}</dd>
        <dt>Description</dt>
        <dd>{{ selected.description }}</dd>
        <dt>Change log</dt>
        <dd>{{ selected.changelog }}</dd>
      </dl>
      <div class="toolbar">
        <button type="button" @click="download(selected.version)">Download</button>
        <button type="button" @click="closeVersion">Close</button>
      </div>
    </section>
  </template>
</template>
