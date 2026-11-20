import { createRouter, createMemoryHistory, type Router } from "vue-router";
import { defineComponent } from "vue";

const Empty = defineComponent({ template: "<div />" });

export function testRouter(): Router {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/:pathMatch(.*)*", component: Empty }],
  });
}

export const flush = () => new Promise((resolve) => setTimeout(resolve, 0));
