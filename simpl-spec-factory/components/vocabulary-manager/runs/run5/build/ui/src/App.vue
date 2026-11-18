<script setup lang="ts">
import { logout, userName } from './lib/auth';
</script>

<template>
  <header class="topbar">
    <span class="brand">Vocabulary Manager</span>
    <nav aria-label="Main menu">
      <RouterLink to="/vocabularies">Vocabularies</RouterLink>
    </nav>
    <span class="user">
      {{ userName() }}
      <button type="button" class="link" @click="logout()">Log out</button>
    </span>
  </header>
  <main class="content">
    <RouterView />
  </main>
</template>
