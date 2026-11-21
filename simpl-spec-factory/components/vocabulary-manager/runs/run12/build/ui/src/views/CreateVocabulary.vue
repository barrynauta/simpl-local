<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import AlertMessage from '../components/AlertMessage.vue';
import RequiredMark from '../components/RequiredMark.vue';
import ValidationReport from '../components/ValidationReport.vue';
import VocabularyFileField from '../components/VocabularyFileField.vue';
import { api, type Problem } from '../api';
import { isAdmin } from '../auth';
import { setFlash } from '../flash';
import { validateName, validateText } from '../format';
import { submitOutcome } from '../submitError';
import { TEXT } from '../texts';
import { btnPrimary, btnSecondary, fieldErrorClass, inputClass } from '../ui';

/** Create form at /vocabularies/new (D13, D14, D32, D37, D60, D62, D72, D74, D76, D101, D141). */
const router = useRouter();
const admin = computed(() => isAdmin());

const form = reactive({ name: '', description: '' });
const file = ref<File | null>(null);
const fileBlocked = ref(false);
const touched = reactive({ name: false, description: false });
const submitted = ref(false);
const busy = ref(false);
const report = ref<Problem | null>(null);
const alert = ref<{ text: string; detail: string } | null>(null);
const fileField = ref<InstanceType<typeof VocabularyFileField> | null>(null);

const nameError = computed(() => (touched.name || submitted.value ? validateName(form.name) : ''));
const descriptionError = computed(() =>
  touched.description || submitted.value ? validateText(form.description, 'Description') : '',
);
const fileError = computed(() =>
  submitted.value && !file.value ? 'Vocabulary file is required.' : '',
);

function onFile(chosen: File | null, blocked: boolean) {
  file.value = chosen;
  fileBlocked.value = blocked;
}

async function submit() {
  if (busy.value) return;
  submitted.value = true;
  await nextTick();
  if (nameError.value) return document.getElementById('name')?.focus();
  if (descriptionError.value) return document.getElementById('description')?.focus();
  if (fileError.value || !file.value) return fileField.value?.focus();
  if (fileBlocked.value) return;

  busy.value = true;
  report.value = null;
  alert.value = null;
  const data = new FormData();
  data.append('name', form.name);
  data.append('description', form.description);
  data.append('changelog', TEXT.initialChangelog);
  data.append('vocabularyFile', file.value, file.value.name);
  try {
    const result = await api.createVocabulary(data);
    setFlash(
      result.name,
      `Version ${result.version} of ${result.name} was created.`,
      result.validationWarnings ?? [],
    );
    await router.push(`/vocabularies/${result.name}`);
  } catch (error) {
    const outcome = submitOutcome(error);
    if (outcome.kind === 'report') report.value = outcome.problem;
    else alert.value = { text: outcome.text, detail: outcome.detail };
  } finally {
    busy.value = false;
  }
}

onMounted(() => {
  document.title = 'New vocabulary - Vocabulary Manager';
});
</script>

<template>
  <div class="max-w-2xl">
    <AlertMessage v-if="!admin" :text="TEXT.notAuthorized" />
    <template v-else>
      <h1 class="mb-4 text-2xl font-semibold">New vocabulary</h1>
      <ValidationReport v-if="report" :problem="report" />
      <AlertMessage v-if="alert" :text="alert.text" :detail="alert.detail" />

      <form novalidate @submit.prevent="submit">
        <div class="mb-4">
          <label for="name" class="inline-block text-sm font-medium">Name</label><RequiredMark />
          <InputText
            id="name"
            v-model="form.name"
            aria-required="true"
            :aria-invalid="nameError ? 'true' : 'false'"
            aria-describedby="name-error"
            autocomplete="off"
            :class="inputClass"
            @blur="touched.name = true"
          />
          <p id="name-error" :class="fieldErrorClass">{{ nameError }}</p>
        </div>

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

        <VocabularyFileField ref="fileField" :field-error="fileError" @change="onFile" />

        <div class="flex gap-2">
          <Button type="submit" label="Submit" :class="btnPrimary" :disabled="fileBlocked" />
          <Button
            type="button"
            label="Cancel"
            :class="btnSecondary"
            @click="router.push('/vocabularies')"
          />
        </div>
      </form>
    </template>
  </div>
</template>
