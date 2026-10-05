import type { UiManifest, UiManifestEntry } from '@reconark/module-sdk';

/**
 * Keeps only manifest entries this build can actually load, enabled, ordered. Unknown ids are reported rather than
 * crashing the shell: the manifest may be ahead of the build during a rollout.
 */
export function resolveManifest(
  manifest: UiManifest,
  available: ReadonlySet<string>,
): { mounted: UiManifestEntry[]; unknown: string[] } {
  const enabled = manifest.modules.filter((m) => m.enabled);
  return {
    mounted: enabled.filter((m) => available.has(m.id)).sort((a, b) => a.order - b.order),
    unknown: enabled.filter((m) => !available.has(m.id)).map((m) => m.id),
  };
}

/** Loads the manifest from the BFF; falls back to the bundled development manifest when no BFF is running. */
export async function loadManifest(): Promise<UiManifest> {
  for (const url of ['/bff/ui-manifest', '/ui-manifest.dev.json']) {
    try {
      const res = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'application/json' } });
      if (res.ok && res.headers.get('content-type')?.includes('json'))
        return (await res.json()) as UiManifest;
    } catch {
      // try the next source
    }
  }
  return { environment: 'unavailable', modules: [] };
}
