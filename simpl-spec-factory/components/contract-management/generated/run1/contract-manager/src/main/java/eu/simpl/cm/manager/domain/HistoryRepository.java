package eu.simpl.cm.manager.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<HistoryEntry, Long> {
    List<HistoryEntry> findByContractNegotiationIdOrderByIdAsc(String nid);
}
