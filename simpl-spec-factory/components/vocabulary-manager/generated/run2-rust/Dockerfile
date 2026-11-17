# syntax=docker/dockerfile:1
# Build stage
FROM rust:1-slim-bookworm AS build
WORKDIR /src
# Cache dependencies: build once with a stub main.
COPY Cargo.toml Cargo.lock ./
RUN mkdir src && echo "fn main() {}" > src/main.rs && echo "" > src/lib.rs \
    && cargo build --release --locked \
    && rm -rf src target/release/deps/vocabulary_manager* target/release/deps/libvocabulary_manager* target/release/vocabulary-manager*
COPY src ./src
RUN find src -type f -exec touch {} + && cargo build --release --locked && strip target/release/vocabulary-manager

# Runtime stage
FROM debian:bookworm-slim
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
COPY --from=build /src/target/release/vocabulary-manager /usr/local/bin/vocabulary-manager
USER 10001:10001
ENV BIND_ADDR=0.0.0.0:8080
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --retries=20 CMD ["vocabulary-manager", "healthcheck"]
ENTRYPOINT ["vocabulary-manager"]
