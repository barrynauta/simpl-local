import { createRouter, createWebHistory } from 'vue-router';
import SchemaList from './views/SchemaList.vue';
import CreateSchema from './views/CreateSchema.vue';
import SchemaDetails from './views/SchemaDetails.vue';
import NewVersion from './views/NewVersion.vue';

// Routes of D45.
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/schemas' },
    { path: '/schemas', component: SchemaList },
    { path: '/schemas/new', component: CreateSchema },
    { path: '/schemas/:name', component: SchemaDetails, props: true },
    { path: '/schemas/:name/versions/new', component: NewVersion, props: true },
    { path: '/:pathMatch(.*)*', redirect: '/schemas' },
  ],
});
