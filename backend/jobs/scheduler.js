import cron from "node-cron";
import NotificationLog from "../models/NotificationLog.js";
import Order from "../models/Order.js";
import { sendToTopic, TOPIC_ALL } from "../services/fcm.js";

const STATUS_TARGET = "primljena";
const DAYS_AHEAD = 2;
const FORCE_SEND_FOR_TEST = false;

function belgradeDayBounds(base = new Date()) {
  const tz = "Europe/Belgrade";
  const nowLocal = new Date(base.toLocaleString("en-US", { timeZone: tz }));
  const startLocal = new Date(
    nowLocal.getFullYear(),
    nowLocal.getMonth(),
    nowLocal.getDate(),
    0,
    0,
    0,
    0
  );
  const startUtc = new Date(
    new Date(startLocal.toLocaleString("en-US", { timeZone: "UTC" })).getTime()
  );
  const addDaysUtc = (d, n) => new Date(d.getTime() + n * 86400000);
  return { startUtc, plusDays: (n) => addDaysUtc(startUtc, n) };
}

const srDate = (d) => (d ? new Date(d).toLocaleDateString("sr-RS") : "");
const shortId = (id) => `#${String(id).slice(-6).toUpperCase()}`;

function buildMessage(order, now = new Date()) {
  const dd = order?.logistics?.deliveryDate
    ? new Date(order.logistics.deliveryDate)
    : null;

  let title;
  if (dd) {
    const days = Math.round(
      (new Date(dd).setHours(0, 0, 0, 0) - new Date().setHours(0, 0, 0, 0)) /
        86400000
    );
    if (days < 0) title = "⏰ Porudžbina kasni";
    else if (days === 0) title = "🔔 Rok isporuke je danas";
    else if (days === 1) title = "🔔 Porudžbina stiže sutra";
    else title = `🔔 Porudžbina stiže za ${days} dana`;
  } else {
    title = "ℹ️ Ažuriranje porudžbine";
  }

  const body = [
    `${shortId(order._id)} za ${order?.customer?.fullName || "kupca"}`,
    order?.logistics?.status ? `• status: ${order.logistics.status}` : null,
    dd ? `• rok: ${srDate(dd)}` : null,
  ]
    .filter(Boolean)
    .join("  ");

  const data = {
    type: dd ? (new Date(dd) < now ? "late" : "debug_topic") : "debug_topic",
    orderId: String(order._id),
    customer: order?.customer?.fullName || "",
    deliveryDate: dd ? new Date(dd).toISOString() : "",
    status: order?.logistics?.status || "",
  };

  return { title, body, data };
}

export async function scanAndNotify() {
  const { startUtc, plusDays } = belgradeDayBounds(new Date());
  const from = startUtc;
  const toExclusive = plusDays(DAYS_AHEAD + 1);

  console.log("[scheduler] window UTC:", {
    from: from.toISOString(),
    toExclusive: toExclusive.toISOString(),
  });

  const orders = await Order.find({
    "logistics.status": STATUS_TARGET,
    "logistics.deliveryDate": { $gte: from, $lt: toExclusive },
  }).lean();

  const dayKey = from.toISOString().slice(0, 10);
  console.log(`[scheduler] candidates=${orders.length}`);

  let sent = 0,
    skipped = 0,
    errors = 0;

  for (const o of orders) {
    if (!FORCE_SEND_FOR_TEST) {
      try {
        const res = await NotificationLog.updateOne(
          { orderId: o._id, kind: "due_in_2d", dayKey },
          { $setOnInsert: { createdAt: new Date() } },
          { upsert: true }
        );
        const createdNow =
          (res.upsertedCount && res.upsertedCount > 0) ||
          res.upsertedId != null;

        if (!createdNow) {
          skipped++;
          continue;
        }
      } catch (e) {
        console.error("[scheduler] dedupe error:", e?.message || e);
        errors++;
        continue;
      }
    }

    const msg = buildMessage(o);

    console.log("[scheduler] sending", {
      orderId: o._id,
      title: msg.title,
      body: msg.body,
      data: msg.data,
      forced: FORCE_SEND_FOR_TEST,
    });

    try {
      await sendToTopic(TOPIC_ALL, msg);
      sent++;
    } catch (e) {
      console.error("[scheduler] sendToTopic error:", e?.message || e);
      errors++;
    }
  }

  console.log(
    `[scheduler] done: sent=${sent}, skipped=${skipped}, errors=${errors}`
  );
  return { totalCandidates: orders.length, sent, skipped, errors };
}

export function startCron() {
  const expr = "0 9 * * *";
  cron.schedule(
    expr,
    () => {
      scanAndNotify().catch((e) => console.error("scanAndNotify error:", e));
    },
    { timezone: "Europe/Belgrade" }
  );

  scanAndNotify().then(
    (r) => console.log("[scheduler] initial run:", r),
    (e) => console.error("[scheduler] initial error:", e)
  );
}
