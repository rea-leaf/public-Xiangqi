package com.chf.chess.license;

import com.chf.chess.util.DialogUtils;
import com.chf.chess.util.PathUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.PublicKey;
import java.security.Signature;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;

/**
 * License loading, verification, and feature gating.
 */
public final class LicenseManager {

    private static final String LICENSE_FILE_NAME = "license.lic";
    private static final String LICENSE_POINTER_FILE_NAME = "license.path";
    private static final int PUBLIC_KEY_MASK = 0x17;
    private static final int[][] SM2_PUBLIC_KEY_SEGMENTS = {
            {90, 81, 124, 96, 82, 96, 78, 95, 92, 120, 77, 94, 109, 125, 39, 84},
            {86, 70, 78, 94, 92, 120, 82, 116, 109, 38, 66, 85, 112, 126, 39, 83},
            {70, 112, 86, 82, 94, 78, 94, 37, 35, 122, 111, 110, 116, 98, 90, 86},
            {98, 123, 100, 126, 66, 92, 112, 94, 113, 77, 109, 84, 114, 100, 77, 101},
            {116, 123, 79, 113, 100, 102, 56, 98, 82, 70, 98, 69, 46, 98, 100, 116},
            {96, 98, 99, 125, 111, 126, 34, 79, 122, 81, 117, 120, 82, 65, 117, 110},
            {56, 117, 68, 111, 69, 56, 82, 127, 113, 91, 94, 124, 99, 95, 113, 122},
            {113, 80, 79, 118, 102, 117, 90, 92, 112, 96, 42, 42}
    };

    private static final LicenseManager INSTANCE = new LicenseManager();

    private LicenseInfo current;
    private LicenseStatus status;
    private String statusMessage;

    private LicenseManager() {
        reload();
    }

    public static LicenseManager getInstance() {
        return INSTANCE;
    }

    public synchronized void reload() {
        File licenseFile = resolveActiveLicenseFile();
        if (licenseFile == null || !licenseFile.exists()) {
            current = null;
            status = LicenseStatus.MISSING;
            statusMessage = "License file not imported";
            return;
        }
        try {
            LicenseInfo info = readLicenseInfo(licenseFile);
            LicenseStatus validateStatus = validate(info);
            current = info;
            status = validateStatus;
            statusMessage = statusText(validateStatus, info);
        } catch (Exception e) {
            current = null;
            status = LicenseStatus.INVALID_FILE;
            statusMessage = "Invalid license file";
        }
    }

    public synchronized boolean importLicense(File source) {
        if (source == null || !source.exists()) {
            return false;
        }
        try {
            LicenseInfo info = readLicenseInfo(source);
            LicenseStatus validateStatus = validate(info);
            current = info;
            status = validateStatus;
            statusMessage = statusText(validateStatus, info);
            if (validateStatus != LicenseStatus.VALID) {
                return false;
            }
            writeLicensePointer(source);
            reload();
            return status == LicenseStatus.VALID;
        } catch (Exception e) {
            reload();
            return false;
        }
    }

    public synchronized void clearLicense() {
        try {
            Files.deleteIfExists(getLicensePointerFile().toPath());
            Files.deleteIfExists(getLegacyLicenseFile().toPath());
        } catch (Exception ignored) {
        }
        reload();
    }

    public synchronized boolean isFeatureEnabled(LicensedFeature feature) {
        return status == LicenseStatus.VALID && current != null && current.hasFeature(feature);
    }

    public synchronized boolean hasValidLicense() {
        return status == LicenseStatus.VALID && current != null;
    }

    public synchronized void showFeatureBlocked(LicensedFeature feature) {
        String message = "Current license does not allow: " + feature.getDisplayName()
                + System.lineSeparator() + feature.getDescription()
                + System.lineSeparator() + "License status: " + getStatusText();
        DialogUtils.showWarningDialog("License Restricted", message);
    }

    public synchronized String getStatusText() {
        return statusMessage == null ? "Unknown status" : statusMessage;
    }

    public synchronized LicenseStatus getStatus() {
        return status;
    }

    public synchronized LicenseInfo getCurrent() {
        return current;
    }

    public String getMachineCode() {
        return DeviceFingerprint.machineCode();
    }

    public String getLicensePath() {
        File licenseFile = resolveActiveLicenseFile();
        return licenseFile == null ? "" : licenseFile.getAbsolutePath();
    }

    private LicenseStatus validate(LicenseInfo info) {
        if (info == null) {
            return LicenseStatus.INVALID_FILE;
        }
        if (!verifySignature(info)) {
            return LicenseStatus.INVALID_SIGNATURE;
        }
        if (info.isExpired(LocalDate.now())) {
            return LicenseStatus.EXPIRED;
        }
        String machineCode = getMachineCode();
        if (!machineCode.equalsIgnoreCase(info.getMachineCode())) {
            return LicenseStatus.DEVICE_MISMATCH;
        }
        return LicenseStatus.VALID;
    }

    private boolean verifySignature(LicenseInfo info) {
        try {
            Signature signature = Sm2Support.signature();
            signature.initVerify(loadPublicKey());
            signature.update(info.canonicalPayload().getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(info.getSignature()));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private PublicKey loadPublicKey() throws Exception {
        byte[] data = Base64.getDecoder().decode(resolveObfuscatedPublicKey());
        return Sm2Support.loadPublicKey(data);
    }

    private String resolveObfuscatedPublicKey() {
        StringBuilder builder = new StringBuilder(128);
        for (int[] segment : SM2_PUBLIC_KEY_SEGMENTS) {
            for (int value : segment) {
                builder.append((char) (value ^ PUBLIC_KEY_MASK));
            }
        }
        return builder.toString();
    }

    private String statusText(LicenseStatus status, LicenseInfo info) {
        return switch (status) {
            case VALID -> "Valid license"
                    + (info == null || info.getExpiresAt() == null ? "" : ", expires " + info.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
            case MISSING -> "License file not imported";
            case INVALID_FILE -> "Invalid license file";
            case INVALID_SIGNATURE -> "License signature verification failed";
            case EXPIRED -> "License expired";
            case DEVICE_MISMATCH -> "Machine code mismatch";
        };
    }

    private LicenseInfo readLicenseInfo(File licenseFile) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(licenseFile)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return LicenseInfo.fromProperties(properties);
        }
    }

    private void writeLicensePointer(File source) throws Exception {
        Files.writeString(getLicensePointerFile().toPath(), source.getAbsolutePath(), StandardCharsets.UTF_8);
    }

    private File resolveActiveLicenseFile() {
        File pointerFile = getLicensePointerFile();
        if (pointerFile.exists()) {
            try {
                String configuredPath = Files.readString(pointerFile.toPath(), StandardCharsets.UTF_8).trim();
                if (!configuredPath.isEmpty()) {
                    return new File(configuredPath);
                }
            } catch (Exception ignored) {
            }
        }
        File legacyLicenseFile = getLegacyLicenseFile();
        return legacyLicenseFile.exists() ? legacyLicenseFile : null;
    }

    private File getLicensePointerFile() {
        return new File(PathUtils.getDataPath(), LICENSE_POINTER_FILE_NAME);
    }

    private File getLegacyLicenseFile() {
        return new File(PathUtils.getDataPath(), LICENSE_FILE_NAME);
    }
}
