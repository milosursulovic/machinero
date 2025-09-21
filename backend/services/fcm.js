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
    channelId = "machinero_default",
    collapseKey,
    dryRun = false,
  } = {}
) {
  const message = {
    topic,
    notification: title || body ? { title, body } : undefined,
    data,
    android: {
      priority: priority.toLowerCase() === "high" ? "high" : "normal",
      collapseKey,
      notification: {
        channelId,
        priority: priority.toUpperCase(),
      },
    },
  };
  return fcm.send(message, dryRun);
}
