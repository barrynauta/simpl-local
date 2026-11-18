import { createRouter, createWebHistory } from 'vue-router';
import VocabularyList from './views/VocabularyList.vue';
import VocabularyDetails from './views/VocabularyDetails.vue';
import CreateVocabulary from './views/CreateVocabulary.vue';
import NewVersion from './views/NewVersion.vue';

/** Screens of D31: Vocabulary list and Vocabulary details, plus the two forms. */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/vocabularies' },
    { path: '/vocabularies', name: 'list', component: VocabularyList },
    { path: '/vocabularies/new', name: 'create', component: CreateVocabulary },
    { path: '/vocabularies/:name', name: 'details', component: VocabularyDetails, props: true },
    { path: '/vocabularies/:name/update', name: 'update', component: NewVersion, props: true },
    { path: '/:pathMatch(.*)*', redirect: '/vocabularies' },
  ],
});
