<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../services/api';

const route = useRoute();
const router = useRouter();

const id = route.params.id;
const loading = ref(false);
const saving = ref(false);
const error = ref('');

const form = reactive({
    customer: { fullName: '', phone: '', email: '', address: '' },
    order: { item: '', quantity: 1, price: 0, paymentMethod: 'kes' },
    logistics: { courier: '', deliveryMethod: '', status: 'primljena', shipDate: '', deliveryDate: '' },
    note: ''
});

const fetchOne = async () => {
    loading.value = true; error.value = '';
    try {
        const { data } = await api.get(`/orders/${id}`);
        // mapiranje u formu (konverzija datuma u yyyy-mm-dd za <input type="date">)
        Object.assign(form, data);
        const toYMD = (v) => v ? new Date(v).toISOString().slice(0, 10) : '';
        form.logistics.shipDate = toYMD(data?.logistics?.shipDate);
        form.logistics.deliveryDate = toYMD(data?.logistics?.deliveryDate);
    } catch {
        error.value = 'Greška pri učitavanju porudžbine.';
    } finally {
        loading.value = false;
    }
};

onMounted(fetchOne);

const save = async () => {
    saving.value = true; error.value = '';
    try {
        const payload = JSON.parse(JSON.stringify(form));
        if (payload.logistics.shipDate) payload.logistics.shipDate = new Date(payload.logistics.shipDate);
        if (payload.logistics.deliveryDate) payload.logistics.deliveryDate = new Date(payload.logistics.deliveryDate);
        await api.put(`/orders/${id}`, payload);
        router.push('/orders');
    } catch (e) {
        error.value = e?.response?.data?.message || 'Greška pri izmeni porudžbine.';
    } finally {
        saving.value = false;
    }
};

const removeOrder = async () => {
    if (!confirm('Obrisati porudžbinu?')) return;
    try {
        await api.delete(`/orders/${id}`);
        router.push('/orders');
    } catch {
        alert('Greška pri brisanju.');
    }
};
</script>

<template>
    <div class="max-w-3xl">
        <h2 class="text-lg font-medium mb-4">Izmena porudžbine</h2>

        <div v-if="loading" class="text-slate-500">Učitavanje…</div>
        <div v-else>
            <form @submit.prevent="save" class="bg-white border rounded-xl p-5 shadow-sm space-y-6">
                <!-- Kupac -->
                <section>
                    <h3 class="font-semibold text-slate-800 mb-3">Podaci o kupcu</h3>
                    <div class="grid sm:grid-cols-2 gap-3">
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Ime i prezime *</label>
                            <input v-model="form.customer.fullName" required
                                class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Telefon *</label>
                            <input v-model="form.customer.phone" required class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Email</label>
                            <input type="email" v-model="form.customer.email"
                                class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div class="sm:col-span-2">
                            <label class="block text-sm text-slate-600 mb-1">Adresa</label>
                            <input v-model="form.customer.address" class="w-full border rounded-lg px-3 py-2" />
                        </div>
                    </div>
                </section>

                <!-- Porudžbina -->
                <section>
                    <h3 class="font-semibold text-slate-800 mb-3">Podaci o porudžbini</h3>
                    <div class="grid sm:grid-cols-4 gap-3">
                        <div class="sm:col-span-2">
                            <label class="block text-sm text-slate-600 mb-1">Artikal *</label>
                            <input v-model="form.order.item" required class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Količina *</label>
                            <input type="number" min="1" v-model.number="form.order.quantity" required
                                class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Cena (RSD) *</label>
                            <input type="number" step="0.01" min="0" v-model.number="form.order.price" required
                                class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Način plaćanja</label>
                            <select v-model="form.order.paymentMethod" class="w-full border rounded-lg px-3 py-2">
                                <option value="kes">Keš</option>
                                <option value="kartica">Kartica</option>
                                <option value="racun">Račun</option>
                                <option value="drugo">Drugo</option>
                            </select>
                        </div>
                    </div>
                </section>

                <!-- Logistika -->
                <section>
                    <h3 class="font-semibold text-slate-800 mb-3">Logistika</h3>
                    <div class="grid sm:grid-cols-4 gap-3">
                        <div class="sm:col-span-2">
                            <label class="block text-sm text-slate-600 mb-1">Kurir</label>
                            <input v-model="form.logistics.courier" class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div class="sm:col-span-2">
                            <label class="block text-sm text-slate-600 mb-1">Način isporuke</label>
                            <input v-model="form.logistics.deliveryMethod" class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Status</label>
                            <select v-model="form.logistics.status" class="w-full border rounded-lg px-3 py-2">
                                <option value="primljena">Primljena</option>
                                <option value="u_isporuci">U isporuci</option>
                                <option value="isporucena">Isporučena</option>
                                <option value="otkazana">Otkazana</option>
                            </select>
                        </div>
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Datum slanja</label>
                            <input type="date" v-model="form.logistics.shipDate"
                                class="w-full border rounded-lg px-3 py-2" />
                        </div>
                        <div>
                            <label class="block text-sm text-slate-600 mb-1">Datum isporuke</label>
                            <input type="date" v-model="form.logistics.deliveryDate"
                                class="w-full border rounded-lg px-3 py-2" />
                        </div>
                    </div>
                </section>

                <!-- Napomena -->
                <section>
                    <h3 class="font-semibold text-slate-800 mb-3">Napomena</h3>
                    <textarea v-model="form.note" rows="3" class="w-full border rounded-lg px-3 py-2"></textarea>
                </section>

                <div class="flex items-center gap-3">
                    <button :disabled="saving"
                        class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-60">
                        {{ saving ? 'Čuvanje…' : 'Sačuvaj izmene' }}
                    </button>
                    <button type="button" @click="removeOrder" class="text-rose-600 hover:underline">
                        Obriši porudžbinu
                    </button>
                    <span v-if="error" class="text-rose-600 text-sm">{{ error }}</span>
                </div>
            </form>
        </div>
    </div>
</template>
