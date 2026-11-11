package com.comprasco.bakeprofit.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.config.StagingProperties;
import com.comprasco.bakeprofit.entity.Store;

/**
 * Reglas del procesador que dependen del dominio de la tienda, es decir, de su
 * categoría raíz (Supermercado, Droguería, Cuidado facial, Repuestos).
 *
 * <p>
 * Son dos listas separadas a propósito: hoy Repuestos está en ambas y toda oferta de
 * repuestos queda sin clasificar por identidad pendiente. Cuando el Sprint 3 agregue
 * la referencia del fabricante, se quita de {@code identity-pending-domains} y la
 * regla de marcas existentes empieza a aplicar sin tocar el código.
 * </p>
 */
@Component
public class StoreDomainPolicy {

    private final Set<String> existingBrandsOnlyDomains;
    private final Set<String> identityPendingDomains;

    public StoreDomainPolicy(StagingProperties properties) {
        this.existingBrandsOnlyDomains = normalized(properties.existingBrandsOnlyDomains());
        this.identityPendingDomains = normalized(properties.identityPendingDomains());
    }

    /** En este dominio una marca desconocida no se crea: la oferta queda para revisión. */
    public boolean requiresExistingBrand(Store store) {
        return existingBrandsOnlyDomains.contains(domainOf(store));
    }

    /** En este dominio todavía no hay cómo identificar el producto. */
    public boolean hasPendingIdentity(Store store) {
        return identityPendingDomains.contains(domainOf(store));
    }

    /**
     * Dominios configurados que no coinciden con ninguna categoría raíz. Si alguien
     * renombra "Repuestos", la regla se apagaría en silencio; esto permite advertirlo.
     */
    public List<String> unmatchedDomains(Collection<String> rootCategoryNames) {
        Set<String> roots = normalized(rootCategoryNames);
        return configuredDomains().stream()
                .filter(domain -> !roots.contains(domain))
                .sorted()
                .toList();
    }

    private Set<String> configuredDomains() {
        Set<String> all = new TreeSet<>(existingBrandsOnlyDomains);
        all.addAll(identityPendingDomains);
        return all;
    }

    private static String domainOf(Store store) {
        return normalize(store.getCategory().getName());
    }

    private static Set<String> normalized(Collection<String> names) {
        return names.stream()
                .map(StoreDomainPolicy::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
