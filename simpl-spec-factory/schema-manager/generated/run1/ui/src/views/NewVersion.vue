<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ApiError, createVersion, type SchemaType } from '../api';
import { isAdmin } from '../auth';
import { notifyError, notifySuccess, clear } from '../notify';
import { NO_PERMISSION } from '../download';

// Create New Version form (SIMPL-13358, SIMPL-32808, SIMPL-20770, D50). No version field.
const props = defineProps<{ name: string }>();
const route = useRoute();
const router = useRouter();
const admin = isAdmin();
const schemaType = computed<SchemaType>(() => (route.query.schemaType === 'SEARCH' ? 'SEARCH' : 'RESOURCE'));

const changelog = ref('');
const file = ref<File | null>(null);
const touched = reactive<Record<string, boolean>>({});
const submitting = ref(false);

const errors = computed(() => {
  const e: Record<string, string> = {};
  if (!changelog.value.trim()) {
    e.changelog = 'Changelog is required.';
  }
  if (!file.value) {
    e.file = 'A .ttl file is required.';
  }
  return e;
});
const valid = computed(() => Object.keys(errors.value).length === 0);
const show = (field: string): string | undefined => (touched[field] ? errors.value[field] : undefined);

function fileChanged(event: Event): void {
  const input = event.target as HTMLInputElement;
  file.value = input.files && input.files.length > 0 ? input.files[0] : null;
  touched.file = true;
}

function details() {
  return { path: `/schemas/${encodeURIComponent(props.name)}`, query: { schemaType: schemaType.value } };
}

async function submit(): Promise<void> {
  if (!valid.value || submitting.value) {
    return;
  }
  submitting.value = true;
  clear();
  const data = new FormData();
  data.append('changelog', changelog.value);
  data.append('schemaType', schemaType.value);
  data.append('schemaFile', file.value as File, (file.value as File).name);
  try {
    const result = await createVersion(props.name, data);
    notifySuccess(`Schema version ${result.version} created successfully.`);
    await router.push(details());
  } catch (e) {
    notifyError(e instanceof ApiError ? e.detail : 'The schema version could not be created.');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section>
    <h1>Create New Version of {{ name }}</h1>
    <div v-if="!admin" class="state-box" data-testid="error-state">{{ NO_PERMISSION }}</div>
    <form v-else class="form" novalidate @submit.prevent="submit">
      <label>
        Schema type
        <select :value="schemaType" data-testid="field-schema-type" disabled>
          <option value="RESOURCE">RESOURCE</option>
          <option value="SEARCH">SEARCH</option>
        </select>
      </label>
      <label>
        Changelog
        <textarea
          v-model="changelog"
          rows="3"
          data-testid="field-changelog"
          @blur="touched.changelog = true"
          @input="touched.changelog = true"
        ></textarea>
        <span v-if="show('changelog')" class="field-error" data-testid="error-changelog">{{ show('changelog') }}</span>
      </label>
      <label>
        Schema file (.ttl)
        <input type="file" accept=".ttl,text/turtle" data-testid="field-file" @change="fileChanged" />
        <span v-if="show('file')" class="field-error" data-testid="error-file">{{ show('file') }}</span>
      </label>
      <div class="form-actions">
        <button type="button" @click="router.push(details())">Cancel</button>
        <button type="submit" class="primary" data-testid="submit-button" :disabled="!valid || submitting">Create Version</button>
      </div>
    </form>
  </section>
</template>
