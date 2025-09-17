import express from "express";
import Order from "../models/Order.js";

const router = express.Router();

// GET /api/orders - lista porudžbina sa filterima + paginacija + upcomingFirst
router.get("/", async (req, res) => {
  try {
    const { status, search } = req.query;

    // paginacija
    const page = Math.max(parseInt(req.query.page || "1", 10), 1);
    const limit = Math.min(
      Math.max(parseInt(req.query.limit || "10", 10), 1),
      100
    );
    const skip = (page - 1) * limit;

    // upcomingFirst: aktivne porudžbine (primljena/u_isporuci) idu pre ostalih,
    // zatim po datumu isporuke uz NULL na kraj
    const upcomingFirst = String(req.query.upcomingFirst || "0") === "1";

    // bazni filter
    const filter = {};
    if (status) filter["logistics.status"] = status;
    if (search) {
      filter.$or = [
        { "customer.fullName": { $regex: search, $options: "i" } },
        { "order.item": { $regex: search, $options: "i" } },
      ];
    }

    // total prebrojavanje
    const total = await Order.countDocuments(filter);

    // sortiranje
    // - ako je upcomingFirst=1: statusPriority(primljena=0, u_isporuci=1, isporucena=2, otkazana=3),
    //   zatim najbliži deliveryDate (NULL na kraj), pa createdAt noviji
    // - u suprotnom, ostaje postojeće: deliveryDate ASC (NULL na kraj), pa createdAt DESC
    const pipeline = [
      { $match: filter },
      {
        $addFields: {
          deliveryDateOrMax: {
            $ifNull: [
              "$logistics.deliveryDate",
              new Date("9999-12-31T23:59:59.999Z"),
            ],
          },
          statusPriority: {
            $switch: {
              branches: [
                { case: { $eq: ["$logistics.status", "primljena"] }, then: 0 },
                { case: { $eq: ["$logistics.status", "u_isporuci"] }, then: 1 },
                { case: { $eq: ["$logistics.status", "isporucena"] }, then: 2 },
                { case: { $eq: ["$logistics.status", "otkazana"] }, then: 3 },
              ],
              default: 9,
            },
          },
        },
      },
      {
        $sort: upcomingFirst
          ? { statusPriority: 1, deliveryDateOrMax: 1, createdAt: -1 }
          : { deliveryDateOrMax: 1, createdAt: -1 },
      },
      { $skip: skip },
      { $limit: limit },
    ];

    const data = await Order.aggregate(pipeline);

    res.json({
      data,
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit) || 1,
    });
  } catch (err) {
    res.status(500).json({ message: "Greška pri čitanju porudžbina." });
  }
});

// GET /api/orders/:id
router.get("/:id", async (req, res) => {
  try {
    const doc = await Order.findById(req.params.id).lean();
    if (!doc) return res.status(404).json({ message: "Nije pronađeno." });
    res.json(doc);
  } catch (err) {
    res.status(500).json({ message: "Greška pri čitanju porudžbine." });
  }
});

// POST /api/orders
router.post("/", async (req, res) => {
  try {
    const created = await Order.create(req.body);
    res.status(201).json(created);
  } catch (err) {
    res.status(500).json({ message: "Greška pri kreiranju." });
  }
});

// PUT /api/orders/:id
router.put("/:id", async (req, res) => {
  try {
    const updated = await Order.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true,
    });
    if (!updated) return res.status(404).json({ message: "Nije pronađeno." });
    res.json(updated);
  } catch (err) {
    res.status(500).json({ message: "Greška pri izmeni." });
  }
});

// DELETE /api/orders/:id
router.delete("/:id", async (req, res) => {
  try {
    const deleted = await Order.findByIdAndDelete(req.params.id);
    if (!deleted) return res.status(404).json({ message: "Nije pronađeno." });
    res.json({ message: "Porudžbina obrisana." });
  } catch (err) {
    res.status(500).json({ message: "Greška pri brisanju." });
  }
});

export default router;
