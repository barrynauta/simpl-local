<script setup lang="ts">
import { hasRole, keycloak, username } from './auth'
import { config } from './config'

const allowed = hasRole('contract_manager')
function logout() {
  keycloak.logout({ redirectUri: window.location.origin + '/' })
}
</script>

<template>
  <header class="top">
    <span class="brand">Contract manager</span>
    <span class="party">{{ config.partyName }} ({{ config.partyRole === 'PROVIDER' ? 'Provider' : 'Consumer' }})</span>
    <span class="user">{{ username() }}</span>
    <button type="button" class="link" @click="logout">Sign out</button>
  </header>
  <main>
    <div v-if="!allowed" data-testid="access-denied" class="denied">
      You do not have a role that allows the management of usage contracts.
    </div>
    <router-view v-else />
  </main>
</template>
