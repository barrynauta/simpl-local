<template>
  <div>
    <label v-if="!federated">
      Schema
      <select data-testid="schema-select" v-model="schemaId">
        <option value="">Choose a schema</option>
        <option v-for="s in schemas" :key="s.schemaId" :value="s.schemaId">{{ s.title }}</option>
      </select>
    </label>
    <p v-else>Federated search uses the search schema {{ schemaId }}.</p>
    <p v-if="error" class="error">{{ error }}</p>
    <fieldset v-for="s in sections" :key="s.nodeType">
      <legend>{{ s.nodeType }}</legend>
      <div class="row">
        <template v-for="f in s.fields" :key="f.property">
          <label v-if="!f.numeric">
            {{ f.property }}
            <input type="text" :data-testid="`adv-field-${s.nodeLocalName}-${f.property}`" v-model="values[key(s, f)].text" />
          </label>
          <div v-else class="row">
            <label>
              {{ f.property }} operator
              <select :data-testid="`adv-field-${s.nodeLocalName}-${f.property}-op1`" v-model="values[key(s, f)].op1">
                <option v-for="o in ops" :key="o" :value="o">{{ o }}</option>
              </select>
            </label>
            <label>
              value
              <input type="text" inputmode="decimal" :data-testid="`adv-field-${s.nodeLocalName}-${f.property}-value1`" v-model="values[key(s, f)].value1" />
            </label>
            <label>
              second operator
              <select :data-testid="`adv-field-${s.nodeLocalName}-${f.property}-op2`" v-model="values[key(s, f)].op2">
                <option value=""></option>
                <option v-for="o in ops" :key="o" :value="o">{{ o }}</option>
              </select>
            </label>
            <label>
              second value
              <input type="text" inputmode="decimal" :data-testid="`adv-field-${s.nodeLocalName}-${f.property}-value2`" v-model="values[key(s, f)].value2" />
            </label>
          </div>
        </template>
      </div>
    </fieldset>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { getJson, getText, type SchemaInfo } from '../api';
import { buildCriteria, buildForm, emptyValue, type FieldValue, type FormField, type FormSection } from '../shapeForm';

const props = defineProps<{ federated: boolean }>();
const emit = defineEmits<{ (e: 'criteria', v: Record<string, unknown>): void }>();
const ops = ['=', '>', '>=', '<', '<='];
const schemas = ref<SchemaInfo[]>([]);
const schemaId = ref('');
const sections = ref<FormSection[]>([]);
const values = reactive<Record<string, FieldValue>>({});
const error = ref('');

const key = (s: FormSection, f: FormField) => `${s.nodeLocalName}-${f.property}`;

async function loadSchema(id: string) {
  sections.value = [];
  Object.keys(values).forEach((k) => delete values[k]);
  if (!id) return;
  try {
    const built = buildForm(await getText(`/api/v2/schemas/${encodeURIComponent(id)}/content`));
    built.forEach((s) => s.fields.forEach((f) => { values[key(s, f)] = emptyValue(); }));
    sections.value = built;
  } catch (e: any) {
    error.value = `The schema could not be loaded: ${e?.message ?? e}`;
  }
}

// D74, SIMPL-35874: only the own catalogue: pick a RESOURCE schema; any federated catalogue: the SEARCH schema.
async function rebuild() {
  error.value = '';
  schemaId.value = '';
  sections.value = [];
  schemas.value = [];
  Object.keys(values).forEach((k) => delete values[k]);
  try {
    if (props.federated) {
      const res = await getJson<{ schemas: SchemaInfo[] }>('/api/v2/schemas?schemaType=SEARCH');
      schemaId.value = res.schemas[0]?.schemaId ?? '';
    } else {
      const res = await getJson<{ schemas: SchemaInfo[] }>('/api/v2/schemas');
      schemas.value = res.schemas;
    }
  } catch (e: any) {
    error.value = `The schema list could not be loaded: ${e?.message ?? e}`;
  }
  await loadSchema(schemaId.value);
}

watch(() => props.federated, rebuild, { immediate: true });
watch(schemaId, (id, old) => { if (!props.federated && id !== old) loadSchema(id); });
watch(values, () => emit('criteria', buildCriteria(sections.value, values)), { deep: true });
watch(sections, () => emit('criteria', buildCriteria(sections.value, values)));
</script>
