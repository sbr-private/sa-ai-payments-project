import { Router } from 'express';
import fs from 'node:fs/promises';
import yaml from 'js-yaml';
import swaggerUi from 'swagger-ui-express';
import { config } from '../config.js';

const router = Router();

async function loadSpec(filePath, servers) {
  const raw = await fs.readFile(filePath, 'utf8');
  const spec = yaml.load(raw);
  return { ...spec, servers };
}

const ledgerSpec = await loadSpec(config.openapiPath, [
  { url: config.apiUrl, description: 'Ledger API (Spring Boot)' },
]);

const frontendSpec = await loadSpec(config.frontendOpenapiPath, [
  { url: `http://localhost:${config.port}`, description: 'Frontend server' },
]);

router.use('/ledger', swaggerUi.serve, swaggerUi.setup(ledgerSpec, { customSiteTitle: 'Ledger API — ISO 20022' }));
router.use('/ui', swaggerUi.serve, swaggerUi.setup(frontendSpec, { customSiteTitle: 'Frontend UI API' }));

router.get('/', (_req, res) => {
  res.redirect('/api-docs/ledger');
});

export default router;
