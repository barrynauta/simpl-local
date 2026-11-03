package eu.simpl.cm.manager.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<ContractRecord, Long> {
    Optional<ContractRecord> findByContractNegotiationId(String nid);

    List<ContractRecord> findByContractAgreementId(String aid);

    List<ContractRecord> findByAssetIdAndProviderIdAndConsumerIdAndStatusIn(String assetId, String providerId,
                                                                           String consumerId, Collection<ContractStatus> statuses);

    List<ContractRecord> findByStatusIn(Collection<ContractStatus> statuses);

    long countByStatus(ContractStatus status);
}
