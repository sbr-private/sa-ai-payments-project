// Load demo seed into MongoDB
// Usage: mongosh "$MONGODB_URI" --file docs/adapters/mongodb/reference/examples/seed.js
// Requires collections created via reference/create-collections.js

const seed = JSON.parse(cat('docs/adapters/mongodb/reference/examples/seed.json'));

const dbName = db.getName();

print(`Seeding database: ${dbName}`);

db.accounts.deleteMany({});
db.payment_transactions.deleteMany({});
db.statement_entries.deleteMany({});

if (seed.accounts.length) {
  db.accounts.insertMany(seed.accounts);
  print(`Inserted ${seed.accounts.length} accounts`);
}

if (seed.payment_transactions.length) {
  db.payment_transactions.insertMany(seed.payment_transactions);
  print(`Inserted ${seed.payment_transactions.length} payment_transactions`);
}

if (seed.statement_entries.length) {
  db.statement_entries.insertMany(seed.statement_entries);
  print(`Inserted ${seed.statement_entries.length} statement_entries`);
}

print('Demo seed complete. Users are hardcoded in the API — see docs/fixtures/seed-users.json');
