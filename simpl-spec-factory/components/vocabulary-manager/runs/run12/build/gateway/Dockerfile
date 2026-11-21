# API gateway of the local stack (nginx, unprivileged). Proxies host port 8196 to the backend replicas.
FROM nginxinc/nginx-unprivileged:alpine-slim@sha256:a8cb0f33d51fa107c96fd2bd3a71aaf564ec131e9d2bdf8700944f0ed8b170d7
USER root
RUN apk upgrade --no-cache && rm -f /etc/nginx/conf.d/default.conf
USER 101
COPY default.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080 8081
HEALTHCHECK --interval=5s --timeout=3s --retries=20 CMD wget -q -O /dev/null http://127.0.0.1:8081/healthz || exit 1
