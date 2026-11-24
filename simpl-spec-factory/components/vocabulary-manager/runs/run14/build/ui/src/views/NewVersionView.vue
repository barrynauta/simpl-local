<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import FileField from '../components/FileField.vue';
import ValidationReport from '../components/ValidationReport.vue';
import { ApiError, api, type Issue, type Vocabulary } from '../lib/api';
import { isAdmin } from '../lib/auth';
import { submitFailure, textError } from '../lib/forms';
import { setNotice } from '../lib/notices';
import { TEXT, problemDetail } from '../lib/texts';

const route = useRoute();
const router = useRouter();
const admin = computed(() => isAdmin());
const name = computed(() => String(route.params.name));
const vocabulary = ref<Vocabulary | null>(null);
const currentContent = ref<Blob | null>(null);
const description = ref('');
const changelog = ref('');
const file = ref<File | null>(null);
const fileBlocking = ref(false);
const errors = reactive({ description: '', changelog: '' });
const report = ref<Issue[]>([]);
const message = ref('');
const loadError = ref('');
const submitting = ref(false);

async function load(): Promise<void> {
  try {
    vocabulary.value = await api.getVocabulary(name.value);
    description.value = vocabulary.value.description;
    currentContent.value = await api.content(name.value);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      loadError.value = TEXT.vocabularyNotFound;
    } else if (error instanceof ApiError && error.status === 403) {
      loadError.value = TEXT.notAuthorized;
    } else {
      loadError.value = `${TEXT.generic} ${problemDetail(error)}`.trim();
    }
  }
}

function onFile(chosen: File | null, blocking: boolean): void {
  file.value = chosen;
  fileBlocking.value = blocking;
}

function checkAll(): boolean {
  errors.description = textError('Description', description.value, true);
  errors.changelog = textError('Change log', changelog.value, true);
  return !errors.description && !errors.changelog;
}

async function submit(): Promise<void> {
  report.value = [];
  message.value = '';
  if (!checkAll()) {
    await nextTick();
    document.getElementById(errors.description ? 'description' : 'changelog')?.focus();
    return;
  }
  const form = new FormData();
  form.append('description', description.value);
  form.append('changelog', changelog.value);
  if (file.value) {
    form.append('vocabularyFile', file.value, file.value.name);
  } else {
    let content = currentContent.value;
    try {
      content = content ?? (await api.content(name.value));
    } catch (error) {
      message.value = submitFailure(error).message;
      return;
    }
    form.append('vocabularyFile', content, `${name.value}.ttl`);
  }
  submitting.value = true;
  try {
    const created = await api.createVersion(name.value, form);
    setNotice({
      success: `Version ${created.version} of ${created.name} was created.`,
      warnings: created.validationWarnings ?? [],
    });
    await router.push(`/vocabularies/${created.name}/versions/${created.version}`);
  } catch (error) {
    const failure = submitFailure(error);
    report.value = failure.report;
    message.value = failure.message;
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  if (admin.value) {
    void load();
  }
});
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold">New version of {{ name }}</h1>
    <p v-if="!admin" role="alert" class="alert-error">{{ TEXT.notAuthorized }}</p>
    <p v-else-if="loadError" role="alert" class="alert-error">{{ loadError }}</p>
    <form v-else-if="vocabulary" class="mt-4 max-w-2xl" novalidate @submit.prevent="submit">
      <p v-if="message" role="alert" class="alert-error">{{ message }}</p>
      <ValidationReport v-if="report.length > 0" :issues="report" />
      <p class="mb-3 text-sm">
        Submitting creates version {{ vocabulary.version + 1 }} and deprecates every older ACTIVE version.
      </p>

      <label for="description" class="block text-sm font-medium"
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

      <label for="changelog" class="mt-3 block text-sm font-medium"
        >Change log<span aria-hidden="true" class="text-red-800"> *</span></label
      >
      <textarea
        id="changelog"
        v-model="changelog"
        rows="3"
        class="field-input"
        aria-required="true"
        :aria-invalid="errors.changelog ? 'true' : 'false'"
        aria-describedby="changelog-error"
        @blur="errors.changelog = textError('Change log', changelog, true)"
      ></textarea>
      <p id="changelog-error" class="field-error">{{ errors.changelog }}</p>

      <FileField
        id="vocabulary-file"
        :required="true"
        :current-name="`${name}.ttl (current content)`"
        @change="onFile"
      />

      <div class="mt-4 flex gap-2">
        <button type="submit" class="btn" :disabled="fileBlocking || submitting">Submit</button>
        <RouterLink :to="`/vocabularies/${name}`" class="btn-secondary no-underline">Cancel</RouterLink>
      </div>
    </form>
  </div>
</template>
