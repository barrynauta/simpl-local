import { createRouter, createWebHistory } from 'vue-router'
import { session, login } from './auth'
import ListView from './views/ListView.vue'
import CreateView from './views/CreateView.vue'
import DetailsView from './views/DetailsView.vue'
import VersionView from './views/VersionView.vue'
import NewVersionView from './views/NewVersionView.vue'
import CallbackView from './views/CallbackView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'list', component: ListView },
    { path: '/vocabularies/new', name: 'create', component: CreateView, meta: { admin: true } },
    { path: '/vocabularies/:name', name: 'details', component: DetailsView, props: true },
    { path: '/vocabularies/:name/new-version', name: 'new-version', component: NewVersionView, props: true,
      meta: { admin: true } },
    { path: '/vocabularies/:name/versions/:version', name: 'version', component: VersionView, props: true },
    { path: '/callback', name: 'callback', component: CallbackView, meta: { public: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// SIMPL-18195: unauthenticated users are sent to the identity provider to sign in.
router.beforeEach(async (to) => {
  if (to.meta.public) return true
  if (!session.user || session.user.expired) {
    await login(to.fullPath)
    return false
  }
  return true
})
