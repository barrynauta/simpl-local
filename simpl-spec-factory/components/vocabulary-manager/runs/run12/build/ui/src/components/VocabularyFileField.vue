<script setup lang="ts">
import { ref } from 'vue';
import RequiredMark from './RequiredMark.vue';
import { isFileTooLarge } from '../format';
import { checkTurtle, describeSyntaxError } from '../turtle';
import { TEXT } from '../texts';
import { fieldErrorClass } from '../ui';

/**
 * D13, D60, D62, D76: the 'Vocabulary file' field with the browser upload check.
 * Emits the chosen file and whether the upload check blocks Submit.
 */
const props = defineProps<{ fieldError?: string; currentFileLabel?: string }>();
const emit = defineEmits<{ change: [file: File | null, blocked: boolean] }>();

const input = ref<HTMLInputElement | null>(null);
const uploadError = ref('');
const passed = ref(false);
const chosenName = ref('');

async function onChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files && target.files.length > 0 ? target.files[0] : null;
  uploadError.value = '';
  passed.value = false;
  chosenName.value = file ? file.name : '';
  if (!file) {
    emit('change', null, false);
    return;
  }
  if (isFileTooLarge(file.size)) {
    uploadError.value = TEXT.fileTooLarge;
    emit('change', file, true);
    return;
  }
  const check = checkTurtle(await file.text());
  if (check.ok) {
    passed.value = true;
    emit('change', file, false);
  } else {
    uploadError.value = describeSyntaxError(check);
    emit('change', file, true);
  }
}

function focus() {
  input.value?.focus();
}

defineExpose({ focus });
const shownError = () => uploadError.value || props.fieldError || '';
</script>

<template>
  <div class="mb-4">
    <label for="vocabularyFile" class="inline-block text-sm font-medium text-gray-900"
      >Vocabulary file</label
    ><RequiredMark />
    <p v-if="currentFileLabel && !chosenName" id="vocabularyFile-current" class="text-sm">
      Attached: {{ currentFileLabel }}
    </p>
    <input
      id="vocabularyFile"
      ref="input"
      type="file"
      accept=".ttl,text/turtle"
      aria-required="true"
      :aria-invalid="shownError() ? 'true' : 'false'"
      :aria-describedby="
        currentFileLabel && !chosenName
          ? 'vocabularyFile-current vocabularyFile-error'
          : 'vocabularyFile-error'
      "
      class="mt-1 block text-sm"
      @change="onChange"
    />
    <div id="vocabularyFile-error" :class="fieldErrorClass">
      <p v-if="uploadError" role="alert">{{ uploadError }}</p>
      <p v-else-if="fieldError">{{ fieldError }}</p>
      <p v-else-if="passed" role="status" class="text-green-900">{{ TEXT.syntaxPassed }}</p>
    </div>
  </div>
</template>
