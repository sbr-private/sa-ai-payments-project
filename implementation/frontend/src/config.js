import path from 'node:path';
import { fileURLToPath } from 'node:url';
import dotenv from 'dotenv';

dotenv.config();

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, '../../..');

export const config = {
  port: Number(process.env.PORT) || 3000,
  sessionSecret: process.env.SESSION_SECRET || 'demo-session-secret',
  apiUrl: process.env.API_URL || 'http://localhost:8080/v1',
  useFixtures: process.env.USE_FIXTURES === 'true',
  projectRoot,
  openapiPath: path.join(projectRoot, 'docs/openapi.yaml'),
  frontendOpenapiPath: path.join(projectRoot, 'implementation/frontend/openapi/frontend.yaml'),
  fixturesPath: path.join(projectRoot, 'docs/fixtures'),
  seed: {
    acmeAccountId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    supplierAccountId: 'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    demoE2eRejected: 'E2E-INV-2024-0999',
    demoE2eSuccess: 'E2E-INV-2024-0558',
  },
};
