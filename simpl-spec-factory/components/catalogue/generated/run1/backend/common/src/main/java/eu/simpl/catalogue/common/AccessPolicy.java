package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;

/**
 * Discovery access policy rule of D30. An RD is discoverable by a caller with attribute set S if its
 * ODRL access policy has a permission with the search action whose assignee uid is in S.
 */
public final class AccessPolicy {
    public static final String SEARCH_ACTION = "http://simpl.eu/odrl/actions/search";

    private AccessPolicy() {}

    public static boolean discoverable(JsonNode vc, Collection<String> attributes) {
        JsonNode subject = vc.path("credentialSubject");
        JsonNode policy = subject.path("simpl:servicePolicy");
        if (policy.isArray()) {
            for (JsonNode p : policy) if (check(p, attributes)) return true;
            return false;
        }
        return check(policy, attributes);
    }

    private static boolean check(JsonNode servicePolicy, Collection<String> attributes) {
        JsonNode raw = servicePolicy.path("simpl:access-policy");
        String text = Json.literal(raw);
        JsonNode odrl;
        if (raw.isObject() && !raw.has("@value")) {
            odrl = raw;
        } else {
            odrl = Json.parseOrNull(text);
        }
        if (odrl == null || !odrl.isObject()) return false;
        JsonNode perms = odrl.path("permission");
        if (perms.isObject()) return permits(perms, attributes);
        if (!perms.isArray()) return false;
        for (JsonNode perm : perms) if (permits(perm, attributes)) return true;
        return false;
    }

    private static boolean permits(JsonNode perm, Collection<String> attributes) {
        boolean action = Json.textList(perm.path("action")).contains(SEARCH_ACTION);
        if (!action) return false;
        JsonNode assignee = perm.path("assignee");
        if (assignee.isArray()) {
            for (JsonNode a : assignee) if (assigneeIn(a, attributes)) return true;
            return false;
        }
        return assigneeIn(assignee, attributes);
    }

    private static boolean assigneeIn(JsonNode assignee, Collection<String> attributes) {
        String uid = assignee.isTextual() ? assignee.asText() : Json.literal(assignee.path("uid"));
        return uid != null && attributes.contains(uid);
    }
}
