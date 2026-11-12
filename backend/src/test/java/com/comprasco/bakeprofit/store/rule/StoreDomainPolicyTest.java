package com.comprasco.bakeprofit.store.rule;

import static com.comprasco.bakeprofit.staging.StagingFixtures.RESTRICTED_ONLY;
import static com.comprasco.bakeprofit.staging.StagingFixtures.SPARE_PARTS;
import static com.comprasco.bakeprofit.staging.StagingFixtures.SUPERMARKET;
import static com.comprasco.bakeprofit.staging.StagingFixtures.store;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.staging.StagingFixtures;
import com.comprasco.bakeprofit.store.rule.StoreDomainPolicy;

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
