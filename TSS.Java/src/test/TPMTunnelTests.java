package test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tss.*;

public class TPMTunnelTests {

    @Test
    public void nonSecureTPMTbsTunnel() {
        try {
            /**
             * This is a device
             * ----------------
             * Open a port to expose the device's TPM over the network
             * This is useful for many reason, e.g., provisioning purposes, a full-blown TPM lib is not available on a device, ...
             */
            TpmDeviceTbs tpmDeviceTbs = new TpmDeviceTbs();
            TpmDeviceTunnelServerRunnable tpmServerRunnable = new TpmDeviceTunnelServerRunnable(1234, tpmDeviceTbs);
            Thread tpmServerThread = new Thread(tpmServerRunnable);
            tpmServerThread.start();

            /**
             * This is a cloud service
             * -----------------------
             * Make connection with the device then issue commands directly to the device's TPM TCTI layer
             */
            Tpm tpm = TpmFactory.remoteTpm("localhost", 1234, false);
            byte[] r = tpm.GetRandom(8);
            System.out.println("GetRandom: " + Helpers.toHex(r));
            r = tpm.GetRandom(16);
            System.out.println("GetRandom: " + Helpers.toHex(r));
            r = tpm.GetRandom(24);
            System.out.println("GetRandom: " + Helpers.toHex(r));
            r = tpm.GetRandom(32);
            System.out.println("GetRandom: " + Helpers.toHex(r));
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    /**
     * Disclaimer:
     * This test will fail. Keystore for server and client are not implemented yet.
     *
     * ref notes:
     * https://www.baeldung.com/java-ssl-handshake-failures
     * https://community.oracle.com/tech/developers/discussion/1533716/how-to-use-self-signed-certificate-with-sslserversocket
     */
    @Disabled("TLS will fail without Keystore implementation")
    @Test
    public void secureTPMTbsTunnel() {
        try {
            /**
             * This is a device
             * ----------------
             * Open a port to expose the device's TPM over the network
             * This is useful for many reason, e.g., provisioning purposes, a full-blown TPM lib is not available on a device, ...
             */
            TpmDeviceTbs tpmDeviceTbs = new TpmDeviceTbs();
            TpmDeviceSSLTunnelServerRunnable tpmServerRunnable = new TpmDeviceSSLTunnelServerRunnable(1234, tpmDeviceTbs);
            Thread tpmServerThread = new Thread(tpmServerRunnable);
            tpmServerThread.start();

            /**
             * This is a cloud service
             * -----------------------
             * Make connection with the device then issue commands directly to the device's TPM TCTI layer
             */
            Tpm tpm = TpmFactory.remoteTpm("localhost", 1234, true);
            byte[] r = tpm.GetRandom(8);
            System.out.println("GetRandom: " + Helpers.toHex(r));
            r = tpm.GetRandom(16);
            System.out.println("GetRandom: " + Helpers.toHex(r));
            r = tpm.GetRandom(24);
            System.out.println("GetRandom: " + Helpers.toHex(r));
            r = tpm.GetRandom(32);
            System.out.println("GetRandom: " + Helpers.toHex(r));
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }
}
