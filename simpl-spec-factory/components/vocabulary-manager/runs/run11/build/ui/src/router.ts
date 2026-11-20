import { createRouter, createWebHistory } from "vue-router";
import VocabularyList from "./views/VocabularyList.vue";
import VocabularyDetails from "./views/VocabularyDetails.vue";
import VocabularyCreate from "./views/VocabularyCreate.vue";
import VocabularyNewVersion from "./views/VocabularyNewVersion.vue";

/** Routes of D31 and D74. */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/vocabularies" },
    { path: "/vocabularies", component: VocabularyList },
    { path: "/vocabularies/new", component: VocabularyCreate },
    { path: "/vocabularies/:name/versions/new", component: VocabularyNewVersion, props: true },
    { path: "/vocabularies/:name", component: VocabularyDetails, props: true },
    { path: "/:pathMatch(.*)*", redirect: "/vocabularies" },
  ],
});
