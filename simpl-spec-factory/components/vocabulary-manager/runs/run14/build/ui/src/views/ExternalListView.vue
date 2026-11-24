<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import ConfirmDialog from '../components/ConfirmDialog.vue';
import PageControls from '../components/PageControls.vue';
import { api, saveBlob, type ExternalVocabulary } from '../lib/api';
import { isAdmin } from '../lib/auth';
import { takeNotice } from '../lib/notices';
import { formatDate, problemDetail, writeErrorText } from '../lib/texts';

const items = ref<ExternalVocabulary[]>([]);
const page = ref(1);
const totalPages = ref(0);
const total = ref(0);
const loading = ref(true);
const loadError = ref('');
const actionError = ref('');
const success = ref('');
const pending = ref<ExternalVocabulary | null>(null);
const admin = computed(() => isAdmin());

async function load(): Promise<void> {
  loading.value = true;
  loadError.value = '';
  try {
    const result = await api.listExternal(page.value);
    items.value = result.items;
    totalPages.value = result.totalPages;
    total.value = result.total;
  } catch (error) {
    items.value = [];
    loadError.value = `The external vocabulary list could not be loaded. ${problemDetail(error)}`.trim();
  } finally {
    loading.value = false;
  }
}

async function download(vocabulary: ExternalVocabulary): Promise<void> {
  actionError.value = '';
  try {
    saveBlob(await api.externalContent(vocabulary.name), `${vocabulary.name}.ttl`);
  } catch (error) {
    actionError.value = writeErrorText(error);
  }
}

async function confirmRemove(): Promise<void> {
  const target = pending.value;
  pending.value = null;
  if (!target) {
    return;
  }
  success.value = '';
  actionError.value = '';
  try {
    await api.removeExternal(target.name);
    success.value = `External vocabulary ${target.name} was removed.`;
    await load();
  } catch (error) {
    actionError.value = writeErrorText(error);
  }
}

function go(target: number): void {
  page.value = target;
  void load();
}

onMounted(() => {
  success.value = takeNotice().success ?? '';
  void load();
});
</script>

<template>
  <div>
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-semibold">External vocabularies</h1>
      <RouterLink v-if="admin" to="/external-vocabularies/new" class="btn no-underline"
        >Register external vocabulary +</RouterLink
      >
    </div>
    <p v-if="success" role="status" class="alert-success">{{ success }}</p>
    <p v-if="actionError" role="alert" class="alert-error">{{ actionError }}</p>
    <p v-if="loadError" role="alert" class="alert-error">{{ loadError }}</p>
    <p v-else-if="!loading && total === 0" class="mt-4">No external vocabularies found.</p>

    <table v-if="items.length > 0" class="data mt-4">
      <caption class="sr-only">
        External vocabularies
      </caption>
      <thead>
        <tr>
          <th scope="col">Name</th>
          <th scope="col">Namespace URI</th>
          <th scope="col">Alias URIs</th>
          <th scope="col">Description</th>
          <th scope="col">Created by</th>
          <th scope="col">Date created</th>
          <th scope="col">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="vocabulary in items" :key="vocabulary.name">
          <td>{{ vocabulary.name }}</td>
          <td class="break-all">{{ vocabulary.namespaceUri }}</td>
          <td class="break-all">
            <span v-for="alias in vocabulary.aliasUris" :key="alias" class="block">{{ alias }}</span>
          </td>
          <td>{{ vocabulary.description }}</td>
          <td>{{ vocabulary.createdBy }}</td>
          <td>{{ formatDate(vocabulary.creationDate) }}</td>
          <td class="whitespace-nowrap">
            <button type="button" class="btn-secondary" @click="download(vocabulary)">Download</button>
            <button v-if="admin" type="button" class="btn-secondary ml-2" @click="pending = vocabulary">
              Remove
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <PageControls v-if="total > 0" :page="page" :total-pages="totalPages" @go="go" />

    <ConfirmDialog
      :visible="pending !== null"
      title="Remove external vocabulary"
      :text="pending ? `Do you want to remove '${pending.name}'?` : ''"
      @confirm="confirmRemove"
      @cancel="pending = null"
    />
  </div>
</template>
