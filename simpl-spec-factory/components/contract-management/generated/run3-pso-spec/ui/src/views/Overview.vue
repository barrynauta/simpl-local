<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listContracts, type ContractItem } from '../api'
import { STATUS_LABELS, statusLabel } from '../labels'
import { config } from '../config'

const isConsumer = config.partyRole === 'CONSUMER'
const counterpartyLabel = isConsumer ? 'Provider name' : 'Consumer name'
const items = ref<ContractItem[]>([])
const loading = ref(false)
const error = ref('')
const filters = reactive({ contractNegotiationId: '', resourceName: '', counterpartyName: '', status: '' })

function counterparty(c: ContractItem): string {
  return (isConsumer ? c.providerName : c.consumerName) ?? ''
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = (await listContracts({ ...filters })).items
  } catch (e) {
    error.value = (e as Error).message
    items.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <h1>Usage contracts</h1>
  <form class="filters" @submit.prevent="load">
    <label>Contract negotiation ID
      <input v-model.trim="filters.contractNegotiationId" data-testid="filter-negotiation-id" />
    </label>
    <label>Resource name
      <input v-model.trim="filters.resourceName" data-testid="filter-resource-name" />
    </label>
    <label>{{ counterpartyLabel }}
      <input v-model.trim="filters.counterpartyName" data-testid="filter-counterparty-name" />
    </label>
    <label>Status
      <select v-model="filters.status" data-testid="filter-status">
        <option value="">All</option>
        <option v-for="(label, value) in STATUS_LABELS" :key="value" :value="value">{{ label }}</option>
      </select>
    </label>
    <button type="submit" data-testid="filter-apply">Apply filters</button>
  </form>
  <p v-if="error" class="error" data-testid="error-message">{{ error }}</p>
  <table data-testid="contracts-table">
    <thead>
      <tr>
        <th>Contract negotiation ID</th>
        <th>Resource description ID</th>
        <th>Resource name</th>
        <th>{{ counterpartyLabel }}</th>
        <th>Status</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="c in items" :key="c.contractNegotiationId" :data-testid="`contract-row-${c.contractNegotiationId}`">
        <td data-testid="cell-negotiation-id">{{ c.contractNegotiationId }}</td>
        <td data-testid="cell-resource-description-id">{{ c.resourceDescriptionId }}</td>
        <td data-testid="cell-resource-name">{{ c.resourceName }}</td>
        <td data-testid="cell-counterparty-name">{{ counterparty(c) }}</td>
        <td data-testid="cell-status">{{ statusLabel(c.status) }}</td>
        <td>
          <router-link :to="`/contracts/${encodeURIComponent(c.contractNegotiationId)}`"
                       :data-testid="`open-details-${c.contractNegotiationId}`">Details</router-link>
        </td>
      </tr>
    </tbody>
  </table>
  <p v-if="!loading && items.length === 0 && !error" data-testid="contracts-empty">No usage contracts found.</p>
</template>
