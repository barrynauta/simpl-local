<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  ApiError,
  createVersion,
  createVocabulary,
  getVocabulary,
  latestContent,
  type ProblemIssue,
} from '../api';
import { isAdmin } from '../auth';
import { setNotice } from '../notice';
import { TEXT, countText, writeErrorText } from '../texts';
import { MAX_FILE_BYTES, checkTurtle, syntaxErrorText } from '../turtleCheck';

/**
 * The create form (mode 'create', D32, D37) and the new version form (mode 'version', D33, D46): field rules shown
 * under each field, the browser syntax check of D13, the validation report of D14 and D90.
 */
const props = defineProps<{ mode: 'create' | 'version'; name?: string }>();

type Field = 'name' | 'description' | 'changelog' | 'file';
const NAME_PATTERN = /^[A-Z][a-zA-Z0-9]*$/;

const router = useRouter();
const authorized = isAdmin();
const values = reactive({ name: '', description: '', changelog: '' });
const file = ref<{ blob: Blob; fileName: string; label: string } | null>(null);
const shown = reactive<Record<Field, string>>({ name: '', description: '', changelog: '', file: '' });
const uploadError = ref('');
const uploadPassed = ref(false);
const report = ref<ProblemIssue[]>([]);
const formError = ref('');
const loadError = ref('');
const submitting = ref(false);
const latestVersion = ref(0);
const fileInput = ref<HTMLInputElement | null>(null);

const fields = computed<Field[]>(() =>
  props.mode === 'create' ? ['name', 'description', 'file'] : ['description', 'changelog', 'file'],
);
const submitDisabled = computed(() => uploadError.value !== '' || submitting.value);

function errorOf(field: Field): string {
  if (field === 'name') {
    const name = values.name;
    if (name.trim() === '') return 'Name is required.';
    if (name.length < 3 || name.length > 64) return 'Name must be 3 to 64 characters.';
    if (!NAME_PATTERN.test(name))
      return 'Name must start with an upper-case letter and contain only letters and digits.';
    return '';
  }
  if (field === 'description' || field === 'changelog') {
    const label = field === 'description' ? 'Description' : 'Change log';
    const text = values[field].trim();
    if (text === '') return `${label} is required.`;
    if (text.length > 1000) return `${label} must be at most 1000 characters.`;
    return '';
  }
  return file.value === null ? 'Vocabulary file is required.' : '';
}

function blur(field: Field): void {
  shown[field] = errorOf(field);
}

async function chooseFile(event: Event): Promise<void> {
  const chosen = (event.target as HTMLInputElement).files?.[0];
  uploadError.value = '';
  uploadPassed.value = false;
  shown.file = '';
  if (!chosen) {
    file.value = null;
    return;
  }
  file.value = { blob: chosen, fileName: chosen.name, label: chosen.name };
  if (chosen.size > MAX_FILE_BYTES) {
    uploadError.value = TEXT.fileTooLarge;
    return;
  }
  const result = checkTurtle(await chosen.text());
  if (result.ok) {
    uploadPassed.value = true;
  } else {
    uploadError.value = syntaxErrorText(result);
  }
}

async function submit(): Promise<void> {
  formError.value = '';
  report.value = [];
  let first: Field | null = null;
  for (const field of fields.value) {
    shown[field] = errorOf(field);
    if (shown[field] && first === null) first = field;
  }
  if (first !== null) {
    await nextTick();
    document.getElementById(`field-${first}`)?.focus();
    return;
  }
  const chosen = file.value as { blob: Blob; fileName: string };
  submitting.value = true;
  try {
    const created =
      props.mode === 'create'
        ? await createVocabulary(values.name, values.description.trim(), chosen.blob, chosen.fileName)
        : await createVersion(
            props.name as string,
            values.description.trim(),
            values.changelog.trim(),
            chosen.blob,
            chosen.fileName,
          );
    setNotice({ name: created.name, version: created.version, warnings: created.validationWarnings ?? [] });
    await router.push(`/vocabularies/${created.name}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 400 && (error.problem.issues?.length ?? 0) > 0) {
      report.value = error.problem.issues ?? [];
    } else {
      formError.value = error instanceof ApiError ? writeErrorText(error) : String(error);
    }
  } finally {
    submitting.value = false;
  }
}

function cancel(): void {
  void router.push(props.mode === 'create' ? '/vocabularies' : `/vocabularies/${props.name}`);
}

onMounted(async () => {
  if (!authorized || props.mode === 'create') return;
  try {
    const vocabulary = await getVocabulary(props.name as string);
    values.description = vocabulary.description;
    latestVersion.value = vocabulary.version;
    const blob = await latestContent(vocabulary.name);
    file.value = {
      blob,
      fileName: `${vocabulary.name}.ttl`,
      label: `${vocabulary.name}.ttl (current content)`,
    };
  } catch (error) {
    loadError.value =
      error instanceof ApiError && error.status === 404
        ? TEXT.vocabularyNotFound
        : error instanceof ApiError
          ? writeErrorText(error)
          : String(error);
  }
});

defineExpose({ submit });
</script>

<template>
  <section aria-labelledby="form-title">
    <h1 id="form-title">{{ mode === 'create' ? 'New vocabulary' : `New version of ${name}` }}</h1>
    <p v-if="!authorized" role="alert" class="error">{{ TEXT.notAuthorized }}</p>
    <p v-else-if="loadError" role="alert" class="error">{{ loadError }}</p>
    <template v-else>
      <p v-if="formError" role="alert" class="error">{{ formError }}</p>
      <section v-if="report.length > 0" class="report" aria-label="Validation report">
        <p role="alert">{{ countText(report.length, 'error', 'errors') }}</p>
        <ul>
          <li v-for="(issue, index) in report" :key="index">
            {{ issue.detail }} <span v-if="issue.location" class="location">({{ issue.location }})</span>
          </li>
        </ul>
      </section>
      <p v-if="mode === 'version' && latestVersion > 0">
        Submitting creates version {{ latestVersion + 1 }} and deprecates every older ACTIVE version.
      </p>

      <form novalidate @submit.prevent="submit">
        <div v-if="mode === 'create'" class="field">
          <label for="field-name">Name</label>
          <input
            id="field-name"
            v-model="values.name"
            type="text"
            maxlength="64"
            autocomplete="off"
            :aria-invalid="shown.name ? 'true' : 'false'"
            aria-describedby="error-name"
            @blur="blur('name')"
          />
          <p id="error-name" class="field-error">{{ shown.name }}</p>
        </div>
        <div class="field">
          <label for="field-description">Description</label>
          <textarea
            id="field-description"
            v-model="values.description"
            rows="3"
            :aria-invalid="shown.description ? 'true' : 'false'"
            aria-describedby="error-description"
            @blur="blur('description')"
          ></textarea>
          <p id="error-description" class="field-error">{{ shown.description }}</p>
        </div>
        <div v-if="mode === 'version'" class="field">
          <label for="field-changelog">Change log</label>
          <textarea
            id="field-changelog"
            v-model="values.changelog"
            rows="3"
            :aria-invalid="shown.changelog ? 'true' : 'false'"
            aria-describedby="error-changelog"
            @blur="blur('changelog')"
          ></textarea>
          <p id="error-changelog" class="field-error">{{ shown.changelog }}</p>
        </div>
        <div class="field">
          <label for="field-file">Vocabulary file</label>
          <input
            id="field-file"
            ref="fileInput"
            type="file"
            accept=".ttl,text/turtle"
            :aria-invalid="shown.file || uploadError ? 'true' : 'false'"
            aria-describedby="file-current error-file"
            @change="chooseFile"
          />
          <p id="file-current" class="hint">{{ file ? file.label : '' }}</p>
          <p id="error-file" class="field-error">{{ uploadError || shown.file }}</p>
          <p v-if="uploadPassed" class="success">{{ TEXT.syntaxPassed }}</p>
        </div>
        <div class="buttons">
          <button type="submit" :disabled="submitDisabled">Submit</button>
          <button type="button" class="secondary" @click="cancel">Cancel</button>
        </div>
      </form>
    </template>
  </section>
</template>
