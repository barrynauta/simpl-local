<script setup lang="ts">
import { session } from "./auth";

const user = session();
</script>

<template>
  <header class="topbar">
    <nav aria-label="Main">
      <RouterLink to="/vocabularies">Vocabularies</RouterLink>
    </nav>
    <div class="user">
      <span>{{ user?.username() }}</span>
      <button type="button" @click="user?.logout()">Log out</button>
    </div>
  </header>
  <main>
    <RouterView />
  </main>
</template>
