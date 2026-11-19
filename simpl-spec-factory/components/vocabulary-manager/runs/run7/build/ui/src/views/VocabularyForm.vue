<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createVersion,
  createVocabulary,
  fetchContent,
  getVocabulary,
  type CreatedVersion,
  type ProblemIssue,
} from '../lib/api'
import { isAdmin } from '../lib/auth'
import { setFlash } from '../lib/flash'
import { problemDetail, status, TEXT } from '../lib/messages'
import { checkTurtle, MAX_FILE_BYTES, nameError, textError } from '../lib/rules'

/**
 * Create form (/vocabularies/new) and new version form (/vocabularies/<Name>/versions/new):
 * SIMPL-18169, 18172, 18180, 18182, 18189, 18193; D13, D14, D32, D33, D37, D46, D60, D62, D72, D76.
 */
const props = defineProps<{ mode: 'create' | 'version' }>()
const route = useRoute()
const router = useRouter()
const admin = isAdmin()
const vocabularyName = computed(() => (props.mode === 'version' ? String(route.params.name) : ''))

const name = ref('')
const description = ref('')
const changelog = ref('')
const file = ref<File | null>(null)
const latestVersion = ref(0)
const loaded = ref(props.mode === 'create')

const errors = ref<Record<string, string>>({})
const fileError = ref('')
const syntaxOk = ref(false)
const formMessage = ref('')
const report = ref<ProblemIssue[]>([])
const submitting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const contentBase = 'https://authority.example/vocabulary-manager'

/** D62: Submit is disabled only while the upload check shows a syntax error or a refused file. */
const submitDisabled = computed(() => submitting.value || fileError.value !== '')

onMounted(async () => {
  if (!admin || props.mode === 'create') return
  try {
    const vocabulary = await getVocabulary(vocabularyName.value)
    description.value = vocabulary.description
    latestVersion.value = vocabulary.version
    loaded.value = true
  } catch (error) {
    formMessage.value =
      status(error) === 404 ? TEXT.vocabularyNotFound : `${TEXT.genericError} ${problemDetail(error)}`
  }
})

async function onFile(event: Event) {
  const input = event.target as HTMLInputElement
  const chosen = input.files?.[0] ?? null
  file.value = chosen
  fileError.value = ''
  syntaxOk.value = false
  delete errors.value.vocabularyFile
  if (!chosen) return
  if (chosen.size > MAX_FILE_BYTES) {
    fileError.value = TEXT.fileTooLarge
    return
  }
  const target = props.mode === 'create' ? name.value || 'New' : vocabularyName.value
  const result = checkTurtle(await chosen.text(), `${contentBase}/content/${target}/versions/1`)
  if (result.ok) {
    syntaxOk.value = true
  } else {
    fileError.value = result.message
  }
}

function validate(): boolean {
  const found: Record<string, string> = {}
  if (props.mode === 'create') {
    const message = nameError(name.value)
    if (message) found.name = message
  }
  const descriptionMessage = textError('Description', description.value)
  if (descriptionMessage) found.description = descriptionMessage
  if (props.mode === 'version') {
    const changelogMessage = textError('Change log', changelog.value)
    if (changelogMessage) found.changelog = changelogMessage
  }
  if (props.mode === 'create' && !file.value) found.vocabularyFile = 'Vocabulary file is required.'
  errors.value = found
  return Object.keys(found).length === 0
}

async function focusFirstError() {
  await nextTick()
  const order = ['name', 'description', 'changelog', 'vocabularyFile']
  const first = order.find((field) => errors.value[field])
  if (first) document.getElementById(`field-${first}`)?.focus()
}

async function submit() {
  formMessage.value = ''
  report.value = []
  if (!validate()) {
    await focusFirstError()
    return
  }
  submitting.value = true
  try {
    const form = new FormData()
    let created: CreatedVersion
    if (props.mode === 'create') {
      form.append('name', name.value)
      form.append('description', description.value)
      form.append('changelog', 'Initial version.')
      form.append('vocabularyFile', file.value as File, (file.value as File).name)
      created = await createVocabulary(form)
    } else {
      form.append('description', description.value)
      form.append('changelog', changelog.value)
      if (file.value) {
        form.append('vocabularyFile', file.value, file.value.name)
      } else {
        const current = await fetchContent(vocabularyName.value)
        form.append('vocabularyFile', current, `${vocabularyName.value}.ttl`)
      }
      created = await createVersion(vocabularyName.value, form)
    }
    setFlash(`Version ${created.version} of ${created.name} was created.`, created.validationWarnings ?? [])
    await router.push(`/vocabularies/${created.name}`)
  } catch (error) {
    handleError(error)
  } finally {
    submitting.value = false
  }
}

function handleError(error: unknown) {
  const code = status(error)
  if (code === 400) {
    const issues = (error as { problem?: { issues?: ProblemIssue[] } }).problem?.issues ?? []
    report.value = issues.length ? issues : [{ detail: problemDetail(error) }]
  } else if (code === 403) {
    formMessage.value = TEXT.notAuthorized
  } else if (code === 404) {
    formMessage.value = TEXT.vocabularyNotFound
  } else if (code === 409 || code === 413 || code === 415) {
    formMessage.value = problemDetail(error)
  } else {
    formMessage.value = `${TEXT.genericError} ${problemDetail(error)}`.trim()
  }
}

function cancel() {
  void router.push(props.mode === 'create' ? '/vocabularies' : `/vocabularies/${vocabularyName.value}`)
}

function describedBy(field: string): string | undefined {
  return errors.value[field] ? `error-${field}` : undefined
}
</script>

<template>
  <h1 v-if="props.mode === 'create'">New vocabulary</h1>
  <h1 v-else>New version of {{ vocabularyName }}</h1>

  <div v-if="!admin" role="alert" class="message error">{{ TEXT.notAuthorized }}</div>
  <template v-else>
    <div v-if="formMessage" role="alert" class="message error">{{ formMessage }}</div>
    <section v-if="report.length" aria-label="Validation report" class="panel report">
      <h2>Validation report</h2>
      <ul>
        <li v-for="(issue, index) in report" :key="index">
          {{ issue.detail }}<span v-if="issue.location" class="location"> ({{ issue.location }})</span>
          <span v-else-if="issue.name" class="location"> ({{ issue.name }})</span>
        </li>
      </ul>
    </section>

    <form v-if="loaded" class="vocabulary-form" novalidate @submit.prevent="submit">
      <p v-if="props.mode === 'version'" class="hint">
        Submitting creates version {{ latestVersion + 1 }} and deprecates every older ACTIVE version.
      </p>
      <div v-if="props.mode === 'create'" class="field">
        <label for="field-name">Name</label>
        <input
          id="field-name"
          v-model="name"
          type="text"
          maxlength="64"
          autocomplete="off"
          required
          :aria-invalid="errors.name ? 'true' : 'false'"
          :aria-describedby="describedBy('name')"
        />
        <p v-if="errors.name" id="error-name" class="field-error">{{ errors.name }}</p>
      </div>
      <div class="field">
        <label for="field-description">Description</label>
        <textarea
          id="field-description"
          v-model="description"
          rows="3"
          required
          :aria-invalid="errors.description ? 'true' : 'false'"
          :aria-describedby="describedBy('description')"
        ></textarea>
        <p v-if="errors.description" id="error-description" class="field-error">{{ errors.description }}</p>
      </div>
      <div v-if="props.mode === 'version'" class="field">
        <label for="field-changelog">Change log</label>
        <textarea
          id="field-changelog"
          v-model="changelog"
          rows="3"
          required
          :aria-invalid="errors.changelog ? 'true' : 'false'"
          :aria-describedby="describedBy('changelog')"
        ></textarea>
        <p v-if="errors.changelog" id="error-changelog" class="field-error">{{ errors.changelog }}</p>
      </div>
      <div class="field">
        <label for="field-vocabularyFile">Vocabulary file</label>
        <p v-if="props.mode === 'version' && !file" class="current-file">
          {{ vocabularyName }}.ttl (current content)
        </p>
        <input
          id="field-vocabularyFile"
          ref="fileInput"
          type="file"
          accept=".ttl,text/turtle"
          :aria-invalid="errors.vocabularyFile || fileError ? 'true' : 'false'"
          :aria-describedby="errors.vocabularyFile || fileError ? 'error-vocabularyFile' : undefined"
          @change="onFile"
        />
        <p v-if="errors.vocabularyFile || fileError" id="error-vocabularyFile" class="field-error">
          {{ fileError || errors.vocabularyFile }}
        </p>
        <p v-if="syntaxOk" role="status" class="field-ok">{{ TEXT.syntaxPassed }}</p>
      </div>
      <div class="toolbar">
        <button type="submit" class="primary" :disabled="submitDisabled">Submit</button>
        <button type="button" @click="cancel">Cancel</button>
      </div>
    </form>
  </template>
</template>
