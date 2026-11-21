# Stage 1: install, lint, format check, test and build the SPA.
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run lint && npm run format:check && npm test && npm run build

# Stage 2: static files and the proxy, non-root (uid 101).
FROM nginxinc/nginx-unprivileged:1.29-alpine@sha256:0c79d56aee561a1d81c63f00eee5fb5fe29279560cdc55e91425133104c7fbe6
ENV KEYCLOAK_URL=http://localhost:8193 \
    KEYCLOAK_REALM=simpl \
    KEYCLOAK_CLIENT_ID=vocabulary-manager-ui
USER root
# apk upgrade removes the HIGH and CRITICAL findings of the base image that have a fixed package.
RUN apk upgrade --no-cache && rm -f /etc/nginx/conf.d/default.conf && mkdir -p /etc/nginx/snippets \
    && chown -R 101:101 /etc/nginx/conf.d
COPY nginx/problem-pages.inc nginx/proxy.inc /etc/nginx/snippets/
COPY nginx/vocabulary-manager.conf.template nginx/ui-headers.inc.template /etc/nginx/templates/
COPY --from=build /app/dist /usr/share/nginx/html
USER 101
EXPOSE 8080 8081
HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD wget -q -O /dev/null http://127.0.0.1:8082/healthz || exit 1
