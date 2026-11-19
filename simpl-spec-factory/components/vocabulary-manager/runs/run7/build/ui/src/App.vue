<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { logout, userName } from './lib/auth'
</script>

<template>
  <header class="app-header">
    <span class="app-title">Vocabulary Manager</span>
    <nav aria-label="Main">
      <RouterLink to="/vocabularies">Vocabularies</RouterLink>
    </nav>
    <span class="user">{{ userName() }}</span>
    <button type="button" class="link-button" @click="logout">Log out</button>
  </header>
  <main id="main">
    <RouterView />
  </main>
</template>
