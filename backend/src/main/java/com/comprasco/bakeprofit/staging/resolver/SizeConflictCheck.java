package com.comprasco.bakeprofit.staging.resolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.catalog.util.Gtin;
import com.comprasco.bakeprofit.catalog.util.NetContentLabel;
import com.comprasco.bakeprofit.staging.repository.PendingOfferSize;
import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;
import com.comprasco.bakeprofit.staging.repository.ScrapedOfferRepository;

/**
 * Antes de crear un producto con un EAN nuevo, comprueba que las demás ofertas PENDING
 * de ese EAN traigan el mismo tamaño.
 *
 * <p>
 * Un producto toma el tamaño de la primera oferta que lo crea y nunca se modifica (RN-02).
 * Si esa oferta trae el tamaño mal leído (un pack leído como unidad, "Roa 10 x2.5kg" leído
 * como 25 kg), las ofertas correctas de las demás tiendas quedarían rechazadas para siempre,
 * y cuál gana dependería del orden de llegada. Con un desacuerdo no se sabe cuál lectura es
 * la buena, así que ninguna crea el producto: todas quedan UNCLASSIFIED con el mismo motivo,
 * que lista los tamaños y las tiendas, hasta que la lectura se corrija y coincidan.
 * </p>
 *
 * <p>
 * Corre dentro de la transacción de la oferta y antes de cualquier escritura del producto
 * o la marca. Las demás ofertas del conflicto se marcan en la misma transacción; cuando el
 * orquestador llega a ellas ya no están PENDING y las cuenta como SKIPPED.
 * </p>
 */
@Component
public class SizeConflictCheck {

    static final String SIZE_CONFLICT_REASON =
            "Tamaño en conflicto entre ofertas del EAN %s: %s. No se crea el producto hasta que coincidan";
    private static final String SIZE_SEPARATOR = "; ";
    private static final String STORE_SEPARATOR = ", ";

    private final ScrapedOfferRepository offerRepository;

    public SizeConflictCheck(ScrapedOfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    private record Sighting(Long offerId, BigDecimal netContent, UnitBase unitBase, String storeName) {

        static Optional<Sighting> from(PendingOfferSize other) {
            return UnitBase.parse(other.getUnitBase())
                    .map(unit -> new Sighting(other.getId(), other.getNetContent(), unit, other.getStoreName()));
        }

        boolean sameSizeAs(Sighting other) {
            return unitBase == other.unitBase && netContent.compareTo(other.netContent) == 0;
        }

        String sizeLabel() {
            return NetContentLabel.format(netContent, unitBase);
        }
    }

    /**
     * Devuelve el motivo del conflicto si alguna otra oferta PENDING del mismo EAN trae otro
     * tamaño; en ese caso, esas otras ofertas ya quedaron UNCLASSIFIED con el mismo motivo.
     * Las ofertas sin unidad reconocible no cuentan: los filtros las rechazan por su cuenta.
     */
    public Optional<String> check(ScrapedOffer offer, OfferScreening.Accepted accepted) {
        Sighting own = new Sighting(offer.getId(), offer.getNetContent(), accepted.unitBase(),
                offer.getStore().getName());
        List<Sighting> others = offerRepository.findOtherPendingSizes(accepted.gtin(), offer.getId()).stream()
                .map(Sighting::from)
                .flatMap(Optional::stream)
                .toList();
        if (others.stream().allMatch(own::sameSizeAs)) {
            return Optional.empty();
        }
        String reason = SIZE_CONFLICT_REASON.formatted(Gtin.toDisplay(accepted.gtin()), describe(own, others));
        offerRepository.unclassifyConflicting(others.stream().map(Sighting::offerId).toList(),
                reason, LocalDateTime.now());
        return Optional.of(reason);
    }

    // "500 g en D1, Éxito; 25 kg en Jumbo": un grupo por tamaño, en orden estable
    private static String describe(Sighting own, List<Sighting> others) {
        Map<String, TreeSet<String>> storesBySize = Stream.concat(Stream.of(own), others.stream())
                .collect(Collectors.groupingBy(Sighting::sizeLabel, TreeMap::new,
                        Collectors.mapping(Sighting::storeName, Collectors.toCollection(TreeSet::new))));
        return storesBySize.entrySet().stream()
                .map(entry -> entry.getKey() + " en " + String.join(STORE_SEPARATOR, entry.getValue()))
                .collect(Collectors.joining(SIZE_SEPARATOR));
    }
}
