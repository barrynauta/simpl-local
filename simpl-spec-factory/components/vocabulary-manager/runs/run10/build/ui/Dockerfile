# UI build (lint, format check, typecheck, component tests, Vite build), then nginx serving the UI and proxying the API.
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /ui
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:alpine@sha256:b5047b9b76845eac124358373c6852377c52e4b05834a47647e955dd91cb8f1e
USER root
RUN apk upgrade --no-cache
USER 101
COPY --from=build /ui/dist /usr/share/nginx/html
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
EXPOSE 4433 8196
HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=12 \
  CMD wget -q -O /dev/null http://127.0.0.1:4433/config.json || exit 1
