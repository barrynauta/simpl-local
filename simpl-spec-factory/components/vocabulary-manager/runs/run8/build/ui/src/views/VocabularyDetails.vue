<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ApiError, saveBlob, type ValidationWarning, type Vocabulary, type VocabularyVersion } from '../api';
import ActionsMenu, { type MenuItem } from '../components/ActionsMenu.vue';
import ProblemMessage from '../components/ProblemMessage.vue';
import WarningsPanel from '../components/WarningsPanel.vue';
import { useApp } from '../context';
import { formatDate, statusLabel } from '../format';
import { TEXT, statusChanged } from '../texts';

/**
 * Vocabulary details with the version history (route /vocabularies/<Name>): SIMPL-18167, 18176, 18452, 18559;
 * D35, D36, D64, D72.
 */
const app = useApp();
const route = useRoute();
const router = useRouter();

const name = computed(() => String(route.params.name));
const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const notFound = ref(false);
const loadError = ref<string | null>(null);
const notice = ref<string | null>(null);
const warnings = ref<ValidationWarning[]>([]);
const success = ref<string | null>(null);
const failure = ref<{ text: string; detail?: string | null } | null>(null);

const selectedNumber = computed(() => {
  const value = Number(route.query.version);
  return Number.isInteger(value) && value > 0 ? value : null;
});
const selected = computed(() => versions.value.find((version) => version.version === selectedNumber.value) ?? null);

/** Latest ACTIVE version (D5): highest-numbered version with status ACTIVE. */
const latestActive = computed(() =>
  versions.value
    .filter((version) => version.status === 'ACTIVE')
    .reduce<number | null>((max, version) => (max === null || version.version > max ? version.version : max), null),
);

function effectivelyDeprecated(version: VocabularyVersion): boolean {
  return version.status === 'DEPRECATED' || vocabulary.value?.status === 'DEPRECATED';
}

/** D36, D47, D64: no link for a DEPRECATED vocabulary, never to the version itself. */
function activeLink(version: VocabularyVersion): number | null {
  if (vocabulary.value?.status !== 'ACTIVE' || latestActive.value === null) {
    return null;
  }
  return latestActive.value === version.version ? null : latestActive.value;
}

async function load() {
  notFound.value = false;
  loadError.value = null;
  try {
    const [loadedVocabulary, loadedVersions] = await Promise.all([
      app.api.getVocabulary(name.value),
      app.api.allVersions(name.value),
    ]);
    vocabulary.value = loadedVocabulary;
    versions.value = loadedVersions;
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true;
    } else {
      loadError.value = error instanceof ApiError ? (error.problem.detail ?? '') : '';
    }
  }
}

function select(version: number) {
  void router.replace({ path: `/vocabularies/${name.value}`, query: { version: String(version) } });
}

async function download(version: VocabularyVersion) {
  failure.value = null;
  try {
    saveBlob(await app.api.content(version.name, version.version), `${version.name}-v${version.version}.ttl`);
  } catch (error) {
    failure.value = { text: TEXT.genericError, detail: error instanceof ApiError ? error.problem.detail : null };
  }
}

async function changeStatus() {
  if (!vocabulary.value) {
    return;
  }
  success.value = null;
  failure.value = null;
  const target = vocabulary.value.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  try {
    vocabulary.value = await app.api.changeStatus(vocabulary.value.name, target);
    success.value = statusChanged(vocabulary.value.name, vocabulary.value.status);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      failure.value = { text: TEXT.statusNotFound };
    } else if (error instanceof ApiError && error.status === 403) {
      failure.value = { text: TEXT.notAuthorized };
    } else {
      const detail = error instanceof ApiError ? error.problem.detail : null;
      failure.value = { text: `${TEXT.statusFailed} ${TEXT.genericError}`, detail };
    }
  }
}

function versionActions(version: VocabularyVersion): MenuItem[] {
  return [
    { label: 'View version details', run: () => select(version.version) },
    { label: 'Download', run: () => void download(version) },
  ];
}

onMounted(() => {
  if (app.notice) {
    notice.value = app.notice.message;
    warnings.value = app.notice.warnings;
    app.notice = null;
  }
  void load();
});
watch(name, () => void load());
</script>

<template>
  <section aria-labelledby="details-title">
    <RouterLink to="/vocabularies">{{ TEXT.backToList }}</RouterLink>

    <div role="status" class="message success" :class="{ hidden: !notice && !success }">
      {{ notice ?? success }}
    </div>
    <WarningsPanel :warnings="warnings" />
    <ProblemMessage v-if="failure" :text="failure.text" :detail="failure.detail" />

    <template v-if="notFound">
      <h1 id="details-title">{{ name }}</h1>
      <ProblemMessage :text="TEXT.vocabularyNotFound" />
    </template>
    <template v-else-if="loadError !== null">
      <h1 id="details-title">{{ name }}</h1>
      <ProblemMessage :text="TEXT.historyLoadError" :detail="loadError" />
    </template>
    <template v-else-if="vocabulary">
      <div class="title-row">
        <h1 id="details-title">{{ vocabulary.name }}</h1>
        <div v-if="app.isAdmin()" class="actions">
          <RouterLink class="button" :to="`/vocabularies/${vocabulary.name}/versions/new`">Update</RouterLink>
          <button type="button" @click="changeStatus">
            {{ vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate' }}
          </button>
        </div>
      </div>
      <div v-if="vocabulary.status === 'DEPRECATED'" class="banner" role="note">
        {{ TEXT.vocabularyDeprecated }}
      </div>
      <dl class="summary">
        <dt>Vocabulary status</dt>
        <dd>{{ statusLabel(vocabulary.status) }}</dd>
        <dt>Description</dt>
        <dd>{{ vocabulary.description }}</dd>
      </dl>

      <h2 id="history-title">Version history</h2>
      <table class="table" aria-labelledby="history-title">
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
          <tr v-for="version in versions" :key="version.version">
            <td>
              <button type="button" class="link" @click="select(version.version)">{{ version.version }}</button>
            </td>
            <td>
              {{ statusLabel(version.status) }}
              <div v-if="effectivelyDeprecated(version)" class="inline-warning">
                <span>{{ TEXT.versionDeprecated }}</span>
                <template v-if="activeLink(version) !== null">
                  <RouterLink :to="`/vocabularies/${version.name}?version=${activeLink(version)}`">
                    Latest ACTIVE version: {{ activeLink(version) }}
                  </RouterLink>
                </template>
              </div>
            </td>
            <td>{{ formatDate(version.creationDate) }}</td>
            <td>{{ version.description }}</td>
            <td>{{ version.changelog }}</td>
            <td>
              <ActionsMenu :label="`Actions for version ${version.version}`" :items="versionActions(version)" />
            </td>
          </tr>
        </tbody>
      </table>

      <section v-if="selected" class="panel" :aria-label="`Version ${selected.version}`">
        <h2>Version {{ selected.version }}</h2>
        <div v-if="effectivelyDeprecated(selected)" class="inline-warning">
          <span>{{ TEXT.versionDeprecated }}</span>
          <template v-if="activeLink(selected) !== null">
            <RouterLink :to="`/vocabularies/${selected.name}?version=${activeLink(selected)}`">
              Latest ACTIVE version: {{ activeLink(selected) }}
            </RouterLink>
          </template>
        </div>
        <dl class="summary">
          <dt>Version</dt>
          <dd>{{ selected.version }}</dd>
          <dt>Status</dt>
          <dd>{{ statusLabel(selected.status) }}</dd>
          <dt>Date created</dt>
          <dd>{{ formatDate(selected.creationDate) }}</dd>
          <dt>Creator</dt>
          <dd>{{ selected.createdBy }}</dd>
          <dt>Description</dt>
          <dd>{{ selected.description }}</dd>
          <dt>Change log</dt>
          <dd>{{ selected.changelog }}</dd>
        </dl>
        <button type="button" @click="download(selected)">Download</button>
      </section>
    </template>
  </section>
</template>
