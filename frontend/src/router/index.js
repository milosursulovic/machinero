import { createRouter, createWebHistory } from "vue-router";
import OrdersList from "../views/OrdersList.vue";
import OrdersCreate from "../views/OrdersCreate.vue";
import OrdersEdit from "../views/OrdersEdit.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", redirect: "/orders" },
    { path: "/orders", name: "orders", component: OrdersList },
    { path: "/orders/new", name: "orders-new", component: OrdersCreate },
    {
      path: "/orders/:id/edit",
      name: "orders-edit",
      component: OrdersEdit,
      props: true,
    },
  ],
});

export default router;
