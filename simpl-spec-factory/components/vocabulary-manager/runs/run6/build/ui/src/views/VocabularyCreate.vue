<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ApiError, createVocabulary, type ProblemIssue } from "../api";
import { isAdmin } from "../auth";
import { describeError } from "../errors";
import { nameError, textError } from "../format";
import { setNotice } from "../notifications";
import { TEXTS } from "../texts";
import { checkFile, type FileCheck } from "../turtleCheck";
import ProblemReport from "../components/ProblemReport.vue";

// Create form (SIMPL-18169, 18180, 18186, 18193; decisions D13, D14, D15, D32, D37).
const router = useRouter();
const admin = isAdmin();
const fields = reactive({ name: "", description: "" });
const touched = reactive({ name: false, description: false, file: false });
const file = ref<File | null>(null);
const fileCheck = ref<FileCheck | null>(null);
const submitting = ref(false);
const report = ref<{ message: string; detail: string; issues: ProblemIssue[] } | null>(null);

const errors = computed(() => ({
  name: nameError(fields.name),
  description: textError("Description", fields.description),
  file: file.value ? null : "Vocabulary file is required.",
}));
// Decision D13: the submit button is disabled while the upload check shows errors; other field errors are
// shown under the fields and stop the submit.
const submitDisabled = computed(() => submitting.value || fileCheck.value?.ok === false);
const canSubmit = computed(
  () =>
    !submitDisabled.value &&
    !errors.value.name &&
    !errors.value.description &&
    !errors.value.file &&
    fileCheck.value?.ok === true,
);

async function onFile(event: Event): Promise<void> {
  touched.file = true;
  const input = event.target as HTMLInputElement;
  file.value = input.files?.[0] ?? null;
  fileCheck.value = file.value ? await checkFile(file.value) : null;
}

async function submit(): Promise<void> {
  touched.name = touched.description = touched.file = true;
  if (!canSubmit.value || !file.value) {
    return;
  }
  submitting.value = true;
  report.value = null;
  const form = new FormData();
  form.append("name", fields.name);
  form.append("description", fields.description.trim());
  form.append("changelog", TEXTS.initialChangelog);
  form.append("vocabularyFile", file.value, file.value.name);
  try {
    const created = await createVocabulary(form);
    setNotice(TEXTS.versionCreated(created.version, created.name), created.validationWarnings);
    await router.push(`/vocabularies/${created.name}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 400) {
      report.value = {
        message: "Validation failed. Fix the errors below.",
        detail: "",
        issues: e.problem.issues ?? [],
      };
    } else {
      const described = describeError(e);
      report.value = { message: described.message, detail: described.detail, issues: [] };
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <h1>New vocabulary</h1>
  <div v-if="!admin" class="message error" role="alert">{{ TEXTS.notAuthorized }}</div>
  <template v-else>
    <ProblemReport v-if="report" :message="report.message" :detail="report.detail" :issues="report.issues" />
    <form novalidate @submit.prevent="submit">
      <label>
        Name
        <input v-model="fields.name" name="name" type="text" maxlength="64" @blur="touched.name = true" />
        <span v-if="touched.name && errors.name" class="field-error">{{ errors.name }}</span>
      </label>
      <label>
        Description
        <textarea
          v-model="fields.description"
          name="description"
          rows="3"
          @blur="touched.description = true"
        />
        <span v-if="touched.description && errors.description" class="field-error">{{
          errors.description
        }}</span>
      </label>
      <label>
        Vocabulary file
        <input name="vocabularyFile" type="file" accept=".ttl,text/turtle" @change="onFile" />
        <span v-if="touched.file && errors.file" class="field-error">{{ errors.file }}</span>
      </label>
      <div v-if="fileCheck" :class="['message', fileCheck.ok ? 'success' : 'error']" role="status">
        {{ fileCheck.message }}
      </div>
      <div>
        <button type="submit" :disabled="submitDisabled">{{ TEXTS.submit }}</button>
        <button type="button" class="secondary" @click="router.push('/vocabularies')">
          {{ TEXTS.cancel }}
        </button>
      </div>
    </form>
  </template>
</template>
