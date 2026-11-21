import { createRouter, createWebHistory, type RouterHistory } from 'vue-router';
import VocabularyList from './views/VocabularyList.vue';
import VocabularyDetails from './views/VocabularyDetails.vue';
import CreateVocabulary from './views/CreateVocabulary.vue';
import NewVersion from './views/NewVersion.vue';

export const routes = [
  { path: '/', redirect: '/vocabularies' },
  { path: '/vocabularies', component: VocabularyList },
  { path: '/vocabularies/new', component: CreateVocabulary },
  { path: '/vocabularies/:name/versions/new', component: NewVersion },
  { path: '/vocabularies/:name/versions/:version(\\d+)', component: VocabularyDetails },
  { path: '/vocabularies/:name', component: VocabularyDetails },
  { path: '/:pathMatch(.*)*', redirect: '/vocabularies' },
];

export function makeRouter(history: RouterHistory = createWebHistory()) {
  return createRouter({ history, routes });
}
