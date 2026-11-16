<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DOMPurify from 'dompurify'
import { decideOffer, getContract, lifecycleAction, type ContractDetails } from '../api'
import { ACTIONS, statusLabel } from '../labels'
import ReasonDialog from '../components/ReasonDialog.vue'

const props = defineProps<{ nid: string }>()
const router = useRouter()
const contract = ref<ContractDetails | null>(null)
const error = ref('')
const busy = ref(false)
const pending = ref<string | null>(null)

// D75: no scripts, no event handlers, no external resources.
const safeHtml = computed(() => {
  const html = contract.value?.humanReadable ?? contract.value?.humanReadableData ?? ''
  return DOMPurify.sanitize(html, {
    FORBID_TAGS: ['script', 'style', 'img', 'iframe', 'object', 'embed', 'link', 'video', 'audio', 'source',
      'picture', 'form', 'input', 'svg', 'math', 'base', 'meta'],
    FORBID_ATTR: ['src', 'srcset', 'style', 'background', 'poster', 'href', 'action', 'formaction', 'xlink:href']
  })
})

const actions = computed(() => (contract.value?.allowedActions ?? []).filter((a) => ACTIONS[a]))
const isProvider = computed(() => contract.value?.partyRole === 'PROVIDER')

async function load() {
  error.value = ''
  try {
    contract.value = await getContract(props.nid)
  } catch (e) {
    error.value = (e as Error).message
    contract.value = null
  }
}

async function run(action: string, reason?: string) {
  busy.value = true
  error.value = ''
  try {
    if (action === 'ACCEPT' || action === 'REJECT') {
      await decideOffer(props.nid, action === 'ACCEPT' ? 'accept' : 'reject')
      await load()
    } else {
      contract.value = await lifecycleAction(props.nid, action, reason)
    }
  } catch (e) {
    error.value = (e as Error).message
    await load()
  } finally {
    busy.value = false
    pending.value = null
  }
}

function click(action: string) {
  if (ACTIONS[action].reason === 'required') {
    pending.value = action
  } else {
    run(action)
  }
}

onMounted(load)
watch(() => props.nid, load)
</script>

<template>
  <button type="button" data-testid="back-to-overview" @click="router.push('/')">Back to overview</button>
  <p v-if="error" class="error" data-testid="error-message">{{ error }}</p>
  <section v-if="contract" data-testid="contract-details">
    <h1>Usage contract</h1>
    <dl class="fields">
      <dt>Contract negotiation ID</dt><dd data-testid="detail-negotiation-id">{{ contract.contractNegotiationId }}</dd>
      <dt>Resource description ID</dt><dd data-testid="detail-resource-description-id">{{ contract.resourceDescriptionId }}</dd>
      <dt>Resource name</dt><dd data-testid="detail-resource-name">{{ contract.resourceName }}</dd>
      <dt>Provider name</dt><dd data-testid="detail-provider-name">{{ contract.providerName }}</dd>
      <dt>Consumer name</dt><dd data-testid="detail-consumer-name">{{ contract.consumerName }}</dd>
      <dt>Status</dt><dd data-testid="detail-status">{{ statusLabel(contract.status) }}</dd>
      <template v-if="isProvider">
        <dt>Consent status</dt><dd data-testid="detail-consent-status">{{ contract.consentStatus }}</dd>
      </template>
      <template v-if="contract.reasonCode">
        <dt>Reason</dt><dd>{{ contract.reasonCode }}</dd>
      </template>
    </dl>

    <div class="actions">
      <template v-for="a in actions" :key="a">
        <a v-if="a === 'UPDATE_CONSENT'" :href="contract.consentManagerUrl" :data-testid="ACTIONS[a].testId">
          {{ ACTIONS[a].label }}</a>
        <button v-else type="button" :data-testid="ACTIONS[a].testId" :disabled="busy" @click="click(a)">
          {{ ACTIONS[a].label }}</button>
      </template>
    </div>

    <template v-if="!isProvider">
      <h2>Usage contract offer</h2>
      <div class="offer" data-testid="human-readable" v-html="safeHtml"></div>
    </template>

    <h2>History</h2>
    <table>
      <thead><tr><th>At</th><th>From</th><th>To</th><th>Type</th><th>Trigger</th><th>Actor</th><th>Reason</th></tr></thead>
      <tbody>
        <tr v-for="(h, i) in contract.history" :key="i">
          <td>{{ h.at }}</td>
          <td>{{ statusLabel(h.fromStatus) }}</td>
          <td>{{ statusLabel(h.toStatus) }}</td>
          <td>{{ h.entryType }}</td>
          <td>{{ h.trigger }}</td>
          <td>{{ h.actor }}</td>
          <td>{{ h.reasonCode }} {{ h.reason }}</td>
        </tr>
      </tbody>
    </table>
  </section>
  <ReasonDialog v-if="pending" :title="ACTIONS[pending].label" :required="ACTIONS[pending].reason === 'required'"
                @submit="(r) => run(pending!, r)" @cancel="pending = null" />
</template>
