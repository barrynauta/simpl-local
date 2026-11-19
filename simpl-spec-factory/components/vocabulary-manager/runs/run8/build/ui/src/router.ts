import { createRouter, createWebHistory, type Router } from 'vue-router';
import VocabularyDetails from './views/VocabularyDetails.vue';
import VocabularyForm from './views/VocabularyForm.vue';
import VocabularyList from './views/VocabularyList.vue';

/** Routes of D31 and D74. 'new' cannot be a vocabulary name (names start with an upper-case letter). */
export function createAppRouter(): Router {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', redirect: '/vocabularies' },
      { path: '/vocabularies', name: 'list', component: VocabularyList },
      { path: '/vocabularies/new', name: 'new-vocabulary', component: VocabularyForm },
      { path: '/vocabularies/:name/versions/new', name: 'new-version', component: VocabularyForm },
      { path: '/vocabularies/:name', name: 'details', component: VocabularyDetails },
      { path: '/:pathMatch(.*)*', redirect: '/vocabularies' },
    ],
  });
}
