<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ApiError,
  createVersion,
  fetchContent,
  getVocabulary,
  type ProblemIssue,
  type Vocabulary,
} from "../api";
import { isAdmin } from "../auth";
import { describeError } from "../errors";
import { textError } from "../format";
import { setNotice } from "../notifications";
import { TEXTS } from "../texts";
import { checkFile, type FileCheck } from "../turtleCheck";
import ProblemReport from "../components/ProblemReport.vue";

// New version form (SIMPL-18172, 18189, 18191; decisions D33, D37, D46).
const props = defineProps<{ name: string }>();
const router = useRouter();
const admin = isAdmin();
const vocabulary = ref<Vocabulary | null>(null);
const loadError = ref<{ message: string; detail: string } | null>(null);
const fields = reactive({ description: "", changelog: "" });
const touched = reactive({ description: false, changelog: false });
const newFile = ref<File | null>(null);
const fileCheck = ref<FileCheck | null>(null);
const submitting = ref(false);
const report = ref<{ message: string; detail: string; issues: ProblemIssue[] } | null>(null);

const errors = computed(() => ({
  description: textError("Description", fields.description),
  changelog: textError("Change log", fields.changelog),
}));
const submitDisabled = computed(() => submitting.value || fileCheck.value?.ok === false);

onMounted(async () => {
  if (!admin) {
    return;
  }
  try {
    vocabulary.value = await getVocabulary(props.name);
    fields.description = vocabulary.value.description;
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      loadError.value = { message: TEXTS.vocabularyNotFound, detail: "" };
    } else {
      loadError.value = describeError(e);
    }
  }
});

async function onFile(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  newFile.value = input.files?.[0] ?? null;
  fileCheck.value = newFile.value ? await checkFile(newFile.value) : null;
}

async function submit(): Promise<void> {
  touched.description = touched.changelog = true;
  if (!vocabulary.value || submitDisabled.value || errors.value.description || errors.value.changelog) {
    return;
  }
  submitting.value = true;
  report.value = null;
  try {
    const form = new FormData();
    form.append("description", fields.description.trim());
    form.append("changelog", fields.changelog.trim());
    // Decision D33: always send a file, the new one or the current content of the latest version.
    const content = newFile.value ?? (await fetchContent(props.name));
    form.append("vocabularyFile", content, newFile.value?.name ?? `${props.name}.ttl`);
    const created = await createVersion(props.name, form);
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
  <h1>Update {{ name }}</h1>
  <div v-if="!admin" class="message error" role="alert">{{ TEXTS.notAuthorized }}</div>
  <div v-else-if="loadError" class="message error" role="alert">
    <p>{{ loadError.message }}</p>
    <p v-if="loadError.detail">{{ loadError.detail }}</p>
    <RouterLink to="/vocabularies">{{ TEXTS.backToList }}</RouterLink>
  </div>
  <template v-else-if="vocabulary">
    <ProblemReport v-if="report" :message="report.message" :detail="report.detail" :issues="report.issues" />
    <p class="message info">{{ TEXTS.submitCreates(vocabulary.version + 1) }}</p>
    <form novalidate @submit.prevent="submit">
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
        Change log
        <textarea v-model="fields.changelog" name="changelog" rows="3" @blur="touched.changelog = true" />
        <span v-if="touched.changelog && errors.changelog" class="field-error">{{ errors.changelog }}</span>
      </label>
      <label>
        Vocabulary file
        <span class="current-file">{{ newFile ? newFile.name : TEXTS.currentContent(name) }}</span>
        <input name="vocabularyFile" type="file" accept=".ttl,text/turtle" @change="onFile" />
      </label>
      <div v-if="fileCheck" :class="['message', fileCheck.ok ? 'success' : 'error']" role="status">
        {{ fileCheck.message }}
      </div>
      <div>
        <button type="submit" :disabled="submitDisabled">{{ TEXTS.submit }}</button>
        <button type="button" class="secondary" @click="router.push(`/vocabularies/${name}`)">
          {{ TEXTS.cancel }}
        </button>
      </div>
    </form>
  </template>
</template>
