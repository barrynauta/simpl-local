// Local-stack overlay (simpl-local/simpl-contract) — see ui-overlay/README.md.
//
// Replaces the upstream placeholder ("Here contract informations") with a real
// read-path integration: fetches a contract agreement from the backend and
// renders it. The request goes to a RELATIVE URL (/contract/v1/agreements/{id});
// the stack's nginx reverse-proxies it to the backend and injects the API key,
// so this component knows nothing about the backend host or the key.
import { useEffect, useState, useCallback } from 'react';
import { SIMPLTitle } from '~/shared/ui/title/SIMPLTitle';
import { LoadingWrapper } from '~/shared/ui/spiner/SimpleLoaderWrapper';
import { SIMPLMenuHeader } from '~/app/layout/header/SIMPLMenuHeader';
import { httpClient } from '~/shared/api/httpClient';

type ContractAgreement = {
  contractAgreementId: string;
  contractDefinitionId: string | null;
  status: string | null;
  contractNegotiationId: string | null;
  assetId: string | null;
  providerId: string | null;
  consumerId: string | null;
  contractOfferId: string | null;
  consumerSignatureDate: string | null;
  providerSignatureDate: string | null;
};

// Matches the row seeded by samples/seed.sql.
const DEFAULT_ID = '11111111-1111-1111-1111-111111111111';

function Field({ label, value }: { label: string; value: string | null }) {
  return (
    <>
      <dt className="font-semibold text-gray-700">{label}</dt>
      <dd className="break-all text-gray-900">{value ?? '—'}</dd>
    </>
  );
}

export default function ContractViewPage() {
  const [id, setId] = useState(DEFAULT_ID);
  const [agreement, setAgreement] = useState<ContractAgreement | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);

  const load = useCallback(async (agreementId: string) => {
    setIsLoading(true);
    setIsError(false);
    try {
      const data = await httpClient<ContractAgreement>(
        `/contract/v1/agreements/${agreementId.trim()}`
      );
      setAgreement(data);
    } catch {
      setAgreement(null);
      setIsError(true);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const param = new URLSearchParams(window.location.search).get('id');
    const initial = param ?? DEFAULT_ID;
    setId(initial);
    load(initial);
  }, [load]);

  return (
    <SIMPLMenuHeader>
      <div className="mx-auto mt-3 mb-5 max-w-screen-xl px-8">
        <SIMPLTitle title="Contract Review" />

        <div className="mb-4 flex gap-2">
          <input
            className="w-[30rem] rounded border border-gray-300 px-3 py-2"
            value={id}
            onChange={e => setId(e.target.value)}
            placeholder="Contract agreement id (UUID)"
          />
          <button
            className="rounded bg-[#376bda] px-4 py-2 font-medium text-white"
            onClick={() => load(id)}
          >
            Load
          </button>
        </div>

        <LoadingWrapper
          loadingTitle="Loading Contract data..."
          isLoading={isLoading}
          isError={isError}
        >
          {agreement ? (
            <dl className="grid max-w-3xl grid-cols-[14rem_1fr] gap-y-2 rounded border border-gray-200 bg-white p-6">
              <Field label="Agreement id" value={agreement.contractAgreementId} />
              <Field label="Status" value={agreement.status} />
              <Field label="Definition id" value={agreement.contractDefinitionId} />
              <Field label="Negotiation id" value={agreement.contractNegotiationId} />
              <Field label="Asset id" value={agreement.assetId} />
              <Field label="Provider id" value={agreement.providerId} />
              <Field label="Consumer id" value={agreement.consumerId} />
              <Field label="Offer id" value={agreement.contractOfferId} />
              <Field label="Consumer signed" value={agreement.consumerSignatureDate} />
              <Field label="Provider signed" value={agreement.providerSignatureDate} />
            </dl>
          ) : (
            <p className="text-gray-500">
              No contract loaded (enter an agreement id and press Load).
            </p>
          )}
        </LoadingWrapper>
      </div>
    </SIMPLMenuHeader>
  );
}
