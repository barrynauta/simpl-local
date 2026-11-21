package eu.simpl.vocabularymanager.domain;

import eu.simpl.vocabularymanager.domain.Model.Status;
import eu.simpl.vocabularymanager.store.ReferenceStore;
import eu.simpl.vocabularymanager.store.ReferenceStore.InternalInfo;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Internal vocabulary search (D38): one store query, request order, duplicates once. */
@Service
public class SearchService {

    /**
     * A reference: name and optional version.
     *
     * @param name exact vocabulary name (D54)
     * @param version version number, or null for the latest version
     */
    public record Reference(String name, Integer version) {}

    /**
     * A found reference.
     *
     * @param name name
     * @param version version number (latest version when latest)
     * @param status vocabulary status when latest, else the version status
     * @param latest true for a reference without version
     */
    public record Result(String name, int version, Status status, boolean latest) {}

    private final ReferenceStore store;

    public SearchService(ReferenceStore store) {
        this.store = store;
    }

    public List<Result> search(List<Reference> refs) {
        Set<Reference> distinct = new LinkedHashSet<>(refs);
        Set<String> names = new LinkedHashSet<>();
        Set<Integer> versions = new LinkedHashSet<>();
        for (Reference r : distinct) {
            names.add(r.name());
            if (r.version() != null) {
                versions.add(r.version());
            }
        }
        ReferenceStore.Lookup lookup = store.lookup(names, versions, false, List.of());
        List<Result> out = new ArrayList<>();
        for (Reference r : distinct) {
            InternalInfo info = lookup.internal().get(r.name());
            if (info == null) {
                continue;
            }
            if (r.version() == null) {
                out.add(new Result(r.name(), info.latest(), info.status(), true));
            } else {
                Status s = info.versions().get(r.version());
                if (s != null) {
                    out.add(new Result(r.name(), r.version(), s, false));
                }
            }
        }
        return out;
    }
}
