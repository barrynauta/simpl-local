<script setup lang="ts">
import { notification, clear } from './notify';
import { username, logout } from './auth';
</script>

<template>
  <header class="topbar">
    <router-link to="/schemas" class="brand">Schema Management</router-link>
    <span class="spacer"></span>
    <span class="user" data-testid="current-user">{{ username() }}</span>
    <button type="button" class="link" data-testid="logout-button" @click="logout">Sign out</button>
  </header>
  <div class="notifications">
    <div v-if="notification.kind === 'success'" class="note success" role="status" data-testid="notification-success">
      <span>{{ notification.text }}</span>
      <button type="button" class="close" aria-label="Close" @click="clear">x</button>
    </div>
    <div v-if="notification.kind === 'error'" class="note error" role="alert" data-testid="notification-error">
      <span>{{ notification.text }}</span>
      <button type="button" class="close" aria-label="Close" @click="clear">x</button>
    </div>
  </div>
  <main class="content">
    <router-view :key="$route.fullPath" />
  </main>
</template>
