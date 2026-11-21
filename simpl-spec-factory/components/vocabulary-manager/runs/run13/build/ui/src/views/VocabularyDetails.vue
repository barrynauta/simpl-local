<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { MenuItem } from 'primevue/menuitem';
import ActionsMenu from '../components/ActionsMenu.vue';
import WarningsPanel from '../components/WarningsPanel.vue';
import {
  ApiError,
  downloadContent,
  getVocabulary,
  listAllVersions,
  patchVocabularyStatus,
  type Status,
  type Vocabulary,
  type VocabularyVersion,
} from '../api';
import { isAdmin } from '../auth';
import { loadErrorText, statusErrorText, submitErrorText } from '../errors';
import { formatDate, statusLabel } from '../format';
import { takeNotice, TEXT, type CreationNotice } from '../messages';

const route = useRoute();
const router = useRouter();
const admin = isAdmin();

const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const notFound = ref(false);
const loadError = ref('');
const actionError = ref('');
const success = ref('');
const notice = ref<CreationNotice | null>(takeNotice());

const name = computed(() => String(route.params.name));
const selectedVersion = computed(() =>
  route.params.version === undefined ? undefined : Number(route.params.version),
);
const selected = computed(() => versions.value.find((v) => v.version === selectedVersion.value));

/** Highest-numbered version whose own status is ACTIVE (D5). */
const latestActive = computed(() => {
  const active = versions.value.filter((v) => v.status === 'ACTIVE');
  return active.length === 0 ? undefined : Math.max(...active.map((v) => v.version));
});

function effectivelyDeprecated(version: VocabularyVersion): boolean {
  return version.status === 'DEPRECATED' || vocabulary.value?.status === 'DEPRECATED';
}

/** D36, D47, D64: link only for an ACTIVE vocabulary and never to the version itself. */
function activeLink(version: VocabularyVersion): number | undefined {
  if (vocabulary.value?.status !== 'ACTIVE') return undefined;
  const target = latestActive.value;
  return target === undefined || target === version.version ? undefined : target;
}

async function load(): Promise<void> {
  notFound.value = false;
  loadError.value = '';
  try {
    const [vocab, list] = await Promise.all([
      getVocabulary(name.value),
      listAllVersions(name.value),
    ]);
    vocabulary.value = vocab;
    versions.value = list;
  } catch (error) {
    vocabulary.value = null;
    versions.value = [];
    if (error instanceof ApiError && error.status === 404) notFound.value = true;
    else loadError.value = loadErrorText(error, TEXT.historyLoadError);
  }
}

async function download(version?: number): Promise<void> {
  actionError.value = '';
  try {
    await downloadContent(name.value, version);
  } catch (error) {
    actionError.value = submitErrorText(error);
  }
}

async function changeStatus(): Promise<void> {
  if (!vocabulary.value) return;
  const target: Status = vocabulary.value.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  actionError.value = '';
  success.value = '';
  try {
    await patchVocabularyStatus(name.value, target);
    success.value = `Status of ${name.value} changed to ${statusLabel(target)}.`;
    await load();
  } catch (error) {
    actionError.value = statusErrorText(error);
  }
}

function versionActions(version: VocabularyVersion): MenuItem[] {
  return [
    {
      label: 'View version details',
      command: () => void router.push(`/vocabularies/${name.value}/versions/${version.version}`),
    },
    { label: 'Download', command: () => void download(version.version) },
  ];
}

const headerActions = computed<MenuItem[]>(() => {
  const menu: MenuItem[] = [{ label: 'Download vocabulary', command: () => void download() }];
  if (admin && vocabulary.value) {
    menu.push({
      label: 'Update',
      command: () => void router.push(`/vocabularies/${name.value}/versions/new`),
    });
    menu.push({
      label: vocabulary.value.status === 'ACTIVE' ? 'Deprecate' : 'Activate',
      command: () => void changeStatus(),
    });
  }
  return menu;
});

onMounted(load);
watch(name, load);
defineExpose({ load, changeStatus, download });
</script>

<template>
  <div>
    <RouterLink to="/vocabularies" class="text-blue-800 underline"
      >Back to vocabulary list</RouterLink
    >

    <div v-if="notFound" role="alert" class="mt-4 rounded bg-red-50 p-3 text-red-900">
      {{ TEXT.notFound }}
    </div>
    <div v-else-if="loadError" role="alert" class="mt-4 rounded bg-red-50 p-3 text-red-900">
      {{ loadError }}
    </div>

    <h1 class="mt-4 text-2xl font-bold text-slate-900">{{ name }}</h1>

    <template v-if="vocabulary">
      <p
        v-if="vocabulary.status === 'DEPRECATED'"
        role="status"
        class="mt-3 rounded border border-amber-700 bg-amber-50 p-2 text-amber-950"
      >
        {{ TEXT.vocabularyDeprecated }}
      </p>
      <div v-if="notice" class="mt-3">
        <p role="status" class="mb-3 rounded bg-green-50 p-2 text-green-900">
          {{ notice.message }}
        </p>
        <WarningsPanel v-if="notice.warnings.length > 0" :warnings="notice.warnings" />
      </div>
      <p v-if="success" role="status" class="mt-3 rounded bg-green-50 p-2 text-green-900">
        {{ success }}
      </p>
      <p v-if="actionError" role="alert" class="mt-3 rounded bg-red-50 p-2 text-red-900">
        {{ actionError }}
      </p>

      <dl class="mt-3 grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1">
        <dt class="font-semibold">Status</dt>
        <dd>{{ statusLabel(vocabulary.status) }}</dd>
        <dt class="font-semibold">Description</dt>
        <dd>{{ vocabulary.description }}</dd>
        <dt class="font-semibold">Latest version</dt>
        <dd>{{ vocabulary.version }}</dd>
      </dl>
      <div class="mt-3 flex items-center gap-2">
        <span class="text-sm font-medium">Actions</span>
        <ActionsMenu :label="`Actions for ${vocabulary.name}`" :items="headerActions" />
      </div>

      <h2 class="mt-6 text-xl font-semibold text-slate-900">Version history</h2>
      <table class="mt-2 w-full border-collapse text-left text-sm">
        <caption class="sr-only">
          Version history
        </caption>
        <thead>
          <tr class="border-b border-slate-400">
            <th scope="col" class="p-2">Version</th>
            <th scope="col" class="p-2">Status</th>
            <th scope="col" class="p-2">Date created</th>
            <th scope="col" class="p-2">Description</th>
            <th scope="col" class="p-2">Change log</th>
            <th scope="col" class="p-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="version in versions"
            :key="version.version"
            class="border-b border-slate-200"
            :class="{ 'bg-blue-50': version.version === selectedVersion }"
            :aria-current="version.version === selectedVersion ? 'true' : undefined"
          >
            <td class="p-2">
              <RouterLink
                :to="`/vocabularies/${name}/versions/${version.version}`"
                class="text-blue-800 underline"
                >{{ version.version }}</RouterLink
              >
            </td>
            <td class="p-2">
              {{ statusLabel(version.status) }}
              <span v-if="effectivelyDeprecated(version)" class="block text-amber-900">
                {{ TEXT.versionDeprecated }}
                <RouterLink
                  v-if="activeLink(version) !== undefined"
                  :to="`/vocabularies/${name}/versions/${activeLink(version)}`"
                  class="text-blue-800 underline"
                  >Latest ACTIVE version: {{ activeLink(version) }}</RouterLink
                >
              </span>
            </td>
            <td class="p-2">{{ formatDate(version.creationDate) }}</td>
            <td class="p-2">{{ version.description }}</td>
            <td class="p-2">{{ version.changelog }}</td>
            <td class="p-2">
              <ActionsMenu
                :label="`Actions for version ${version.version}`"
                :items="versionActions(version)"
              />
            </td>
          </tr>
        </tbody>
      </table>

      <section
        v-if="selected"
        :aria-label="`Version ${selected.version}`"
        class="mt-6 rounded border border-slate-400 p-4"
      >
        <h2 class="text-lg font-semibold">Version {{ selected.version }}</h2>
        <p v-if="effectivelyDeprecated(selected)" class="mt-2 text-amber-900">
          {{ TEXT.versionDeprecated }}
          <RouterLink
            v-if="activeLink(selected) !== undefined"
            :to="`/vocabularies/${name}/versions/${activeLink(selected)}`"
            class="text-blue-800 underline"
            >Latest ACTIVE version: {{ activeLink(selected) }}</RouterLink
          >
        </p>
        <dl class="mt-2 grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1">
          <dt class="font-semibold">Version</dt>
          <dd>{{ selected.version }}</dd>
          <dt class="font-semibold">Status</dt>
          <dd>{{ statusLabel(selected.status) }}</dd>
          <dt class="font-semibold">Date created</dt>
          <dd>{{ formatDate(selected.creationDate) }}</dd>
          <dt class="font-semibold">Creator</dt>
          <dd>{{ selected.createdBy }}</dd>
          <dt class="font-semibold">Description</dt>
          <dd>{{ selected.description }}</dd>
          <dt class="font-semibold">Change log</dt>
          <dd>{{ selected.changelog }}</dd>
        </dl>
        <button
          type="button"
          class="mt-3 rounded bg-blue-800 px-3 py-1 text-white hover:bg-blue-900"
          @click="download(selected.version)"
        >
          Download
        </button>
      </section>
    </template>
  </div>
</template>
