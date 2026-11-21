<script setup lang="ts">
import { getSession } from './auth';

const session = getSession();
</script>

<template>
  <div class="min-h-screen bg-white text-slate-900">
    <header class="border-b border-slate-300 bg-slate-50">
      <div class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
        <span class="text-lg font-semibold">Vocabulary Manager</span>
        <nav aria-label="Main">
          <RouterLink to="/vocabularies" class="text-blue-800 underline">Vocabularies</RouterLink>
        </nav>
        <div v-if="session" class="flex items-center gap-3 text-sm">
          <span>{{ session.username() }}</span>
          <button
            type="button"
            class="rounded border border-slate-500 px-2 py-1"
            @click="session.logout()"
          >
            Log out
          </button>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-7xl px-4 py-6">
      <RouterView :key="$route.fullPath" />
    </main>
  </div>
</template>
