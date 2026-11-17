<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{ title: string; required: boolean }>()
const emit = defineEmits<{ submit: [reason: string]; cancel: [] }>()
const reason = ref('')
const canSubmit = computed(() => !props.required || reason.value.trim().length > 0)

function submit() {
  if (!canSubmit.value) return
  emit('submit', reason.value.trim())
}
</script>

<template>
  <div class="dialog" role="dialog" aria-modal="true">
    <div class="box">
      <h2>{{ title }}</h2>
      <label>Reason
        <textarea v-model="reason" data-testid="reason-input" maxlength="1000"></textarea>
      </label>
      <div class="actions">
        <button type="button" data-testid="reason-submit" :disabled="!canSubmit" @click="submit">Submit</button>
        <button type="button" data-testid="reason-cancel" @click="emit('cancel')">Cancel</button>
      </div>
    </div>
  </div>
</template>
