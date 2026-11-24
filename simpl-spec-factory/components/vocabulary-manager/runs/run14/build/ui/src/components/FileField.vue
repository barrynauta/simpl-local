<script setup lang="ts">
import { ref } from 'vue';
import { MAX_FILE_BYTES, checkTurtle, syntaxErrorText } from '../lib/turtle';
import { TEXT } from '../lib/texts';

const props = defineProps<{ id: string; required: boolean; currentName?: string; error?: string }>();
const emit = defineEmits<{ change: [file: File | null, blocking: boolean] }>();

const checkMessage = ref('');
const checkOk = ref(false);
const chosen = ref<string>('');

async function onChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] ?? null;
  checkMessage.value = '';
  checkOk.value = false;
  chosen.value = file?.name ?? '';
  if (!file) {
    emit('change', null, false);
    return;
  }
  if (file.size > MAX_FILE_BYTES) {
    checkMessage.value = TEXT.fileTooLarge;
    emit('change', file, true);
    return;
  }
  const result = checkTurtle(await file.text());
  if (result.ok) {
    checkOk.value = true;
    checkMessage.value = TEXT.syntaxPassed;
    emit('change', file, false);
  } else {
    checkMessage.value = syntaxErrorText(result);
    emit('change', file, true);
  }
}
</script>

<template>
  <div class="mt-3">
    <label :for="id" class="block text-sm font-medium">
      Vocabulary file<span v-if="props.required" aria-hidden="true" class="text-red-800"> *</span>
    </label>
    <p v-if="currentName && !chosen" class="text-sm text-gray-800">{{ currentName }}</p>
    <input
      :id="id"
      type="file"
      accept=".ttl,text/turtle"
      class="field-input"
      :aria-required="props.required ? 'true' : 'false'"
      :aria-invalid="(!checkOk && !!checkMessage) || !!error ? 'true' : 'false'"
      :aria-describedby="`${id}-message`"
      @change="onChange"
    />
    <p :id="`${id}-message`" :class="checkOk ? 'min-h-5 text-sm text-green-800' : 'field-error'">
      {{ error || checkMessage }}
    </p>
  </div>
</template>
