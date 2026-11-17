<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { currentUser, login, logout, rolesOf, loadConfig } from './auth'

const route = useRoute()
const ready = ref(false)
const allowed = ref(false)
const username = ref('')
const party = ref('')

async function check() {
  if (route.path === '/callback') {
    ready.value = true
    return
  }
  const cfg = await loadConfig()
  party.value = cfg.party
  const user = await currentUser()
  if (!user) {
    await login(route.fullPath)
    return
  }
  username.value = (user.profile.preferred_username as string) ?? ''
  allowed.value = rolesOf(user.access_token).includes('contract_manager')
  ready.value = true
}

watch(() => route.path, check, { immediate: false })
router_ready()

async function router_ready() {
  // Wait for the first navigation so that route.path is known.
  await new Promise((r) => setTimeout(r, 0))
  await check()
}
</script>

<template>
  <header class="top">
    <h1>Contract manager <span class="party">{{ party }}</span></h1>
    <div v-if="username" class="user">
      {{ username }} <button type="button" @click="logout()">Sign out</button>
    </div>
  </header>
  <main v-if="ready">
    <router-view v-if="route.path === '/callback' || allowed" />
    <div v-else data-testid="access-denied" class="denied">
      Access denied. You need the role to manage usage contracts.
    </div>
  </main>
  <main v-else><p>Loading...</p></main>
</template>
