<script setup lang="ts">
import { computed } from 'vue';
import { session } from './auth';

const userName = computed(() => session().userName());

function signOut(): void {
  session().logout();
}
</script>

<template>
  <header class="top">
    <span class="brand">Simpl-Open Vocabulary Manager</span>
    <nav aria-label="Main">
      <RouterLink to="/vocabularies">Vocabularies</RouterLink>
    </nav>
    <span class="user">{{ userName }}</span>
    <button type="button" class="secondary" @click="signOut">Sign out</button>
  </header>
  <main>
    <RouterView />
  </main>
</template>
