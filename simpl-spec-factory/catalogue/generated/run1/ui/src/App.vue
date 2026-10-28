<template>
  <header>
    <strong>Simpl-Open Catalogue</strong>
    <span>{{ user }} <button @click="logout">Sign out</button></span>
  </header>
  <main><router-view /></main>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { keycloak } from './auth';

const user = computed(() => (keycloak.tokenParsed as any)?.preferred_username ?? '');
function logout() {
  keycloak.logout({ redirectUri: window.location.origin + '/' });
}
</script>
