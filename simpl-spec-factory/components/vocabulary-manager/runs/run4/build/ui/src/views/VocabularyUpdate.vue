<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import FileUpload from '../components/FileUpload.vue';
import ValidationReport from '../components/ValidationReport.vue';
import { ApiError, createVersion, fetchContent, getVocabulary } from '../api';
import { isAdmin } from '../auth';
import { describeError, statusOf } from '../errors';
import { notifySuccess } from '../notifications';
import { NOT_AUTHORIZED, textError } from '../labels';
import type { ProblemIssue, Vocabulary } from '../types';

const props = defineProps<{ name: string }>();
const router = useRouter();
const allowed = isAdmin();
const vocabulary = ref<Vocabulary | undefined>();
const loadError = ref<{ text: string; detail?: string; notFound?: boolean } | undefined>();
const form = reactive({ description: '', changelog: '' });
const touched = reactive({ description: false, changelog: false });
const newFile = ref<File | null>(null);
const fileValid = ref(true);
const submitting = ref(false);
const report = ref<ProblemIssue[]>([]);
const message = ref<{ text: string; detail?: string } | undefined>();

const errors = computed(() => ({
  description: textError('Description', form.description),
  changelog: textError('Change log', form.changelog),
}));

/** Pre-fills the form with the latest version (decision D33). */
onMounted(async () => {
  if (!allowed) return;
  try {
    vocabulary.value = await getVocabulary(props.name);
    form.description = vocabulary.value.description;
  } catch (error) {
    if (statusOf(error) === 404) {
      loadError.value = { text: 'Vocabulary not found.', notFound: true };
    } else {
      const shown = describeError(error, 'The vocabulary could not be loaded.');
      loadError.value = { text: shown.message, detail: shown.detail };
    }
  }
});

function onFile(chosen: File | null, valid: boolean): void {
  newFile.value = chosen;
  fileValid.value = valid;
}

async function submit(): Promise<void> {
  touched.description = touched.changelog = true;
  report.value = [];
  message.value = undefined;
  if (!vocabulary.value || errors.value.description || errors.value.changelog || !fileValid.value) return;
  submitting.value = true;
  try {
    const content = newFile.value ?? (await fetchContent(vocabulary.value.name)).blob;
    const created = await createVersion(
      vocabulary.value.name,
      form.description.trim(),
      form.changelog.trim(),
      content,
    );
    notifySuccess(`Version ${created.version} of ${created.name} was created.`, created.validationWarnings);
    await router.push(`/vocabularies/${created.name}`);
  } catch (error) {
    if (error instanceof ApiError && error.status === 400 && error.problem.issues?.length) {
      report.value = error.problem.issues;
    } else {
      const shown = describeError(error, 'The new version could not be created.');
      message.value = { text: shown.message, detail: shown.detail };
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="form-page">
    <h1>Update {{ name }}</h1>
    <p v-if="!allowed" class="error-panel" role="alert">{{ NOT_AUTHORIZED }}</p>
    <div v-else-if="loadError" class="error-panel" role="alert">
      <p>{{ loadError.text }}</p>
      <p v-if="loadError.detail" class="detail">{{ loadError.detail }}</p>
      <router-link to="/vocabularies">Back to the vocabulary list</router-link>
    </div>
    <template v-else-if="vocabulary">
      <p class="hint">
        Submitting creates version {{ vocabulary.version + 1 }} and deprecates every older ACTIVE version.
      </p>
      <div v-if="message" class="error-panel" role="alert">
        <p>{{ message.text }}</p>
        <p v-if="message.detail" class="detail">{{ message.detail }}</p>
      </div>
      <ValidationReport v-if="report.length > 0" :issues="report" />
      <form novalidate @submit.prevent="submit">
        <div class="field">
          <label for="description">Description</label>
          <textarea
            id="description"
            v-model="form.description"
            rows="3"
            maxlength="1000"
            @blur="touched.description = true"
          ></textarea>
          <p v-if="touched.description && errors.description" class="field-error">{{ errors.description }}</p>
        </div>
        <div class="field">
          <label for="changelog">Change log</label>
          <textarea
            id="changelog"
            v-model="form.changelog"
            rows="3"
            maxlength="1000"
            required
            @blur="touched.changelog = true"
          ></textarea>
          <p v-if="touched.changelog && errors.changelog" class="field-error">{{ errors.changelog }}</p>
        </div>
        <FileUpload :current-label="`${vocabulary.name}.ttl (current content)`" @change="onFile" />
        <div class="form-actions">
          <button type="submit" class="primary" :disabled="submitting || !fileValid">Submit</button>
          <router-link :to="`/vocabularies/${vocabulary.name}`" class="button">Cancel</router-link>
        </div>
      </form>
    </template>
    <p v-else class="hint">Loading...</p>
  </section>
</template>
