<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listContracts, type ContractItem } from '../api'
import { loadConfig } from '../auth'
import { STATUS_LABELS, statusLabel, counterpartyName } from '../labels'

const items = ref<ContractItem[]>([])
const error = ref('')
const loaded = ref(false)
// C6: the counterparty column is 'Provider name' at the Consumer and 'Consumer name' at the Provider.
const counterpartyLabel = ref('Provider name')
const filters = ref({ contractNegotiationId: '', resourceName: '', counterpartyName: '', status: '' })

async function load() {
  error.value = ''
  try {
    const res = await listContracts({ ...filters.value })
    items.value = res.items
  } catch (e) {
    error.value = (e as Error).message
    items.value = []
  }
  loaded.value = true
}

onMounted(async () => {
  const cfg = await loadConfig()
  counterpartyLabel.value = cfg.party === 'PROVIDER' ? 'Consumer name' : 'Provider name'
  await load()
})
</script>

<template>
  <section>
    <h2>Usage contracts</h2>
    <form class="filters" @submit.prevent="load">
      <label>Contract negotiation ID <input v-model.trim="filters.contractNegotiationId" data-testid="filter-negotiation-id" /></label>
      <label>Resource name <input v-model.trim="filters.resourceName" data-testid="filter-resource-name" /></label>
      <label>{{ counterpartyLabel }} <input v-model.trim="filters.counterpartyName" data-testid="filter-counterparty-name" /></label>
      <label>Status
        <select v-model="filters.status" data-testid="filter-status">
          <option value="">Any</option>
          <option v-for="(label, value) in STATUS_LABELS" :key="value" :value="value">{{ label }}</option>
        </select>
      </label>
      <button type="submit" data-testid="filter-apply">Apply</button>
    </form>
    <p v-if="error" data-testid="error-message" class="error">{{ error }}</p>
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
          <td data-testid="cell-counterparty-name">{{ counterpartyName(c) }}</td>
          <td data-testid="cell-status">{{ statusLabel(c.status) }}</td>
          <td>
            <router-link :to="`/contracts/${c.contractNegotiationId}`" :data-testid="`open-details-${c.contractNegotiationId}`">Details</router-link>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-if="loaded && items.length === 0 && !error" data-testid="contracts-empty" class="empty">No usage contracts found.</p>
  </section>
</template>
