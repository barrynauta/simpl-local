<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ApiError, createSchema, type SchemaType } from '../api';
import { isAdmin } from '../auth';
import { notifyError, notifySuccess, clear } from '../notify';
import { NO_PERMISSION, PASCAL, RESOURCE_TYPES } from '../download';

// Create Schema form (SIMPL-13355, SIMPL-32800, D49).
const NAME_CLASH = 'A schema with that name already exists.';
const SEARCH_SLOT = 'A published federated search schema already exists.';

const router = useRouter();
const admin = isAdmin();

const form = reactive({
  name: '',
  title: '',
  description: '',
  schemaType: 'RESOURCE' as SchemaType,
  resourceType: '',
});
const file = ref<File | null>(null);
const touched = reactive<Record<string, boolean>>({});
const submitting = ref(false);

const errors = computed(() => {
  const e: Record<string, string> = {};
  if (!form.name.trim()) {
    e.name = 'Schema name is required.';
  } else if (form.name.length < 3 || form.name.length > 64 || !PASCAL.test(form.name)) {
    e.name = 'Use PascalCase letters and digits only, 3 to 64 characters (for example DataAsset).';
  }
  if (!form.title.trim()) {
    e.title = 'Title is required.';
  }
  if (!form.description.trim()) {
    e.description = 'Description is required.';
  }
  if (form.schemaType === 'RESOURCE' && !form.resourceType) {
    e.resourceType = 'Resource type is required for RESOURCE schemas.';
  }
  if (!file.value) {
    e.file = 'A .ttl file is required.';
  }
  return e;
});
const valid = computed(() => Object.keys(errors.value).length === 0);
const show = (field: string): string | undefined => (touched[field] ? errors.value[field] : undefined);

function typeChanged(): void {
  if (form.schemaType === 'SEARCH') {
    form.resourceType = '';
  }
}

function fileChanged(event: Event): void {
  const input = event.target as HTMLInputElement;
  file.value = input.files && input.files.length > 0 ? input.files[0] : null;
  touched.file = true;
}

function friendly(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.status === 409) {
      return e.detail.includes('federated search') ? SEARCH_SLOT : NAME_CLASH;
    }
    return e.detail;
  }
  return 'The schema could not be created.';
}

async function submit(): Promise<void> {
  if (!valid.value || submitting.value) {
    return;
  }
  submitting.value = true;
  clear();
  const data = new FormData();
  data.append('name', form.name);
  data.append('title', form.title);
  data.append('description', form.description);
  data.append('schemaType', form.schemaType);
  if (form.schemaType === 'RESOURCE') {
    data.append('resourceType', form.resourceType);
  }
  data.append('schemaFile', file.value as File, (file.value as File).name);
  try {
    await createSchema(data);
    notifySuccess(`Schema "${form.name}" created successfully.`);
    await router.push({ path: `/schemas/${encodeURIComponent(form.name)}`, query: { schemaType: form.schemaType } });
  } catch (e) {
    notifyError(friendly(e));
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section>
    <h1>Create New Schema</h1>
    <div v-if="!admin" class="state-box" data-testid="error-state">{{ NO_PERMISSION }}</div>
    <form v-else class="form" novalidate @submit.prevent="submit">
      <label>
        Schema name
        <input v-model="form.name" type="text" data-testid="field-name" @blur="touched.name = true" @input="touched.name = true" />
        <span v-if="show('name')" class="field-error" data-testid="error-name">{{ show('name') }}</span>
      </label>
      <label>
        Title
        <input v-model="form.title" type="text" data-testid="field-title" @blur="touched.title = true" @input="touched.title = true" />
        <span v-if="show('title')" class="field-error" data-testid="error-title">{{ show('title') }}</span>
      </label>
      <label>
        Description
        <textarea
          v-model="form.description"
          rows="3"
          data-testid="field-description"
          @blur="touched.description = true"
          @input="touched.description = true"
        ></textarea>
        <span v-if="show('description')" class="field-error" data-testid="error-description">{{ show('description') }}</span>
      </label>
      <label>
        Schema type
        <select v-model="form.schemaType" data-testid="field-schema-type" @change="typeChanged">
          <option value="RESOURCE">RESOURCE</option>
          <option value="SEARCH">SEARCH</option>
        </select>
      </label>
      <label>
        Resource type
        <select
          v-model="form.resourceType"
          data-testid="field-resource-type"
          :disabled="form.schemaType === 'SEARCH'"
          @blur="touched.resourceType = true"
          @change="touched.resourceType = true"
        >
          <option value="">Select a resource type</option>
          <option v-for="rt in RESOURCE_TYPES" :key="rt" :value="rt">{{ rt }}</option>
        </select>
        <span v-if="show('resourceType')" class="field-error" data-testid="error-resource-type">{{ show('resourceType') }}</span>
      </label>
      <label>
        Schema file (.ttl)
        <input type="file" accept=".ttl,text/turtle" data-testid="field-file" @change="fileChanged" />
        <span v-if="show('file')" class="field-error" data-testid="error-file">{{ show('file') }}</span>
      </label>
      <div class="form-actions">
        <button type="button" @click="router.push('/schemas')">Cancel</button>
        <button type="submit" class="primary" data-testid="submit-button" :disabled="!valid || submitting">Create</button>
      </div>
    </form>
  </section>
</template>
