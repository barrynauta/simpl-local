package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.jwk.JWKSet;

/** Fetches the realm signing keys from the identity provider. */
public interface KeySource {

    /**
     * Fetches the current key set.
     *
     * @return the key set
     * @throws IdentityProviderUnavailableException when the identity provider does not answer
     */
    JWKSet fetch();
}
