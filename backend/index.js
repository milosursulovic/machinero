import express from "express";
import mongoose from "mongoose";
import dotenv from "dotenv";
import cors from "cors";
import ordersRouter from "./routes/orders.js";
import { startCron } from "./jobs/scheduler.js";

dotenv.config();

const app = express();
const PORT = process.env.PORT;

app.use(express.json());
app.use(cors({ origin: process.env.CORS_ORIGIN?.split(",") || "*" }));

app.get("/", (_req, res) => res.send("Machinero API radi."));
app.use("/api/orders", ordersRouter);

mongoose
  .connect(process.env.MONGO_URI)
  .then(() => {
    console.log("✅ Povezan sa MongoDB");
    app.listen(PORT, () =>
      console.log(`🚀 Backend sluša na http://localhost:${PORT}`)
    );
    startCron();
  })
  .catch((err) => {
    console.error("❌ Greška konekcije na MongoDB:", err.message);
    process.exit(1);
  });
