# Build stage: install the pinned dependencies, type-check, run the unit tests and build.
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm test && npm run build

# Run stage: unprivileged nginx serves the UI and proxies the API (same public API as any client, MACH-04).
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /build/dist /usr/share/nginx/html
ENV API_UPSTREAM=http://backend:8080 \
    KEYCLOAK_URL=http://localhost:8193 \
    KEYCLOAK_REALM=simpl \
    KEYCLOAK_CLIENT_ID=vocabulary-manager-ui
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=5s --retries=20 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/config.json || exit 1
