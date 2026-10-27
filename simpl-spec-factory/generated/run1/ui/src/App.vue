<script setup lang="ts">
import { session, logout } from './auth'
import { notices, dismiss } from './notify'
</script>

<template>
  <header class="container topbar">
    <nav>
      <ul><li><strong><RouterLink to="/">Vocabulary Management</RouterLink></strong></li></ul>
    </nav>
    <nav v-if="session.user">
      <ul>
        <li>
          <small>
            Signed in as <strong data-testid="username">{{ session.username }}</strong>
            ({{ session.isAdmin ? 'administrator' : session.canRead ? 'viewer' : 'no vocabulary role' }})
          </small>
        </li>
        <li><button class="secondary outline" style="width:auto;margin:0" @click="logout">Sign out</button></li>
      </ul>
    </nav>
  </header>
  <div class="notices" aria-live="polite">
    <div v-for="n in notices" :key="n.id" role="status"
         :class="n.kind === 'success' ? 'success-box' : n.kind === 'error' ? 'error-box' : 'warning'">
      <button class="close" aria-label="Close" @click="dismiss(n.id)">x</button>
      <div>{{ n.text }}</div>
      <ul v-if="n.warnings && n.warnings.length" style="margin:0.4rem 0 0 0">
        <li v-for="(w, i) in n.warnings" :key="i">Warning: {{ w.description }} <small v-if="w.location">({{ w.location }})</small></li>
      </ul>
    </div>
  </div>
  <main class="container">
    <div v-if="session.user && !session.canRead" class="error-box" data-testid="no-role">
      Access denied. Your account has no vocabulary management role (GA_VOCABULARY_VIEWER or GA_VOCABULARY_ADMIN).
    </div>
    <RouterView />
  </main>
</template>
