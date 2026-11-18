<script setup lang="ts">
import { dismiss, notifications } from '../notifications';
</script>

<template>
  <section v-if="notifications.items.length > 0" class="notifications" aria-label="Notifications">
    <div
      v-for="n in notifications.items"
      :key="n.id"
      :class="['notification', n.kind]"
      :role="n.kind === 'error' ? 'alert' : 'status'"
    >
      <p class="message">{{ n.message }}</p>
      <p v-if="n.detail" class="detail">{{ n.detail }}</p>
      <div v-if="n.warnings && n.warnings.length > 0" class="warnings-panel" aria-label="Validation warnings">
        <h3>Validation warnings</h3>
        <ul>
          <li v-for="(w, i) in n.warnings" :key="i">
            <span class="warning-description">{{ w.description }}</span>
            <span class="warning-location">Location: {{ w.location }}</span>
          </li>
        </ul>
      </div>
      <button type="button" class="link-button" @click="dismiss(n.id)">Close</button>
    </div>
  </section>
</template>
