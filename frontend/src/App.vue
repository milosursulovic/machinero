<template>
  <div>
    <header class="bg-white border-b">
      <div
        class="max-w-5xl mx-auto px-4 py-4 flex items-center justify-between"
      >
        <h1 class="text-xl font-semibold text-slate-800">CNC Orders</h1>
        <nav class="flex gap-2">
          <RouterLink
            class="px-3 py-2 rounded-lg hover:bg-slate-100"
            to="/orders"
            >Porudžbine</RouterLink
          >
          <RouterLink
            class="px-3 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700"
            to="/orders/new"
            >Dodaj porudžbinu</RouterLink
          >
        </nav>
      </div>
    </header>

    <main class="max-w-5xl mx-auto px-4 py-6">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterView } from "vue-router";
</script>