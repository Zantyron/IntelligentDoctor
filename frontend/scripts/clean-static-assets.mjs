import { mkdir, rm } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const staticAssetsDir = fileURLToPath(
  new URL("../../src/main/resources/static/assets/", import.meta.url),
);

await rm(staticAssetsDir, { recursive: true, force: true });
await mkdir(staticAssetsDir, { recursive: true });
