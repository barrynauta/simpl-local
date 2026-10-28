package eu.simpl.schemamanager.web;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.schemamanager.domain.Concept;
import eu.simpl.schemamanager.domain.SchemaStatus;
import eu.simpl.schemamanager.domain.SchemaType;
import eu.simpl.schemamanager.domain.SchemaVersion;
import eu.simpl.schemamanager.service.SchemaService;
import eu.simpl.schemamanager.service.SchemaService.Page;
import eu.simpl.schemamanager.service.SchemaService.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

/**
 * Schema operations of section 2.3 of the digest. Access control runs before this
 * controller (SecurityConfig, D5). Each handler follows the processing order of D23.
 */
@RestController
public class SchemaController {

    private final SchemaService service;
    private final Json json;

    public SchemaController(SchemaService service, Json json) {
        this.service = service;
        this.json = json;
    }

    static String user(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null && !preferred.isBlank() ? preferred : jwt.getSubject();
    }

    // ---------------------------------------------------------------- GET /schemas

    @GetMapping("/schemas")
    public ResponseEntity<byte[]> list(@RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String resourceType,
                                       @RequestParam(required = false) String schemaType,
                                       @RequestParam(required = false) String page,
                                       @RequestParam(required = false) String pageSize) {
        Negotiation.requireJson(accept);
        SchemaType type = Params.schemaType(schemaType, "Query parameter");
        SchemaStatus st = Params.status(status);
        String rt = Params.resourceTypeFilter(resourceType, type, "Query parameter");
        int p = Params.page(page);
        int size = Params.pageSize(pageSize);

        Page<Concept> result = service.list(type, st, rt, p, size);
        String next = null;
        if (p < result.totalPages()) {
            StringBuilder q = new StringBuilder("/schemas?");
            if (st != null) {
                q.append("status=").append(st).append('&');
            }
            if (rt != null) {
                q.append("resourceType=").append(enc(rt)).append('&');
            }
            q.append("schemaType=").append(type).append("&page=").append(p + 1).append("&pageSize=").append(size);
            next = q.toString();
        }
        Map<String, Object> body = envelope(result, next);
        body.put("metadata", result.items().stream().map(SchemaController::listItem).toList());
        return json.response(HttpStatus.OK, body, null);
    }

    private static Map<String, Object> envelope(Page<?> page, String next) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page", page.page());
        body.put("totalPages", page.totalPages());
        body.put("total", page.total());
        body.put("pageSize", page.pageSize());
        body.put("next", next);
        return body;
    }

    static Map<String, Object> listItem(Concept c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.name());
        m.put("description", c.description());
        m.put("title", c.title());
        m.put("resourceType", c.resourceType());
        m.put("schemaType", c.schemaType().name());
        m.put("version", c.latestVersion() == null ? null : String.valueOf(c.latestVersion()));
        m.put("changelog", c.latestChangelog());
        m.put("status", c.status().name());
        m.put("created", Json.iso(c.created()));
        m.put("modified", Json.iso(c.modified()));
        m.put("createdBy", c.createdBy());
        return m;
    }

    private Map<String, Object> versionItem(Concept c, SchemaVersion v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.name());
        m.put("version", String.valueOf(v.version()));
        m.put("changelog", v.changelog());
        m.put("created", Json.iso(v.created()));
        m.put("createdBy", v.createdBy());
        m.put("schemaType", c.schemaType().name());
        m.put("resourceType", c.resourceType());
        m.put("status", SchemaService.versionStatus(c, v.version()).name());
        m.put("graphName", service.versionUri(c, v.version()));
        m.put("title", c.title());
        m.put("description", c.description());
        return m;
    }

    static Map<String, Object> category(Concept c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.name());
        m.put("title", c.title());
        m.put("description", c.description());
        m.put("resourceType", c.resourceType());
        m.put("schemaType", c.schemaType().name());
        m.put("status", c.status().name());
        m.put("latestVersion", c.latestVersion() == null ? null : String.valueOf(c.latestVersion()));
        m.put("created", Json.iso(c.created()));
        m.put("modified", Json.iso(c.modified()));
        m.put("createdBy", c.createdBy());
        return m;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- POST /schemas

    @PostMapping("/schemas")
    public ResponseEntity<byte[]> create(HttpServletRequest request,
                                         @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                         @AuthenticationPrincipal Jwt jwt) throws IOException {
        Negotiation.requireJson(accept);
        MultipartHttpServletRequest form = multipart(request);

        String name = form.getParameter("name");
        String title = form.getParameter("title");
        String description = form.getParameter("description");
        String rawType = emptyToNull(form.getParameter("schemaType"));
        String rawResourceType = form.getParameter("resourceType");
        byte[] content = file(form);

        requireField("name", name);
        if (!Params.isPascal(name)) {
            throw ApiException.badRequest(
                    "Field 'name' must contain only alphanumeric characters and follow PascalCase (3 to 64 characters).");
        }
        requireField("title", title);
        requireField("description", description);
        SchemaType type = Params.schemaType(rawType, "Field");
        String resourceType;
        if (type == SchemaType.SEARCH) {
            if (rawResourceType != null && !rawResourceType.isEmpty()) {
                throw ApiException.badRequest("Field 'resourceType' is not allowed when 'schemaType' is SEARCH.");
            }
            resourceType = null;
        } else {
            if (rawResourceType == null || rawResourceType.isBlank()) {
                throw ApiException.badRequest("Required field 'resourceType' is missing for schemaType RESOURCE.");
            }
            resourceType = Params.resourceTypeFilter(rawResourceType, type, "Field");
        }
        if (content == null) {
            throw ApiException.badRequest("Required field 'schemaFile' is missing.");
        }

        Result r = service.create(new SchemaService.CreateCommand(name, title.trim(), description.trim(), type,
                resourceType, content, user(jwt)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schemaName", r.concept().name());
        body.put("schemaVersion", String.valueOf(r.version().version()));
        body.put("validationWarnings", r.warnings());
        body.put("status", r.concept().status().name());
        body.put("schemaType", r.concept().schemaType().name());
        body.put("resourceType", r.concept().resourceType());
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.LOCATION, service.conceptUri(r.concept()));
        return json.response(HttpStatus.CREATED, body, h);
    }

    // ---------------------------------------------------------------- POST /schemas/{name}/versions

    @PostMapping("/schemas/{schemaName}/versions")
    public ResponseEntity<byte[]> newVersion(@PathVariable String schemaName, HttpServletRequest request,
                                             @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                             @AuthenticationPrincipal Jwt jwt) throws IOException {
        Negotiation.requireJson(accept);
        MultipartHttpServletRequest form = multipart(request);
        Params.pathName(schemaName);

        String changelog = form.getParameter("changelog");
        String rawType = emptyToNull(form.getParameter("schemaType"));
        String rawResourceType = emptyToNull(form.getParameter("resourceType"));
        byte[] content = file(form);
        // A 'version' field, if sent, is ignored (D14).

        requireField("changelog", changelog);
        SchemaType type = Params.schemaType(rawType, "Field");
        String resourceType = Params.resourceTypeFilter(rawResourceType, type, "Field");
        if (content == null) {
            throw ApiException.badRequest("Required field 'schemaFile' is missing.");
        }

        Result r = service.newVersion(new SchemaService.NewVersionCommand(schemaName, type, resourceType,
                changelog.trim(), content, user(jwt)));

        Concept c = r.concept();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", c.name());
        body.put("description", c.description());
        body.put("title", c.title());
        body.put("resourceType", c.resourceType());
        body.put("schemaType", c.schemaType().name());
        body.put("version", String.valueOf(r.version().version()));
        body.put("changelog", r.version().changelog());
        body.put("status", c.status().name());
        body.put("validationWarnings", r.warnings());
        body.put("created", Json.iso(r.version().created()));
        body.put("createdBy", r.version().createdBy());
        body.put("graphName", service.versionUri(c, r.version().version()));
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.LOCATION, service.versionUri(c, r.version().version()));
        return json.response(HttpStatus.CREATED, body, h);
    }

    // ---------------------------------------------------------------- PATCH /schemas/{name}

    @PatchMapping("/schemas/{schemaName}")
    public ResponseEntity<byte[]> patch(@PathVariable String schemaName,
                                        @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                        @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
                                        @RequestParam(required = false) String schemaType,
                                        @RequestParam(required = false) String resourceType,
                                        @RequestBody(required = false) byte[] body) {
        Negotiation.requireJson(accept);
        Negotiation.requireJsonBody(contentType);
        Params.pathName(schemaName);
        SchemaType type = Params.schemaType(schemaType, "Query parameter");
        String rt = Params.resourceTypeFilter(resourceType, type, "Query parameter");
        SchemaStatus target = parseStatusBody(body);

        Concept updated = service.changeStatus(type, schemaName, rt, target);
        return json.response(HttpStatus.OK, category(updated), null);
    }

    private SchemaStatus parseStatusBody(byte[] body) {
        String invalid = "Request body must be a JSON object with field 'status' set to REVOKED or PUBLISHED.";
        if (body == null || body.length == 0) {
            throw ApiException.badRequest(invalid);
        }
        JsonNode node;
        try {
            node = json.mapper().readTree(body);
        } catch (IOException e) {
            throw ApiException.badRequest("Request body is not valid JSON.");
        }
        if (node == null || !node.isObject() || !node.has("status") || !node.get("status").isTextual()) {
            throw ApiException.badRequest(invalid);
        }
        return switch (node.get("status").asText()) {
            case "REVOKED" -> SchemaStatus.REVOKED;
            case "PUBLISHED" -> SchemaStatus.PUBLISHED;
            default -> throw ApiException.badRequest(invalid);
        };
    }

    // ---------------------------------------------------------------- GET /schemas/{name}/versions

    @GetMapping("/schemas/{schemaName}/versions")
    public ResponseEntity<byte[]> versions(@PathVariable String schemaName,
                                           @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                           @RequestParam(required = false) String schemaType,
                                           @RequestParam(required = false) String resourceType,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String pageSize) {
        Negotiation.requireJson(accept);
        Params.pathName(schemaName);
        SchemaType type = Params.schemaType(schemaType, "Query parameter");
        String rt = Params.resourceTypeFilter(resourceType, type, "Query parameter");
        int p = Params.page(page);
        int size = Params.pageSize(pageSize);

        Concept c = service.find(type, schemaName, rt);
        Page<SchemaVersion> result = service.versions(c, p, size);
        String next = null;
        if (p < result.totalPages()) {
            next = "/schemas/" + enc(c.name()) + "/versions?"
                    + (rt != null ? "resourceType=" + enc(rt) + "&" : "")
                    + "schemaType=" + type + "&page=" + (p + 1) + "&pageSize=" + size;
        }
        Map<String, Object> body = envelope(result, next);
        body.put("metadata", result.items().stream().map(v -> versionItem(c, v)).toList());
        return json.response(HttpStatus.OK, body, null);
    }

    // ---------------------------------------------------------------- GET /schemas/{name} (D9)

    @GetMapping("/schemas/{schemaName}")
    public ResponseEntity<byte[]> latest(@PathVariable String schemaName,
                                         @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                         @RequestParam(required = false) String schemaType,
                                         @RequestParam(required = false) String resourceType) {
        Negotiation.content(accept, false);
        Params.pathName(schemaName);
        SchemaType type = Params.schemaType(schemaType, "Query parameter");
        String rt = Params.resourceTypeFilter(resourceType, type, "Query parameter");
        Concept c = service.find(type, schemaName, rt);
        int version = c.latestVersion();
        byte[] content = service.content(c, version);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(Negotiation.TEXT_TURTLE);
        h.set(HttpHeaders.CONTENT_LOCATION, service.versionUri(c, version));
        h.add(HttpHeaders.VARY, HttpHeaders.ACCEPT);
        return new ResponseEntity<>(content, h, HttpStatus.OK);
    }

    // ---------------------------------------------------------------- GET /schemas/{name}/{version} (D10)

    @GetMapping("/schemas/{schemaName}/{version}")
    public ResponseEntity<byte[]> version(@PathVariable String schemaName, @PathVariable String version,
                                          @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                          @RequestParam(required = false) String schemaType,
                                          @RequestParam(required = false) String resourceType) {
        Negotiation.ContentChoice choice = Negotiation.content(accept, true);
        Params.pathName(schemaName);
        SchemaType type = Params.schemaType(schemaType, "Query parameter");
        String rt = Params.resourceTypeFilter(resourceType, type, "Query parameter");
        Concept c = service.find(type, schemaName, rt);
        SchemaVersion v = service.version(c, Params.version(version), version);
        String uri = service.versionUri(c, v.version());
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.CONTENT_LOCATION, uri);
        h.set(HttpHeaders.LOCATION, uri);
        if (choice == Negotiation.ContentChoice.METADATA) {
            return json.response(HttpStatus.OK, versionItem(c, v), h);
        }
        h.setContentType(Negotiation.TEXT_TURTLE);
        h.add(HttpHeaders.VARY, HttpHeaders.ACCEPT);
        return new ResponseEntity<>(service.content(c, v.version()), h, HttpStatus.OK);
    }

    // ---------------------------------------------------------------- helpers

    private static MultipartHttpServletRequest multipart(HttpServletRequest request) {
        Negotiation.requireMultipart(request.getContentType());
        MultipartHttpServletRequest form = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
        if (form == null) {
            throw ApiException.unsupportedMediaType();
        }
        return form;
    }

    /** The schemaFile part as bytes; a plain text field of that name is accepted too (D29). */
    private static byte[] file(MultipartHttpServletRequest form) throws IOException {
        MultipartFile f = form.getFile("schemaFile");
        if (f != null) {
            return f.getBytes();
        }
        String text = form.getParameter("schemaFile");
        return text == null ? null : text.getBytes(StandardCharsets.UTF_8);
    }

    private static void requireField(String field, String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("Required field '" + field + "' is missing or blank.");
        }
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

}
