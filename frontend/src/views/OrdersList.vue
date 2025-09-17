<script setup>
import { onMounted, ref, watch, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { api } from "../services/api";

const router = useRouter();
const route = useRoute();

const loading = ref(false);
const error = ref("");
const orders = ref([]);

const search = ref("");
const status = ref("");

const upcomingFirst = ref(true);

const page = ref(1);
const limit = ref(10);
const total = ref(0);
const totalPages = computed(() =>
  Math.max(Math.ceil(total.value / limit.value) || 1, 1)
);

const formatDate = (d) => (d ? new Date(d).toLocaleDateString("sr-RS") : "—");
const formatPrice = (p) =>
  typeof p === "number"
    ? p.toLocaleString("sr-RS", { style: "currency", currency: "RSD" })
    : "—";

const statusBadge = (s) => {
  const map = {
    primljena: "bg-slate-200 text-slate-800",
    u_isporuci: "bg-amber-200 text-amber-900",
    isporucena: "bg-green-200 text-green-900",
    otkazana: "bg-rose-200 text-rose-900",
  };
  return map[s] || "bg-slate-100 text-slate-700";
};

const fetchOrders = async () => {
  loading.value = true;
  error.value = "";
  try {
    const params = {
      search: search.value || undefined,
      status: status.value || undefined,
      page: page.value,
      limit: limit.value,
      upcomingFirst: upcomingFirst.value ? 1 : 0,
    };
    const { data } = await api.get("/orders", { params });
    orders.value = data?.data || [];
    total.value = data?.total || 0;
  } catch {
    error.value = "Greška pri učitavanju porudžbina.";
  } finally {
    loading.value = false;
  }
};

const parseBool = (v) => v === true || v === "true" || v === "1" || v === 1;

const syncFromRoute = () => {
  const q = route.query;
  search.value = typeof q.search === "string" ? q.search : "";
  status.value = typeof q.status === "string" ? q.status : "";
  page.value = q.page ? Math.max(parseInt(q.page, 10) || 1, 1) : 1;
  limit.value = q.limit
    ? Math.min(Math.max(parseInt(q.limit, 10) || 10, 1), 100)
    : 10;
  upcomingFirst.value =
    q.upcomingFirst != null ? parseBool(q.upcomingFirst) : true;
};

const syncToRoute = () => {
  const q = {
    search: search.value || undefined,
    status: status.value || undefined,
    page: page.value > 1 ? String(page.value) : "1",
    limit: String(limit.value),
    upcomingFirst: upcomingFirst.value ? "1" : "0",
  };
  router.replace({ query: q });
};

let searchDebounce;
watch(search, () => {
  page.value = 1;
  clearTimeout(searchDebounce);
  searchDebounce = setTimeout(syncToRoute, 300);
});

watch([status, upcomingFirst, limit], () => {
  page.value = 1;
  syncToRoute();
});

watch(page, () => {
  syncToRoute();
});

watch(
  () => route.query,
  () => {
    syncFromRoute();
    fetchOrders();
  },
  { deep: true }
);

onMounted(() => {
  syncFromRoute();
  fetchOrders();
});

const goEdit = (id) => router.push(`/orders/${id}/edit`);

const deleteOrder = async (id) => {
  if (!confirm("Obrisati porudžbinu?")) return;
  try {
    await api.delete(`/orders/${id}`);
    if (orders.value.length === 1 && page.value > 1) {
      page.value -= 1;
    } else {
      fetchOrders();
    }
  } catch {
    alert("Greška pri brisanju.");
  }
};

const showingFrom = computed(() =>
  total.value === 0 ? 0 : (page.value - 1) * limit.value + 1
);
const showingTo = computed(() =>
  Math.min(page.value * limit.value, total.value)
);
</script>

<template>
  <div>
    <h2 class="text-lg font-medium mb-4">Lista porudžbina</h2>

    <div class="flex flex-wrap items-center gap-3 mb-4">
      <input
        v-model="search"
        placeholder="Pretraga po imenu kupca ili artiklu"
        class="border rounded-lg px-3 py-2"
      />
      <select v-model="status" class="border rounded-lg px-3 py-2">
        <option value="">Svi statusi</option>
        <option value="primljena">Primljena</option>
        <option value="u_isporuci">U isporuci</option>
        <option value="isporucena">Isporučena</option>
        <option value="otkazana">Otkazana</option>
      </select>

      <label class="inline-flex items-center gap-2 ml-auto">
        <input type="checkbox" v-model="upcomingFirst" />
        <span class="text-sm text-slate-700">Najskorije isporuke</span>
      </label>

      <label class="inline-flex items-center gap-2">
        <span class="text-sm text-slate-600">Po strani:</span>
        <select v-model.number="limit" class="border rounded-lg px-2 py-1">
          <option :value="5">5</option>
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </label>
    </div>

    <div v-if="loading" class="text-slate-500">Učitavanje…</div>
    <div v-else-if="error" class="text-rose-600">{{ error }}</div>

    <div v-else>
      <div class="grid gap-3">
        <div
          v-for="o in orders"
          :key="o._id"
          class="bg-white border rounded-xl p-4 shadow-sm"
        >
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div class="font-semibold text-slate-800">
                {{ o.customer?.fullName || "Nepoznat kupac" }}
              </div>
              <div class="text-sm text-slate-600">
                {{ o.customer?.phone }} • {{ o.customer?.email }}
              </div>
            </div>
            <div class="flex items-center gap-2">
              <span
                class="text-sm px-2 py-1 rounded-lg"
                :class="statusBadge(o.logistics?.status)"
              >
                {{ o.logistics?.status || "—" }}
              </span>
              <button
                @click="goEdit(o._id)"
                class="text-blue-600 hover:underline text-sm"
              >
                Izmeni
              </button>
              <button
                @click="deleteOrder(o._id)"
                class="text-rose-600 hover:underline text-sm"
              >
                Obriši
              </button>
            </div>
          </div>

          <div class="mt-3 grid sm:grid-cols-2 gap-2 text-sm text-slate-700">
            <div>
              <div>
                <span class="text-slate-500">Artikal:</span> {{ o.order?.item }}
              </div>
              <div>
                <span class="text-slate-500">Količina:</span>
                {{ o.order?.quantity }}
              </div>
              <div>
                <span class="text-slate-500">Cena:</span>
                {{ formatPrice(o.order?.price) }}
              </div>
              <div>
                <span class="text-slate-500">Plaćanje:</span>
                {{ o.order?.paymentMethod }}
              </div>
            </div>
            <div>
              <div>
                <span class="text-slate-500">Kurir / način:</span>
                {{ o.logistics?.courier }} / {{ o.logistics?.deliveryMethod }}
              </div>
              <div>
                <span class="text-slate-500">Datum slanja:</span>
                {{ formatDate(o.logistics?.shipDate) }}
              </div>
              <div>
                <span class="text-slate-500">Datum isporuke:</span>
                {{ formatDate(o.logistics?.deliveryDate) }}
              </div>
            </div>
          </div>

          <div v-if="o.note" class="mt-2 text-sm text-slate-600 border-t pt-2">
            <span class="text-slate-500">Napomena:</span> {{ o.note }}
          </div>
        </div>

        <div v-if="orders.length === 0" class="text-slate-500">
          Nema porudžbina.
        </div>
      </div>

      <div class="mt-4 flex flex-wrap items-center gap-3">
        <div class="text-sm text-slate-600">
          Prikaz {{ showingFrom }}–{{ showingTo }} od {{ total }} porudžbina
        </div>
        <div class="ml-auto flex items-center gap-2">
          <button
            class="px-3 py-2 border rounded-lg disabled:opacity-50"
            :disabled="page <= 1"
            @click="page--"
          >
            Prethodna
          </button>
          <span class="text-sm">Strana {{ page }} / {{ totalPages }}</span>
          <button
            class="px-3 py-2 border rounded-lg disabled:opacity-50"
            :disabled="page >= totalPages"
            @click="page++"
          >
            Sledeća
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
