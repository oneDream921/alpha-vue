# Task 5 report: pluggable file storage

## Delivered scope

- Added `StorageProvider` with the planned `store`, `delete`, and `publicUrl` operations.
- Added configuration-backed local and MinIO providers. The MinIO client is constructed without a network call; object operations are deferred until use.
- Added `sys_file` MyBatis-Plus entity/mapper and a service that validates extension and size, creates UUID object keys, persists metadata, lists metadata, and deletes storage before soft-deleting metadata.
- Added the protected `POST /api/files/upload`, `GET /api/files`, and `DELETE /api/files/{id}` APIs. Upload/delete carry `@OperationLog` annotations and use `file:upload`, `file:list`, and `file:delete` permissions.
- Local storage resolves every key below its configured root and rejects absolute or `..` traversal keys.

## TDD evidence

1. Added `FileServiceTests` before production implementation.
2. RED: `mvn -Dtest=FileServiceTests test` failed at test compilation because `FileService` did not exist (`cannot find symbol: class FileService`). The local Maven binary used was `/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn` because `mvn` is not on PATH.
3. GREEN: focused `FileServiceTests` passed with 6 tests after implementation:
   - rejected extension;
   - rejected configured maximum size;
   - real local upload, UUID object key, metadata, and local bytes;
   - deletion ordering (failed object deletion leaves metadata active);
   - local traversal prevention;
   - MinIO construction/public URL without a local MinIO server.

## Fresh verification

- Full backend suite: `mvn test` completed successfully with `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`.
- Manual multipart verification used the running test-profile backend on port 18080 with H2 and `alpha.file.provider=local`. Logging in as `admin` then posting `manual.txt` produced HTTP 200 and file metadata including `storageProvider: "local"`, UUID `.txt` key, 11 byte size, and `/uploads/<uuid>.txt` public URL.

## Auth-header discrepancy outside Task 5 scope

On the running server, `/api/auth/login` returned HTTP 200 and a token, but a subsequent request using either `Authorization: Bearer <token>` or raw `Authorization: <token>` returned HTTP 401. The same token in the default `satoken: <token>` header returned HTTP 200. Existing MockMvc authentication/RBAC tests use `Authorization: Bearer` and pass. No auth configuration or controller code was changed for Task 5; a focused auth compatibility fix is required before final end-to-end verification can use the documented Bearer header.
