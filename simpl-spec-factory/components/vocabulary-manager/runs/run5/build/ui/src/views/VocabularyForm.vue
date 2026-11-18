<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  ApiError,
  createVersion,
  createVocabulary,
  fetchContent,
  getVocabulary,
  type CreatedVersion,
  type ProblemIssue,
} from '../lib/api';
import { isAdmin } from '../lib/auth';
import { errorLines } from '../lib/errors';
import { flash } from '../lib/flash';
import { NOT_AUTHORIZED, VOCABULARY_NOT_FOUND, nextVersionLine, versionCreated } from '../lib/messages';
import { checkTurtle, nameError, textError, type UploadCheck } from '../lib/validation';

/**
 * Create form (mode 'create', SIMPL-18169) and new version form (mode 'version', SIMPL-18172; D32, D33, D46).
 * Field errors before submit (D37), upload check in the browser (D13), report panel on 400 (D14).
 */
const props = defineProps<{ mode: 'create' | 'version'; name?: string }>();
const router = useRouter();
const admin = isAdmin();

const name = ref('');
const description = ref('');
const changelog = ref('');
const file = ref<File | null>(null);
const currentContent = ref<Blob | null>(null);
const currentFileLabel = ref('');
const latestVersion = ref(0);
const uploadCheck = ref<UploadCheck | null>(null);
const touched = ref<Record<string, boolean>>({});
const submitted = ref(false);
const submitting = ref(false);
const report = ref<ProblemIssue[]>([]);
const formError = ref<string[] | null>(null);
const loadError = ref<string[] | null>(null);

const errors = computed(() => ({
  name: props.mode === 'create' ? nameError(name.value) : null,
  description: textError('Description', description.value),
  changelog: props.mode === 'version' ? textError('Change log', changelog.value) : null,
  file: file.value || currentContent.value ? null : 'Vocabulary file is required.',
}));

function show(field: 'name' | 'description' | 'changelog' | 'file'): string | null {
  return submitted.value || touched.value[field] ? errors.value[field] : null;
}

const uploadBlocked = computed(() => uploadCheck.value !== null && !uploadCheck.value.ok);

async function onFile(event: Event): Promise<void> {
  touched.value.file = true;
  const input = event.target as HTMLInputElement;
  const chosen = input.files && input.files.length ? input.files[0] : null;
  file.value = chosen;
  uploadCheck.value = null;
  if (chosen) {
    const text = chosen.size > 5 * 1024 * 1024 ? '' : await chosen.text();
    uploadCheck.value = checkTurtle(text, chosen.size);
  }
}

async function loadCurrent(): Promise<void> {
  if (props.mode !== 'version' || !props.name || !admin) return;
  try {
    const vocabulary = await getVocabulary(props.name);
    description.value = vocabulary.description ?? '';
    latestVersion.value = vocabulary.version;
    const content = await fetchContent(props.name);
    currentContent.value = content.blob;
    currentFileLabel.value = `${props.name}.ttl (current content)`;
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      loadError.value = [VOCABULARY_NOT_FOUND];
    } else {
      loadError.value = errorLines(error, 'The vocabulary could not be loaded.');
    }
  }
}

async function submit(): Promise<void> {
  submitted.value = true;
  report.value = [];
  formError.value = null;
  const e = errors.value;
  if (e.name || e.description || e.changelog || e.file || uploadBlocked.value) {
    return;
  }
  const form = new FormData();
  if (file.value) {
    form.append('vocabularyFile', file.value, file.value.name);
  } else if (currentContent.value) {
    form.append('vocabularyFile', currentContent.value, `${props.name}.ttl`);
  }
  form.append('description', description.value);
  submitting.value = true;
  try {
    let created: CreatedVersion;
    if (props.mode === 'create') {
      form.append('name', name.value);
      form.append('changelog', 'Initial version.');
      created = await createVocabulary(form);
    } else {
      form.append('changelog', changelog.value);
      created = await createVersion(props.name ?? '', form);
    }
    flash.value = {
      message: versionCreated(created.version, created.name),
      warnings: created.validationWarnings ?? [],
    };
    await router.push(`/vocabularies/${created.name}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 400 && error.problem.issues?.length) {
      report.value = error.problem.issues;
    } else if (error instanceof ApiError && error.status === 404) {
      formError.value = [VOCABULARY_NOT_FOUND, error.problem.detail ?? ''];
    } else {
      formError.value = errorLines(error);
    }
  } finally {
    submitting.value = false;
  }
}

function cancel(): void {
  void router.push(props.mode === 'version' && props.name ? `/vocabularies/${props.name}` : '/vocabularies');
}

onMounted(loadCurrent);
</script>

<template>
  <section>
    <h1 v-if="mode === 'create'">New vocabulary</h1>
    <h1 v-else>New version of {{ name }}</h1>

    <div v-if="!admin" class="message error" role="alert">
      <p>{{ NOT_AUTHORIZED }}</p>
    </div>
    <div v-else-if="loadError" class="message error" role="alert">
      <p v-for="line in loadError" :key="line">{{ line }}</p>
    </div>

    <template v-else>
      <div v-if="report.length" class="message error" role="alert" aria-label="Validation report">
        <p><strong>Validation report</strong></p>
        <ul>
          <li v-for="(issue, i) in report" :key="i">
            <span class="issue-detail">{{ issue.detail }}</span>
            <span v-if="issue.location" class="issue-location"> ({{ issue.location }})</span>
            <span v-else-if="issue.name" class="issue-location"> ({{ issue.name }})</span>
          </li>
        </ul>
      </div>
      <div v-if="formError" class="message error" role="alert">
        <p v-for="line in formError" :key="line">{{ line }}</p>
      </div>

      <p v-if="mode === 'version' && latestVersion > 0" class="next-version">
        {{ nextVersionLine(latestVersion) }}
      </p>

      <form class="vocabulary-form" novalidate @submit.prevent="submit">
        <label v-if="mode === 'create'">
          Name
          <input v-model="name" type="text" name="name" @blur="touched.name = true" />
          <span v-if="show('name')" class="field-error">{{ show('name') }}</span>
        </label>
        <label>
          Description
          <textarea
            v-model="description"
            name="description"
            rows="3"
            @blur="touched.description = true"
          ></textarea>
          <span v-if="show('description')" class="field-error">{{ show('description') }}</span>
        </label>
        <label v-if="mode === 'version'">
          Change log
          <textarea v-model="changelog" name="changelog" rows="3" @blur="touched.changelog = true"></textarea>
          <span v-if="show('changelog')" class="field-error">{{ show('changelog') }}</span>
        </label>
        <label>
          Vocabulary file
          <input type="file" name="vocabularyFile" accept=".ttl,text/turtle" @change="onFile" />
          <span v-if="!file && currentFileLabel" class="current-file">{{ currentFileLabel }}</span>
          <span v-if="uploadCheck && uploadCheck.ok" class="check-ok" role="status">{{
            uploadCheck.message
          }}</span>
          <span v-if="uploadCheck && !uploadCheck.ok" class="field-error" role="alert">{{
            uploadCheck.message
          }}</span>
          <span v-if="show('file')" class="field-error">{{ show('file') }}</span>
        </label>
        <div class="header-actions">
          <button type="submit" class="primary" :disabled="uploadBlocked || submitting">Submit</button>
          <button type="button" @click="cancel">Cancel</button>
        </div>
      </form>
    </template>
  </section>
</template>
