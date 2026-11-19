import { createRouter, createWebHistory } from 'vue-router'
import VocabularyDetails from './views/VocabularyDetails.vue'
import VocabularyForm from './views/VocabularyForm.vue'
import VocabularyList from './views/VocabularyList.vue'

/** Routes of D31 and D74. 'new' cannot be a vocabulary name (names start with an upper-case letter). */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/vocabularies' },
    { path: '/vocabularies', component: VocabularyList, meta: { title: 'Vocabularies' } },
    {
      path: '/vocabularies/new',
      component: VocabularyForm,
      props: { mode: 'create' },
      meta: { title: 'New vocabulary' },
    },
    {
      path: '/vocabularies/:name/versions/new',
      component: VocabularyForm,
      props: { mode: 'version' },
      meta: { title: 'New version' },
    },
    { path: '/vocabularies/:name', component: VocabularyDetails, meta: { title: 'Vocabulary details' } },
    { path: '/:pathMatch(.*)*', redirect: '/vocabularies' },
  ],
  scrollBehavior(to) {
    return to.hash ? { el: to.hash } : { top: 0 }
  },
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? 'Vocabularies')} - Vocabulary Manager`
})
