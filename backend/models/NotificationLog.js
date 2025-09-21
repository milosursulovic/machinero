import mongoose from "mongoose";

const NotificationLogSchema = new mongoose.Schema(
  {
    orderId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Order",
      index: true,
      required: true,
    },
    kind: {
      type: String,
      required: true,
      index: true,
      enum: ["upcoming_7", "upcoming_3", "upcoming_1", "late", "due_in_2d"],
    },
    dayKey: {
      type: String,
      required: true,
      index: true,
    },
    sentAt: { type: Date, default: Date.now, index: true },
  },
  { timestamps: true, strict: true }
);

NotificationLogSchema.index(
  { orderId: 1, kind: 1, dayKey: 1 },
  { unique: true, name: "uniq_order_kind_day" }
);

export default mongoose.model("NotificationLog", NotificationLogSchema);
