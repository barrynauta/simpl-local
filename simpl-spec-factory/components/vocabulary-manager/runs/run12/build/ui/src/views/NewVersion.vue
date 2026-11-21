<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Button from 'primevue/button';
import Textarea from 'primevue/textarea';
import AlertMessage from '../components/AlertMessage.vue';
import RequiredMark from '../components/RequiredMark.vue';
import ValidationReport from '../components/ValidationReport.vue';
import VocabularyFileField from '../components/VocabularyFileField.vue';
import { api, type Problem, type Vocabulary } from '../api';
import { isAdmin } from '../auth';
import { setFlash } from '../flash';
import { isStatus, problemDetail, validateText } from '../format';
import { submitOutcome } from '../submitError';
import { TEXT } from '../texts';
import { btnPrimary, btnSecondary, fieldErrorClass, inputClass, linkClass } from '../ui';

/**
 * New version form at /vocabularies/<Name>/versions/new
 * (D13, D14, D33, D37, D46, D60, D62, D74, D76, D101, D113, D141, D161).
 */
const route = useRoute();
const router = useRouter();
const admin = computed(() => isAdmin());
const name = computed(() => String(route.params.name ?? ''));

const vocabulary = ref<Vocabulary | null>(null);
const notFound = ref(false);
const loadError = ref<{ text: string; detail: string } | null>(null);
const form = reactive({ description: '', changelog: '' });
const file = ref<File | null>(null);
const fileBlocked = ref(false);
const touched = reactive({ description: false, changelog: false });
const submitted = ref(false);
const busy = ref(false);
const report = ref<Problem | null>(null);
const alert = ref<{ text: string; detail: string } | null>(null);

const descriptionError = computed(() =>
  touched.description || submitted.value ? validateText(form.description, 'Description') : '',
);
const changelogError = computed(() =>
  touched.changelog || submitted.value ? validateText(form.changelog, 'Change log') : '',
);

function onFile(chosen: File | null, blocked: boolean) {
  file.value = chosen;
  fileBlocked.value = blocked;
}

async function load() {
  try {
    vocabulary.value = await api.getVocabulary(name.value);
    form.description = vocabulary.value.description ?? '';
  } catch (error) {
    if (isStatus(error, 404)) notFound.value = true;
    else if (isStatus(error, 403)) loadError.value = { text: TEXT.notAuthorized, detail: '' };
    else loadError.value = { text: TEXT.genericFailure, detail: problemDetail(error) };
  }
}

async function submit() {
  if (busy.value || !vocabulary.value) return;
  submitted.value = true;
  await nextTick();
  if (descriptionError.value) return document.getElementById('description')?.focus();
  if (changelogError.value) return document.getElementById('changelog')?.focus();
  if (fileBlocked.value) return;

  busy.value = true;
  report.value = null;
  alert.value = null;
  try {
    let upload = file.value;
    if (!upload) {
      // D33: without a new file the current content of the latest version is sent.
      const current = await api.downloadContent(name.value);
      upload = new File([current.blob], `${name.value}.ttl`, { type: 'text/turtle' });
    }
    const data = new FormData();
    data.append('description', form.description);
    data.append('changelog', form.changelog);
    data.append('vocabularyFile', upload, upload.name);
    const result = await api.createVersion(name.value, data);
    setFlash(
      result.name,
      `Version ${result.version} of ${result.name} was created.`,
      result.validationWarnings ?? [],
    );
    await router.push(`/vocabularies/${result.name}/versions/${result.version}`);
  } catch (error) {
    const outcome = submitOutcome(error);
    if (outcome.kind === 'report') report.value = outcome.problem;
    else alert.value = { text: outcome.text, detail: outcome.detail };
  } finally {
    busy.value = false;
  }
}

onMounted(() => {
  document.title = `New version of ${name.value} - Vocabulary Manager`;
  if (admin.value) void load();
});
</script>

<template>
  <div class="max-w-2xl">
    <AlertMessage v-if="!admin" :text="TEXT.notAuthorized" />
    <template v-else>
      <p class="mb-3">
        <RouterLink to="/vocabularies" :class="linkClass">{{ TEXT.backToList }}</RouterLink>
      </p>
      <AlertMessage v-if="notFound" :text="TEXT.vocabularyNotFound" />
      <AlertMessage v-if="loadError" :text="loadError.text" :detail="loadError.detail" />

      <template v-if="vocabulary">
        <h1 class="mb-4 text-2xl font-semibold">New version of {{ vocabulary.name }}</h1>
        <ValidationReport v-if="report" :problem="report" />
        <AlertMessage v-if="alert" :text="alert.text" :detail="alert.detail" />
        <p class="mb-4">
          Submitting creates version {{ vocabulary.version + 1 }} and deprecates every older ACTIVE
          version.
        </p>

        <form novalidate @submit.prevent="submit">
          <div class="mb-4">
            <label for="description" class="inline-block text-sm font-medium">Description</label
            ><RequiredMark />
            <Textarea
              id="description"
              v-model="form.description"
              rows="3"
              aria-required="true"
              :aria-invalid="descriptionError ? 'true' : 'false'"
              aria-describedby="description-error"
              :class="inputClass"
              @blur="touched.description = true"
            />
            <p id="description-error" :class="fieldErrorClass">{{ descriptionError }}</p>
          </div>

          <div class="mb-4">
            <label for="changelog" class="inline-block text-sm font-medium">Change log</label
            ><RequiredMark />
            <Textarea
              id="changelog"
              v-model="form.changelog"
              rows="3"
              aria-required="true"
              :aria-invalid="changelogError ? 'true' : 'false'"
              aria-describedby="changelog-error"
              :class="inputClass"
              @blur="touched.changelog = true"
            />
            <p id="changelog-error" :class="fieldErrorClass">{{ changelogError }}</p>
          </div>

          <VocabularyFileField
            :current-file-label="`${vocabulary.name}.ttl (current content)`"
            @change="onFile"
          />

          <div class="flex gap-2">
            <Button type="submit" label="Submit" :class="btnPrimary" :disabled="fileBlocked" />
            <Button
              type="button"
              label="Cancel"
              :class="btnSecondary"
              @click="router.push(`/vocabularies/${vocabulary.name}`)"
            />
          </div>
        </form>
      </template>
    </template>
  </div>
</template>
