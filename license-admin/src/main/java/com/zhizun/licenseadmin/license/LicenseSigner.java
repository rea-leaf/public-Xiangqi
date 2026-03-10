package com.zhizun.licenseadmin.license;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Set;

public final class LicenseSigner {

    private static final int PRIVATE_KEY_MASK = 0x23;
    private static final int[][] SM2_PRIVATE_KEY_SEGMENTS = {
            {110, 106, 100, 119, 98, 68, 102, 98, 110, 97, 110, 100, 97, 90, 82, 100},
            {112, 110, 23, 26, 98, 68, 102, 100, 96, 96, 82, 97, 107, 110, 26, 117},
            {98, 122, 106, 87, 97, 107, 72, 84, 71, 84, 106, 97, 98, 114, 114, 68},
            {86, 74, 121, 85, 114, 19, 84, 101, 118, 117, 113, 109, 22, 89, 68, 116},
            {122, 96, 81, 20, 123, 77, 72, 20, 114, 112, 79, 98, 65, 80, 70, 96},
            {104, 115, 73, 123, 71, 101, 114, 16, 80, 79, 86, 68, 96, 68, 122, 106},
            {104, 76, 102, 64, 89, 18, 118, 97, 68, 74, 17, 75, 113, 98, 109, 96},
            {98, 98, 114, 75, 68, 73, 65, 74, 65, 107, 105, 90, 23, 84, 96, 21},
            {116, 90, 105, 114, 82, 98, 75, 26, 77, 110, 105, 21, 91, 78, 87, 90},
            {117, 71, 8, 90, 81, 8, 23, 113, 96, 22, 107, 17, 21, 91, 89, 96},
            {21, 17, 115, 100, 111, 79, 70, 122, 117, 86, 68, 113, 117, 85, 111, 26},
            {87, 111, 101, 107, 27, 112, 101, 27, 80, 74, 112, 19, 71, 8, 121, 27},
            {121, 71, 82, 83, 80, 84, 82, 103}
    };

    private LicenseSigner() {
    }

    public static void signToFile(Path outputPath,
                                  String holder, String edition, LocalDate issuedAt, LocalDate expiresAt,
                                  String machineCode, Set<LicensedFeature> features) throws Exception {
        PrivateKey privateKey = loadPrivateKey();
        LicenseInfo unsigned = new LicenseInfo(holder, edition, issuedAt, expiresAt, machineCode, features, "");

        Signature signature = Sm2Support.signature();
        signature.initSign(privateKey);
        signature.update(unsigned.canonicalPayload().getBytes(StandardCharsets.UTF_8));
        String signed = Base64.getEncoder().encodeToString(signature.sign());

        LicenseInfo finalInfo = new LicenseInfo(holder, edition, issuedAt, expiresAt, machineCode, features, signed);
        try (FileOutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
            finalInfo.toProperties().store(outputStream, "License");
        }
    }

    private static PrivateKey loadPrivateKey() throws Exception {
        byte[] data = Base64.getDecoder().decode(resolveObfuscatedPrivateKey());
        return Sm2Support.loadPrivateKey(data);
    }

    private static String resolveObfuscatedPrivateKey() {
        StringBuilder builder = new StringBuilder(192);
        for (int[] segment : SM2_PRIVATE_KEY_SEGMENTS) {
            for (int value : segment) {
                builder.append((char) (value ^ PRIVATE_KEY_MASK));
            }
        }
        return builder.toString();
    }
}
