import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backup and restore of the vocabulary store (Q3, D89), run inside the store container with the admin user (D83).
 *
 * <p>backup FILE: writes the whole dataset as N-Quads and checks the quad count. restore FILE: loads the file into
 * the staging dataset, checks the count, and only then replaces the live dataset in one transaction, so a failed
 * restore never leaves the store emptier than before. count: prints the live quad count.
 */
public final class StoreOps {

    private static final String BASE = "http://localhost:3030/";
    private static final String COUNT =
            "SELECT (COUNT(*) AS ?c) WHERE { { ?s ?p ?o } UNION { GRAPH ?g { ?s ?p ?o } } }";
    private static final Pattern VALUE = Pattern.compile("\"value\"\\s*:\\s*\"(\\d+)\"");

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String authorization;

    private StoreOps(String password) {
        String raw = "vmadmin:" + password;
        this.authorization = "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws Exception {
        String password = System.getenv("STORE_ADMIN_PASSWORD");
        if (password == null || password.isEmpty() || args.length < 1) {
            System.err.println("usage: StoreOps backup|restore|count [FILE]; STORE_ADMIN_PASSWORD must be set");
            System.exit(2);
        }
        StoreOps ops = new StoreOps(password);
        switch (args[0]) {
            case "backup" -> ops.backup(Path.of(args[1]));
            case "restore" -> ops.restore(Path.of(args[1]));
            case "count" -> System.out.println(ops.count("vocabulary"));
            default -> {
                System.err.println("unknown command " + args[0]);
                System.exit(2);
            }
        }
    }

    private void backup(Path file) throws IOException, InterruptedException {
        long before = count("vocabulary");
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + "vocabulary/data"))
                .header("Authorization", authorization)
                .header("Accept", "application/n-quads")
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(file));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("backup failed with HTTP " + response.statusCode());
        }
        long lines = quadLines(file);
        System.out.println("backup " + file + " quads=" + lines + " storeCountBefore=" + before);
        if (lines < before) {
            throw new IllegalStateException("backup holds fewer quads than the store");
        }
    }

    private void restore(Path file) throws IOException, InterruptedException {
        long expected = quadLines(file);
        long liveBefore = count("vocabulary");
        put("vocabulary-restore", file);
        long staged = count("vocabulary-restore");
        System.out.println("staged quads=" + staged + " expected=" + expected + " liveBefore=" + liveBefore);
        if (staged != expected) {
            throw new IllegalStateException("staging count differs from the backup; live dataset not touched");
        }
        put("vocabulary", file);
        long live = count("vocabulary");
        System.out.println("restored live quads=" + live);
        if (live != expected) {
            throw new IllegalStateException("live count differs from the backup after restore");
        }
        clear("vocabulary-restore");
    }

    private void put(String dataset, Path file) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + dataset + "/data"))
                .header("Authorization", authorization)
                .header("Content-Type", "application/n-quads")
                .timeout(Duration.ofMinutes(10))
                .PUT(HttpRequest.BodyPublishers.ofFile(file))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("load into " + dataset + " failed with HTTP " + response.statusCode());
        }
    }

    private void clear(String dataset) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + dataset + "/update"))
                .header("Authorization", authorization)
                .header("Content-Type", "application/sparql-update")
                .POST(HttpRequest.BodyPublishers.ofString("DROP ALL"))
                .build();
        client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private long count(String dataset) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + dataset + "/query"))
                .header("Authorization", authorization)
                .header("Content-Type", "application/sparql-query")
                .header("Accept", "application/sparql-results+json")
                .POST(HttpRequest.BodyPublishers.ofString(COUNT))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("count on " + dataset + " failed with HTTP " + response.statusCode());
        }
        Matcher matcher = VALUE.matcher(response.body());
        if (!matcher.find()) {
            throw new IllegalStateException("count on " + dataset + " gave no value");
        }
        return Long.parseLong(matcher.group(1));
    }

    private static long quadLines(Path file) throws IOException {
        long count = 0;
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[1 << 16];
            boolean lineHasContent = false;
            int read;
            while ((read = in.read(buffer)) > 0) {
                for (int i = 0; i < read; i++) {
                    byte b = buffer[i];
                    if (b == '\n') {
                        if (lineHasContent) {
                            count++;
                        }
                        lineHasContent = false;
                    } else if (b != ' ' && b != '\r' && b != '\t') {
                        lineHasContent = true;
                    }
                }
            }
            if (lineHasContent) {
                count++;
            }
        }
        return count;
    }
}
