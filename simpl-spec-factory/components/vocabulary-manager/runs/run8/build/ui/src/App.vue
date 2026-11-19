<script setup lang="ts">
import { useApp } from './context';

const app = useApp();
</script>

<template>
  <a class="skip-link" href="#main">Skip to content</a>
  <header class="app-header">
    <span class="brand">Vocabulary Manager</span>
    <nav aria-label="Main">
      <RouterLink to="/vocabularies">Vocabularies</RouterLink>
    </nav>
    <div class="user">
      <span>{{ app.session.username() }}</span>
      <button type="button" @click="app.session.logout()">Log out</button>
    </div>
  </header>
  <main id="main" tabindex="-1">
    <RouterView :key="$route.fullPath.split('?')[0]" />
  </main>
</template>
