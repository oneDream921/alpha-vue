# Alpha Vue

Alpha Vue is a Vue and Spring Boot application. This foundation provides the `alpha-server` backend module and local MySQL, Redis, and MinIO services.

## Local backend verification

```bash
/Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml test
```

## Local infrastructure

```bash
cp deploy/.env.example deploy/.env
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d
```

The backend defaults to the development profile. Configure database, Redis, MinIO, and application secrets through environment variables; see `deploy/.env.example` for local values.
