# Build stage: lint, format check, type check, component tests and the Vite build (D95).
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-fund --no-audit
COPY . .
RUN npm run build

# Runtime: unprivileged nginx serving the SPA and proxying /v1 to the backend.
FROM nginxinc/nginx-unprivileged:alpine-slim@sha256:a8cb0f33d51fa107c96fd2bd3a71aaf564ec131e9d2bdf8700944f0ed8b170d7
# Pick up Alpine security fixes released after the base image was built (musl, zlib).
USER root
RUN apk upgrade --no-cache
USER 101
ENV KEYCLOAK_PUBLIC_URL=http://localhost:8193 \
    KEYCLOAK_REALM=simpl \
    KEYCLOAK_CLIENT_ID=vocabulary-manager-ui \
    NGINX_ENVSUBST_FILTER=^KEYCLOAK_
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
# Render the template once with the defaults and check the configuration at build time.
RUN envsubst '${KEYCLOAK_PUBLIC_URL} ${KEYCLOAK_REALM} ${KEYCLOAK_CLIENT_ID}' \
      < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf \
    && nginx -t
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
