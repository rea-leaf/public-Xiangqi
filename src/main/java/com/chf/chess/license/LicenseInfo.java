package com.chf.chess.license;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 已签发授权的数据模型。
 */
public final class LicenseInfo {

    public static final String PERPETUAL = "PERPETUAL";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final String holder;
    private final String edition;
    private final LocalDate issuedAt;
    private final LocalDate expiresAt;
    private final String machineCode;
    private final EnumSet<LicensedFeature> features;
    private final String signature;

    public LicenseInfo(String holder, String edition, LocalDate issuedAt, LocalDate expiresAt,
                       String machineCode, Set<LicensedFeature> features, String signature) {
        this.holder = safe(holder);
        this.edition = safe(edition);
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.machineCode = safe(machineCode);
        this.features = features == null || features.isEmpty()
                ? EnumSet.noneOf(LicensedFeature.class)
                : EnumSet.copyOf(features);
        this.signature = safe(signature);
    }

    public static LicenseInfo fromProperties(Properties properties) {
        LocalDate issuedAt = parseDate(properties.getProperty("license.issuedAt"));
        LocalDate expiresAt = parseDate(properties.getProperty("license.expiresAt"));
        EnumSet<LicensedFeature> features = EnumSet.noneOf(LicensedFeature.class);
        String rawFeatures = safe(properties.getProperty("license.features"));
        if (!rawFeatures.isBlank()) {
            for (String token : rawFeatures.split(",")) {
                String name = token.trim();
                if (name.isEmpty()) {
                    continue;
                }
                try {
                    features.add(LicensedFeature.valueOf(name));
                } catch (Exception ignored) {
                }
            }
        }
        return new LicenseInfo(
                properties.getProperty("license.holder"),
                properties.getProperty("license.edition"),
                issuedAt,
                expiresAt,
                properties.getProperty("license.machineCode"),
                features,
                properties.getProperty("license.signature")
        );
    }

    public boolean hasFeature(LicensedFeature feature) {
        return features.contains(feature);
    }

    public boolean isExpired(LocalDate today) {
        return expiresAt != null && expiresAt.isBefore(today);
    }

    public String canonicalPayload() {
        return "holder=" + holder + "\n"
                + "edition=" + edition + "\n"
                + "issuedAt=" + formatDate(issuedAt) + "\n"
                + "expiresAt=" + formatDate(expiresAt) + "\n"
                + "machineCode=" + machineCode + "\n"
                + "features=" + features.stream()
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));
    }

    private static String formatDate(LocalDate date) {
        return date == null ? PERPETUAL : DATE_FORMATTER.format(date);
    }

    private static LocalDate parseDate(String raw) {
        String value = safe(raw);
        if (value.isBlank() || PERPETUAL.equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("license.holder", holder);
        properties.setProperty("license.edition", edition);
        properties.setProperty("license.issuedAt", formatDate(issuedAt));
        properties.setProperty("license.expiresAt", formatDate(expiresAt));
        properties.setProperty("license.machineCode", machineCode);
        properties.setProperty("license.features", features.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(",")));
        properties.setProperty("license.signature", signature);
        return properties;
    }

    public String getFeatureText() {
        return features.isEmpty()
                ? "无"
                : Arrays.stream(LicensedFeature.values())
                .filter(features::contains)
                .map(LicensedFeature::getDisplayName)
                .collect(Collectors.joining("、"));
    }

    public String getHolder() {
        return holder;
    }

    public String getEdition() {
        return edition;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public EnumSet<LicensedFeature> getFeatures() {
        return EnumSet.copyOf(features);
    }

    public String getSignature() {
        return signature;
    }
}
