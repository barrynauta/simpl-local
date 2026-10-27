<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '../api'
import { session } from '../auth'
import { notify } from '../notify'
import { precheck, type PrecheckIssue } from '../turtle'
import ProblemReport from '../components/ProblemReport.vue'

/** Create a vocabulary (SIMPL-18169, 18180, 18182, 18193). No version or change log field (D12). */
const router = useRouter()
const form = reactive({ name: '', description: '' })
const file = ref<File | null>(null)
const errors = reactive<Record<string, string>>({})
const pre = ref<PrecheckIssue[] | null>(null)
const busy = ref(false)
const apiError = ref<ApiError | null>(null)

async function onFile(ev: Event) {
  const f = (ev.target as HTMLInputElement).files?.[0] ?? null
  file.value = f
  pre.value = null
  delete errors.file
  if (!f) return
  if (!/\.ttl$/i.test(f.name) && f.type !== 'text/turtle') {
    errors.file = 'Unsupported file format. Upload a Turtle (.ttl) file.'
    return
  }
  pre.value = precheck(await f.text())
}

function validate(): boolean {
  for (const k of Object.keys(errors)) delete errors[k]
  if (!form.name.trim()) errors.name = 'Name is required.'
  else if (!/^[A-Z][a-zA-Z0-9]*$/.test(form.name.trim()) || form.name.trim().length < 3 || form.name.trim().length > 64)
    errors.name = 'Use 3 to 64 letters or digits in PascalCase, starting with a capital letter (for example HealthcareVocabulary).'
  if (!form.description.trim()) errors.description = 'Description is required.'
  if (!file.value) errors.file = 'A vocabulary file is required.'
  else if (!/\.ttl$/i.test(file.value.name) && file.value.type !== 'text/turtle')
    errors.file = 'Unsupported file format. Upload a Turtle (.ttl) file.'
  return Object.keys(errors).length === 0
}

async function submit() {
  apiError.value = null
  if (!validate()) return
  busy.value = true
  try {
    const created = await api.create(form.name.trim(), form.description.trim(), file.value!, file.value!.name)
    notify('success',
      `Validation passed. Vocabulary ${created.name} was created as version ${created.version} with status Active.`,
      created.validationWarnings)
    router.push(`/vocabularies/${created.name}`)
  } catch (e) {
    const err = e as ApiError
    apiError.value = err
    if (err.status === 409) errors.name = 'A vocabulary with this name already exists. The name must be unique.'
    if (err.status === 415) errors.file = 'Unsupported file format. Upload a Turtle (.ttl) file.'
    if (err.status === 413) errors.file = err.problem?.detail ?? 'The file is too large.'
    for (const issue of err.problem?.issues ?? []) {
      if (issue.name === 'name' && !errors.name) errors.name = issue.detail ?? 'Invalid name.'
      if (issue.name === 'description') errors.description = issue.detail ?? 'Invalid description.'
    }
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <h2>New vocabulary</h2>
  <div v-if="!session.isAdmin" class="error-box">You are not authorised to create vocabularies.</div>
  <form v-else novalidate data-testid="create-form" @submit.prevent="submit">
    <label>Name
      <input v-model="form.name" name="name" required placeholder="HealthcareVocabulary"
             :aria-invalid="errors.name ? 'true' : undefined" />
      <small v-if="errors.name" class="field-error" data-testid="error-name">{{ errors.name }}</small>
      <small v-else class="hint">PascalCase, letters and digits, 3 to 64 characters. Must be unique.</small>
    </label>
    <label>Description
      <textarea v-model="form.description" name="description" required rows="3"
                :aria-invalid="errors.description ? 'true' : undefined"></textarea>
      <small v-if="errors.description" class="field-error" data-testid="error-description">{{ errors.description }}</small>
    </label>
    <label>Vocabulary file (Turtle)
      <input type="file" name="vocabularyFile" accept=".ttl,text/turtle" required
             :aria-invalid="errors.file ? 'true' : undefined" @change="onFile" />
      <small v-if="errors.file" class="field-error" data-testid="error-file">{{ errors.file }}</small>
    </label>
    <div v-if="pre && pre.length === 0" class="success-box" data-testid="precheck-ok">
      Syntax and structure pre-check passed. The full validation runs when you submit.
    </div>
    <div v-if="pre && pre.length" class="error-box" data-testid="precheck-errors">
      <strong>Syntax and structure pre-check failed.</strong> Fix these errors before submitting:
      <ul><li v-for="(i, n) in pre" :key="n">{{ i.detail }} <code>{{ i.location }}</code></li></ul>
    </div>
    <ProblemReport v-if="apiError" :error="apiError" title="The vocabulary was not created." />
    <div class="row-actions">
      <button type="submit" :aria-busy="busy" :disabled="busy || (pre !== null && pre.length > 0)">Create vocabulary</button>
      <button type="button" class="secondary outline" @click="router.push('/')">Cancel</button>
    </div>
  </form>
</template>
