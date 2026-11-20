<script setup lang="ts">
import { session } from './auth';
import { TEXT } from './texts';

const user = session();
</script>

<template>
  <header class="top">
    <nav aria-label="Main">
      <RouterLink to="/vocabularies" class="brand">{{ TEXT.menu }}</RouterLink>
    </nav>
    <div class="user">
      <span>{{ user.username() }}</span>
      <button type="button" class="secondary" @click="user.logout()">Log out</button>
    </div>
  </header>
  <main>
    <RouterView />
  </main>
</template>
