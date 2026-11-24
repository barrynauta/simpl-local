<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import ActionsMenu from '../components/ActionsMenu.vue';
import ConfirmDialog from '../components/ConfirmDialog.vue';
import WarningsPanel from '../components/WarningsPanel.vue';
import {
  ApiError,
  api,
  saveBlob,
  type ValidationWarning,
  type Vocabulary,
  type VocabularyVersion,
} from '../lib/api';
import { isAdmin } from '../lib/auth';
import { takeNotice } from '../lib/notices';
import { TEXT, formatDate, problemDetail, statusLabel } from '../lib/texts';

const route = useRoute();
const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const notFound = ref(false);
const loadError = ref('');
const actionError = ref('');
const success = ref('');
const warnings = ref<ValidationWarning[]>([]);
const confirming = ref(false);

const name = computed(() => String(route.params.name));
const selectedNumber = computed(() => (route.params.version ? Number(route.params.version) : null));
const selected = computed(() => versions.value.find((v) => v.version === selectedNumber.value) ?? null);
const admin = computed(() => isAdmin());
const vocabularyDeprecated = computed(() => vocabulary.value?.status === 'DEPRECATED');
const latestActive = computed(() => {
  const active = versions.value.filter((v) => v.status === 'ACTIVE').map((v) => v.version);
  return active.length > 0 ? Math.max(...active) : null;
});

function effectivelyDeprecated(version: VocabularyVersion): boolean {
  return version.status === 'DEPRECATED' || vocabularyDeprecated.value;
}

/** The 'Latest ACTIVE version' link target of D36, D64 and D118, or null. */
function activeLinkFor(version: VocabularyVersion): number | null {
  if (vocabularyDeprecated.value || latestActive.value === null || latestActive.value === version.version) {
    return null;
  }
  return latestActive.value;
}

async function load(): Promise<void> {
  notFound.value = false;
  loadError.value = '';
  try {
    vocabulary.value = await api.getVocabulary(name.value);
    const all: VocabularyVersion[] = [];
    let page = 1;
    let totalPages = 1;
    do {
      const result = await api.listVersions(name.value, page);
      all.push(...result.items);
      totalPages = result.totalPages;
      page += 1;
    } while (page <= totalPages);
    versions.value = all;
  } catch (error) {
    vocabulary.value = null;
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true;
    } else {
      loadError.value = `${TEXT.historyLoadError} ${problemDetail(error)}`.trim();
    }
  }
}

async function download(version?: number): Promise<void> {
  actionError.value = '';
  try {
    const file = version === undefined ? `${name.value}.ttl` : `${name.value}-v${version}.ttl`;
    saveBlob(await api.content(name.value, version), file);
  } catch (error) {
    actionError.value = error instanceof ApiError && error.status === 403 ? TEXT.notAuthorized : TEXT.generic;
  }
}

const dialogText = computed(() =>
  vocabulary.value?.status === 'ACTIVE'
    ? `Do you want to deprecate '${name.value}'?`
    : `Do you want to activate '${name.value}'?`,
);

async function confirmStatus(): Promise<void> {
  confirming.value = false;
  const current = vocabulary.value;
  if (!current) {
    return;
  }
  success.value = '';
  actionError.value = '';
  const next = current.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  try {
    await api.changeStatus(current.name, next);
    success.value = `Status of ${current.name} changed to ${statusLabel(next)}.`;
    await load();
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      actionError.value = TEXT.statusNotFound;
    } else if (error instanceof ApiError && error.status === 403) {
      actionError.value = TEXT.notAuthorized;
    } else if (error instanceof ApiError && error.status < 500) {
      actionError.value = error.problem.detail ?? TEXT.statusFailed;
    } else {
      actionError.value = `${TEXT.statusFailed} ${TEXT.generic} ${problemDetail(error)}`.trim();
    }
  }
}

function showNotice(): void {
  const value = takeNotice();
  if (value.success) {
    success.value = value.success;
    warnings.value = value.warnings ?? [];
  }
}

watch(name, () => void load());
onMounted(() => {
  showNotice();
  void load();
});
</script>

<template>
  <div>
    <RouterLink to="/vocabularies">{{ TEXT.backToList }}</RouterLink>

    <p v-if="success" role="status" class="alert-success">{{ success }}</p>
    <WarningsPanel :warnings="warnings" />
    <p v-if="actionError" role="alert" class="alert-error">{{ actionError }}</p>

    <div v-if="notFound" role="alert" class="alert-error">{{ TEXT.vocabularyNotFound }}</div>
    <p v-else-if="loadError" role="alert" class="alert-error">{{ loadError }}</p>

    <template v-if="vocabulary">
      <div class="mt-3 flex flex-wrap items-center justify-between gap-2">
        <h1 class="text-2xl font-semibold">{{ vocabulary.name }}</h1>
        <div class="flex gap-2">
          <button type="button" class="btn-secondary" @click="download()">Download vocabulary</button>
          <template v-if="admin">
            <RouterLink :to="`/vocabularies/${vocabulary.name}/versions/new`" class="btn no-underline"
              >Update</RouterLink
            >
            <button type="button" class="btn-secondary" @click="confirming = true">
              {{ vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate' }}
            </button>
          </template>
        </div>
      </div>
      <p class="mt-1 text-sm">Status: {{ statusLabel(vocabulary.status) }}</p>
      <p class="mt-1">{{ vocabulary.description }}</p>
      <p v-if="vocabularyDeprecated" role="status" class="alert-warning">{{ TEXT.vocabularyDeprecated }}</p>

      <h2 class="mt-6 text-xl font-semibold">Version history</h2>
      <table class="data mt-2">
        <caption class="sr-only">
          Version history of
          {{
            vocabulary.name
          }}
        </caption>
        <thead>
          <tr>
            <th scope="col">Version</th>
            <th scope="col">Status</th>
            <th scope="col">Date created</th>
            <th scope="col">Description</th>
            <th scope="col">Change log</th>
            <th scope="col">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="version in versions"
            :key="version.version"
            :aria-current="version.version === selectedNumber ? 'true' : undefined"
            :class="version.version === selectedNumber ? 'bg-blue-50' : ''"
          >
            <td>
              <RouterLink :to="`/vocabularies/${vocabulary.name}/versions/${version.version}`">{{
                version.version
              }}</RouterLink>
            </td>
            <td>
              {{ statusLabel(version.status) }}
              <div v-if="effectivelyDeprecated(version)" class="mt-1 text-amber-900">
                <span>{{ TEXT.versionDeprecated }}</span>
                <template v-if="activeLinkFor(version) !== null">
                  <RouterLink
                    :to="`/vocabularies/${vocabulary.name}/versions/${activeLinkFor(version)}`"
                    class="ml-1"
                    >Latest ACTIVE version: {{ activeLinkFor(version) }}</RouterLink
                  >
                </template>
              </div>
            </td>
            <td>{{ formatDate(version.creationDate) }}</td>
            <td>{{ version.description }}</td>
            <td>{{ version.changelog }}</td>
            <td>
              <ActionsMenu
                :label="`Actions for version ${version.version}`"
                :items="[{ label: 'Download', action: () => void download(version.version) }]"
              />
            </td>
          </tr>
        </tbody>
      </table>

      <section
        v-if="selected"
        :aria-label="`Version ${selected.version}`"
        class="mt-6 rounded border border-gray-400 p-4"
      >
        <h2 class="text-lg font-semibold">Version {{ selected.version }}</h2>
        <div v-if="effectivelyDeprecated(selected)" class="alert-warning">
          <span>{{ TEXT.versionDeprecated }}</span>
          <template v-if="activeLinkFor(selected) !== null">
            <RouterLink
              :to="`/vocabularies/${vocabulary.name}/versions/${activeLinkFor(selected)}`"
              class="ml-1"
              >Latest ACTIVE version: {{ activeLinkFor(selected) }}</RouterLink
            >
          </template>
        </div>
        <dl class="mt-2 grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1 text-sm">
          <dt class="font-medium">Version</dt>
          <dd>{{ selected.version }}</dd>
          <dt class="font-medium">Status</dt>
          <dd>{{ statusLabel(selected.status) }}</dd>
          <dt class="font-medium">Date created</dt>
          <dd>{{ formatDate(selected.creationDate) }}</dd>
          <dt class="font-medium">Creator</dt>
          <dd>{{ selected.createdBy }}</dd>
          <dt class="font-medium">Description</dt>
          <dd>{{ selected.description }}</dd>
          <dt class="font-medium">Change log</dt>
          <dd>{{ selected.changelog }}</dd>
        </dl>
        <button type="button" class="btn-secondary mt-3" @click="download(selected.version)">Download</button>
      </section>
    </template>

    <ConfirmDialog
      :visible="confirming"
      title="Update vocabulary status"
      :text="dialogText"
      @confirm="confirmStatus"
      @cancel="confirming = false"
    />
  </div>
</template>
