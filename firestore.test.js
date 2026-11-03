const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_sat_123";
const BOB_UID = "bob_sat_456";
const CHARLIE_UID = "charlie_sat_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Smith",
      email: "alice@satlimited.com",
      statusMessage: "Senior Solutions Architect",
      isOnline: true,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot forge someone else's profile", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Imposter",
      email: "fake@satlimited.com",
      createdAt: new Date(),
    })
  );
});

test("Chat membership: member can read chat, non-member cannot", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_1").set({
      id: "chat_1",
      type: "direct",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("chats").doc("chat_1").get());

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("chats").doc("chat_1").get());
});

test("Chat messages: member can create message, non-member cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").collection("messages").doc("msg_1").set({
      id: "msg_1",
      chatId: "chat_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hello team SAT!",
      memberIds: [ALICE_UID, BOB_UID],
      status: "sent",
      createdAt: new Date(),
    })
  );

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("chats").doc("chat_1").collection("messages").doc("msg_2").set({
      id: "msg_2",
      chatId: "chat_1",
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Trespassing message",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );
});
