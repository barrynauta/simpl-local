# syntax=docker/dockerfile:1.7
# Build stage: Node 24 on Alpine, pinned by digest (OWT-A03).
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# Runtime: unprivileged nginx on Alpine, pinned by digest.
FROM nginxinc/nginx-unprivileged:alpine@sha256:b5047b9b76845eac124358373c6852377c52e4b05834a47647e955dd91cb8f1e
USER root
RUN apk upgrade --no-cache
USER 101
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html
ENV KEYCLOAK_URL=http://localhost:8193 \
    KEYCLOAK_REALM=simpl \
    KEYCLOAK_CLIENT_ID=vocabulary-manager-ui \
    API_UPSTREAM=http://backend:8080
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=5s --retries=12 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1
