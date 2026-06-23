#!/usr/bin/env node

import { ApiClient } from "./lib/client.mjs";
import { runAllScenarios, SCENARIOS } from "./lib/scenarios.mjs";

function parseArgs(argv) {
  const options = {
    baseUrl: process.env.BASE_URL ?? "http://localhost:8080/v1",
    demoUser: process.env.DEMO_USER ?? "benchmark@demo",
    adapter: process.env.DATABASE ?? "mongo",
    filter: [],
    list: false,
    skipReady: false,
  };

  for (let i = 2; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === "--list") {
      options.list = true;
    } else if (arg === "--skip-ready") {
      options.skipReady = true;
    } else if (arg.startsWith("--base-url=")) {
      options.baseUrl = arg.slice("--base-url=".length);
    } else if (arg.startsWith("--adapter=")) {
      options.adapter = arg.slice("--adapter=".length);
    } else if (arg.startsWith("--scenario=")) {
      options.filter.push(...arg.slice("--scenario=".length).split(","));
    } else if (arg === "--help" || arg === "-h") {
      printHelp();
      process.exit(0);
    } else {
      console.error(`Unknown argument: ${arg}`);
      printHelp();
      process.exit(2);
    }
  }

  return options;
}

function printHelp() {
  console.log(`Usage: node run.mjs [options]

HTTP acceptance scenario runner (SC-001–SC-015).

Options:
  --base-url=URL     API base URL (default: http://localhost:8080/v1)
  --adapter=NAME     Adapter label for reporting (default: mongo)
  --scenario=ID,...  Run specific scenarios (e.g. SC-001,SC-002)
  --list             List scenarios and exit
  --skip-ready       Skip GET /ready preflight
  --help             Show this help

Environment:
  BASE_URL, DEMO_USER, DATABASE

Prerequisites:
  - Payments API running with ENABLE_TEST_HELPERS=true
  - MongoDB or PostgreSQL reachable (per adapter)
`);
}

async function main() {
  const options = parseArgs(process.argv);

  if (options.list) {
    for (const [id, scenario] of Object.entries(SCENARIOS)) {
      console.log(`${id}  ${scenario.title}`);
    }
    return;
  }

  const client = new ApiClient({
    baseUrl: options.baseUrl,
    demoUser: options.demoUser,
  });

  if (!options.skipReady) {
    const ready = await client.ready();
    if (ready.status !== 200) {
      console.error(
        `Preflight failed: GET /ready returned HTTP ${ready.status}. Is the API up with a reachable database?`
      );
      process.exit(1);
    }
  }

  console.log(`Running scenarios against ${options.baseUrl} (adapter: ${options.adapter})`);
  console.log("");

  const results = await runAllScenarios(client, options.filter);
  let passed = 0;
  let failed = 0;

  for (const result of results) {
    if (result.ok) {
      passed++;
      console.log(`PASS  ${result.id}  ${result.title}  (${result.ms}ms)`);
    } else {
      failed++;
      console.log(`FAIL  ${result.id}  ${result.title}  (${result.ms}ms)`);
      console.log(`      ${result.error}`);
    }
  }

  console.log("");
  console.log(`${passed}/${results.length} passed, ${failed} failed`);

  process.exit(failed > 0 ? 1 : 0);
}

main().catch((err) => {
  console.error(err.message ?? err);
  process.exit(1);
});
