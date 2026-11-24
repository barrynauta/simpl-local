<script setup lang="ts">
import { logout, session } from './lib/auth';
</script>

<template>
  <div class="min-h-screen">
    <header class="border-b border-gray-300 bg-gray-50">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <p class="text-lg font-semibold">Vocabulary Manager</p>
        <nav aria-label="Main">
          <ul class="flex gap-4">
            <li><RouterLink to="/vocabularies">Vocabularies</RouterLink></li>
            <li><RouterLink to="/external-vocabularies">External vocabularies</RouterLink></li>
          </ul>
        </nav>
        <div class="flex items-center gap-3 text-sm">
          <span>{{ session.username }}</span>
          <button type="button" class="btn-secondary" @click="logout">Log out</button>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-6xl px-4 py-6">
      <RouterView :key="$route.fullPath.split('/versions/')[0]" />
    </main>
  </div>
</template>
