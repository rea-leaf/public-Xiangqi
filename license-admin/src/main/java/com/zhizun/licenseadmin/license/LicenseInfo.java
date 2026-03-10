package com.zhizun.licenseadmin.license;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("license.holder", holder);
        properties.setProperty("license.edition", edition);
        properties.setProperty("license.issuedAt", formatDate(issuedAt));
        properties.setProperty("license.expiresAt", formatDate(expiresAt));
        properties.setProperty("license.machineCode", machineCode);
        properties.setProperty("license.features", features.stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
        properties.setProperty("license.signature", signature);
        return properties;
    }

    public String getFeatureText() {
        return features.stream()
                .map(LicensedFeature::getDisplayName)
                .collect(Collectors.joining("、"));
    }

    private static String formatDate(LocalDate date) {
        return date == null ? PERPETUAL : DATE_FORMATTER.format(date);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
