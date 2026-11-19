<script setup lang="ts">
import { isAdmin, logout, userName } from "./auth";
import { TEXTS } from "./texts";

const user = userName();
const role = isAdmin() ? "GA_VOCABULARY_ADMIN" : "";
</script>

<template>
  <header class="topbar">
    <span class="brand">Vocabulary Manager</span>
    <nav aria-label="Main">
      <RouterLink to="/vocabularies">{{ TEXTS.menu }}</RouterLink>
    </nav>
    <span class="user" :data-role="role">{{ user }}</span>
    <button type="button" class="secondary" @click="logout">Log out</button>
  </header>
  <main>
    <RouterView />
  </main>
</template>
