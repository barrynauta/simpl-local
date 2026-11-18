<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { download, getVocabulary, listAllVersions } from '../api';
import { isAdmin } from '../auth';
import { describeError, statusOf } from '../errors';
import { notifyError } from '../notifications';
import {
  isEffectivelyDeprecated,
  latestActiveVersion,
  statusLabel,
  VERSION_DEPRECATED,
  VOCABULARY_DEPRECATED,
} from '../labels';
import { changeStatus, statusActionLabel } from '../statusChange';
import type { Vocabulary, VocabularyVersion } from '../types';

const props = defineProps<{ name: string }>();
const route = useRoute();
const router = useRouter();
const admin = isAdmin();
const vocabulary = ref<Vocabulary | undefined>();
const versions = ref<VocabularyVersion[]>([]);
const notFound = ref(false);
const loadError = ref<{ message: string; detail?: string } | undefined>();
const selectedNumber = ref<number | undefined>(route.query.version ? Number(route.query.version) : undefined);

const latestActive = computed(() => latestActiveVersion(versions.value));
const selected = computed(() => versions.value.find((v) => v.version === selectedNumber.value));

async function load(): Promise<void> {
  try {
    vocabulary.value = await getVocabulary(props.name);
    versions.value = await listAllVersions(props.name);
  } catch (error) {
    if (statusOf(error) === 404) {
      notFound.value = true;
    } else {
      const shown = describeError(error, 'The version history could not be loaded.');
      loadError.value = {
        message: 'The version history could not be loaded.',
        detail:
          shown.message === 'The version history could not be loaded.'
            ? shown.detail
            : (shown.detail ?? shown.message),
      };
    }
  }
}

function deprecated(version: VocabularyVersion): boolean {
  return vocabulary.value !== undefined && isEffectivelyDeprecated(version, vocabulary.value.status);
}

function select(version: number): void {
  selectedNumber.value = version;
  void router.replace({ query: { version: String(version) } });
}

async function downloadVersion(version: number): Promise<void> {
  try {
    await download(props.name, version);
  } catch (error) {
    const shown = describeError(error, 'The download failed.');
    notifyError(shown.message, shown.detail);
  }
}

async function onChangeStatus(): Promise<void> {
  if (!vocabulary.value) return;
  const updated = await changeStatus(vocabulary.value);
  if (updated) vocabulary.value = updated;
}

onMounted(load);
</script>

<template>
  <section class="vocabulary-details">
    <div v-if="notFound" class="error-panel" role="alert">
      <p>Vocabulary not found.</p>
      <router-link to="/vocabularies">Back to the vocabulary list</router-link>
    </div>
    <div v-else-if="loadError" class="error-panel" role="alert">
      <p>{{ loadError.message }}</p>
      <p v-if="loadError.detail" class="detail">{{ loadError.detail }}</p>
    </div>
    <template v-else-if="vocabulary">
      <header class="details-header">
        <div>
          <h1>{{ vocabulary.name }}</h1>
          <p>
            Status:
            <span class="status" :class="vocabulary.status.toLowerCase()">{{
              statusLabel(vocabulary.status)
            }}</span>
          </p>
          <p class="description">{{ vocabulary.description }}</p>
        </div>
        <div class="header-actions">
          <router-link to="/vocabularies" class="button">Back to list</router-link>
          <button type="button" @click="downloadVersion(vocabulary.version)">Download vocabulary</button>
          <template v-if="admin">
            <router-link :to="`/vocabularies/${vocabulary.name}/update`" class="button">Update</router-link>
            <button type="button" @click="onChangeStatus">{{ statusActionLabel(vocabulary) }}</button>
          </template>
        </div>
      </header>

      <p v-if="vocabulary.status === 'DEPRECATED'" class="banner warning" role="alert">
        <span aria-hidden="true">&#9888;</span> {{ VOCABULARY_DEPRECATED }}
      </p>

      <h2>Version history</h2>
      <table class="data-table">
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
            v-for="v in versions"
            :key="v.version"
            :class="{ selected: v.version === selectedNumber }"
            :data-version="v.version"
          >
            <td>
              <button
                type="button"
                class="link-button"
                :aria-label="`View version ${v.version}`"
                @click="select(v.version)"
              >
                {{ v.version }}
              </button>
            </td>
            <td>
              <span class="status" :class="v.status.toLowerCase()">{{ statusLabel(v.status) }}</span>
              <div v-if="deprecated(v)" class="deprecation-warning">
                <span aria-hidden="true">&#9888;</span> {{ VERSION_DEPRECATED }}
                <span v-if="latestActive !== undefined">
                  <a href="#" @click.prevent="select(latestActive)"
                    >Latest ACTIVE version: {{ latestActive }}</a
                  >
                </span>
              </div>
            </td>
            <td>{{ v.creationDate }}</td>
            <td>{{ v.description }}</td>
            <td>{{ v.changelog }}</td>
            <td><button type="button" @click="downloadVersion(v.version)">Download</button></td>
          </tr>
        </tbody>
      </table>

      <aside v-if="selected" class="version-panel" aria-label="Version details">
        <h2>Version {{ selected.version }}</h2>
        <div v-if="deprecated(selected)" class="deprecation-warning" role="note">
          <span aria-hidden="true">&#9888;</span> {{ VERSION_DEPRECATED }}
          <span v-if="latestActive !== undefined">
            <a href="#" @click.prevent="select(latestActive)">Latest ACTIVE version: {{ latestActive }}</a>
          </span>
        </div>
        <dl>
          <dt>Version</dt>
          <dd>{{ selected.version }}</dd>
          <dt>Status</dt>
          <dd>{{ statusLabel(selected.status) }}</dd>
          <dt>Date created</dt>
          <dd>{{ selected.creationDate }}</dd>
          <dt>Creator</dt>
          <dd>{{ selected.createdBy }}</dd>
          <dt>Description</dt>
          <dd>{{ selected.description }}</dd>
          <dt>Change log</dt>
          <dd>{{ selected.changelog }}</dd>
        </dl>
        <button type="button" class="primary" @click="downloadVersion(selected.version)">Download</button>
      </aside>
    </template>
    <p v-else class="hint">Loading...</p>
  </section>
</template>
