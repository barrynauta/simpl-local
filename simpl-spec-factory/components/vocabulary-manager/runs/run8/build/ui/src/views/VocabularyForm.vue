<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ApiError, type Problem } from '../api';
import ProblemMessage from '../components/ProblemMessage.vue';
import { useApp } from '../context';
import { nameError, textError } from '../fieldRules';
import { TEXT, nextVersionLine, versionCreated } from '../texts';
import { checkTurtleFile, checkTurtleText } from '../turtleCheck';

/**
 * Create form (/vocabularies/new) and new version form (/vocabularies/<Name>/versions/new): SIMPL-18169, 18172,
 * 18180, 18189, 18193; D13, D14, D32, D33, D37, D46, D60, D62, D74, D76.
 */
const app = useApp();
const route = useRoute();
const router = useRouter();

const isVersion = computed(() => route.name === 'new-version');
const vocabularyName = computed(() => String(route.params.name ?? ''));
const authorized = computed(() => app.isAdmin());

const fields = reactive({ name: '', description: '', changelog: '' });
const touched = reactive({ name: false, description: false, changelog: false, file: false });
const file = ref<Blob | null>(null);
const fileName = ref('');
const currentContentLabel = ref<string | null>(null);
const fileFieldError = ref<string | null>(null);
const syntaxMessage = ref<string | null>(null);
const syntaxOk = ref(true);
const latestVersion = ref<number | null>(null);
const loadError = ref<string | null>(null);
const submitting = ref(false);
const report = ref<Problem | null>(null);
const failure = ref<{ text: string; detail?: string | null } | null>(null);

const errors = computed(() => ({
  name: isVersion.value ? null : nameError(fields.name),
  description: textError('Description', fields.description),
  changelog: isVersion.value ? textError('Change log', fields.changelog) : null,
  file: fileFieldError.value ?? (file.value ? null : 'Vocabulary file is required.'),
}));

/** D62: Submit is disabled only while the upload check shows a syntax error or a refused file. */
const submitDisabled = computed(() => submitting.value || !syntaxOk.value || fileFieldError.value !== null);

async function onFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const chosen = input.files?.[0];
  touched.file = true;
  report.value = null;
  if (!chosen) {
    return;
  }
  file.value = chosen;
  fileName.value = chosen.name;
  currentContentLabel.value = null;
  const check = await checkTurtleFile(chosen);
  fileFieldError.value = check.fieldError;
  syntaxOk.value = check.ok;
  syntaxMessage.value = check.fieldError ? null : check.message;
}

async function loadCurrent() {
  try {
    const vocabulary = await app.api.getVocabulary(vocabularyName.value);
    latestVersion.value = vocabulary.version;
    fields.description = vocabulary.description;
    const content = await app.api.content(vocabularyName.value);
    file.value = content;
    fileName.value = `${vocabularyName.value}.ttl`;
    currentContentLabel.value = `${vocabularyName.value}.ttl (current content)`;
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      loadError.value = TEXT.vocabularyNotFound;
    } else {
      loadError.value = TEXT.genericError;
    }
  }
}

async function focusFirstError() {
  await nextTick();
  const order: [keyof typeof touched, string][] = [
    ['name', 'field-name'],
    ['description', 'field-description'],
    ['changelog', 'field-changelog'],
    ['file', 'field-file'],
  ];
  for (const [key, id] of order) {
    if (errors.value[key]) {
      document.getElementById(id)?.focus();
      return;
    }
  }
}

async function submit() {
  touched.name = touched.description = touched.changelog = touched.file = true;
  report.value = null;
  failure.value = null;
  const hasErrors = Object.values(errors.value).some((error) => error !== null);
  if (hasErrors || !file.value) {
    await focusFirstError();
    return;
  }
  submitting.value = true;
  try {
    const created = isVersion.value
      ? await app.api.createVersion(
          vocabularyName.value,
          fields.description.trim(),
          fields.changelog.trim(),
          file.value,
          fileName.value,
        )
      : await app.api.createVocabulary(fields.name, fields.description.trim(), file.value, fileName.value);
    app.notice = { message: versionCreated(created.version, created.name), warnings: created.validationWarnings };
    await router.push(`/vocabularies/${created.name}`);
  } catch (error) {
    showError(error);
  } finally {
    submitting.value = false;
  }
}

function showError(error: unknown) {
  if (!(error instanceof ApiError)) {
    failure.value = { text: TEXT.genericError };
    return;
  }
  if (error.status === 400 && error.problem.issues?.length) {
    report.value = error.problem;
  } else if (error.status === 403) {
    failure.value = { text: TEXT.notAuthorized };
  } else if (error.status === 404) {
    failure.value = { text: TEXT.vocabularyNotFound };
  } else if (error.status === 409 || error.status === 413 || error.status === 400 || error.status === 415) {
    failure.value = { text: error.problem.detail ?? TEXT.genericError };
  } else {
    failure.value = { text: TEXT.genericError, detail: error.problem.detail };
  }
}

function cancel() {
  void router.push(isVersion.value ? `/vocabularies/${vocabularyName.value}` : '/vocabularies');
}

onMounted(() => {
  if (authorized.value && isVersion.value) {
    void loadCurrent();
  }
});

defineExpose({ checkTurtleText });
</script>

<template>
  <section aria-labelledby="form-title">
    <h1 id="form-title">{{ isVersion ? `New version of ${vocabularyName}` : 'New vocabulary' }}</h1>
    <ProblemMessage v-if="!authorized" :text="TEXT.notAuthorized" />
    <ProblemMessage v-else-if="loadError" :text="loadError" />
    <form v-else novalidate @submit.prevent="submit">
      <p v-if="isVersion && latestVersion !== null" class="hint">{{ nextVersionLine(latestVersion) }}</p>

      <section v-if="report" class="panel report" aria-label="Validation report" role="alert">
        <h2>Validation report</h2>
        <ul>
          <li v-for="(issue, index) in report.issues" :key="index">
            <span>{{ issue.detail }}</span>
            <span v-if="issue.location" class="location">{{ issue.location }}</span>
          </li>
        </ul>
      </section>
      <ProblemMessage v-if="failure" :text="failure.text" :detail="failure.detail" />

      <div v-if="!isVersion" class="field">
        <label for="field-name">Name</label>
        <input
          id="field-name"
          v-model="fields.name"
          type="text"
          autocomplete="off"
          :aria-invalid="touched.name && errors.name ? 'true' : 'false'"
          aria-describedby="error-name"
          @blur="touched.name = true"
          @input="touched.name = true"
        />
        <p id="error-name" class="field-error">{{ touched.name ? errors.name : '' }}</p>
      </div>

      <div class="field">
        <label for="field-description">Description</label>
        <textarea
          id="field-description"
          v-model="fields.description"
          rows="3"
          :aria-invalid="touched.description && errors.description ? 'true' : 'false'"
          aria-describedby="error-description"
          @blur="touched.description = true"
          @input="touched.description = true"
        ></textarea>
        <p id="error-description" class="field-error">{{ touched.description ? errors.description : '' }}</p>
      </div>

      <div v-if="isVersion" class="field">
        <label for="field-changelog">Change log</label>
        <textarea
          id="field-changelog"
          v-model="fields.changelog"
          rows="3"
          :aria-invalid="touched.changelog && errors.changelog ? 'true' : 'false'"
          aria-describedby="error-changelog"
          @blur="touched.changelog = true"
          @input="touched.changelog = true"
        ></textarea>
        <p id="error-changelog" class="field-error">{{ touched.changelog ? errors.changelog : '' }}</p>
      </div>

      <div class="field">
        <label for="field-file">Vocabulary file</label>
        <input
          id="field-file"
          type="file"
          accept=".ttl,text/turtle"
          :aria-invalid="touched.file && errors.file ? 'true' : 'false'"
          aria-describedby="error-file file-status"
          @change="onFile"
        />
        <p v-if="currentContentLabel" class="hint">{{ currentContentLabel }}</p>
        <p id="error-file" class="field-error">{{ touched.file ? errors.file : '' }}</p>
        <p id="file-status" :class="syntaxOk ? 'syntax-ok' : 'field-error'" :role="syntaxOk ? 'status' : 'alert'">
          {{ syntaxMessage }}
        </p>
      </div>

      <div class="form-actions">
        <button type="submit" class="primary" :disabled="submitDisabled">Submit</button>
        <button type="button" @click="cancel">Cancel</button>
      </div>
    </form>
  </section>
</template>
