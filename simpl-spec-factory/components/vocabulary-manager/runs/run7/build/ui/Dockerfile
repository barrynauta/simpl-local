# Vocabulary Manager UI: built with Node, served by unprivileged nginx. Images pinned by digest (OWT-A03).
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY tsconfig.json vite.config.ts index.html .prettierrc.json ./
COPY src ./src
RUN npx vue-tsc --noEmit && npx vite build

FROM nginxinc/nginx-unprivileged:alpine@sha256:b5047b9b76845eac124358373c6852377c52e4b05834a47647e955dd91cb8f1e
USER root
RUN apk upgrade --no-cache && rm -f /etc/nginx/conf.d/default.conf
USER 101
COPY --chown=101:101 nginx/default.conf.template nginx/security-headers.inc.template /etc/nginx/templates/
COPY --from=build --chown=101:101 /build/dist /usr/share/nginx/html
ENV NGINX_ENVSUBST_FILTER="^(KEYCLOAK_|API_UPSTREAM)"
EXPOSE 8080
