package eu.simpl.cm.manager.domain;

public enum PartyRole {
    PROVIDER, CONSUMER;

    public PartyRole other() {
        return this == PROVIDER ? CONSUMER : PROVIDER;
    }
}
