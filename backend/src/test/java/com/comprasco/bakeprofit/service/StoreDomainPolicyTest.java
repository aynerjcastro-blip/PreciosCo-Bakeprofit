package com.comprasco.bakeprofit.service;

import static com.comprasco.bakeprofit.service.StagingFixtures.RESTRICTED_ONLY;
import static com.comprasco.bakeprofit.service.StagingFixtures.SPARE_PARTS;
import static com.comprasco.bakeprofit.service.StagingFixtures.SUPERMARKET;
import static com.comprasco.bakeprofit.service.StagingFixtures.store;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class StoreDomainPolicyTest {

    private final StoreDomainPolicy policy = StagingFixtures.domainPolicy();

    @Test
    void elDominioEsLaCategoriaRaizDeLaTiendaSinDistinguirMayusculas() {
        assertThat(policy.requiresExistingBrand(store(" repuestos "))).isTrue();
        assertThat(policy.hasPendingIdentity(store(SPARE_PARTS))).isTrue();
        assertThat(policy.requiresExistingBrand(store(RESTRICTED_ONLY))).isTrue();
        assertThat(policy.hasPendingIdentity(store(RESTRICTED_ONLY))).isFalse();
        assertThat(policy.requiresExistingBrand(store(SUPERMARKET))).isFalse();
    }

    @Test
    void reportaLosDominiosConfiguradosQueNoSonCategoriasRaiz() {
        assertThat(policy.unmatchedDomains(List.of(SUPERMARKET, "REPUESTOS")))
                .containsExactly("ferretería");
    }
}
