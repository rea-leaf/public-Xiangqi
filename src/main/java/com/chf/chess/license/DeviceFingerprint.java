package com.chf.chess.license;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;

/**
 * 生成本机设备码，用于授权绑定。
 */
public final class DeviceFingerprint {

    private DeviceFingerprint() {
    }

    public static String machineCode() {
        try {
            Sm2Support.signature();
            List<String> seeds = new ArrayList<>();
            seeds.add(System.getProperty("os.name", ""));
            seeds.add(System.getProperty("os.arch", ""));
            seeds.add(System.getenv().getOrDefault("COMPUTERNAME", ""));
            seeds.add(System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", ""));
            seeds.add(resolveHostName());
            seeds.addAll(resolveMacs());

            MessageDigest digest = MessageDigest.getInstance("SM3", Sm2Support.PROVIDER);
            byte[] value = digest.digest(String.join("|", seeds).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12 && i < value.length; i++) {
                if (i > 0 && i % 3 == 0) {
                    sb.append('-');
                }
                sb.append(String.format("%02X", value[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNBOUND-DEVICE";
        }
    }

    private static String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> resolveMacs() {
        List<String> macs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) {
                    continue;
                }
                byte[] mac = network.getHardwareAddress();
                if (mac == null || mac.length == 0) {
                    continue;
                }
                try (Formatter formatter = new Formatter()) {
                    for (byte b : mac) {
                        formatter.format("%02X", b);
                    }
                    macs.add(formatter.toString());
                }
            }
        } catch (Exception ignored) {
        }
        Collections.sort(macs);
        return macs;
    }
}
