<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  ApiError,
  download,
  getAllVersions,
  getVocabulary,
  type Vocabulary,
  type VocabularyVersion,
} from "../api";
import ActionsMenu from "../components/ActionsMenu.vue";
import ValidationWarnings from "../components/ValidationWarnings.vue";
import { takeFlash } from "../flash";
import { TEXT, formatDate, statusLabel } from "../texts";

/**
 * Vocabulary details with the version history (SIMPL-18167, 18176, 18452, 18559; D35, D36, D64,
 * D113, D115, D118).
 */
const props = defineProps<{ name: string }>();
const route = useRoute();

const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const selected = ref<VocabularyVersion | null>(null);
const notFound = ref(false);
const loadError = ref("");
const actionError = ref("");
const notice = takeFlash();

const latestActive = computed(() => {
  const active = versions.value.filter((v) => v.status === "ACTIVE").map((v) => v.version);
  return active.length ? Math.max(...active) : null;
});

function effectivelyDeprecated(v: VocabularyVersion): boolean {
  return v.status === "DEPRECATED" || vocabulary.value?.status === "DEPRECATED";
}

/** 'Latest ACTIVE version: n' only for an ACTIVE vocabulary and never for the version itself (D64). */
function activeLink(v: VocabularyVersion): number | null {
  if (vocabulary.value?.status !== "ACTIVE") return null;
  const target = latestActive.value;
  return target !== null && target !== v.version ? target : null;
}

function select(version: number | undefined): void {
  selected.value = versions.value.find((v) => v.version === version) ?? null;
}

async function load(): Promise<void> {
  try {
    vocabulary.value = await getVocabulary(props.name);
    versions.value = await getAllVersions(props.name);
    notFound.value = false;
    loadError.value = "";
    const wanted = Number(route.query.version);
    if (Number.isInteger(wanted) && wanted > 0) select(wanted);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound.value = true;
    } else {
      const detail = error instanceof ApiError ? (error.problem.detail ?? "") : String(error);
      loadError.value = `${TEXT.historyLoadFailed} ${detail}`.trim();
    }
  }
}

async function downloadVersion(version: number): Promise<void> {
  actionError.value = "";
  try {
    await download(props.name, version);
  } catch (error) {
    const detail = error instanceof ApiError ? (error.problem.detail ?? "") : String(error);
    actionError.value = `${TEXT.genericError} ${detail}`.trim();
  }
}

watch(
  () => route.query.version,
  (value) => select(Number(value)),
);
onMounted(load);
</script>

<template>
  <section>
    <RouterLink to="/vocabularies">{{ TEXT.backToList }}</RouterLink>

    <p v-if="notFound" role="alert" class="error">{{ TEXT.vocabularyNotFound }}</p>
    <p v-else-if="loadError" role="alert" class="error">{{ loadError }}</p>

    <template v-if="vocabulary && !notFound && !loadError">
      <h1>{{ vocabulary.name }}</h1>
      <p v-if="notice.message" role="status" class="notice">{{ notice.message }}</p>
      <ValidationWarnings v-if="notice.warnings.length" :warnings="notice.warnings" />
      <p v-if="vocabulary.status === 'DEPRECATED'" role="status" class="banner">
        {{ TEXT.vocabularyDeprecated }}
      </p>
      <dl class="facts">
        <dt>Status</dt>
        <dd>{{ statusLabel(vocabulary.status) }}</dd>
        <dt>Description</dt>
        <dd>{{ vocabulary.description }}</dd>
      </dl>
      <p v-if="actionError" role="alert" class="error">{{ actionError }}</p>

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
          <tr v-for="v in versions" :key="v.version" :class="{ selected: selected?.version === v.version }">
            <td>
              <RouterLink :to="{ path: `/vocabularies/${name}`, query: { version: v.version } }">{{
                v.version
              }}</RouterLink>
            </td>
            <td>
              {{ statusLabel(v.status) }}
              <span v-if="effectivelyDeprecated(v)" class="version-warning">
                {{ TEXT.versionDeprecated }}
                <RouterLink
                  v-if="activeLink(v) !== null"
                  :to="{ path: `/vocabularies/${name}`, query: { version: activeLink(v) } }"
                  >Latest ACTIVE version: {{ activeLink(v) }}</RouterLink
                >
              </span>
            </td>
            <td>{{ formatDate(v.creationDate) }}</td>
            <td>{{ v.description }}</td>
            <td>{{ v.changelog }}</td>
            <td>
              <ActionsMenu
                :label="`Actions for version ${v.version}`"
                :items="[
                  { label: 'View', run: () => select(v.version) },
                  { label: 'Download', run: () => void downloadVersion(v.version) },
                ]"
              />
            </td>
          </tr>
        </tbody>
      </table>

      <section v-if="selected" class="panel" :aria-label="`Version ${selected.version}`">
        <h2>Version {{ selected.version }}</h2>
        <p v-if="effectivelyDeprecated(selected)" class="version-warning">
          {{ TEXT.versionDeprecated }}
          <RouterLink
            v-if="activeLink(selected) !== null"
            :to="{ path: `/vocabularies/${name}`, query: { version: activeLink(selected) } }"
            >Latest ACTIVE version: {{ activeLink(selected) }}</RouterLink
          >
        </p>
        <dl class="facts">
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
        <button type="button" @click="downloadVersion(selected.version)">Download</button>
      </section>
    </template>
  </section>
</template>
