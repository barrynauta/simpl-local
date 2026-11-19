<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ApiError,
  allVersions,
  changeStatus,
  downloadContent,
  getVocabulary,
  type Vocabulary,
  type VocabularyVersion,
} from "../api";
import { isAdmin } from "../auth";
import { describeError } from "../errors";
import { formatUtc } from "../format";
import { clearNotice, notice, setNotice } from "../notifications";
import { STATUS_LABEL, TEXTS } from "../texts";
import NoticePanel from "../components/NoticePanel.vue";

// Vocabulary details with the version history (SIMPL-18167, 18176, 18452, 18559; decisions D35, D36, D60).
const props = defineProps<{ name: string }>();
const route = useRoute();
const router = useRouter();
const admin = isAdmin();
const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const notFound = ref(false);
const loadError = ref<{ message: string; detail: string } | null>(null);
const actionError = ref<{ message: string; detail: string } | null>(null);
const selected = ref<number | null>(null);

const latestActive = computed(() => {
  const active = versions.value.filter((v) => v.status === "ACTIVE").map((v) => v.version);
  return active.length > 0 ? Math.max(...active) : null;
});
const selectedVersion = computed(() => versions.value.find((v) => v.version === selected.value) ?? null);

/** Decision D3: a version is effectively deprecated when it or its vocabulary is DEPRECATED. */
function effectivelyDeprecated(v: VocabularyVersion): boolean {
  return v.status === "DEPRECATED" || vocabulary.value?.status === "DEPRECATED";
}

/** Decision D36 and SIMPL-18559: the link goes to the latest ACTIVE version when it is another version. */
function activeReference(v: VocabularyVersion): number | null {
  return latestActive.value !== null && latestActive.value !== v.version ? latestActive.value : null;
}

async function load(): Promise<void> {
  loadError.value = null;
  notFound.value = false;
  try {
    vocabulary.value = await getVocabulary(props.name);
    versions.value = await allVersions(props.name);
    const wanted = Number(route.query.version);
    selected.value = Number.isInteger(wanted) && wanted > 0 ? wanted : null;
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      notFound.value = true;
    } else {
      const described = describeError(e);
      const detail = e instanceof ApiError ? (e.problem.detail ?? "") : described.detail;
      loadError.value = { message: described.message, detail };
    }
  }
}

function select(version: number): void {
  selected.value = version;
  void router.replace({ query: { version: String(version) } });
}

async function download(version?: number): Promise<void> {
  actionError.value = null;
  try {
    await downloadContent(props.name, version);
  } catch (e) {
    actionError.value = describeError(e);
  }
}

async function toggleStatus(): Promise<void> {
  if (!vocabulary.value) return;
  actionError.value = null;
  clearNotice();
  const target = vocabulary.value.status === "ACTIVE" ? "DEPRECATED" : "ACTIVE";
  try {
    const changed = await changeStatus(props.name, target);
    vocabulary.value = changed;
    setNotice(TEXTS.statusChanged(props.name, STATUS_LABEL[changed.status]));
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      actionError.value = { message: TEXTS.vocabularyNotFound, detail: e.problem.detail ?? "" };
    } else if (e instanceof ApiError && e.status === 403) {
      actionError.value = { message: TEXTS.notAuthorized, detail: "" };
    } else {
      const described = describeError(e);
      actionError.value = {
        message: `${TEXTS.statusUpdateError} ${described.message}`,
        detail: described.detail,
      };
    }
  }
}

watch(
  () => route.query.version,
  (value) => {
    const wanted = Number(value);
    selected.value = Number.isInteger(wanted) && wanted > 0 ? wanted : null;
  },
);
watch(() => props.name, load);
onMounted(load);
onBeforeUnmount(clearNotice);
</script>

<template>
  <RouterLink to="/vocabularies">{{ TEXTS.backToList }}</RouterLink>
  <NoticePanel :success="notice.success" :warnings="notice.warnings" />
  <div v-if="actionError" class="message error" role="alert">
    <p>{{ actionError.message }}</p>
    <p v-if="actionError.detail">{{ actionError.detail }}</p>
  </div>

  <div v-if="notFound" class="message error" role="alert">
    <p>{{ TEXTS.vocabularyNotFound }}</p>
    <RouterLink to="/vocabularies">{{ TEXTS.backToList }}</RouterLink>
  </div>
  <div v-else-if="loadError" class="message error" role="alert">
    <p>{{ TEXTS.historyLoadError }}</p>
    <p v-if="loadError.message !== loadError.detail">{{ loadError.message }}</p>
    <p v-if="loadError.detail">{{ loadError.detail }}</p>
  </div>

  <template v-else-if="vocabulary">
    <header class="vocabulary-header">
      <h1>{{ vocabulary.name }}</h1>
      <p>
        Status: <span class="vocabulary-status">{{ STATUS_LABEL[vocabulary.status] }}</span>
      </p>
      <p class="vocabulary-description">{{ vocabulary.description }}</p>
      <div v-if="vocabulary.status === 'DEPRECATED'" class="message warning" role="note">
        <span class="warning-icon" aria-hidden="true">&#9888;</span> {{ TEXTS.vocabularyDeprecated }}
      </div>
      <div v-if="admin">
        <button type="button" @click="router.push(`/vocabularies/${vocabulary.name}/update`)">
          {{ TEXTS.update }}
        </button>
        <button type="button" class="secondary" @click="toggleStatus">
          {{ vocabulary.status === "ACTIVE" ? TEXTS.deprecate : TEXTS.activate }}
        </button>
        <button type="button" class="secondary" @click="download()">{{ TEXTS.download }}</button>
      </div>
      <div v-else>
        <button type="button" class="secondary" @click="download()">{{ TEXTS.download }}</button>
      </div>
    </header>

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
          :class="{ selected: v.version === selected }"
        >
          <td>
            <button type="button" class="secondary version-link" @click="select(v.version)">
              {{ v.version }}
            </button>
          </td>
          <td>
            {{ STATUS_LABEL[v.status] }}
            <div v-if="effectivelyDeprecated(v)" class="version-warning">
              <span class="warning-icon" aria-hidden="true">&#9888;</span>
              {{ TEXTS.versionDeprecated }}
              <a v-if="activeReference(v) !== null" href="#" @click.prevent="select(activeReference(v)!)">
                {{ TEXTS.latestActive(activeReference(v)!) }}
              </a>
            </div>
          </td>
          <td>{{ formatUtc(v.creationDate) }}</td>
          <td>{{ v.description }}</td>
          <td>{{ v.changelog }}</td>
          <td>
            <button type="button" class="secondary" @click="download(v.version)">
              {{ TEXTS.downloadVersion }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <section v-if="selectedVersion" class="panel" aria-label="Version details">
      <h3>Version {{ selectedVersion.version }}</h3>
      <div v-if="effectivelyDeprecated(selectedVersion)" class="message warning version-warning">
        <span class="warning-icon" aria-hidden="true">&#9888;</span>
        {{ TEXTS.versionDeprecated }}
        <a
          v-if="activeReference(selectedVersion) !== null"
          href="#"
          @click.prevent="select(activeReference(selectedVersion)!)"
        >
          {{ TEXTS.latestActive(activeReference(selectedVersion)!) }}
        </a>
      </div>
      <dl>
        <dt>Version</dt>
        <dd>{{ selectedVersion.version }}</dd>
        <dt>Status</dt>
        <dd>{{ STATUS_LABEL[selectedVersion.status] }}</dd>
        <dt>Date created</dt>
        <dd>{{ formatUtc(selectedVersion.creationDate) }}</dd>
        <dt>Creator</dt>
        <dd>{{ selectedVersion.createdBy }}</dd>
        <dt>Description</dt>
        <dd>{{ selectedVersion.description }}</dd>
        <dt>Change log</dt>
        <dd>{{ selectedVersion.changelog }}</dd>
      </dl>
      <button type="button" @click="download(selectedVersion.version)">{{ TEXTS.downloadVersion }}</button>
    </section>
  </template>
</template>
