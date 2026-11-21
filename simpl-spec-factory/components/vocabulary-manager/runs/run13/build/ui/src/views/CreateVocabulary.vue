<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import FieldError from '../components/FieldError.vue';
import FileField from '../components/FileField.vue';
import RequiredLabel from '../components/RequiredLabel.vue';
import ValidationReport from '../components/ValidationReport.vue';
import { ApiError, createVocabulary, type ProblemIssue } from '../api';
import { isAdmin } from '../auth';
import { submitErrorText } from '../errors';
import { pendingNotice, TEXT } from '../messages';
import { validateName, validateText } from '../validation';

const router = useRouter();
const admin = isAdmin();

const name = ref('');
const description = ref('');
const file = ref<File | null>(null);
const uploadBlocked = ref(false);
const errors = reactive({ name: '', description: '', file: '' });
const report = ref<ProblemIssue[]>([]);
const formError = ref('');
const submitting = ref(false);

function checkName(): void {
  errors.name = validateName(name.value);
}
function checkDescription(): void {
  errors.description = validateText(description.value, 'Description');
}

function onFile(selected: File | null, blocking: boolean): void {
  file.value = selected;
  uploadBlocked.value = blocking;
  errors.file = '';
}

async function focusFirstError(): Promise<void> {
  await nextTick();
  const id = errors.name ? 'field-name' : errors.description ? 'field-description' : 'field-file';
  document.getElementById(id)?.focus();
}

async function submit(): Promise<void> {
  checkName();
  checkDescription();
  errors.file = file.value ? '' : 'Vocabulary file is required.';
  if (errors.name || errors.description || errors.file) {
    await focusFirstError();
    return;
  }
  report.value = [];
  formError.value = '';
  submitting.value = true;
  const form = new FormData();
  form.append('name', name.value);
  form.append('description', description.value.trim());
  form.append('changelog', 'Initial version.');
  form.append('vocabularyFile', file.value as File, (file.value as File).name);
  try {
    const created = await createVocabulary(form);
    pendingNotice.value = {
      message: `Version ${created.version} of ${created.name} was created.`,
      warnings: created.validationWarnings ?? [],
    };
    await router.push(`/vocabularies/${created.name}`);
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

defineExpose({ submit, onFile });
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-slate-900">New vocabulary</h1>
    <p v-if="!admin" role="alert" class="mt-4 rounded bg-red-50 p-3 text-red-900">
      {{ TEXT.notAuthorized }}
    </p>
    <form v-else class="mt-4 max-w-2xl" novalidate @submit.prevent="submit">
      <ValidationReport v-if="report.length > 0" :issues="report" />
      <p v-if="formError" role="alert" class="mb-4 rounded bg-red-50 p-3 text-red-900">
        {{ formError }}
      </p>

      <RequiredLabel for-id="field-name" text="Name" />
      <input
        id="field-name"
        v-model="name"
        type="text"
        aria-required="true"
        :aria-invalid="errors.name ? 'true' : 'false'"
        aria-describedby="field-name-error"
        class="mt-1 w-full rounded border border-slate-500 px-2 py-1"
        @blur="checkName"
      />
      <FieldError id="field-name-error" :message="errors.name" />

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

      <FileField :submit-error="errors.file" @change="onFile" />

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
          @click="router.push('/vocabularies')"
        >
          Cancel
        </button>
      </div>
    </form>
  </div>
</template>
