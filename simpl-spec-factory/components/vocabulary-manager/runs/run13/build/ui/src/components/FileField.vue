<script setup lang="ts">
import { ref } from 'vue';
import FieldError from './FieldError.vue';
import RequiredLabel from './RequiredLabel.vue';
import { checkFile, errorText } from '../turtleCheck';

const props = defineProps<{ currentLabel?: string; submitError: string }>();
const emit = defineEmits<{ change: [file: File | null, blocking: boolean] }>();

const checkMessage = ref('');
const blockingError = ref('');
const fileName = ref(props.currentLabel ?? '');

async function onChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] ?? null;
  checkMessage.value = '';
  blockingError.value = '';
  if (!file) {
    fileName.value = props.currentLabel ?? '';
    emit('change', null, false);
    return;
  }
  fileName.value = file.name;
  const result = await checkFile(file);
  if (result.ok) {
    checkMessage.value = result.message;
    emit('change', file, false);
  } else {
    blockingError.value = result.tooLarge ? result.message : errorText(result);
    emit('change', file, true);
  }
}

defineExpose({ onChange });
</script>

<template>
  <div>
    <RequiredLabel for-id="field-file" text="Vocabulary file" />
    <input
      id="field-file"
      type="file"
      accept=".ttl,text/turtle"
      aria-required="true"
      :aria-invalid="blockingError || submitError ? 'true' : 'false'"
      aria-describedby="field-file-status field-file-error"
      class="mt-1 block"
      @change="onChange"
    />
    <p v-if="fileName" class="text-sm text-slate-800">{{ fileName }}</p>
    <p id="field-file-status" role="status" class="text-sm text-green-900">{{ checkMessage }}</p>
    <FieldError id="field-file-error" :message="blockingError || submitError" />
  </div>
</template>
