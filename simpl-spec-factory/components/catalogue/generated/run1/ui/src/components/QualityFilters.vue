<template>
  <div class="row">
    <label>
      Minimum score (0 to 100)
      <input data-testid="min-score-input" type="text" inputmode="decimal" v-model="score" />
      <span v-if="scoreInvalid" class="error" data-testid="min-score-error">Enter a number from 0 to 100.</span>
    </label>
    <label>
      Minimum classification
      <select data-testid="min-classification-select" v-model="classification">
        <option value=""></option>
        <option v-for="l in labels" :key="l" :value="l">{{ l }}</option>
      </select>
    </label>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { classificationLabels } from '../api';

const emit = defineEmits<{ (e: 'change', v: { minimumScore?: number; minimumClassification?: string; valid: boolean }): void }>();
const score = ref('');
const classification = ref('');
const labels = ref<string[]>([]);

const scoreInvalid = computed(() => {
  const s = score.value.trim();
  if (s === '') return false;
  if (!/^\d+(\.\d+)?$/.test(s)) return true;
  const n = Number(s);
  return !(n >= 0 && n <= 100);
});

function publish() {
  const s = score.value.trim();
  emit('change', {
    minimumScore: s === '' || scoreInvalid.value ? undefined : Number(s),
    minimumClassification: classification.value || undefined,
    valid: !scoreInvalid.value,
  });
}

watch([score, classification], publish);
onMounted(async () => {
  publish();
  try {
    labels.value = await classificationLabels();
  } catch {
    labels.value = [];
  }
});
</script>
