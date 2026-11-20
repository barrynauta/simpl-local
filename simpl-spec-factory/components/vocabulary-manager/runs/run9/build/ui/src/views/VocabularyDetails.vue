<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api, ApiError, saveFile, type Vocabulary, type VocabularyVersion } from '../api';
import { session } from '../auth';
import ActionsMenu, { type MenuAction } from '../components/ActionsMenu.vue';
import WarningsPanel from '../components/WarningsPanel.vue';
import { clearNotice, notice } from '../notice';
import { formatDate, statusLabel, TEXT } from '../texts';

/** Vocabulary details with the version history (D35, D36, D64, D72). */
const props = defineProps<{ name: string }>();
const router = useRouter();
const admin = session().isAdmin();

const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const selected = ref<number | null>(null);
const notFound = ref(false);
const loadError = ref('');
const message = ref('');
const created = ref({ message: notice.message, warnings: [...notice.warnings] });
const actionError = ref<string[]>([]);
clearNotice();

const selectedVersion = computed(() => versions.value.find((v) => v.version === selected.value) ?? null);
const latestActive = computed(() =>
  versions.value.filter((v) => v.status === 'ACTIVE').reduce((max, v) => Math.max(max, v.version), 0),
);

function effectivelyDeprecated(version: VocabularyVersion): boolean {
  return version.status === 'DEPRECATED' || vocabulary.value?.status === 'DEPRECATED';
}

/** D64: no link for a DEPRECATED vocabulary, never a link to the version itself. */
function recommended(version: VocabularyVersion): number | null {
  if (vocabulary.value?.status === 'DEPRECATED' || latestActive.value === 0) {
    return null;
  }
  return latestActive.value === version.version ? null : latestActive.value;
}

async function load(): Promise<void> {
  notFound.value = false;
  loadError.value = '';
  try {
    vocabulary.value = await api.getVocabulary(props.name);
    versions.value = await api.listAllVersions(props.name);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true;
    } else {
      loadError.value = error instanceof ApiError ? (error.problem.detail ?? '') : String(error);
    }
  }
}

async function download(version?: number): Promise<void> {
  try {
    const name = props.name;
    saveFile(await api.content(name, version), version ? `${name}-v${version}.ttl` : `${name}.ttl`);
  } catch (error) {
    actionError.value = [TEXT.genericError, error instanceof ApiError ? (error.problem.detail ?? '') : ''];
  }
}

async function changeStatus(): Promise<void> {
  if (!vocabulary.value) {
    return;
  }
  message.value = '';
  actionError.value = [];
  const next = vocabulary.value.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  try {
    vocabulary.value = await api.changeStatus(props.name, next);
    message.value = `Status of ${props.name} changed to ${statusLabel(next)}.`;
  } catch (error) {
    const problem = error instanceof ApiError ? error.problem : { status: 500, detail: String(error) };
    if (problem.status === 404) {
      actionError.value = [TEXT.statusNotFound];
    } else if (problem.status === 403) {
      actionError.value = [TEXT.notAuthorized];
    } else {
      actionError.value = [TEXT.statusError, TEXT.genericError, problem.detail ?? ''];
    }
  }
}

function versionActions(version: VocabularyVersion): MenuAction[] {
  return [
    { label: 'View version', run: () => (selected.value = version.version) },
    { label: 'Download', run: () => void download(version.version) },
  ];
}

onMounted(load);
defineExpose({ load, selected });
</script>

<template>
  <p>
    <RouterLink to="/vocabularies">{{ TEXT.backToList }}</RouterLink>
  </p>
  <div v-if="notFound" role="alert" class="panel error">
    <p>{{ TEXT.notFound }}</p>
  </div>
  <div v-else-if="loadError" role="alert" class="panel error">
    <p>{{ TEXT.historyError }}</p>
    <p>{{ loadError }}</p>
  </div>
  <template v-else-if="vocabulary">
    <p v-if="created.message" role="status" class="panel success">{{ created.message }}</p>
    <WarningsPanel :warnings="created.warnings" />
    <p v-if="message" role="status" class="panel success">{{ message }}</p>
    <div v-if="actionError.length" role="alert" class="panel error">
      <p v-for="(line, index) in actionError.filter((l) => l)" :key="index">{{ line }}</p>
    </div>

    <h1>{{ vocabulary.name }}</h1>
    <p v-if="vocabulary.status === 'DEPRECATED'" class="panel warning">{{ TEXT.deprecatedVocabulary }}</p>
    <dl>
      <dt>Vocabulary status</dt>
      <dd>{{ statusLabel(vocabulary.status) }}</dd>
      <dt>Description</dt>
      <dd>{{ vocabulary.description }}</dd>
      <dt>Latest version</dt>
      <dd>{{ vocabulary.version }}</dd>
    </dl>
    <div class="actions">
      <button type="button" class="secondary" @click="download()">Download vocabulary</button>
      <template v-if="admin">
        <button type="button" @click="router.push(`/vocabularies/${vocabulary.name}/versions/new`)">
          Update
        </button>
        <button type="button" class="secondary" @click="changeStatus">
          {{ vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate' }}
        </button>
      </template>
    </div>

    <h2 id="history">Version history</h2>
    <table aria-label="Version history">
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
            <button type="button" class="secondary" @click="selected = version.version">
              {{ version.version }}
            </button>
          </td>
          <td>
            {{ statusLabel(version.status) }}
            <template v-if="effectivelyDeprecated(version)">
              <br />{{ TEXT.deprecatedVersion }}
              <template v-if="recommended(version)">
                <br /><a href="#" @click.prevent="selected = recommended(version)"
                  >Latest ACTIVE version: {{ recommended(version) }}</a
                >
              </template>
            </template>
          </td>
          <td>{{ formatDate(version.creationDate) }}</td>
          <td>{{ version.description }}</td>
          <td>{{ version.changelog }}</td>
          <td>
            <ActionsMenu
              :label="`Actions for version ${version.version}`"
              :actions="versionActions(version)"
            />
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="selectedVersion" :aria-label="`Version ${selectedVersion.version}`" class="panel">
      <h3>Version {{ selectedVersion.version }}</h3>
      <p v-if="effectivelyDeprecated(selectedVersion)">
        {{ TEXT.deprecatedVersion }}
        <template v-if="recommended(selectedVersion)">
          <a href="#" @click.prevent="selected = recommended(selectedVersion)"
            >Latest ACTIVE version: {{ recommended(selectedVersion) }}</a
          >
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
      <div class="actions">
        <button type="button" class="secondary" @click="download(selectedVersion.version)">Download</button>
        <button type="button" class="secondary" @click="selected = null">Close</button>
      </div>
    </section>
  </template>
</template>
