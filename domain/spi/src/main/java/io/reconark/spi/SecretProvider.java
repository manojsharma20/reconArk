package io.reconark.spi;

/** Resolves a secret reference name (never a value) from the secret manager (ADR-0018). */
public interface SecretProvider {

    /**
     * @param reference reference name, e.g. {@code prod/reconark/acquirer-a/pgp-private-key}
     * @throws IllegalArgumentException if the reference is unknown — fails the run, not the pod
     */
    SecretValue resolve(String reference);
}
