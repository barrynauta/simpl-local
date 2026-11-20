<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ApiError,
  createVersion,
  getContent,
  getVocabulary,
  type CreatedVersion,
  type Vocabulary,
} from "../api";
import { isAdmin } from "../auth";
import VocabularyForm from "../components/VocabularyForm.vue";
import { setFlash } from "../flash";
import { TEXT } from "../texts";

/** New version form at /vocabularies/<Name>/versions/new (SIMPL-18172, 18189; D33, D46, D74, D113). */
const props = defineProps<{ name: string }>();
const router = useRouter();
const admin = isAdmin();
const vocabulary = ref<Vocabulary | null>(null);
const loadError = ref("");

onMounted(async () => {
  if (!admin) return;
  try {
    vocabulary.value = await getVocabulary(props.name);
  } catch (error) {
    loadError.value =
      error instanceof ApiError && error.status === 404
        ? TEXT.vocabularyNotFound
        : `${TEXT.genericError} ${error instanceof ApiError ? (error.problem.detail ?? "") : ""}`.trim();
  }
});

function submit(form: FormData): Promise<CreatedVersion> {
  return createVersion(props.name, form);
}

function created(version: CreatedVersion): void {
  setFlash(`Version ${version.version} of ${version.name} was created.`, version.validationWarnings ?? []);
  void router.push({ path: `/vocabularies/${version.name}`, query: { version: version.version } });
}
</script>

<template>
  <section>
    <h1>New version of {{ name }}</h1>
    <p v-if="!admin" role="alert" class="error">{{ TEXT.notAuthorized }}</p>
    <p v-else-if="loadError" role="alert" class="error">{{ loadError }}</p>
    <VocabularyForm
      v-else-if="vocabulary"
      mode="version"
      :initial-description="vocabulary.description"
      :current-file-name="`${name}.ttl (current content)`"
      :current-content="() => getContent(name)"
      :submit="submit"
      @created="created"
      @cancel="router.push('/vocabularies')"
    >
      <p class="hint">
        Submitting creates version {{ vocabulary.version + 1 }} and deprecates every older ACTIVE version.
      </p>
    </VocabularyForm>
  </section>
</template>
