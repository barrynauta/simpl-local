# Builds the UI (Vue 3, Vite, PrimeVue unstyled, Tailwind) and serves it with nginx, which also proxies the API.
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /ui
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run lint && npm run test && npm run build

FROM nginxinc/nginx-unprivileged:alpine@sha256:b5047b9b76845eac124358373c6852377c52e4b05834a47647e955dd91cb8f1e
USER root
# Unused dynamic modules are removed; they pull in libxml2, libxslt and GeoIP (smaller attack surface, fewer findings).
RUN apk upgrade --no-cache \
 && apk del --no-cache nginx-module-geoip nginx-module-image-filter nginx-module-njs nginx-module-xslt \
 && (apk del --no-cache libxslt libxml2 2>/dev/null || true)
USER 101
COPY nginx/nginx.conf /etc/nginx/templates-src/nginx.conf.template
COPY nginx/problems.conf /etc/nginx/problems.conf
COPY --from=build /ui/dist /usr/share/nginx/html
COPY docker-entrypoint-proxy.sh /docker-entrypoint-proxy.sh
EXPOSE 8080 8081
HEALTHCHECK --interval=5s --timeout=3s --start-period=5s --retries=20 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/index.html || exit 1
ENTRYPOINT ["/bin/sh", "/docker-entrypoint-proxy.sh"]
