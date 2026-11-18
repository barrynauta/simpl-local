<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  ApiError,
  download,
  getAllVersions,
  getVocabulary,
  setVocabularyStatus,
  type ValidationWarning,
  type Vocabulary,
  type VocabularyVersion,
} from '../lib/api';
import { isAdmin } from '../lib/auth';
import { errorLines } from '../lib/errors';
import { takeFlash } from '../lib/flash';
import { formatDate } from '../lib/format';
import {
  HISTORY_NOT_LOADED,
  STATUS_NOT_UPDATED,
  VERSION_DEPRECATED,
  VOCABULARY_DEPRECATED,
  VOCABULARY_NOT_FOUND,
  statusChanged,
  statusLabel,
} from '../lib/messages';

/** Vocabulary details with the version history (SIMPL-18167, SIMPL-18176, SIMPL-18452, SIMPL-18559; D35, D36). */
const props = defineProps<{ name: string }>();
const route = useRoute();
const router = useRouter();
const admin = isAdmin();

const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const notFound = ref(false);
const loadError = ref<string[] | null>(null);
const notice = ref<{ kind: 'success' | 'error'; lines: string[] } | null>(null);
const warnings = ref<ValidationWarning[]>([]);

const flash = takeFlash();
if (flash) {
  notice.value = { kind: 'success', lines: [flash.message] };
  warnings.value = flash.warnings;
}

const selectedVersion = computed<VocabularyVersion | null>(() => {
  const wanted = Number(route.query.version);
  return versions.value.find((v) => v.version === wanted) ?? null;
});

/** Latest ACTIVE version: the highest-numbered version with version status ACTIVE (D5). */
const latestActive = computed<number | null>(() => {
  const active = versions.value.filter((v) => v.status === 'ACTIVE').map((v) => v.version);
  return active.length ? Math.max(...active) : null;
});

function effectivelyDeprecated(v: VocabularyVersion): boolean {
  return v.status === 'DEPRECATED' || vocabulary.value?.status === 'DEPRECATED';
}

function recommended(v: VocabularyVersion): number | null {
  return latestActive.value !== null && latestActive.value !== v.version ? latestActive.value : null;
}

async function load(): Promise<void> {
  notFound.value = false;
  loadError.value = null;
  try {
    vocabulary.value = await getVocabulary(props.name);
    versions.value = await getAllVersions(props.name);
  } catch (error) {
    vocabulary.value = null;
    versions.value = [];
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true;
    } else {
      loadError.value = errorLines(error, HISTORY_NOT_LOADED);
    }
  }
}

function select(version: number): void {
  void router.replace({ query: { version: String(version) } });
}

async function downloadVersion(version?: number): Promise<void> {
  try {
    await download(props.name, version);
  } catch (error) {
    notice.value = { kind: 'error', lines: errorLines(error, 'The vocabulary could not be downloaded.') };
  }
}

async function changeStatus(): Promise<void> {
  if (!vocabulary.value) return;
  notice.value = null;
  const target = vocabulary.value.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  try {
    vocabulary.value = await setVocabularyStatus(props.name, target);
    notice.value = { kind: 'success', lines: [statusChanged(props.name, vocabulary.value.status)] };
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notice.value = {
        kind: 'error',
        lines: [
          VOCABULARY_NOT_FOUND,
          `The vocabulary ${props.name} could not be found. No status change was applied.`,
        ],
      };
    } else if (error instanceof ApiError && error.status === 403) {
      notice.value = { kind: 'error', lines: errorLines(error) };
    } else {
      notice.value = { kind: 'error', lines: errorLines(error, STATUS_NOT_UPDATED) };
    }
  }
}

watch(() => props.name, load);
onMounted(load);
</script>

<template>
  <section>
    <RouterLink to="/vocabularies">Back to vocabulary list</RouterLink>

    <div v-if="notice" :class="['message', notice.kind]" role="status">
      <p v-for="line in notice.lines" :key="line">{{ line }}</p>
    </div>
    <div v-if="warnings.length" class="message warning" role="region" aria-label="Validation warnings">
      <p><strong>Validation warnings</strong></p>
      <ul>
        <li v-for="(w, i) in warnings" :key="i">
          {{ w.description }} <span v-if="w.location">({{ w.location }})</span>
        </li>
      </ul>
    </div>

    <div v-if="notFound" class="message error" role="alert">
      <p>{{ VOCABULARY_NOT_FOUND }}</p>
      <p><RouterLink to="/vocabularies">Back to vocabulary list</RouterLink></p>
    </div>
    <div v-else-if="loadError" class="message error" role="alert">
      <p v-for="line in loadError" :key="line">{{ line }}</p>
    </div>

    <template v-else-if="vocabulary">
      <h1>{{ vocabulary.name }}</h1>
      <p>
        Status: <strong class="vocabulary-status">{{ statusLabel(vocabulary.status) }}</strong>
      </p>
      <div v-if="vocabulary.status === 'DEPRECATED'" class="message warning" role="alert">
        <p>&#9888; {{ VOCABULARY_DEPRECATED }}</p>
      </div>
      <p class="vocabulary-description">{{ vocabulary.description }}</p>
      <div class="header-actions">
        <button type="button" @click="downloadVersion()">Download vocabulary</button>
        <button v-if="admin" type="button" @click="router.push(`/vocabularies/${vocabulary.name}/update`)">
          Update
        </button>
        <button v-if="admin" type="button" @click="changeStatus()">
          {{ vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate' }}
        </button>
      </div>

      <h2>Version history</h2>
      <table aria-label="Version history">
        <thead>
          <tr>
            <th>Version</th>
            <th>Status</th>
            <th>Date created</th>
            <th>Description</th>
            <th>Change log</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="v in versions"
            :key="v.version"
            :data-version="v.version"
            :class="{ selected: selectedVersion?.version === v.version }"
          >
            <td>
              <button type="button" class="link" @click="select(v.version)">{{ v.version }}</button>
            </td>
            <td>
              <span class="status">{{ statusLabel(v.status) }}</span>
              <span v-if="effectivelyDeprecated(v)" class="deprecation">
                &#9888; {{ VERSION_DEPRECATED }}
                <template v-if="recommended(v) !== null">
                  <a href="#" @click.prevent="select(recommended(v)!)"
                    >Latest ACTIVE version: {{ recommended(v) }}</a
                  >
                </template>
              </span>
            </td>
            <td>{{ formatDate(v.creationDate) }}</td>
            <td>{{ v.description }}</td>
            <td>{{ v.changelog }}</td>
            <td>
              <button type="button" @click="downloadVersion(v.version)">Download</button>
              <button type="button" @click="select(v.version)">View</button>
            </td>
          </tr>
        </tbody>
      </table>

      <section v-if="selectedVersion" class="panel" aria-label="Version details">
        <h3>Version {{ selectedVersion.version }}</h3>
        <div v-if="effectivelyDeprecated(selectedVersion)" class="message warning">
          <p>
            {{ VERSION_DEPRECATED }}
            <template v-if="recommended(selectedVersion) !== null">
              <a href="#" @click.prevent="select(recommended(selectedVersion)!)"
                >Latest ACTIVE version: {{ recommended(selectedVersion) }}</a
              >
            </template>
          </p>
        </div>
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
