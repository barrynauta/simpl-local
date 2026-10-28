<template>
  <div v-if="groups.length">
    <div class="tabs">
      <button v-for="g in groups" :key="g.catalogueId" :data-testid="`results-tab-${g.catalogueId}`"
              :class="{ active: g.catalogueId === active }" @click="active = g.catalogueId">
        {{ g.catalogueName }} ({{ g.total }})
      </button>
    </div>
    <div v-for="g in groups" :key="g.catalogueId" v-show="g.catalogueId === active" class="panel"
         :data-testid="`results-panel-${g.catalogueId}`">
      <p v-if="g.status !== 'OK'" class="status" :data-testid="`group-status-${g.catalogueId}`">
        Catalogue {{ g.catalogueName }} could not be searched ({{ g.status === 'UNAUTHENTICATED' ? 'not authenticated' : 'not available' }}).
      </p>
      <p v-else-if="g.items.length === 0" class="status" :data-testid="`group-status-${g.catalogueId}`">No results</p>
      <table v-else>
        <thead>
          <tr>
            <th>Name</th><th>Description</th><th>Type</th><th>Language</th><th>Access point</th>
            <template v-if="g.local"><th>Score</th><th>Classification</th></template>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in g.items" :key="it.i.claimsGraphUri[0]" data-testid="result-row" :data-sd-id="it.i.claimsGraphUri[0]"
              @click="$emit('open', it.i.claimsGraphUri[0], g.catalogueId)">
            <td>{{ it.i.name }}</td>
            <td>{{ it.i.description }}</td>
            <td>{{ it.i.offeringType }}</td>
            <td>{{ it.i.inLanguage }}</td>
            <td>{{ it.i.serviceAccessPoint }}</td>
            <template v-if="g.local">
              <td data-testid="result-score">{{ it.i.qualityScore ?? '' }}</td>
              <td data-testid="result-classification">{{ it.i.qualityClassification ?? '' }}</td>
            </template>
          </tr>
        </tbody>
      </table>
      <div class="pager">
        <button :data-testid="`page-prev-${g.catalogueId}`" :disabled="g.page <= 1" @click="$emit('page', g.catalogueId, g.page - 1)">Previous</button>
        <span :data-testid="`page-indicator-${g.catalogueId}`">Page {{ g.page }} of {{ Math.max(1, g.totalPages) }}</span>
        <button :data-testid="`page-next-${g.catalogueId}`" :disabled="g.page >= g.totalPages" @click="$emit('page', g.catalogueId, g.page + 1)">Next</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { Group } from '../api';

const props = defineProps<{ groups: Group[] }>();
defineEmits<{ (e: 'page', catalogueId: string, page: number): void; (e: 'open', id: string, catalogueId: string): void }>();
const active = ref('');

watch(() => props.groups.map((g) => g.catalogueId).join(','), () => {
  if (!props.groups.some((g) => g.catalogueId === active.value)) active.value = props.groups[0]?.catalogueId ?? '';
}, { immediate: true });
</script>
