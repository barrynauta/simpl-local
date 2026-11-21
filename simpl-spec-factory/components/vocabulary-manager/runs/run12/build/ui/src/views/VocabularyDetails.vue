<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Button from 'primevue/button';
import ActionsMenu from '../components/ActionsMenu.vue';
import AlertMessage from '../components/AlertMessage.vue';
import StatusMessage from '../components/StatusMessage.vue';
import ValidationWarnings from '../components/ValidationWarnings.vue';
import {
  api,
  saveBlob,
  type ValidationWarning,
  type Vocabulary,
  type VocabularyStatus,
  type VocabularyVersion,
} from '../api';
import { isAdmin } from '../auth';
import { takeFlash } from '../flash';
import {
  formatDate,
  isEffectivelyDeprecated,
  isStatus,
  problemDetail,
  recommendedVersion,
  statusChangeError,
  statusLabel,
} from '../format';
import { TEXT } from '../texts';
import { btnPrimary, btnSecondary, linkClass } from '../ui';

/** Vocabulary details with the version history (D35, D36, D64, D72, D113, D115, D118, D163). */
const route = useRoute();
const router = useRouter();

const name = computed(() => String(route.params.name ?? ''));
const selectedVersion = computed(() => {
  const raw = route.params.version;
  if (typeof raw !== 'string' || !/^[0-9]+$/.test(raw)) return undefined;
  return Number(raw);
});

const vocabulary = ref<Vocabulary | null>(null);
const versions = ref<VocabularyVersion[]>([]);
const loading = ref(true);
const notFound = ref(false);
const loadError = ref<{ text: string; detail: string } | null>(null);
const actionError = ref<{ text: string; detail: string } | null>(null);
const actionMessage = ref('');
const created = ref<{ message: string; warnings: ValidationWarning[] } | null>(null);
const panel = ref<HTMLElement | null>(null);
const admin = computed(() => isAdmin());

const selected = computed(() =>
  selectedVersion.value === undefined
    ? undefined
    : versions.value.find((v) => v.version === selectedVersion.value),
);

const vocabularyStatus = computed<VocabularyStatus | undefined>(() => vocabulary.value?.status);

async function load() {
  loading.value = true;
  notFound.value = false;
  loadError.value = null;
  try {
    const [voc, list] = await Promise.all([
      api.getVocabulary(name.value),
      api.listAllVersions(name.value),
    ]);
    vocabulary.value = voc;
    versions.value = list;
  } catch (error) {
    vocabulary.value = null;
    versions.value = [];
    if (isStatus(error, 404)) notFound.value = true;
    else if (isStatus(error, 403)) loadError.value = { text: TEXT.notAuthorized, detail: '' };
    else loadError.value = { text: TEXT.historyLoadFailed, detail: problemDetail(error) };
  } finally {
    loading.value = false;
  }
  await focusPanel();
}

async function focusPanel() {
  await nextTick();
  if (panel.value) {
    panel.value.focus();
    if (typeof panel.value.scrollIntoView === 'function') panel.value.scrollIntoView();
  }
}

function deprecationOf(version: VocabularyVersion) {
  if (!isEffectivelyDeprecated(version, vocabularyStatus.value)) return null;
  return { recommended: recommendedVersion(version, vocabularyStatus.value, versions.value) };
}

async function download(version?: number) {
  actionError.value = null;
  try {
    const { blob, filename } = await api.downloadContent(name.value, version);
    saveBlob(blob, filename);
  } catch (error) {
    actionError.value = isStatus(error, 403)
      ? { text: TEXT.notAuthorized, detail: '' }
      : { text: TEXT.downloadFailed, detail: problemDetail(error) };
  }
}

async function changeStatus() {
  if (!vocabulary.value) return;
  const target: VocabularyStatus = vocabulary.value.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  actionError.value = null;
  actionMessage.value = '';
  try {
    const updated = await api.changeStatus(vocabulary.value.name, target);
    vocabulary.value = updated;
    actionMessage.value = `Status of ${updated.name} changed to ${statusLabel(updated.status)}.`;
  } catch (error) {
    actionError.value = statusChangeError(error);
  }
}

function versionLink(version: number): string {
  return `/vocabularies/${name.value}/versions/${version}`;
}

onMounted(() => {
  document.title = `${name.value} - Vocabulary Manager`;
  created.value = takeFlash(name.value);
  void load();
});

watch(name, () => {
  document.title = `${name.value} - Vocabulary Manager`;
  created.value = takeFlash(name.value);
  void load();
});
watch(selectedVersion, () => void focusPanel());
</script>

<template>
  <div>
    <p class="mb-3">
      <RouterLink to="/vocabularies" :class="linkClass">{{ TEXT.backToList }}</RouterLink>
    </p>

    <StatusMessage v-if="created" :text="created.message" />
    <ValidationWarnings
      v-if="created && created.warnings.length > 0"
      :warnings="created.warnings"
    />

    <p v-if="loading" class="my-3">Loading.</p>

    <AlertMessage v-if="notFound" :text="TEXT.vocabularyNotFound" />
    <AlertMessage v-if="loadError" :text="loadError.text" :detail="loadError.detail" />

    <template v-if="vocabulary">
      <div class="mb-3 flex flex-wrap items-start justify-between gap-2">
        <div>
          <h1 class="text-2xl font-semibold">{{ vocabulary.name }}</h1>
          <p>Status: {{ statusLabel(vocabulary.status) }}</p>
          <p>{{ vocabulary.description }}</p>
        </div>
        <div v-if="admin" class="flex gap-2">
          <Button
            type="button"
            label="Update"
            :class="btnPrimary"
            @click="router.push(`/vocabularies/${vocabulary.name}/versions/new`)"
          />
          <Button
            type="button"
            :label="vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate'"
            :class="btnSecondary"
            @click="changeStatus"
          />
        </div>
      </div>

      <p
        v-if="vocabulary.status === 'DEPRECATED'"
        role="status"
        class="my-3 rounded border border-amber-700 bg-amber-50 px-3 py-2 text-amber-950"
      >
        {{ TEXT.vocabularyDeprecated }}
      </p>

      <StatusMessage v-if="actionMessage" :text="actionMessage" />
      <AlertMessage v-if="actionError" :text="actionError.text" :detail="actionError.detail" />

      <h2 class="mt-4 mb-2 text-xl font-semibold">Version history</h2>
      <div class="overflow-x-auto">
        <table class="min-w-full border-collapse text-left text-sm">
          <caption class="sr-only">
            Version history
          </caption>
          <thead class="border-b-2 border-gray-700">
            <tr>
              <th scope="col" class="px-2 py-2">Version</th>
              <th scope="col" class="px-2 py-2">Status</th>
              <th scope="col" class="px-2 py-2">Date created</th>
              <th scope="col" class="px-2 py-2">Description</th>
              <th scope="col" class="px-2 py-2">Change log</th>
              <th scope="col" class="px-2 py-2">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="version in versions"
              :key="version.version"
              :aria-current="version.version === selectedVersion ? 'true' : undefined"
              :class="[
                'border-b border-gray-300',
                version.version === selectedVersion ? 'bg-blue-50' : '',
              ]"
            >
              <td class="px-2 py-2">
                <RouterLink :to="versionLink(version.version)" :class="linkClass">{{
                  version.version
                }}</RouterLink>
              </td>
              <td class="px-2 py-2">
                <span>{{ statusLabel(version.status) }}</span>
                <template v-if="deprecationOf(version)">
                  <p class="text-amber-900">
                    <span aria-hidden="true">&#9888; </span>{{ TEXT.versionDeprecated }}
                  </p>
                  <p v-if="deprecationOf(version)?.recommended !== undefined">
                    <RouterLink
                      :to="versionLink(deprecationOf(version)?.recommended ?? 0)"
                      :class="linkClass"
                      >Latest ACTIVE version: {{ deprecationOf(version)?.recommended }}</RouterLink
                    >
                  </p>
                </template>
              </td>
              <td class="px-2 py-2 whitespace-nowrap">{{ formatDate(version.creationDate) }}</td>
              <td class="px-2 py-2">{{ version.description }}</td>
              <td class="px-2 py-2">{{ version.changelog }}</td>
              <td class="px-2 py-2">
                <ActionsMenu
                  :label="`Actions for version ${version.version}`"
                  :items="[{ label: 'Download', run: () => void download(version.version) }]"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <section
        v-if="selected"
        ref="panel"
        :aria-label="`Version ${selected.version}`"
        tabindex="-1"
        class="mt-4 rounded border border-gray-500 p-3 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600"
      >
        <h2 class="text-lg font-semibold">Version {{ selected.version }}</h2>
        <template v-if="deprecationOf(selected)">
          <p class="text-amber-900">
            <span aria-hidden="true">&#9888; </span>{{ TEXT.versionDeprecated }}
          </p>
          <p v-if="deprecationOf(selected)?.recommended !== undefined">
            <RouterLink
              :to="versionLink(deprecationOf(selected)?.recommended ?? 0)"
              :class="linkClass"
              >Latest ACTIVE version: {{ deprecationOf(selected)?.recommended }}</RouterLink
            >
          </p>
        </template>
        <dl class="mt-2 grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1">
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
        <Button
          type="button"
          label="Download"
          :class="['mt-3', btnSecondary]"
          @click="download(selected.version)"
        />
      </section>
      <AlertMessage
        v-else-if="selectedVersion !== undefined && !loading"
        :text="`Version ${selectedVersion} of vocabulary '${vocabulary.name}' was not found.`"
      />
    </template>
  </div>
</template>
