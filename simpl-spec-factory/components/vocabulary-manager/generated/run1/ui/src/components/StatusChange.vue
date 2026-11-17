<script setup lang="ts">
import { ref } from 'vue'
import { api, ApiError } from '../api'
import { notify } from '../notify'
import type { Status, VocabularyCategory } from '../types'
import ProblemReport from './ProblemReport.vue'

/** Manual vocabulary status change (SIMPL-18174). */
const props = defineProps<{ vocabulary: VocabularyCategory }>()
const emit = defineEmits<{ (e: 'changed', v: VocabularyCategory): void; (e: 'cancel'): void }>()
const target = ref<Status>(props.vocabulary.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE')
const busy = ref(false)
const error = ref<ApiError | null>(null)

async function apply() {
  busy.value = true
  error.value = null
  try {
    const updated = await api.setStatus(props.vocabulary.name, target.value)
    notify('success', `The status of vocabulary ${updated.name} was successfully updated to ${updated.status === 'ACTIVE' ? 'Active' : 'Deprecated'}.`)
    emit('changed', updated)
  } catch (e) {
    const err = e as ApiError
    error.value = err
    if (err.status === 404) {
      notify('error', `Vocabulary ${props.vocabulary.name} could not be found. No status change was applied.`)
    } else {
      notify('error', `The status of vocabulary ${props.vocabulary.name} could not be updated.`)
    }
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <article data-testid="status-change">
    <header><strong>Change status of {{ vocabulary.name }}</strong></header>
    <p>Current status: <strong>{{ vocabulary.status === 'ACTIVE' ? 'Active' : 'Deprecated' }}</strong></p>
    <label>New status
      <select v-model="target" name="status">
        <option value="ACTIVE">Active</option>
        <option value="DEPRECATED">Deprecated</option>
      </select>
    </label>
    <p v-if="target === 'DEPRECATED'" class="warning">
      Deprecating the vocabulary marks all its versions as Deprecated. They stay available for existing use.
    </p>
    <p v-else class="warning">Re-activating the vocabulary makes its latest version Active again.</p>
    <ProblemReport v-if="error" :error="error" title="The vocabulary status could not be updated." />
    <div class="row-actions">
      <button :aria-busy="busy" :disabled="busy" @click="apply">Apply status</button>
      <button class="secondary outline" @click="emit('cancel')">Cancel</button>
    </div>
  </article>
</template>
