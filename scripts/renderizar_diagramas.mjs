// Requiere: npm install --prefix .tools/diagramas @viz-js/viz
import { instance } from '../.tools/diagramas/node_modules/@viz-js/viz/dist/viz.js';
import { readFile, writeFile } from 'node:fs/promises';
const viz = await instance();
for (const nombre of ['modelo', 'arquitectura']) {
  const entrada = new URL(`../diagramas/${nombre}.dot`, import.meta.url);
  const fuente = await readFile(entrada, 'utf8');
  const resultado = viz.render(fuente, { format: 'svg', engine: 'dot' });
  if (resultado.status !== 'success') throw new Error(JSON.stringify(resultado.errors));
  for (const error of resultado.errors) process.stderr.write(`${nombre}: ${error.message}\n`);
  await writeFile(new URL(`../diagramas/${nombre}.svg`, import.meta.url), resultado.output);
  console.log(`${nombre}.svg generado`);
}
