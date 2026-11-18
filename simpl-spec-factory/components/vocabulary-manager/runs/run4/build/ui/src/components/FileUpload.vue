<script setup lang="ts">
import { ref } from 'vue';
import { checkTurtle, type SyntaxError } from '../turtleCheck';
import { MAX_UPLOAD_BYTES, UPLOAD_TOO_LARGE } from '../labels';

const props = defineProps<{ currentLabel?: string }>();
const emit = defineEmits<{ (e: 'change', file: File | null, valid: boolean): void }>();

const fileName = ref<string | undefined>(props.currentLabel);
const syntaxError = ref<SyntaxError | undefined>();
const sizeError = ref(false);
const passed = ref(false);
const checking = ref(false);

/** Checks size and Turtle syntax as soon as a file is chosen (decision D13). */
async function onChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] ?? null;
  syntaxError.value = undefined;
  sizeError.value = false;
  passed.value = false;
  if (!file) {
    fileName.value = props.currentLabel;
    emit('change', null, true);
    return;
  }
  fileName.value = file.name;
  if (file.size > MAX_UPLOAD_BYTES) {
    sizeError.value = true;
    emit('change', file, false);
    return;
  }
  checking.value = true;
  syntaxError.value = checkTurtle(await file.text());
  checking.value = false;
  passed.value = syntaxError.value === undefined;
  emit('change', file, passed.value);
}
</script>

<template>
  <div class="field">
    <label for="vocabulary-file">File upload</label>
    <input id="vocabulary-file" type="file" accept=".ttl,text/turtle" @change="onChange" />
    <p v-if="fileName" class="file-name">{{ fileName }}</p>
    <p v-if="checking" class="hint">Checking syntax...</p>
    <p v-if="sizeError" class="field-error" role="alert">{{ UPLOAD_TOO_LARGE }}</p>
    <div v-if="syntaxError" class="field-error" role="alert">
      <p>Syntax errors:</p>
      <ul>
        <li>Line {{ syntaxError.line }}, column {{ syntaxError.column }}: {{ syntaxError.message }}</li>
      </ul>
    </div>
    <p v-if="passed" class="field-ok" role="status">Syntax check passed.</p>
  </div>
</template>
