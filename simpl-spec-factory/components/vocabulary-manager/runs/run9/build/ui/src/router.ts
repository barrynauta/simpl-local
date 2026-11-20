import { createRouter, createWebHistory } from 'vue-router';
import VocabularyList from './views/VocabularyList.vue';
import VocabularyDetails from './views/VocabularyDetails.vue';
import VocabularyForm from './views/VocabularyForm.vue';

/** Routes of D31 and D74. */
export const routes = [
  { path: '/', redirect: '/vocabularies' },
  { path: '/vocabularies', component: VocabularyList },
  { path: '/vocabularies/new', component: VocabularyForm, props: { mode: 'create' } },
  {
    path: '/vocabularies/:name/versions/new',
    component: VocabularyForm,
    props: (route: { params: { name: string } }) => ({ mode: 'version', name: route.params.name }),
  },
  {
    path: '/vocabularies/:name',
    component: VocabularyDetails,
    props: (route: { params: { name: string } }) => ({ name: route.params.name }),
  },
];

export const router = createRouter({ history: createWebHistory(), routes });
