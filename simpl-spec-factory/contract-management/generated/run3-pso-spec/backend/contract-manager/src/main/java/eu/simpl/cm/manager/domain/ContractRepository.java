package eu.simpl.cm.manager.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<ContractRecord, Long> {

    Optional<ContractRecord> findByContractNegotiationId(String nid);

    List<ContractRecord> findByContractAgreementIdOrderByCreatedDateDesc(String aid);

    @Query("select c from ContractRecord c where c.assetId = ?1 and c.providerId = ?2 and c.consumerId = ?3 "
            + "and c.contractNegotiationId <> ?4 and c.status in ?5 order by c.createdDate asc")
    List<ContractRecord> findOpenDuplicates(String assetId, String providerId, String consumerId, String nid,
                                            Collection<ContractStatus> statuses);

    List<ContractRecord> findByStatusInOrderByCreatedDateDesc(Collection<ContractStatus> statuses);

    List<ContractRecord> findAllByOrderByCreatedDateDesc();

    @Query("select c.status, count(c) from ContractRecord c group by c.status")
    List<Object[]> countByStatus();
}
