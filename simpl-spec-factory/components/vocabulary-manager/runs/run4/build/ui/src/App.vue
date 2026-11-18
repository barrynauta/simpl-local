<script setup lang="ts">
import NotificationArea from './components/NotificationArea.vue';
import { logout, userName } from './auth';
</script>

<template>
  <header class="app-header">
    <router-link to="/vocabularies" class="brand">Vocabulary Manager</router-link>
    <span class="user">{{ userName() }}</span>
    <button type="button" class="link-button" @click="logout">Sign out</button>
  </header>
  <main>
    <NotificationArea />
    <router-view :key="$route.fullPath" />
  </main>
</template>
