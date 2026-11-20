import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import VocabularyList from './views/VocabularyList.vue';
import VocabularyDetails from './views/VocabularyDetails.vue';
import CreateVocabulary from './views/CreateVocabulary.vue';
import NewVersion from './views/NewVersion.vue';

/** Routes of D31 and D74. 'new' cannot be a vocabulary name, as names start with an upper-case letter. */
export const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/vocabularies' },
  { path: '/vocabularies', name: 'list', component: VocabularyList },
  { path: '/vocabularies/new', name: 'create', component: CreateVocabulary },
  { path: '/vocabularies/:name', name: 'details', component: VocabularyDetails, props: true },
  { path: '/vocabularies/:name/versions/new', name: 'new-version', component: NewVersion, props: true },
  { path: '/:pathMatch(.*)*', redirect: '/vocabularies' },
];

export function createAppRouter() {
  return createRouter({ history: createWebHistory(), routes });
}
