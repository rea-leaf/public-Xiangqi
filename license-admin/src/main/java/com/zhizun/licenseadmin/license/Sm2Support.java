package com.zhizun.licenseadmin.license;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class Sm2Support {

    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    public static final String CURVE = "sm2p256v1";
    public static final String SIGN_ALGORITHM = "SM3withSM2";
    public static final String KEY_ALGORITHM = "EC";

    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Sm2Support() {
    }

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);
        generator.initialize(new ECGenParameterSpec(CURVE));
        return generator.generateKeyPair();
    }

    public static Signature signature() throws Exception {
        return Signature.getInstance(SIGN_ALGORITHM, PROVIDER);
    }

    public static PrivateKey loadPrivateKey(byte[] encoded) throws Exception {
        return KeyFactory.getInstance(KEY_ALGORITHM, PROVIDER)
                .generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    public static PublicKey loadPublicKey(byte[] encoded) throws Exception {
        return KeyFactory.getInstance(KEY_ALGORITHM, PROVIDER)
                .generatePublic(new X509EncodedKeySpec(encoded));
    }
}
