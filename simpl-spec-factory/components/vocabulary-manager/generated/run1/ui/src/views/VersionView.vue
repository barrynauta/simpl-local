<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api, ApiError } from '../api'
import { formatDate } from '../format'
import { notify } from '../notify'
import type { VocabularyMetadata } from '../types'
import DeprecationWarning from '../components/DeprecationWarning.vue'
import ProblemReport from '../components/ProblemReport.vue'
import StatusBadge from '../components/StatusBadge.vue'

/** One version with all its metadata, its content and a download (SIMPL-18176 AC3, 18452). */
const props = defineProps<{ name: string; version: string }>()
const meta = ref<VocabularyMetadata | null>(null)
const latestActive = ref<number | null>(null)
const content = ref('')
const error = ref<ApiError | null>(null)
const number = computed(() => Number(props.version))

async function load() {
  error.value = null
  try {
    meta.value = await api.version(props.name, number.value)
    const all = await api.versions(props.name, 1, 100)
    const active = all.items.filter((v) => v.status === 'ACTIVE').map((v) => v.version)
    latestActive.value = active.length ? Math.max(...active) : null
    content.value = await api.contentText(props.name, number.value)
  } catch (e) {
    error.value = e as ApiError
  }
}

async function download() {
  try {
    notify('success', `Downloaded ${await api.download(props.name, number.value)}.`)
  } catch (e) {
    notify('error', `The vocabulary version could not be downloaded: ${(e as ApiError).message}`)
  }
}

onMounted(load)
watch(() => [props.name, props.version], load)
</script>

<template>
  <p><RouterLink :to="`/vocabularies/${name}`">Back to {{ name }}</RouterLink></p>
  <ProblemReport v-if="error" :error="error"
                 :title="error.status === 404 ? 'The vocabulary version could not be found.' : 'The vocabulary version could not be loaded.'" />
  <template v-if="meta">
    <h2>{{ meta.name }}, version {{ meta.version }}</h2>
    <DeprecationWarning v-if="meta.status === 'DEPRECATED'" :name="meta.name" :version="meta.version"
                        :latest-active="latestActive" subject="version" />
    <dl class="meta" data-testid="version-metadata">
      <dt>Version</dt><dd>{{ meta.version }}</dd>
      <dt>Status</dt><dd><StatusBadge :status="meta.status" /></dd>
      <dt>Description</dt><dd>{{ meta.description }}</dd>
      <dt>Change log</dt><dd>{{ meta.changelog }}</dd>
      <dt>Date created</dt><dd>{{ formatDate(meta.creationDate) }}</dd>
      <dt>Last updated</dt><dd>{{ formatDate(meta.lastUpdateDate) }}</dd>
      <dt>Creator</dt><dd>{{ meta.createdBy }}</dd>
    </dl>
    <div class="row-actions" style="margin:1rem 0"><button @click="download">Download this version</button></div>
    <details open>
      <summary>Content (Turtle)</summary>
      <pre style="max-height:30rem;overflow:auto"><code>{{ content }}</code></pre>
    </details>
  </template>
</template>
