-- Sample contract agreement for the local read-path demo.
-- Applied with ./seed.sh (idempotent). Lets the UI's GET /agreements/{id}
-- return real data without standing up the Kafka/EDC create flow.
INSERT INTO contract_agreements (
  contract_agreement_id,
  contract_definition_id,
  consumer_signature_date,
  provider_signature_date,
  status,
  contract_negotiation_id,
  asset_id,
  provider_id,
  consumer_id,
  contract_offer_id
) VALUES (
  '11111111-1111-1111-1111-111111111111',
  'contract-definition-001',
  '2026-06-20 10:00:00',
  '2026-06-21 09:30:00',
  'FINALIZED',
  'negotiation-1001',
  'asset-dataset-42',
  'did:web:provider01.dev.simpl-europe.eu',
  'did:web:consumer01.dev.simpl-europe.eu',
  'offer-7001'
)
ON CONFLICT (contract_agreement_id) DO NOTHING;
