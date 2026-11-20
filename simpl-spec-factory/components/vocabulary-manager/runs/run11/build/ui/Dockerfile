# Vocabulary Manager UI: built with Node, served by nginx that also proxies the API.
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /ui
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:alpine-slim@sha256:a8cb0f33d51fa107c96fd2bd3a71aaf564ec131e9d2bdf8700944f0ed8b170d7
# Security fixes of the Alpine base that are newer than the pinned digest (SECURITY-SCAN.md).
USER root
RUN apk upgrade --no-cache musl musl-utils zlib
USER 101
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/problems /usr/share/nginx/__problems
COPY --from=build /ui/dist /usr/share/nginx/html
EXPOSE 8080 8081
