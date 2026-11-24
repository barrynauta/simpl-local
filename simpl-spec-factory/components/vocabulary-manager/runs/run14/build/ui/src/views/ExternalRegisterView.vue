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
const namespaceUri = ref('');
const aliases = ref('');
const description = ref('');
const file = ref<File | null>(null);
const fileBlocking = ref(false);
const errors = reactive({ name: '', namespaceUri: '', aliases: '', description: '', file: '' });
const report = ref<Issue[]>([]);
const message = ref('');
const submitting = ref(false);

function uriError(value: string, label: string): string {
  if (!value.trim()) {
    return `${label} is required.`;
  }
  return /^[A-Za-z][A-Za-z0-9+.-]*:\S+$/.test(value.trim()) ? '' : `${label} must be an absolute URI.`;
}

function aliasList(): string[] {
  return aliases.value
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
}

function aliasError(): string {
  const list = aliasList();
  if (list.length > 20) {
    return 'At most 20 alias URIs are allowed.';
  }
  const bad = list.find((alias) => uriError(alias, 'Alias URI'));
  return bad ? 'Every alias URI must be an absolute URI.' : '';
}

function onFile(chosen: File | null, blocking: boolean): void {
  file.value = chosen;
  fileBlocking.value = blocking;
  errors.file = '';
}

async function submit(): Promise<void> {
  report.value = [];
  message.value = '';
  errors.name = nameError(name.value);
  errors.namespaceUri = uriError(namespaceUri.value, 'Namespace URI');
  errors.aliases = aliasError();
  errors.description = textError('Description', description.value, false);
  errors.file = file.value ? '' : 'Vocabulary file is required.';
  const order: [keyof typeof errors, string][] = [
    ['name', 'name'],
    ['namespaceUri', 'namespace-uri'],
    ['aliases', 'alias-uris'],
    ['description', 'description'],
    ['file', 'vocabulary-file'],
  ];
  const first = order.find(([key]) => errors[key]);
  if (first) {
    await nextTick();
    document.getElementById(first[1])?.focus();
    return;
  }
  const form = new FormData();
  form.append('name', name.value);
  form.append('namespaceUri', namespaceUri.value.trim());
  for (const alias of aliasList()) {
    form.append('aliasUris', alias);
  }
  if (description.value.trim()) {
    form.append('description', description.value);
  }
  form.append('vocabularyFile', file.value as File, (file.value as File).name);
  submitting.value = true;
  try {
    const registered = await api.registerExternal(form);
    setNotice({ success: `External vocabulary ${registered.name} was registered.` });
    await router.push('/external-vocabularies');
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
    <h1 class="text-2xl font-semibold">Register external vocabulary</h1>
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

      <label for="namespace-uri" class="mt-3 block text-sm font-medium"
        >Namespace URI<span aria-hidden="true" class="text-red-800"> *</span></label
      >
      <input
        id="namespace-uri"
        v-model="namespaceUri"
        type="text"
        class="field-input"
        aria-required="true"
        :aria-invalid="errors.namespaceUri ? 'true' : 'false'"
        aria-describedby="namespace-uri-error"
        @blur="errors.namespaceUri = uriError(namespaceUri, 'Namespace URI')"
      />
      <p id="namespace-uri-error" class="field-error">{{ errors.namespaceUri }}</p>

      <label for="alias-uris" class="mt-3 block text-sm font-medium">Alias URIs</label>
      <textarea
        id="alias-uris"
        v-model="aliases"
        rows="3"
        class="field-input"
        :aria-invalid="errors.aliases ? 'true' : 'false'"
        aria-describedby="alias-uris-hint alias-uris-error"
        @blur="errors.aliases = aliasError()"
      ></textarea>
      <p id="alias-uris-hint" class="text-sm text-gray-800">One URI per line.</p>
      <p id="alias-uris-error" class="field-error">{{ errors.aliases }}</p>

      <label for="description" class="mt-3 block text-sm font-medium">Description</label>
      <textarea
        id="description"
        v-model="description"
        rows="3"
        class="field-input"
        :aria-invalid="errors.description ? 'true' : 'false'"
        aria-describedby="description-error"
        @blur="errors.description = textError('Description', description, false)"
      ></textarea>
      <p id="description-error" class="field-error">{{ errors.description }}</p>

      <FileField id="vocabulary-file" :required="true" :error="errors.file" @change="onFile" />

      <div class="mt-4 flex gap-2">
        <button type="submit" class="btn" :disabled="fileBlocking || submitting">Submit</button>
        <RouterLink to="/external-vocabularies" class="btn-secondary no-underline">Cancel</RouterLink>
      </div>
    </form>
  </div>
</template>
