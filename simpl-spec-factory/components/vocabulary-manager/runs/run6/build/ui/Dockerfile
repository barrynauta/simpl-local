# syntax=docker/dockerfile:1.7
FROM node:22-alpine@sha256:1314fb30ec37592d5845e5a160d522c0e680972aa534549940b8d73bde88481a AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY tsconfig.json vite.config.ts index.html .prettierrc.json ./
COPY src ./src
COPY tests ./tests
RUN npx vue-tsc --noEmit && npx vite build

FROM nginxinc/nginx-unprivileged:alpine@sha256:b5047b9b76845eac124358373c6852377c52e4b05834a47647e955dd91cb8f1e
# The base image lags behind Alpine security fixes; upgrade its packages, then drop back to the unprivileged user.
USER root
RUN apk upgrade --no-cache
USER 101
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
ENV KEYCLOAK_URL=http://localhost:8193 \
    KEYCLOAK_REALM=simpl \
    KEYCLOAK_CLIENT_ID=vocabulary-manager-ui \
    API_UPSTREAM=http://backend:8080 \
    NGINX_ENVSUBST_FILTER="^(KEYCLOAK_|API_UPSTREAM)"
EXPOSE 8080
