package com.comprasco.bakeprofit.staging.identity;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Compara nombres sin perder números que distinguen SPF, concentración o presentación. */
@Component
public class NormalizedProductNameSimilarity implements ProductNameSimilarity {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9%.,]+");
    private static final Pattern NUMBERS = Pattern.compile("[0-9]+(?:[.,][0-9]+)?%?");
    private static final Pattern SUN_PROTECTION = Pattern.compile("\\b(?:spf|fps)\\s*([0-9]+)");
    private static final Pattern NUMERIC_TOKEN = Pattern.compile(".*[0-9].*");
    private static final Set<String> NOISE = Set.of("crema", "facial", "x", "ml", "gr", "g");
    private static final Set<String> VARIANTS = Set.of("dia", "noche", "night", "day", "gel", "fluido",
            "fluid", "oil", "seca", "seco", "grasa", "graso", "mixta", "color", "tinted", "refill");
    private static final int MAXIMUM_NAME_LENGTH = com.comprasco.bakeprofit.catalog.entity.Product.NAME_MAX_LENGTH;
    private static final String TOKEN_SEPARATOR = " ";

    @Override
    public double score(String offerName, String productName) {
        if (offerName == null || productName == null
                || offerName.length() > MAXIMUM_NAME_LENGTH || productName.length() > MAXIMUM_NAME_LENGTH) {
            return 0;
        }
        String normalizedOffer = normalize(offerName);
        String normalizedProduct = normalize(productName);
        if (!numbers(normalizedOffer).equals(numbers(normalizedProduct))
                || !variants(normalizedOffer).equals(variants(normalizedProduct))
                || !sunProtection(normalizedOffer).equals(sunProtection(normalizedProduct))) {
            return 0;
        }
        String offerTokens = significantTokens(normalizedOffer);
        String productTokens = significantTokens(normalizedProduct);
        if (offerTokens.isBlank() || productTokens.isBlank()) {
            return 0;
        }
        int maximumLength = Math.max(offerTokens.length(), productTokens.length());
        return 1.0 - (double) distance(offerTokens, productTokens) / maximumLength;
    }

    public static String normalize(String text) {
        String decomposed = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    private static Set<String> numbers(String text) {
        return NUMBERS.matcher(text).results()
                .map(match -> match.group().replace(',', '.'))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> sunProtection(String text) {
        return SUN_PROTECTION.matcher(text).results().map(match -> match.group(1))
                .collect(Collectors.toSet());
    }

    private static Set<String> variants(String text) {
        return Arrays.stream(SEPARATORS.split(text)).filter(VARIANTS::contains)
                .collect(Collectors.toSet());
    }

    private static String significantTokens(String text) {
        return Arrays.stream(SEPARATORS.split(text))
                .filter(token -> !token.isBlank() && !NOISE.contains(token))
                .filter(token -> !NUMERIC_TOKEN.matcher(token).matches())
                .collect(Collectors.toCollection(TreeSet::new)).stream()
                .collect(Collectors.joining(TOKEN_SEPARATOR));
    }

    private static int distance(String first, String second) {
        int[] previousRow = new int[second.length() + 1];
        for (int column = 0; column <= second.length(); column++) {
            previousRow[column] = column;
        }
        for (int row = 1; row <= first.length(); row++) {
            int[] currentRow = new int[second.length() + 1];
            currentRow[0] = row;
            for (int column = 1; column <= second.length(); column++) {
                int substitutionCost = first.charAt(row - 1) == second.charAt(column - 1) ? 0 : 1;
                currentRow[column] = Math.min(Math.min(currentRow[column - 1] + 1,
                        previousRow[column] + 1), previousRow[column - 1] + substitutionCost);
            }
            previousRow = currentRow;
        }
        return previousRow[second.length()];
    }
}
