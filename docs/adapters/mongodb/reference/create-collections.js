/**
 * Bootstrap MongoDB collections for the payments ledger.
 *
 * Usage (mongosh):
 *   mongosh "mongodb://localhost:27017/payments_ledger" --file docs/adapters/mongodb/reference/create-collections.js
 *
 * Requires MongoDB 4.2+ for $jsonSchema validation.
 */
const fs = require("fs");
const path = require("path");

const REF_DIR = __dirname;

function loadJson(filename) {
  return JSON.parse(fs.readFileSync(path.join(REF_DIR, filename), "utf8"));
}

const validators = [
  loadJson("accounts.validator.json"),
  loadJson("payment_transactions.validator.json"),
  loadJson("statement_entries.validator.json"),
];

const indexSpec = loadJson("indexes.json");

for (const spec of validators) {
  const exists = db.getCollectionNames().includes(spec.collection);
  if (exists) {
    print(`Updating validator on existing collection: ${spec.collection}`);
    db.runCommand({
      collMod: spec.collection,
      validator: spec.validator,
      validationLevel: spec.validationLevel,
      validationAction: spec.validationAction,
    });
  } else {
    print(`Creating collection: ${spec.collection}`);
    db.createCollection(spec.collection, {
      validator: spec.validator,
      validationLevel: spec.validationLevel,
      validationAction: spec.validationAction,
    });
  }
}

for (const idx of indexSpec.indexes) {
  if (idx.name === "_id_") continue;
  print(`Ensuring index ${idx.collection}.${idx.name}`);
  db.getCollection(idx.collection).createIndex(idx.keys, {
    ...idx.options,
    name: idx.name,
  });
}

print("MongoDB payments ledger collections ready.");
