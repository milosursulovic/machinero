import admin from "firebase-admin";
import fs from "fs";

const serviceAccount = JSON.parse(
  fs.readFileSync(new URL("../firebase-service-account.json", import.meta.url))
);

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}

export const TOPIC_ALL = "orders_all";
export const fcm = admin.messaging();

export async function sendToTopic(
  topic,
  {
    title,
    body,
    data = {},
    priority = "high",
    collapseKey,
    dryRun = false,
  } = {}
) {
  const strData = Object.fromEntries(
    Object.entries(data).map(([k, v]) => [k, v == null ? "" : String(v)])
  );

  const payload = {
    topic,
    data: {
      _title: title ?? "Obaveštenje",
      _body: body ?? "",
      ...strData,
    },
    android: {
      priority: String(priority).toLowerCase() === "high" ? "high" : "normal",
      ...(collapseKey ? { collapseKey } : {}),
    },
  };

  return fcm.send(payload, dryRun);
}
