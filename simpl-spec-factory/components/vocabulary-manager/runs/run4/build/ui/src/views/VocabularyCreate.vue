<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import FileUpload from '../components/FileUpload.vue';
import ValidationReport from '../components/ValidationReport.vue';
import { ApiError, createVocabulary } from '../api';
import { isAdmin } from '../auth';
import { describeError } from '../errors';
import { notifySuccess } from '../notifications';
import { NOT_AUTHORIZED, nameError, textError } from '../labels';
import type { ProblemIssue } from '../types';

const router = useRouter();
const allowed = isAdmin();
const form = reactive({ name: '', description: '' });
const touched = reactive({ name: false, description: false, file: false });
const file = ref<File | null>(null);
const fileValid = ref(true);
const submitting = ref(false);
const report = ref<ProblemIssue[]>([]);
const message = ref<{ text: string; detail?: string } | undefined>();

const errors = computed(() => ({
  name: nameError(form.name),
  description: textError('Description', form.description),
  file: file.value ? undefined : 'File upload is required.',
}));

function onFile(chosen: File | null, valid: boolean): void {
  file.value = chosen;
  fileValid.value = valid;
  touched.file = true;
}

async function submit(): Promise<void> {
  touched.name = touched.description = touched.file = true;
  report.value = [];
  message.value = undefined;
  if (errors.value.name || errors.value.description || errors.value.file || !fileValid.value || !file.value) {
    return;
  }
  submitting.value = true;
  try {
    const created = await createVocabulary(form.name, form.description.trim(), file.value);
    notifySuccess(`Version ${created.version} of ${created.name} was created.`, created.validationWarnings);
    await router.push(`/vocabularies/${created.name}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 400 && error.problem.issues?.length) {
      report.value = error.problem.issues;
    } else {
      const shown = describeError(error, 'The vocabulary could not be created.');
      message.value = { text: shown.message, detail: shown.detail };
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="form-page">
    <h1>New vocabulary</h1>
    <p v-if="!allowed" class="error-panel" role="alert">{{ NOT_AUTHORIZED }}</p>
    <template v-else>
      <div v-if="message" class="error-panel" role="alert">
        <p>{{ message.text }}</p>
        <p v-if="message.detail" class="detail">{{ message.detail }}</p>
      </div>
      <ValidationReport v-if="report.length > 0" :issues="report" />
      <form novalidate @submit.prevent="submit">
        <div class="field">
          <label for="name">Name</label>
          <input
            id="name"
            v-model="form.name"
            type="text"
            maxlength="64"
            required
            @blur="touched.name = true"
          />
          <p v-if="touched.name && errors.name" class="field-error">{{ errors.name }}</p>
        </div>
        <div class="field">
          <label for="description">Description</label>
          <textarea
            id="description"
            v-model="form.description"
            rows="3"
            maxlength="1000"
            required
            @blur="touched.description = true"
          ></textarea>
          <p v-if="touched.description && errors.description" class="field-error">{{ errors.description }}</p>
        </div>
        <FileUpload @change="onFile" />
        <p v-if="touched.file && errors.file" class="field-error">{{ errors.file }}</p>
        <div class="form-actions">
          <button type="submit" class="primary" :disabled="submitting || !fileValid">Submit</button>
          <router-link to="/vocabularies" class="button">Cancel</router-link>
        </div>
      </form>
    </template>
  </section>
</template>
