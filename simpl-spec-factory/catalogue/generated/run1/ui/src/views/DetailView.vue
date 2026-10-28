<template>
  <div>
    <p><router-link to="/">Back to search</router-link></p>
    <p v-if="loading">Loading...</p>
    <p v-else-if="error" class="error" data-testid="detail-error">{{ error }}</p>
    <template v-else>
      <div class="panel" v-if="quality">
        <h3>Metadata quality</h3>
        <p>Overall score: <span data-testid="detail-quality-overall-score">{{ quality.overall?.score }}</span></p>
        <p>Overall classification: <span data-testid="detail-quality-overall-classification">{{ quality.overall?.classification }}</span></p>
        <ul>
          <li v-for="d in quality.dimensions ?? []" :key="d.name" :data-testid="`detail-quality-dimension-${d.name}`">
            {{ d.name }}: {{ d.score }} ({{ d.classification }})
          </li>
        </ul>
      </div>
      <div class="panel">
        <h3>Resource description</h3>
        <table>
          <tbody>
            <tr v-for="r in rows" :key="r.path">
              <th>{{ r.path }}</th>
              <td data-testid="detail-property" :data-path="r.path">{{ r.value }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ApiError, getJson } from '../api';
import { flatten, type PropertyRow } from '../flatten';

const route = useRoute();
const rows = ref<PropertyRow[]>([]);
const quality = ref<any>(null);
const error = ref('');
const loading = ref(true);

onMounted(async () => {
  const id = String(route.query.id ?? '');
  const catalogueId = String(route.query.catalogueId ?? '');
  try {
    const vc = await getJson(`/api/v2/selfDescriptions/${encodeURIComponent(id)}?catalogueId=${encodeURIComponent(catalogueId)}`);
    quality.value = vc.qualityAssessment ?? null;
    rows.value = flatten(vc.credentialSubject ?? {});
  } catch (e: any) {
    const status = e instanceof ApiError ? e.status : 0;
    error.value = status === 400
      ? `The request is not valid: ${e.message}`
      : `The resource description could not be retrieved (${status || 'network error'}): ${e?.message ?? e}`;
  } finally {
    loading.value = false;
  }
});
</script>
