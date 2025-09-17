import mongoose from "mongoose";

const CustomerSchema = new mongoose.Schema(
  {
    fullName: { type: String, required: true, trim: true },
    phone: { type: String, required: true, trim: true },
    email: { type: String, trim: true },
    address: { type: String, trim: true },
  },
  { _id: false }
);

const OrderDataSchema = new mongoose.Schema(
  {
    item: { type: String, required: true, trim: true },
    quantity: { type: Number, required: true, min: 1 },
    price: { type: Number, required: true, min: 0 },
    paymentMethod: {
      type: String,
      enum: ["kes", "kartica", "racun", "drugo"],
      default: "kes",
    },
  },
  { _id: false }
);

const LogisticsSchema = new mongoose.Schema(
  {
    courier: { type: String, trim: true },
    deliveryMethod: { type: String, trim: true },
    status: {
      type: String,
      enum: ["primljena", "u_isporuci", "isporucena", "otkazana"],
      default: "primljena",
    },
    shipDate: { type: Date },
    deliveryDate: { type: Date },
  },
  { _id: false }
);

const OrderSchema = new mongoose.Schema(
  {
    customer: { type: CustomerSchema, required: true },
    order: { type: OrderDataSchema, required: true },
    logistics: { type: LogisticsSchema, default: {} },
    note: { type: String, trim: true },
  },
  { timestamps: true }
);

// korisni indeksi za filter/search
OrderSchema.index({ "logistics.status": 1, "logistics.deliveryDate": 1 });
OrderSchema.index({ "customer.fullName": "text", "order.item": "text" });

export default mongoose.model("Order", OrderSchema);
