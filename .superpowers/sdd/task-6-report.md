# Task 6 Report: Vue application shell, responsive layout, and API client

## Scope

Implemented only `alpha-web` plus this Task 6 report. No files under `alpha-server` or other backend modules were changed.

## Delivered

- Vite + Vue 3 + TypeScript application setup with Tailwind CSS v4, Ant Design Vue, Vitest, ESLint, and Prettier.
- Axios API client that adds `Authorization: Bearer <token>` and clears the authentication store on `401` responses.
- Typed auth API client, browser-persisted auth store, route guard, and placeholder routes ready for Task 7 views.
- Responsive `BaseLayout`: drawer navigation below `768px`, collapsed sidebar behavior from `768px` through `1023px`, and a toggleable desktop sidebar at `1024px` and wider.

## TDD evidence

1. Before the application existed, ran:

   ```sh
   /opt/homebrew/bin/pnpm --dir alpha-web test
   ```

   Result: failed as expected with `ENOENT: no such file or directory, lstat '.../alpha-web'`.

2. Added the requested HTTP and layout tests before their implementation, installed the test tooling, then ran:

   ```sh
   /opt/homebrew/bin/pnpm test
   ```

   Result: failed as expected because `./http` and `./BaseLayout.vue` did not yet exist.

3. Implemented the smallest supporting client and layout code, then reran the focused suite. Final result: `2 passed` test files and `3 passed` tests:

   - attaches the current Bearer token;
   - clears the auth store after a `401` response;
   - opens the rendered drawer navigation below the mobile breakpoint.

## Final verification

Executed from the repository root:

```sh
/opt/homebrew/bin/pnpm --dir alpha-web lint
/opt/homebrew/bin/pnpm --dir alpha-web format
/opt/homebrew/bin/pnpm --dir alpha-web test
/opt/homebrew/bin/pnpm --dir alpha-web typecheck
/opt/homebrew/bin/pnpm --dir alpha-web build
```

Results: all commands passed. The production build completed successfully; Vite emitted only its non-failing size advisory for the initial Ant Design Vue chunk (`1,523.56 kB` uncompressed, `470.68 kB` gzip).

## Rendered smoke check

- Browser-checked `http://127.0.0.1:5173/login` at default desktop and `390x844` mobile dimensions: page title was `Alpha Vue`, the expected `登录` placeholder rendered, and console error/warning logs were empty.
- The authenticated shell itself is intentionally guarded and therefore is exercised by the responsive unit test until Task 7 adds the login flow that supplies a session token.

## Review fixes (2026-07-25)

- Fixed the 768px–1023px sidebar contract: it now initializes collapsed and provides a visible keyboard-accessible control with an accurate `aria-label`, `aria-expanded`, and `aria-controls` relationship to expand or collapse the primary navigation.
- Preserved the existing mobile drawer below 768px and the desktop sidebar toggle at 1024px and wider.
- Added layout coverage for tablet expansion at 768px and desktop collapse/expand at 1024px; the layout suite now covers all three responsive navigation modes.

Verification run from the repository root:

```sh
/opt/homebrew/bin/pnpm --dir alpha-web format
/opt/homebrew/bin/pnpm --dir alpha-web lint
/opt/homebrew/bin/pnpm --dir alpha-web test -- BaseLayout.test.ts
/opt/homebrew/bin/pnpm --dir alpha-web typecheck
/opt/homebrew/bin/pnpm --dir alpha-web build
```

Results: all checks passed. Vitest reported `2 passed` test files and `5 passed` tests. The production build retained only Vite's non-failing initial chunk-size advisory.
