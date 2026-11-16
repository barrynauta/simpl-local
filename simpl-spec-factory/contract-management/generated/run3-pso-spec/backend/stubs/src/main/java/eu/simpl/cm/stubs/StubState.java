package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** In-memory state of the stub (Appendix C). */
@Component
public class StubState {

    public record Call(String at, String method, String path, int status, Object body, String nid) {
    }

    public record Callback(String participantId, String endpoint, String value, String at, String nid) {
    }

    private final Map<String, Set<String>> faults = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> consumerVcOverrides = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> providerVcOverrides = new ConcurrentHashMap<>();
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private final List<Callback> callbacks = new CopyOnWriteArrayList<>();

    public void addFault(String nid, String service, String mode) {
        faults.computeIfAbsent(nid, k -> ConcurrentHashMap.newKeySet()).add(service + ":" + mode);
    }

    public void clearFaults(String nid) {
        faults.remove(nid);
    }

    public boolean hasFault(String nid, String serviceMode) {
        if (nid == null) {
            return false;
        }
        Set<String> s = faults.get(nid);
        return s != null && s.contains(serviceMode);
    }

    public void setConsumerVcOverride(String nid, JsonNode vc) {
        if (vc == null || vc.isNull()) {
            consumerVcOverrides.remove(nid);
        } else {
            consumerVcOverrides.put(nid, vc);
        }
    }

    public void setProviderVcOverride(String nid, JsonNode vc) {
        if (vc == null || vc.isNull()) {
            providerVcOverrides.remove(nid);
        } else {
            providerVcOverrides.put(nid, vc);
        }
    }

    public JsonNode consumerVcOverride(String nid) {
        return consumerVcOverrides.get(nid);
    }

    public JsonNode providerVcOverride(String nid) {
        return providerVcOverrides.get(nid);
    }

    public void recordCall(Call c) {
        calls.add(c);
    }

    public List<Call> calls(String nid) {
        List<Call> out = new ArrayList<>();
        for (Call c : calls) {
            if (nid == null || nid.equals(c.nid())) {
                out.add(c);
            }
        }
        return out;
    }

    public Callback recordCallback(String participantId, String endpoint, String value, String nid) {
        Callback cb = new Callback(participantId, endpoint, value, Instant.now().toString(), nid);
        callbacks.add(cb);
        return cb;
    }

    public List<Callback> callbacks(String nid) {
        return callbacks.stream().filter(c -> nid.equals(c.nid())).toList();
    }
}
