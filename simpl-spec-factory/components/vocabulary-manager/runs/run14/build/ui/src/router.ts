import { createRouter, createWebHistory, type RouterHistory } from 'vue-router';
import CreateView from './views/CreateView.vue';
import DetailsView from './views/DetailsView.vue';
import ExternalListView from './views/ExternalListView.vue';
import ExternalRegisterView from './views/ExternalRegisterView.vue';
import ListView from './views/ListView.vue';
import NewVersionView from './views/NewVersionView.vue';

/** Routes of D31, D74 and D210. */
export const routes = [
  { path: '/', redirect: '/vocabularies' },
  { path: '/vocabularies', component: ListView },
  { path: '/vocabularies/new', component: CreateView },
  { path: '/vocabularies/:name', component: DetailsView },
  { path: '/vocabularies/:name/versions/new', component: NewVersionView },
  { path: '/vocabularies/:name/versions/:version', component: DetailsView },
  { path: '/external-vocabularies', component: ExternalListView },
  { path: '/external-vocabularies/new', component: ExternalRegisterView },
  { path: '/:pathMatch(.*)*', redirect: '/vocabularies' },
];

export function makeRouter(history: RouterHistory = createWebHistory()) {
  return createRouter({ history, routes });
}
