<script setup lang="ts">
import { logout, session } from './auth';
</script>

<template>
  <header class="bg-blue-900 text-white">
    <div class="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3">
      <p class="text-lg font-semibold">Vocabulary Manager</p>
      <nav aria-label="Main">
        <RouterLink to="/vocabularies" class="text-white underline">Vocabularies</RouterLink>
      </nav>
      <div class="flex items-center gap-3 text-sm">
        <span>{{ session.username }}</span>
        <button
          type="button"
          class="rounded border border-white px-2 py-1 hover:bg-blue-800"
          @click="logout"
        >
          Log out
        </button>
      </div>
    </div>
  </header>
  <main class="mx-auto max-w-7xl px-4 py-6 text-gray-900">
    <RouterView :key="$route.path.endsWith('/versions/new') ? $route.fullPath : undefined" />
  </main>
</template>
