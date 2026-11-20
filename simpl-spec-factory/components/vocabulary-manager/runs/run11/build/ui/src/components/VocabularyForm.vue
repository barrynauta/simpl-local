<script setup lang="ts">
import { nextTick, reactive, ref } from "vue";
import { ApiError, type CreatedVersion, type ProblemIssue } from "../api";
import { checkFile } from "../validation";
import { nameError, textError } from "../validation";
import { TEXT } from "../texts";
import FieldError from "./FieldError.vue";
import RequiredLabel from "./RequiredLabel.vue";
import ValidationReport from "./ValidationReport.vue";

/**
 * The create and new version form (SIMPL-18169, 18172, 18180, 18193; D13, D14, D32, D33, D37,
 * D46, D60, D62, D76, D101, D141). Field errors show under the fields; server errors above the form.
 */
const props = defineProps<{
  mode: "create" | "version";
  initialDescription?: string;
  currentFileName?: string;
  submit: (form: FormData) => Promise<CreatedVersion>;
  currentContent?: () => Promise<Blob>;
}>();
const emit = defineEmits<{ created: [CreatedVersion]; cancel: [] }>();

const values = reactive({ name: "", description: props.initialDescription ?? "", changelog: "" });
const errors = reactive({ name: "", description: "", changelog: "", file: "" });
const file = ref<File | null>(null);
const upload = reactive({ ok: true, message: "" });
const serverMessage = ref("");
const reportIssues = ref<ProblemIssue[]>([]);
const busy = ref(false);
const formElement = ref<HTMLFormElement | null>(null);

async function onFile(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  file.value = input.files?.[0] ?? null;
  errors.file = "";
  upload.ok = true;
  upload.message = "";
  if (!file.value) return;
  const result = await checkFile(file.value);
  upload.ok = result.ok;
  upload.message = result.message;
  errors.file = result.fieldError;
}

function checkFields(): boolean {
  errors.name = props.mode === "create" ? nameError(values.name) : "";
  errors.description = textError("Description", values.description);
  errors.changelog = props.mode === "version" ? textError("Change log", values.changelog) : "";
  if (!errors.file) {
    errors.file = !file.value && !props.currentFileName ? "Vocabulary file is required." : "";
  }
  return !errors.name && !errors.description && !errors.changelog && !errors.file;
}

async function focusFirstError(): Promise<void> {
  await nextTick();
  formElement.value?.querySelector<HTMLElement>("[aria-invalid='true']")?.focus();
}

function describeFailure(error: unknown): void {
  if (!(error instanceof ApiError)) {
    serverMessage.value = `${TEXT.genericError} ${String(error)}`;
    return;
  }
  const detail = error.problem.detail ?? "";
  if (error.status === 400 && error.problem.issues?.some((i) => i.name === "vocabularyFile" && i.location)) {
    reportIssues.value = error.problem.issues;
  } else if (error.status === 403) {
    serverMessage.value = TEXT.notAuthorized;
  } else if (error.status >= 500) {
    serverMessage.value = `${TEXT.genericError} ${detail}`.trim();
  } else {
    const issues = (error.problem.issues ?? []).map((i) => i.detail).filter(Boolean);
    serverMessage.value = [detail, ...issues].join(" ");
  }
}

async function onSubmit(): Promise<void> {
  serverMessage.value = "";
  reportIssues.value = [];
  if (!checkFields()) {
    await focusFirstError();
    return;
  }
  busy.value = true;
  try {
    const form = new FormData();
    const content = file.value ?? (props.currentContent ? await props.currentContent() : null);
    if (content) {
      form.append("vocabularyFile", content, file.value?.name ?? props.currentFileName ?? "vocabulary.ttl");
    }
    if (props.mode === "create") form.append("name", values.name);
    form.append("description", values.description);
    form.append("changelog", props.mode === "create" ? "Initial version." : values.changelog);
    emit("created", await props.submit(form));
  } catch (error) {
    describeFailure(error);
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <form ref="formElement" novalidate @submit.prevent="onSubmit">
    <p v-if="serverMessage" role="alert" class="error">{{ serverMessage }}</p>
    <ValidationReport v-if="reportIssues.length" :issues="reportIssues" />

    <div v-if="mode === 'create'" class="field">
      <RequiredLabel target="field-name" text="Name" />
      <input
        id="field-name"
        v-model="values.name"
        type="text"
        maxlength="64"
        aria-required="true"
        :aria-invalid="errors.name ? 'true' : 'false'"
        aria-describedby="error-name"
        @blur="errors.name = nameError(values.name)"
      />
      <FieldError id="error-name" :message="errors.name" />
    </div>

    <div class="field">
      <RequiredLabel target="field-description" text="Description" />
      <textarea
        id="field-description"
        v-model="values.description"
        rows="3"
        aria-required="true"
        :aria-invalid="errors.description ? 'true' : 'false'"
        aria-describedby="error-description"
        @blur="errors.description = textError('Description', values.description)"
      ></textarea>
      <FieldError id="error-description" :message="errors.description" />
    </div>

    <div v-if="mode === 'version'" class="field">
      <RequiredLabel target="field-changelog" text="Change log" />
      <textarea
        id="field-changelog"
        v-model="values.changelog"
        rows="3"
        aria-required="true"
        :aria-invalid="errors.changelog ? 'true' : 'false'"
        aria-describedby="error-changelog"
        @blur="errors.changelog = textError('Change log', values.changelog)"
      ></textarea>
      <FieldError id="error-changelog" :message="errors.changelog" />
    </div>

    <div class="field">
      <RequiredLabel target="field-file" text="Vocabulary file" />
      <p v-if="currentFileName && !file" class="hint">{{ currentFileName }}</p>
      <input
        id="field-file"
        type="file"
        accept=".ttl,text/turtle"
        aria-required="true"
        :aria-invalid="errors.file ? 'true' : 'false'"
        aria-describedby="error-file upload-check"
        @change="onFile"
      />
      <FieldError id="error-file" :message="errors.file" />
      <p id="upload-check" :class="upload.ok ? 'notice' : 'error'" :role="upload.ok ? 'status' : 'alert'">
        {{ upload.message }}
      </p>
    </div>

    <slot />

    <div class="buttons">
      <button
        type="submit"
        class="primary"
        :disabled="busy || !upload.ok || errors.file === TEXT.fileTooLarge"
      >
        Submit
      </button>
      <button type="button" @click="emit('cancel')">Cancel</button>
    </div>
  </form>
</template>
