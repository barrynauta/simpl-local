<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api, ApiError, type Problem } from '../api';
import { session } from '../auth';
import ValidationReport from '../components/ValidationReport.vue';
import { setNotice } from '../notice';
import { TEXT } from '../texts';
import { checkTurtle, MAX_FILE_BYTES } from '../turtleCheck';

/** Create form (D32, D37) and new version form (D33, D46); upload check of D13, D62 and D76. */
const props = defineProps<{ mode: 'create' | 'version'; name?: string }>();
const router = useRouter();
const admin = session().isAdmin();
const NAME_PATTERN = /^[A-Z][a-zA-Z0-9]*$/;

const fields = reactive({ name: '', description: '', changelog: '' });
const errors = reactive<Record<string, string>>({});
const file = ref<Blob | null>(null);
const fileLabel = ref('');
const uploadCheck = ref<{ ok: boolean; message: string } | null>(null);
const latestVersion = ref(0);
const loadError = ref('');
const submitting = ref(false);
const report = ref<Problem['issues'] | null>(null);
const serverMessage = ref<string[]>([]);

const title = computed(() => (props.mode === 'create' ? 'New vocabulary' : `Update ${props.name}`));
const submitDisabled = computed(
  () => submitting.value || (uploadCheck.value !== null && !uploadCheck.value.ok),
);

onMounted(async () => {
  if (!admin || props.mode !== 'version' || !props.name) {
    return;
  }
  try {
    const vocabulary = await api.getVocabulary(props.name);
    latestVersion.value = vocabulary.version;
    fields.description = vocabulary.description;
    file.value = await api.content(props.name);
    fileLabel.value = `${props.name}.ttl (current content)`;
  } catch (error) {
    loadError.value =
      error instanceof ApiError && error.status === 404
        ? TEXT.notFound
        : `${TEXT.genericError} ${error instanceof ApiError ? (error.problem.detail ?? '') : ''}`;
  }
});

function length(text: string): number {
  return [...text.trim()].length;
}

function validateField(field: 'name' | 'description' | 'changelog' | 'file'): void {
  delete errors[field];
  if (field === 'name' && props.mode === 'create') {
    const value = fields.name;
    if (!value) {
      errors.name = 'Name is required.';
    } else if (value.length < 3 || value.length > 64) {
      errors.name = 'Name must have 3 to 64 characters.';
    } else if (!NAME_PATTERN.test(value)) {
      errors.name = 'Name must start with an upper-case letter and contain only letters and digits.';
    }
  }
  if (field === 'description') {
    const n = length(fields.description);
    if (n === 0) {
      errors.description = 'Description is required.';
    } else if (n > 1000) {
      errors.description = 'Description must have at most 1000 characters.';
    }
  }
  if (field === 'changelog' && props.mode === 'version') {
    const n = length(fields.changelog);
    if (n === 0) {
      errors.changelog = 'Change log is required.';
    } else if (n > 1000) {
      errors.changelog = 'Change log must have at most 1000 characters.';
    }
  }
  if (field === 'file' && !file.value) {
    errors.file = 'Vocabulary file is required.';
  }
}

async function chooseFile(event: Event): Promise<void> {
  const chosen = (event.target as HTMLInputElement).files?.[0];
  delete errors.file;
  uploadCheck.value = null;
  if (!chosen) {
    return;
  }
  file.value = chosen;
  fileLabel.value = chosen.name;
  if (chosen.size > MAX_FILE_BYTES) {
    errors.file = TEXT.fileTooLarge;
    uploadCheck.value = { ok: false, message: '' };
    return;
  }
  uploadCheck.value = checkTurtle(await chosen.text());
}

async function submit(): Promise<void> {
  (['name', 'description', 'changelog', 'file'] as const).forEach(validateField);
  const order = ['name', 'description', 'changelog', 'file'];
  const first = order.find((key) => errors[key]);
  if (first) {
    await nextTick();
    document.getElementById(`field-${first}`)?.focus();
    return;
  }
  submitting.value = true;
  report.value = null;
  serverMessage.value = [];
  const form = new FormData();
  form.append(
    'vocabularyFile',
    file.value as Blob,
    fileLabel.value.replace(' (current content)', '') || 'vocabulary.ttl',
  );
  form.append('description', fields.description.trim());
  try {
    let created;
    if (props.mode === 'create') {
      form.append('name', fields.name);
      form.append('changelog', 'Initial version.');
      created = await api.createVocabulary(form);
    } else {
      form.append('changelog', fields.changelog.trim());
      created = await api.createVersion(props.name as string, form);
    }
    setNotice(`Version ${created.version} of ${created.name} was created.`, created.validationWarnings ?? []);
    await router.push(`/vocabularies/${created.name}`);
  } catch (error) {
    const problem = error instanceof ApiError ? error.problem : { status: 500, detail: String(error) };
    if (problem.status === 400 && problem.issues?.length) {
      report.value = problem.issues;
    } else if (problem.status === 403) {
      serverMessage.value = [TEXT.notAuthorized];
    } else if (problem.status >= 500) {
      serverMessage.value = [TEXT.genericError, problem.detail ?? ''];
    } else {
      serverMessage.value = [problem.detail ?? `HTTP ${problem.status}`];
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <p>
    <RouterLink to="/vocabularies">{{ TEXT.backToList }}</RouterLink>
  </p>
  <h1>{{ title }}</h1>
  <p v-if="!admin" role="alert" class="panel error">{{ TEXT.notAuthorized }}</p>
  <p v-else-if="loadError" role="alert" class="panel error">{{ loadError }}</p>
  <form v-else novalidate @submit.prevent="submit">
    <div v-if="serverMessage.length" role="alert" class="panel error">
      <p v-for="(line, index) in serverMessage.filter((l) => l)" :key="index">{{ line }}</p>
    </div>
    <ValidationReport v-if="report" :issues="report" />
    <p v-if="mode === 'version' && latestVersion">
      Submitting creates version {{ latestVersion + 1 }} and deprecates every older ACTIVE version.
    </p>

    <div v-if="mode === 'create'" class="field">
      <label for="field-name">Name</label>
      <input
        id="field-name"
        v-model="fields.name"
        type="text"
        maxlength="64"
        required
        :aria-invalid="errors.name ? 'true' : 'false'"
        :aria-describedby="errors.name ? 'error-name' : undefined"
        @blur="validateField('name')"
      />
      <p v-if="errors.name" id="error-name" class="field-error">{{ errors.name }}</p>
    </div>

    <div class="field">
      <label for="field-description">Description</label>
      <textarea
        id="field-description"
        v-model="fields.description"
        rows="3"
        required
        :aria-invalid="errors.description ? 'true' : 'false'"
        :aria-describedby="errors.description ? 'error-description' : undefined"
        @blur="validateField('description')"
      ></textarea>
      <p v-if="errors.description" id="error-description" class="field-error">{{ errors.description }}</p>
    </div>

    <div v-if="mode === 'version'" class="field">
      <label for="field-changelog">Change log</label>
      <textarea
        id="field-changelog"
        v-model="fields.changelog"
        rows="3"
        required
        :aria-invalid="errors.changelog ? 'true' : 'false'"
        :aria-describedby="errors.changelog ? 'error-changelog' : undefined"
        @blur="validateField('changelog')"
      ></textarea>
      <p v-if="errors.changelog" id="error-changelog" class="field-error">{{ errors.changelog }}</p>
    </div>

    <div class="field">
      <label for="field-file">Vocabulary file</label>
      <input
        id="field-file"
        type="file"
        accept=".ttl,text/turtle"
        :aria-invalid="errors.file ? 'true' : 'false'"
        :aria-describedby="errors.file ? 'error-file' : 'file-status'"
        @change="chooseFile"
      />
      <p v-if="fileLabel" id="file-status">{{ fileLabel }}</p>
      <p v-if="errors.file" id="error-file" class="field-error">{{ errors.file }}</p>
      <p v-if="uploadCheck && uploadCheck.message" :class="uploadCheck.ok ? '' : 'field-error'" role="status">
        {{ uploadCheck.message }}
      </p>
    </div>

    <div class="actions">
      <button type="submit" :disabled="submitDisabled">Submit</button>
      <button type="button" class="secondary" @click="router.back()">Cancel</button>
    </div>
  </form>
</template>
