<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '../api'
import { session } from '../auth'
import { getConfig } from '../config'
import { notify } from '../notify'
import { internalReferences, precheck, type PrecheckIssue } from '../turtle'
import type { SearchItem, VocabularyCategory, VocabularyMetadata } from '../types'
import ProblemReport from '../components/ProblemReport.vue'

/**
 * New version form (SIMPL-18172, 18189, 18184, 18180 AC3). Pre-filled with the latest metadata and content.
 * The version number is assigned by the API and shown in the confirmation.
 */
const props = defineProps<{ name: string }>()
const router = useRouter()
const base = getConfig().referenceBase
const current = ref<VocabularyCategory | null>(null)
const form = reactive({ description: '', changelog: '', content: '' })
const uploaded = ref<File | null>(null)
const errors = reactive<Record<string, string>>({})
const pre = ref<PrecheckIssue[] | null>(null)
const apiError = ref<ApiError | null>(null)
const loadError = ref<ApiError | null>(null)
const busy = ref(false)

// Reference picker and deprecated-reference warnings (SIMPL-18184, D18, D25).
const vocabularies = ref<VocabularyCategory[]>([])
const pickName = ref('')
const pickVersions = ref<VocabularyMetadata[]>([])
const pickVersion = ref<number | ''>('')
const refStatus = ref<SearchItem[]>([])
const refLatest = ref<Record<string, SearchItem>>({})

const picked = computed(() => pickVersions.value.find((v) => v.version === pickVersion.value) ?? null)
const pickedLatestActive = computed(() => {
  const active = pickVersions.value.filter((v) => v.status === 'ACTIVE').map((v) => v.version)
  return active.length ? Math.max(...active) : null
})

async function load() {
  try {
    const vocabulary = await api.get(props.name)
    form.description = vocabulary.description
    form.content = await api.contentText(props.name)
    pre.value = precheck(form.content)
    current.value = vocabulary // show the form only when metadata and content are loaded
    const list = await api.list({ pageSize: 100, sort: 'name' })
    vocabularies.value = list.items.filter((v) => v.name !== props.name)
  } catch (e) {
    loadError.value = e as ApiError
  }
}

watch(pickName, async (n) => {
  pickVersions.value = []
  pickVersion.value = ''
  if (!n) return
  const res = await api.versions(n, 1, 100)
  pickVersions.value = res.items
})

function insertReference() {
  if (!picked.value) return
  const prefix = picked.value.name.replace(/[^A-Za-z0-9]/g, '').toLowerCase()
  const line = `@prefix ${prefix}${picked.value.version}: <${base}/${picked.value.name}/${picked.value.version}#> .\n`
  form.content = line + form.content
  uploaded.value = null
}

let timer: number | undefined
watch(() => form.content, (text) => {
  window.clearTimeout(timer)
  timer = window.setTimeout(() => checkContent(text), 400)
})

async function checkContent(text: string) {
  pre.value = precheck(text)
  const refs = internalReferences(text, base).filter((r) => r.name !== props.name)
  if (refs.length === 0) {
    refStatus.value = []
    return
  }
  try {
    const items = refs.flatMap((r) => (r.version ? [{ name: r.name, version: r.version }, { name: r.name }] : [{ name: r.name }]))
    const res = await api.search(items)
    refStatus.value = res.items.filter((i) => refs.some((r) => r.name === i.name && (r.version ?? null) === (i.latest ? null : i.version)))
    const latest: Record<string, SearchItem> = {}
    for (const i of res.items) if (i.latest) latest[i.name] = i
    refLatest.value = latest
  } catch {
    refStatus.value = []
  }
}

const deprecatedRefs = computed(() => refStatus.value.filter((i) => i.status === 'DEPRECATED'))

async function onFile(ev: Event) {
  const f = (ev.target as HTMLInputElement).files?.[0] ?? null
  delete errors.file
  if (!f) return
  if (!/\.ttl$/i.test(f.name) && f.type !== 'text/turtle') {
    errors.file = 'Unsupported file format. Upload a Turtle (.ttl) file.'
    return
  }
  uploaded.value = f
  form.content = await f.text()
}

async function submit() {
  apiError.value = null
  for (const k of Object.keys(errors)) delete errors[k]
  if (!form.description.trim()) errors.description = 'Description is required.'
  if (!form.changelog.trim()) errors.changelog = 'A change summary is required.'
  if (!form.content.trim()) errors.file = 'The vocabulary content is required.'
  if (Object.keys(errors).length) return
  busy.value = true
  try {
    const blob = new Blob([form.content], { type: 'text/turtle' })
    const created = await api.createVersion(props.name, form.description.trim(), form.changelog.trim(), blob,
      uploaded.value?.name ?? `${props.name}.ttl`)
    notify('success',
      `Validation passed. Version ${created.version} of ${created.name} was successfully created and is now Active. ` +
      'All previous versions are now Deprecated.', created.validationWarnings)
    router.push(`/vocabularies/${props.name}`)
  } catch (e) {
    const err = e as ApiError
    apiError.value = err
    if (err.status >= 500 || err.status === 0) {
      notify('error', 'The new version could not be created. Please retry or contact support.')
    }
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <p><RouterLink :to="`/vocabularies/${name}`">Back to {{ name }}</RouterLink></p>
  <h2>New version of {{ name }}</h2>
  <div v-if="!session.isAdmin" class="error-box">You are not authorised to create vocabulary versions.</div>
  <ProblemReport v-else-if="loadError" :error="loadError" title="The vocabulary could not be loaded." />
  <form v-else-if="current" novalidate data-testid="new-version-form" @submit.prevent="submit">
    <p><small class="hint">Current version: {{ current.version }}. The next version number is assigned automatically
      when you submit.</small></p>
    <div v-if="current.status === 'DEPRECATED'" class="warning">This vocabulary is deprecated. Set its status to Active
      before creating a new version.</div>
    <label>Description
      <textarea v-model="form.description" name="description" rows="2"></textarea>
      <small v-if="errors.description" class="field-error">{{ errors.description }}</small>
    </label>
    <label>Change summary
      <textarea v-model="form.changelog" name="changelog" rows="2" placeholder="What changed in this version?"></textarea>
      <small v-if="errors.changelog" class="field-error">{{ errors.changelog }}</small>
    </label>
    <label>Upload an updated file (Turtle)
      <input type="file" name="vocabularyFile" accept=".ttl,text/turtle" @change="onFile" />
      <small v-if="errors.file" class="field-error">{{ errors.file }}</small>
    </label>
    <label>Content (pre-filled with the current version; edit it or upload a file)
      <textarea v-model="form.content" class="content" name="content"></textarea>
    </label>
    <div v-if="pre && pre.length === 0" class="success-box" data-testid="precheck-ok">
      Syntax and structure pre-check passed. The full validation runs when you submit.
    </div>
    <div v-if="pre && pre.length" class="error-box" data-testid="precheck-errors">
      <strong>Syntax and structure pre-check failed.</strong>
      <ul><li v-for="(i, n) in pre" :key="n">{{ i.detail }} <code>{{ i.location }}</code></li></ul>
    </div>

    <article>
      <header><strong>Reference another vocabulary version</strong></header>
      <div class="grid">
        <label>Vocabulary
          <select v-model="pickName">
            <option value="">Select a vocabulary</option>
            <option v-for="v in vocabularies" :key="v.name" :value="v.name">{{ v.name }}</option>
          </select>
        </label>
        <label>Version
          <select v-model="pickVersion" :disabled="!pickVersions.length">
            <option value="">Select a version</option>
            <option v-for="v in pickVersions" :key="v.version" :value="v.version">
              {{ v.version }} ({{ v.status === 'ACTIVE' ? 'Active' : 'Deprecated' }})
            </option>
          </select>
        </label>
      </div>
      <div v-if="picked && picked.status === 'DEPRECATED'" class="warning" role="alert" data-testid="picked-deprecated">
        <strong>Warning:</strong> {{ picked.name }} version {{ picked.version }} is deprecated.
        <template v-if="pickedLatestActive">Select the latest active version ({{ pickedLatestActive }}) instead.</template>
        <template v-else>No active version of {{ picked.name }} is available.</template>
      </div>
      <button type="button" class="secondary" style="width:auto" :disabled="!picked" @click="insertReference">
        Add prefix for this version</button>
      <small class="hint"> References use the namespace {{ base }}/Name/version#.</small>
    </article>

    <div v-for="r in deprecatedRefs" :key="r.href" class="warning" role="alert" data-testid="reference-deprecated">
      <strong>Warning:</strong> the content references {{ r.name }} version {{ r.version }}, which is deprecated.
      <template v-if="refLatest[r.name] && refLatest[r.name].status === 'ACTIVE'">
        Use the latest active version {{ refLatest[r.name].version }}
        (<code>{{ base }}/{{ r.name }}/{{ refLatest[r.name].version }}#</code>).
      </template>
      <template v-else>No active version of {{ r.name }} is available.</template>
      Submitting will fail validation.
    </div>

    <ProblemReport v-if="apiError" :error="apiError" title="The new version was not created." />
    <div class="row-actions">
      <button type="submit" :aria-busy="busy" :disabled="busy || (pre !== null && pre.length > 0)">Create new version</button>
      <button type="button" class="secondary outline" @click="router.push(`/vocabularies/${name}`)">Cancel</button>
    </div>
  </form>
</template>
