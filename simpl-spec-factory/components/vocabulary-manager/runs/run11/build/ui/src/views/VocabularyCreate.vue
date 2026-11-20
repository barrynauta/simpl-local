<script setup lang="ts">
import { useRouter } from "vue-router";
import { createVocabulary, type CreatedVersion } from "../api";
import { isAdmin } from "../auth";
import VocabularyForm from "../components/VocabularyForm.vue";
import { setFlash } from "../flash";
import { TEXT } from "../texts";

/** Create form at /vocabularies/new (SIMPL-18169; D18, D32, D72, D74). */
const router = useRouter();
const admin = isAdmin();

function created(version: CreatedVersion): void {
  setFlash(`Version ${version.version} of ${version.name} was created.`, version.validationWarnings ?? []);
  void router.push(`/vocabularies/${version.name}`);
}
</script>

<template>
  <section>
    <h1>New vocabulary</h1>
    <p v-if="!admin" role="alert" class="error">{{ TEXT.notAuthorized }}</p>
    <VocabularyForm
      v-else
      mode="create"
      :submit="createVocabulary"
      @created="created"
      @cancel="router.push('/vocabularies')"
    />
  </section>
</template>
