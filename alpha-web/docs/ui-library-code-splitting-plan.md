# UI Library Code-Splitting Plan

## Objective

Remove the current `~500 kB` gzip JavaScript warning caused by the UI library,
without merely moving the same first-screen download from `index` to another
required chunk. The target is on-demand UI components that Rollup can retain in
their route chunks, plus stable chunks for the framework runtime.

## Baseline (2026-07-26)

| Item                                 |                        Current value | Evidence                                |
| ------------------------------------ | -----------------------------------: | --------------------------------------- |
| Largest JavaScript asset             |                           1.6 MB raw | `dist/assets/index-BVqn_1z6.js`         |
| Largest JavaScript asset (gzip)      |                            498,507 B | `gzip -c dist/assets/index-BVqn_1z6.js` |
| Next-largest JavaScript asset (gzip) |                             11,811 B | current application entry               |
| UI usage                             | 418 `<a-*>` tags, 30 component types | `src/**/*.vue`                          |
| Primary cause                        |           whole-library registration | `src/main.ts`: `app.use(Antd)`          |

The lazy routes are already split. However, `main.ts` imports the root
`ant-design-vue` entry and registers every component, so Rollup must retain the
entire library in the initial graph. `manualChunks` alone changes the filename,
not the amount downloaded before the application can render.

## Non-goals

- Do not replace Ant Design Vue or redesign existing pages.
- Do not split every component into an individual network request.
- Do not add a chunk-size limit that fails CI before the baseline and budgets
  are recorded.

## Approach

### Phase 1: separate framework vendor chunks

Add explicit Rollup chunk ownership for Vue, Router, and Pinia. Do not force
Ant Design Vue and its icon package into separate chunks: Ant Design Vue imports
icons internally, and doing so creates a circular chunk dependency.

```ts
build: {
    rollupOptions: {
        output: {
            manualChunks(id) {
                if (!id.includes('node_modules')) return
                if (id.includes('vue-router')) return 'router-vendor'
                if (id.includes('pinia')) return 'state-vendor'
                if (id.includes('/node_modules/.pnpm/vue@')) return 'vue-vendor'
            },
        },
    },
},
```

Keep this mapping after Phase 2. Do not make a generic `vendor` chunk: it
couples unrelated dependencies and invalidates the cache more often.

### Phase 2: eliminate global UI-library registration (target state)

1. Add `unplugin-vue-components` as a development dependency and configure the
   Ant Design Vue resolver. This transforms template tags such as
   `<a-table>` into imports of only the referenced components.
2. Remove `import Antd from 'ant-design-vue'` and `.use(Antd)` from
   `src/main.ts`. Retain `ant-design-vue/dist/reset.css`; it is a small,
   intentional global stylesheet.
3. Let the resolver generate/import component registration. It must cover
   all currently used `a-*` components, including `ConfigProvider` in `App.vue`.
4. Retain direct named imports for imperative APIs (`message`, `Modal`) and
   direct icon imports. They are already tree-shakeable; do not switch them to
   package-root imports.
5. Keep `vue-cropper` only in the lazy profile route. It is already below the
   route boundary through `profile/index.vue`; verify it stays out of the
   initial chunk after the build.
6. Let Rollup create the Ant Design Vue shared and route chunks automatically.
   This preserves route ownership for heavy table, upload, image, and form
   dependencies instead of pulling their union into a single UI vendor chunk.

Recommended Vite configuration:

```ts
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
    plugins: [
        vue(),
        tailwindcss(),
        Components({
            dts: 'src/components.d.ts',
            resolvers: [AntDesignVueResolver({ importStyle: false })],
        }),
    ],
    build: {
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (!id.includes('node_modules')) return
                    if (id.includes('vue-router')) return 'router-vendor'
                    if (id.includes('pinia')) return 'state-vendor'
                    if (id.includes('/node_modules/.pnpm/vue@'))
                        return 'vue-vendor'
                },
            },
        },
    },
})
```

`importStyle: false` is required for this repository's current CSS strategy:
`reset.css` is imported once in `main.ts` and visual styling is supplied by the
application/Tailwind styles. Change it only after deliberately adopting
component CSS imports and measuring the resulting CSS chunks.

## Delivery sequence

1. Record `dist` raw and gzip sizes from a clean production build.
2. Implement Phase 1, deploy to a test environment, and confirm that no
   `manualChunks` cycle or initial-render regression is introduced.
3. Implement Phase 2 on a feature branch. Run type check, unit tests, and
   smoke-test login, the base layout, all system lists, modal forms, upload,
   image preview, and profile avatar cropping.
4. Compare the clean-build manifest and gzip sizes against the baseline.
5. Deploy with release monitoring for JavaScript load failures and UI runtime
   warnings; retain the previous build as rollback.

## Acceptance criteria

| Check                        | Target                                                                                 |
| ---------------------------- | -------------------------------------------------------------------------------------- |
| Application entry JavaScript | `< 150 kB` gzip                                                                        |
| Largest JavaScript chunk     | `< 150 kB` gzip                                                                        |
| UI behavior                  | no unknown custom-element warnings; all listed smoke flows work                        |
| Chunk topology               | `vue-vendor`, router/state chunks are stable; Ant Design Vue follows route ownership   |
| Cache behavior               | a UI-only dependency update does not change unrelated route chunk hashes unnecessarily |

The first Phase 2 build achieved a 120.91 kB gzip largest chunk and a 30.33 kB
gzip Vue runtime chunk. The original `498.49 kB` gzip entry is eliminated. Keep
the current route-aware Ant Design Vue splitting unless a visualizer shows a
specific shared component group worth naming explicitly.

## Verification commands

```sh
pnpm add -D unplugin-vue-components
pnpm build
find dist/assets -name '*.js' -print0 | xargs -0 -I{} sh -c 'gzip -c "{}" | wc -c | tr -d "\n"; printf "  %s\n" "{}"' | sort -rn
pnpm test
```

For review, add `rollup-plugin-visualizer` temporarily (or behind an
`ANALYZE=true` flag) and inspect the generated stats file. Do not ship its
report generation in the default production build.

## Risks and guardrails

| Risk                                                    | Guardrail                                                                                   |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| A component is not resolved after removing `.use(Antd)` | Generate `src/components.d.ts`; type check and smoke-test every route                       |
| `message` / `Modal` behavior changes                    | Keep direct named imports and test error/login/logout flows                                 |
| CSS duplication or missing styles                       | Keep one reset import; evaluate component CSS separately                                    |
| An Ant Design Vue/icon circular chunk is introduced     | Keep those packages out of `manualChunks`; use route-aware automatic chunks                 |
| Chunk count harms HTTP/1 clients                        | Keep framework vendor groups coarse; production is expected to use HTTP/2 or HTTP/3         |
| A route pulls a heavy feature into the shell            | Inspect the visualizer and enforce gzip budgets in CI after the first measured target build |

## Rollback

The change is reversible: restore `app.use(Antd)`, remove the component resolver,
and keep the `manualChunks` mapping. Because the output filenames are hashed,
rolling back the deployed HTML restores the prior asset graph without cache
poisoning.
