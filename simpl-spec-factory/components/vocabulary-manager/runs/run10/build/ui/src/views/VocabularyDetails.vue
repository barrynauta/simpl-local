<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import ActionsMenu from '../components/ActionsMenu.vue';
import WarningsPanel from '../components/WarningsPanel.vue';
import { ApiError, changeStatus, download, getVocabulary, listAllVersions } from '../api';
import type { Status, Vocabulary, VocabularyVersion } from '../api';
import { isAdmin } from '../auth';
import { takeNotice, type CreationNotice } from '../notice';
import { TEXT, createdText, formatDate, statusLabel } from '../texts';

/** Vocabulary details with the version history (D35, D36, D64, D72, D113, D114, D115). */
const props = defineProps<{ name: string }>();

const admin = isAdmin();
const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const selected = ref<number | null>(null);
const notFound = ref(false);
const loadError = ref('');
const notice = ref<CreationNotice | null>(null);
const message = ref('');
const actionError = ref('');
const panel = ref<HTMLElement | null>(null);

const latestActive = computed(() => {
  const active = versions.value
    .filter((version) => version.status === 'ACTIVE')
    .map((version) => version.version);
  return active.length === 0 ? null : Math.max(...active);
});

const selectedVersion = computed(
  () => versions.value.find((version) => version.version === selected.value) ?? null,
);

/** A version counts as deprecated when it or its vocabulary is DEPRECATED (D3). */
function effectivelyDeprecated(version: VocabularyVersion): boolean {
  return version.status === 'DEPRECATED' || vocabulary.value?.status === 'DEPRECATED';
}

/** The latest ACTIVE version to point to: never for a DEPRECATED vocabulary, never the version itself (D64). */
function recommended(version: VocabularyVersion): number | null {
  if (vocabulary.value?.status === 'DEPRECATED') return null;
  const latest = latestActive.value;
  return latest !== null && latest !== version.version ? latest : null;
}

async function load(): Promise<void> {
  notFound.value = false;
  loadError.value = '';
  try {
    vocabulary.value = await getVocabulary(props.name);
    versions.value = await listAllVersions(props.name);
  } catch (error) {
    vocabulary.value = null;
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true;
    } else {
      const detail = error instanceof ApiError ? (error.problem.detail ?? error.message) : String(error);
      loadError.value = `${TEXT.historyFailed} ${detail}`;
    }
  }
}

async function select(version: number): Promise<void> {
  selected.value = version;
  await nextTick();
  panel.value?.focus();
}

async function onVersionAction(version: VocabularyVersion, key: string): Promise<void> {
  if (key === 'view') {
    await select(version.version);
  } else {
    await downloadVersion(version.version);
  }
}

async function downloadVersion(version?: number): Promise<void> {
  actionError.value = '';
  try {
    await download(props.name, version);
  } catch (error) {
    actionError.value =
      error instanceof ApiError && error.status === 403
        ? TEXT.notAuthorized
        : `${TEXT.generic} ${error instanceof ApiError ? (error.problem.detail ?? '') : ''}`.trim();
  }
}

async function setStatus(status: Status): Promise<void> {
  message.value = '';
  actionError.value = '';
  try {
    vocabulary.value = await changeStatus(props.name, status);
    message.value = `Status of ${props.name} changed to ${statusLabel(status)}.`;
  } catch (error) {
    actionError.value =
      error instanceof ApiError && error.status === 404
        ? TEXT.statusNotFound
        : error instanceof ApiError && error.status === 403
          ? TEXT.notAuthorized
          : `${TEXT.statusFailed} ${TEXT.generic} ${error instanceof ApiError ? (error.problem.detail ?? '') : ''}`.trim();
  }
}

async function start(): Promise<void> {
  notice.value = takeNotice(props.name);
  selected.value = null;
  await load();
  if (notice.value && notice.value.version > 1) {
    await select(notice.value.version);
  }
}

onMounted(start);
watch(() => props.name, start);
</script>

<template>
  <section aria-labelledby="details-title">
    <RouterLink to="/vocabularies">{{ TEXT.backToList }}</RouterLink>

    <div v-if="notFound" role="alert" class="error">
      {{ TEXT.vocabularyNotFound }} <RouterLink to="/vocabularies">{{ TEXT.backToList }}</RouterLink>
    </div>
    <p v-if="loadError" role="alert" class="error">{{ loadError }}</p>

    <template v-if="vocabulary">
      <p v-if="notice" role="status" class="success">{{ createdText(notice.name, notice.version) }}</p>
      <WarningsPanel v-if="notice && notice.warnings.length > 0" :warnings="notice.warnings" />

      <div class="title-row">
        <h1 id="details-title">{{ vocabulary.name }}</h1>
        <div v-if="admin" class="actions">
          <RouterLink class="button" :to="`/vocabularies/${vocabulary.name}/versions/new`">Update</RouterLink>
          <button v-if="vocabulary.status === 'ACTIVE'" type="button" @click="setStatus('DEPRECATED')">
            Deprecate
          </button>
          <button v-else type="button" @click="setStatus('ACTIVE')">Activate</button>
        </div>
      </div>
      <p v-if="vocabulary.status === 'DEPRECATED'" role="status" class="banner">
        {{ TEXT.deprecatedVocabulary }}
      </p>
      <p v-if="message" role="status" class="success">{{ message }}</p>
      <p v-if="actionError" role="alert" class="error">{{ actionError }}</p>
      <dl class="summary">
        <dt>Vocabulary status</dt>
        <dd>{{ statusLabel(vocabulary.status) }}</dd>
        <dt>Description</dt>
        <dd>{{ vocabulary.description }}</dd>
        <dt>Latest version</dt>
        <dd>{{ vocabulary.version }}</dd>
      </dl>

      <h2>Version history</h2>
      <table>
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
              <button type="button" class="link" @click="select(version.version)">
                {{ version.version }}
              </button>
            </td>
            <td>
              {{ statusLabel(version.status) }}
              <span v-if="effectivelyDeprecated(version)" class="warning">
                {{ TEXT.deprecatedVersion }}
                <template v-if="recommended(version) !== null">
                  Latest ACTIVE version:
                  <a href="#" @click.prevent="select(recommended(version) as number)">{{
                    recommended(version)
                  }}</a>
                </template>
              </span>
            </td>
            <td>{{ formatDate(version.creationDate) }}</td>
            <td>{{ version.description }}</td>
            <td>{{ version.changelog }}</td>
            <td>
              <ActionsMenu
                :label="`Actions for version ${version.version}`"
                :actions="[
                  { key: 'view', label: 'View version details' },
                  { key: 'download', label: 'Download' },
                ]"
                @select="(key) => onVersionAction(version, key)"
              />
            </td>
          </tr>
        </tbody>
      </table>

      <section
        v-if="selectedVersion"
        ref="panel"
        class="panel"
        tabindex="-1"
        :aria-label="`Version ${selectedVersion.version}`"
      >
        <h2>Version {{ selectedVersion.version }}</h2>
        <p v-if="effectivelyDeprecated(selectedVersion)" class="warning">
          {{ TEXT.deprecatedVersion }}
          <template v-if="recommended(selectedVersion) !== null">
            Latest ACTIVE version:
            <a href="#" @click.prevent="select(recommended(selectedVersion) as number)">{{
              recommended(selectedVersion)
            }}</a>
          </template>
        </p>
        <dl>
          <dt>Version</dt>
          <dd>{{ selectedVersion.version }}</dd>
          <dt>Status</dt>
          <dd>{{ statusLabel(selectedVersion.status) }}</dd>
          <dt>Date created</dt>
          <dd>{{ formatDate(selectedVersion.creationDate) }}</dd>
          <dt>Creator</dt>
          <dd>{{ selectedVersion.createdBy }}</dd>
          <dt>Description</dt>
          <dd>{{ selectedVersion.description }}</dd>
          <dt>Change log</dt>
          <dd>{{ selectedVersion.changelog }}</dd>
        </dl>
        <button type="button" @click="downloadVersion(selectedVersion.version)">Download</button>
      </section>
    </template>
  </section>
</template>
