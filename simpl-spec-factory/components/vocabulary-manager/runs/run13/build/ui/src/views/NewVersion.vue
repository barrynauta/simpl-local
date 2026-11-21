<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import FieldError from '../components/FieldError.vue';
import FileField from '../components/FileField.vue';
import RequiredLabel from '../components/RequiredLabel.vue';
import ValidationReport from '../components/ValidationReport.vue';
import {
  ApiError,
  createVersion,
  fetchContent,
  getVocabulary,
  type ProblemIssue,
  type Vocabulary,
} from '../api';
import { isAdmin } from '../auth';
import { loadErrorText, submitErrorText } from '../errors';
import { pendingNotice, TEXT } from '../messages';
import { validateText } from '../validation';

const route = useRoute();
const router = useRouter();
const admin = isAdmin();
const name = computed(() => String(route.params.name));

const vocabulary = ref<Vocabulary | null>(null);
const loadError = ref('');
const description = ref('');
const changelog = ref('');
const file = ref<File | null>(null);
const uploadBlocked = ref(false);
const errors = reactive({ description: '', changelog: '' });
const report = ref<ProblemIssue[]>([]);
const formError = ref('');
const submitting = ref(false);

async function load(): Promise<void> {
  if (!admin) return;
  try {
    vocabulary.value = await getVocabulary(name.value);
    description.value = vocabulary.value.description;
  } catch (error) {
    loadError.value =
      error instanceof ApiError && error.status === 404
        ? TEXT.notFound
        : loadErrorText(error, TEXT.genericError);
  }
}

function checkDescription(): void {
  errors.description = validateText(description.value, 'Description');
}
function checkChangelog(): void {
  errors.changelog = validateText(changelog.value, 'Change log');
}

function onFile(selected: File | null, blocking: boolean): void {
  file.value = selected;
  uploadBlocked.value = blocking;
}

async function submit(): Promise<void> {
  checkDescription();
  checkChangelog();
  if (errors.description || errors.changelog) {
    await nextTick();
    document.getElementById(errors.description ? 'field-description' : 'field-changelog')?.focus();
    return;
  }
  report.value = [];
  formError.value = '';
  submitting.value = true;
  try {
    // D33: without a new file the current content of the latest version is sent again.
    let upload: Blob = file.value as Blob;
    let filename = file.value?.name ?? `${name.value}.ttl`;
    if (!file.value) {
      const current = await fetchContent(name.value);
      upload = current.blob;
      filename = current.filename;
    }
    const form = new FormData();
    form.append('description', description.value.trim());
    form.append('changelog', changelog.value.trim());
    form.append('vocabularyFile', upload, filename);
    const created = await createVersion(name.value, form);
    pendingNotice.value = {
      message: `Version ${created.version} of ${created.name} was created.`,
      warnings: created.validationWarnings ?? [],
    };
    await router.push(`/vocabularies/${created.name}/versions/${created.version}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 400 && error.problem.issues?.length) {
      report.value = error.problem.issues;
    } else {
      formError.value = submitErrorText(error);
    }
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
defineExpose({ submit, onFile });
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-slate-900">New version of {{ name }}</h1>
    <p v-if="!admin" role="alert" class="mt-4 rounded bg-red-50 p-3 text-red-900">
      {{ TEXT.notAuthorized }}
    </p>
    <template v-else>
      <p v-if="loadError" role="alert" class="mt-4 rounded bg-red-50 p-3 text-red-900">
        {{ loadError }}
      </p>
      <form v-if="vocabulary" class="mt-4 max-w-2xl" novalidate @submit.prevent="submit">
        <p class="mb-4 text-slate-900">
          Submitting creates version {{ vocabulary.version + 1 }} and deprecates every older ACTIVE
          version.
        </p>
        <ValidationReport v-if="report.length > 0" :issues="report" />
        <p v-if="formError" role="alert" class="mb-4 rounded bg-red-50 p-3 text-red-900">
          {{ formError }}
        </p>

        <RequiredLabel for-id="field-description" text="Description" />
        <textarea
          id="field-description"
          v-model="description"
          rows="3"
          aria-required="true"
          :aria-invalid="errors.description ? 'true' : 'false'"
          aria-describedby="field-description-error"
          class="mt-1 w-full rounded border border-slate-500 px-2 py-1"
          @blur="checkDescription"
        ></textarea>
        <FieldError id="field-description-error" :message="errors.description" />

        <RequiredLabel for-id="field-changelog" text="Change log" />
        <textarea
          id="field-changelog"
          v-model="changelog"
          rows="3"
          aria-required="true"
          :aria-invalid="errors.changelog ? 'true' : 'false'"
          aria-describedby="field-changelog-error"
          class="mt-1 w-full rounded border border-slate-500 px-2 py-1"
          @blur="checkChangelog"
        ></textarea>
        <FieldError id="field-changelog-error" :message="errors.changelog" />

        <FileField
          :current-label="`${name}.ttl (current content)`"
          submit-error=""
          @change="onFile"
        />

        <div class="mt-4 flex gap-3">
          <button
            type="submit"
            class="rounded bg-blue-800 px-4 py-2 text-white hover:bg-blue-900 disabled:bg-slate-500"
            :disabled="uploadBlocked || submitting"
          >
            Submit
          </button>
          <button
            type="button"
            class="rounded border border-slate-500 px-4 py-2"
            @click="router.push(`/vocabularies/${name}`)"
          >
            Cancel
          </button>
        </div>
      </form>
    </template>
  </div>
</template>
