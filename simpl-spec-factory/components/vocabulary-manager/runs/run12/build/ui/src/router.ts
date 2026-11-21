import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
  type RouterHistory,
} from 'vue-router';
import VocabularyList from './views/VocabularyList.vue';
import VocabularyDetails from './views/VocabularyDetails.vue';
import CreateVocabulary from './views/CreateVocabulary.vue';
import NewVersion from './views/NewVersion.vue';
import NotFound from './views/NotFound.vue';

/** Routes of D31 and D74. */
export const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/vocabularies' },
  { path: '/vocabularies', component: VocabularyList },
  { path: '/vocabularies/new', component: CreateVocabulary },
  { path: '/vocabularies/:name/versions/new', component: NewVersion },
  { path: '/vocabularies/:name/versions/:version', component: VocabularyDetails },
  { path: '/vocabularies/:name', component: VocabularyDetails },
  { path: '/:pathMatch(.*)*', component: NotFound },
];

export function makeRouter(history: RouterHistory = createWebHistory()) {
  return createRouter({ history, routes });
}
