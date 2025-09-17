import mongoose from "mongoose";
import Order from "../../models/Order.js";
import dotenv from "dotenv";

dotenv.config();

const MONGO_URI = process.env.MONGO_URI;

async function seed() {
  try {
    console.log(MONGO_URI);

    await mongoose.connect(MONGO_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
    });

    const dummyOrders = [];

    for (let i = 1; i <= 30; i++) {
      dummyOrders.push({
        customer: {
          fullName: `Kupac ${i}`,
          phone: `06012345${i}`,
          email: `kupac${i}@mail.com`,
          address: `Ulica ${i}, Grad`,
        },
        order: {
          item: `Proizvod ${i}`,
          quantity: Math.ceil(Math.random() * 5),
          price: Math.floor(Math.random() * 1000) + 500,
          paymentMethod: ["kes", "kartica", "racun", "drugo"][
            Math.floor(Math.random() * 4)
          ],
        },
        logistics: {
          courier: ["DHL", "PostExpress", "FedEx"][
            Math.floor(Math.random() * 3)
          ],
          deliveryMethod: ["standard", "express"][
            Math.floor(Math.random() * 2)
          ],
          status: ["primljena", "u_isporuci", "isporucena", "otkazana"][
            Math.floor(Math.random() * 4)
          ],
          shipDate: new Date(),
          deliveryDate: new Date(Date.now() + 1000 * 60 * 60 * 24 * i),
        },
        note: `Ovo je dummy porudžbina broj ${i}`,
      });
    }

    await Order.insertMany(dummyOrders);

    console.log("✅ Ubaceno 10 dummy porudžbina");
    process.exit(0);
  } catch (err) {
    console.error("❌ Greška:", err);
    process.exit(1);
  }
}

seed();
