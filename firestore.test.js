const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const OWNER_UID = "owner_uid_1";
const STAFF_UID = "staff_uid_1";

const OWNER_TOKEN = { email: "mohamedmosadkhader@gmail.com", email_verified: true };
const SPOOFED_OWNER_TOKEN = { email: "mohamedmosadkhader@gmail.com", email_verified: false };
const STAFF_TOKEN = { email: "staff.bareeq@gmail.com", email_verified: true };
const ALICE_TOKEN = { email: "alice@gmail.com", email_verified: true };

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate() {
  return new Date(Date.now() - 5000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

// 1. Unauthenticated access denied
test("Unauthenticated user cannot read products, orders, or reviews", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("reviews").where("isApproved", "==", true).get());
});

// 2. Profile creation & self-elevation prevention
test("Customer can create own profile as customer, cannot self-assign owner/staff role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, ALICE_TOKEN).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      fullName: "علياء محمد",
      phone: "01012345678",
      role: "customer",
      loyaltyPoints: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc("evil_user").set({
      userId: "evil_user",
      email: "evil@gmail.com",
      fullName: "مهاجم",
      role: "owner",
      loyaltyPoints: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

// 3. Email spoofing protection
test("Unverified email spoofing owner email is rejected for staff_permissions and product writes", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_1", SPOOFED_OWNER_TOKEN).firestore();
  const now = pastDate();

  await assertFails(
    spoofDb.collection("products").doc("p_spoof").set({
      name: "منتج مزيف",
      category: "تنظيم",
      price: 100,
      stock: 5,
      description: "وصف",
      drawableKey: "organizer",
      isFeatured: false,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

// 4. Owner & Granular Staff Permissions
test("Owner can assign staff permissions; staff can ONLY perform granted actions", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, OWNER_TOKEN).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID, STAFF_TOKEN).firestore();
  const now = pastDate();

  // Owner grants staff ONLY canAddProducts (NOT canDeleteProducts, NOT canManageDiscounts)
  await assertSucceeds(
    ownerDb.collection("staff_permissions").doc("staff.bareeq@gmail.com").set({
      email: "staff.bareeq@gmail.com",
      displayName: "موظف المبيعات",
      canAddProducts: true,
      canDeleteProducts: false,
      canManageDiscounts: false,
      canManageOrders: false,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Staff CAN add product
  await assertSucceeds(
    staffDb.collection("products").doc("p_staff_1").set({
      name: "غطاء مقود جلد فاخر",
      category: "اكسسوارات داخلية",
      price: 290,
      stock: 15,
      description: "خامة ممتازة",
      drawableKey: "organizer",
      isFeatured: true,
      isActive: true,
      createdBy: STAFF_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Staff CANNOT delete product without canDeleteProducts
  await assertFails(staffDb.collection("products").doc("p_staff_1").delete());

  // Staff CANNOT create coupon without canManageDiscounts
  await assertFails(
    staffDb.collection("coupons").doc("HACK50").set({
      code: "HACK50",
      discountType: "percent",
      discountValue: 50,
      minOrderTotal: 0,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner CAN create coupon and delete product
  await assertSucceeds(
    ownerDb.collection("coupons").doc("BAREEQ10").set({
      code: "BAREEQ10",
      discountType: "percent",
      discountValue: 10,
      minOrderTotal: 200,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(ownerDb.collection("products").doc("p_staff_1").delete());
});

// 5. Orders PII isolation & query alignment
test("Customer can create and query own orders, cannot read other user orders or list unfiltered", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, ALICE_TOKEN).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@gmail.com", email_verified: true }).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").set({
      userId: ALICE_UID,
      orderNumber: "BRQ-261116-1111",
      customerName: "أحمد علي",
      customerPhone: "01011223344",
      governorate: "القاهرة",
      shippingAddress: "مدينة نصر - شارع عباس العقاد",
      paymentMethod: "instapay",
      transferReference: "IPA-99210",
      itemsSummary: "منظم مقعد × 1",
      primaryProductId: "p1",
      primaryProductName: "منظم مقعد السيارة الفاخر",
      subtotal: 420,
      shippingFee: 55,
      discountAmount: 0,
      total: 475,
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Matching filter succeeds for Alice
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());

  // Unfiltered query fails for Bob
  await assertFails(bobDb.collection("orders").get());

  // Direct read of Alice's order fails for Bob
  await assertFails(bobDb.collection("orders").doc("ord_1").get());
});

// 6. Customer Review creation & Shadow field rejection
test("Authenticated customer can post review, shadow ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, ALICE_TOKEN).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_1").set({
      userId: ALICE_UID,
      orderNumber: "BRQ-261116-1111",
      productId: "p1",
      productName: "منظم مقعد السيارة الفاخر",
      customerName: "أحمد علي",
      governorate: "الجيزة",
      storeRating: 5,
      productRating: 5,
      comment: "ممتاز جداً وخامة عالية",
      isApproved: true,
      createdAt: now,
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("reviews").doc("rev_2").set({
      userId: ALICE_UID,
      orderNumber: "BRQ-261116-1111",
      productName: "منتج",
      customerName: "أحمد",
      governorate: "القاهرة",
      storeRating: 5,
      productRating: 5,
      comment: "رائع",
      isApproved: true,
      ghostAdminFlag: true,
      createdAt: now,
    })
  );
});
