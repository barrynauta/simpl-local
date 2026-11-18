import { createRouter, createWebHistory } from 'vue-router';
import VocabularyList from './views/VocabularyList.vue';
import VocabularyCreate from './views/VocabularyCreate.vue';
import VocabularyDetails from './views/VocabularyDetails.vue';
import VocabularyUpdate from './views/VocabularyUpdate.vue';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/vocabularies' },
    { path: '/vocabularies', name: 'list', component: VocabularyList },
    { path: '/vocabularies/new', name: 'create', component: VocabularyCreate },
    { path: '/vocabularies/:name', name: 'details', component: VocabularyDetails, props: true },
    { path: '/vocabularies/:name/update', name: 'update', component: VocabularyUpdate, props: true },
    { path: '/:pathMatch(.*)*', redirect: '/vocabularies' },
  ],
});
