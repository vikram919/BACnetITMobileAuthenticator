package uni.rostock.de.bacnetitmobileauthenticator.transportBinding;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import uni.rostock.de.bacnetitmobileauthenticator.R;


public class ConfigureDTLS {
    // from ETSI Plugtest test spec
    public static final String PSK_IDENTITY = "password";
    public static final byte[] PSK_SECRET = "sesame".getBytes();

    // from demo-certs
    public static final String SERVER_NAME = "server";
    public static final String CLIENT_NAME = "client";
    private static final String TRUST_NAME = "root";
    private static final char[] TRUST_STORE_PASSWORD = "rootPass".toCharArray();
    private static final char[] KEY_STORE_PASSWORD = "endPass".toCharArray();

    public static void configureCredentials(DtlsConnectorConfig.Builder config, String name, boolean certificateMode,Context context) {

        try {
            // load key store
            KeyStore keyStore = KeyStore.getInstance("BKS");
            InputStream in =context.getResources().openRawResource(R.raw.keystore);
            keyStore.load(in, KEY_STORE_PASSWORD);
            if(in!=null){
                in.close();
            }
            KeyStore trustStore = KeyStore.getInstance("BKS");
            in = context.getResources().openRawResource(R.raw.truststore);
            trustStore.load(in, TRUST_STORE_PASSWORD);
            if(in!=null){
                in.close();
            }

            Certificate[] trustedCertificates = new Certificate[1];
            trustedCertificates[0] = trustStore.getCertificate("root");


            if (certificateMode) {
                config.setTrustStore(trustedCertificates);
                config.setIdentity((PrivateKey) keyStore.getKey(name, KEY_STORE_PASSWORD),
                        keyStore.getCertificateChain(name), true);
            } else {
                config.setIdentity((PrivateKey) keyStore.getKey(name, KEY_STORE_PASSWORD),
                        keyStore.getCertificateChain(name), true);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }
}
