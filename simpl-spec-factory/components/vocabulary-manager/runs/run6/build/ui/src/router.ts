import { createRouter, createWebHistory } from "vue-router";
import VocabularyList from "./views/VocabularyList.vue";
import VocabularyCreate from "./views/VocabularyCreate.vue";
import VocabularyUpdate from "./views/VocabularyUpdate.vue";
import VocabularyDetails from "./views/VocabularyDetails.vue";

/** Screens of decision D31: Vocabulary list and Vocabulary details, plus the two forms. */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/vocabularies" },
    { path: "/vocabularies", component: VocabularyList },
    { path: "/vocabularies/new", component: VocabularyCreate },
    { path: "/vocabularies/:name/update", component: VocabularyUpdate, props: true },
    { path: "/vocabularies/:name", component: VocabularyDetails, props: true },
    { path: "/:pathMatch(.*)*", redirect: "/vocabularies" },
  ],
});
