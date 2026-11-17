<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import DOMPurify from 'dompurify'
import { decideOffer, getContract, lifecycleAction, type ContractDetails } from '../api'
import { ACTIONS, statusLabel } from '../labels'

const props = defineProps<{ nid: string }>()
const contract = ref<ContractDetails | null>(null)
const error = ref('')
const busy = ref(false)
const dialogAction = ref<string | null>(null)
const reason = ref('')

async function load() {
  error.value = ''
  try {
    contract.value = await getContract(props.nid)
  } catch (e) {
    error.value = (e as Error).message
  }
}

onMounted(load)
watch(() => props.nid, load)

// D75: sanitise before rendering; no scripts, no handlers, no external resources.
const safeHtml = computed(() => {
  const html = contract.value?.humanReadableData ?? contract.value?.humanReadable ?? ''
  return DOMPurify.sanitize(html, {
    FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed', 'img', 'link', 'video', 'audio', 'source', 'form'],
    FORBID_ATTR: ['style', 'src', 'srcset', 'href', 'action', 'formaction', 'background', 'poster'],
  })
})

const actions = computed(() => (contract.value?.allowedActions ?? []).filter((a) => a in ACTIONS))
const canConsent = computed(() => (contract.value?.allowedActions ?? []).includes('UPDATE_CONSENT') && !!contract.value?.consentManagerUrl)

async function run(action: string, withReason?: string) {
  busy.value = true
  error.value = ''
  try {
    if (action === 'ACCEPT') await decideOffer(props.nid, 'accept')
    else if (action === 'REJECT') await decideOffer(props.nid, 'reject')
    else await lifecycleAction(props.nid, action, withReason)
    dialogAction.value = null
    reason.value = ''
    await load()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

function click(action: string) {
  if (ACTIONS[action].reasonRequired) {
    dialogAction.value = action
    reason.value = ''
  } else {
    run(action)
  }
}

function submitReason() {
  if (!dialogAction.value || reason.value.trim() === '') return
  run(dialogAction.value, reason.value)
}
</script>

<template>
  <section data-testid="contract-details">
    <router-link to="/" data-testid="back-to-overview">Back to overview</router-link>
    <p v-if="error" data-testid="error-message" class="error">{{ error }}</p>
    <template v-if="contract">
      <h2>Usage contract</h2>
      <dl class="details">
        <dt>Negotiation id</dt><dd data-testid="detail-negotiation-id">{{ contract.contractNegotiationId }}</dd>
        <dt>Resource description id</dt><dd data-testid="detail-resource-description-id">{{ contract.resourceDescriptionId }}</dd>
        <dt>Resource name</dt><dd data-testid="detail-resource-name">{{ contract.resourceName }}</dd>
        <dt>Provider</dt><dd data-testid="detail-provider-name">{{ contract.providerName }}</dd>
        <dt>Consumer</dt><dd data-testid="detail-consumer-name">{{ contract.consumerName }}</dd>
        <dt>Status</dt><dd data-testid="detail-status">{{ statusLabel(contract.status) }}</dd>
        <template v-if="contract.partyRole === 'PROVIDER'">
          <dt>Consent status</dt><dd data-testid="detail-consent-status">{{ contract.consentStatus }}</dd>
        </template>
        <template v-if="contract.reasonCode">
          <dt>Reason</dt><dd>{{ contract.reasonCode }}</dd>
        </template>
      </dl>

      <div class="actions">
        <button v-for="a in actions" :key="a" type="button" :data-testid="ACTIONS[a].testId" :disabled="busy" @click="click(a)">
          {{ ACTIONS[a].label }}
        </button>
        <a v-if="canConsent" :href="contract.consentManagerUrl" data-testid="action-update-consent" class="button">Update consent</a>
      </div>

      <div v-if="dialogAction" class="dialog" role="dialog">
        <label>Reason for {{ ACTIONS[dialogAction].label.toLowerCase() }}
          <textarea v-model="reason" data-testid="reason-input" maxlength="1000"></textarea>
        </label>
        <button type="button" data-testid="reason-submit" :disabled="busy || reason.trim() === ''" @click="submitReason">Submit</button>
        <button type="button" data-testid="reason-cancel" @click="dialogAction = null">Cancel</button>
      </div>

      <template v-if="contract.partyRole === 'CONSUMER'">
        <h3>Usage contract offer</h3>
        <div data-testid="human-readable" class="offer" v-html="safeHtml"></div>
      </template>

      <h3>History</h3>
      <table class="history">
        <thead><tr><th>At</th><th>From</th><th>To</th><th>Type</th><th>Trigger</th><th>Actor</th><th>Reason</th></tr></thead>
        <tbody>
          <tr v-for="(h, i) in contract.history" :key="i">
            <td>{{ h.at }}</td><td>{{ statusLabel(h.fromStatus) }}</td><td>{{ statusLabel(h.toStatus) }}</td>
            <td>{{ h.entryType }}</td><td>{{ h.trigger }}</td><td>{{ h.actor }}</td>
            <td>{{ [h.reasonCode, h.reason].filter(Boolean).join(': ') }}</td>
          </tr>
        </tbody>
      </table>
    </template>
  </section>
</template>
