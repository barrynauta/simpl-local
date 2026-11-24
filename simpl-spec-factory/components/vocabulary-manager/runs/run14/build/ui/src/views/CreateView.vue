<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import FileField from '../components/FileField.vue';
import ValidationReport from '../components/ValidationReport.vue';
import { api, type Issue } from '../lib/api';
import { isAdmin } from '../lib/auth';
import { nameError, submitFailure, textError } from '../lib/forms';
import { setNotice } from '../lib/notices';
import { TEXT } from '../lib/texts';

const router = useRouter();
const admin = computed(() => isAdmin());
const name = ref('');
const description = ref('');
const file = ref<File | null>(null);
const fileBlocking = ref(false);
const errors = reactive({ name: '', description: '', file: '' });
const report = ref<Issue[]>([]);
const message = ref('');
const submitting = ref(false);

function onFile(chosen: File | null, blocking: boolean): void {
  file.value = chosen;
  fileBlocking.value = blocking;
  errors.file = '';
}

function checkAll(): boolean {
  errors.name = nameError(name.value);
  errors.description = textError('Description', description.value, true);
  errors.file = file.value ? '' : 'Vocabulary file is required.';
  return !errors.name && !errors.description && !errors.file;
}

async function submit(): Promise<void> {
  report.value = [];
  message.value = '';
  if (!checkAll()) {
    await nextTick();
    const first = errors.name ? 'name' : errors.description ? 'description' : 'vocabulary-file';
    document.getElementById(first)?.focus();
    return;
  }
  const form = new FormData();
  form.append('name', name.value);
  form.append('description', description.value);
  form.append('changelog', 'Initial version.');
  form.append('vocabularyFile', file.value as File, (file.value as File).name);
  submitting.value = true;
  try {
    const created = await api.createVocabulary(form);
    setNotice({
      success: `Version ${created.version} of ${created.name} was created.`,
      warnings: created.validationWarnings ?? [],
    });
    await router.push(`/vocabularies/${created.name}`);
  } catch (error) {
    const failure = submitFailure(error);
    report.value = failure.report;
    message.value = failure.message;
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold">New vocabulary</h1>
    <p v-if="!admin" role="alert" class="alert-error">{{ TEXT.notAuthorized }}</p>
    <form v-else class="mt-4 max-w-2xl" novalidate @submit.prevent="submit">
      <p v-if="message" role="alert" class="alert-error">{{ message }}</p>
      <ValidationReport v-if="report.length > 0" :issues="report" />

      <label for="name" class="block text-sm font-medium"
        >Name<span aria-hidden="true" class="text-red-800"> *</span></label
      >
      <input
        id="name"
        v-model="name"
        type="text"
        class="field-input"
        aria-required="true"
        :aria-invalid="errors.name ? 'true' : 'false'"
        aria-describedby="name-error"
        @blur="errors.name = nameError(name)"
      />
      <p id="name-error" class="field-error">{{ errors.name }}</p>

      <label for="description" class="mt-3 block text-sm font-medium"
        >Description<span aria-hidden="true" class="text-red-800"> *</span></label
      >
      <textarea
        id="description"
        v-model="description"
        rows="3"
        class="field-input"
        aria-required="true"
        :aria-invalid="errors.description ? 'true' : 'false'"
        aria-describedby="description-error"
        @blur="errors.description = textError('Description', description, true)"
      ></textarea>
      <p id="description-error" class="field-error">{{ errors.description }}</p>

      <FileField id="vocabulary-file" :required="true" :error="errors.file" @change="onFile" />

      <div class="mt-4 flex gap-2">
        <button type="submit" class="btn" :disabled="fileBlocking || submitting">Submit</button>
        <RouterLink to="/vocabularies" class="btn-secondary no-underline">Cancel</RouterLink>
      </div>
    </form>
  </div>
</template>
