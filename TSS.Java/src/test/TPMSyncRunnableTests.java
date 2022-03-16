package test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tss.Helpers;
import tss.TpmDeviceTbs;
import tss.TpmSyncRunnable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Synchronous/Blocking approach is useful for REST service implementation
 * In this approach an application can be coded as usual using TSS.Java apis and run as a thread (TpmSyncRunnable -> TpmCommandSet)
 * The running thread will be put to sleep whenever a TPM command is ready.
 * The command can be retrieved from the thread, transfer to any TPM around the globe for processing.
 * To wake up the thread, the response from a TPM has to be fed to the thread.
 * This process continues until all TPM commands are executed or a timeout has occurred
 */
public class TPMSyncRunnableTests {

    /**
     * - get random test
     * - use latch to wait for thread completion
     */
    @Test
    public void TestOK() {
        try {
            AtomicReference<Exception> savedException = new AtomicReference<>();

            // Setup a latch
            CountDownLatch latch = new CountDownLatch(1);

            TpmSyncRunnable tpmSyncRunnable = new TpmSyncRunnable((tpm) -> {
                try {
                    byte[] r = tpm.GetRandom(8);
                    System.out.println("GetRandom random bytes: " + Helpers.toHex(r));
                } catch (Exception e) {
                    //e.printStackTrace();
                    savedException.set(e);
                    throw e;
                } finally {
                    // release the latch
                    latch.countDown();
                }
            });
            Thread tpmSyncThread = new Thread(tpmSyncRunnable);
            tpmSyncThread.start();

            /* get TPM command */
            tpmSyncRunnable.waitForCommand(5000);
            byte[] txBuffer = tpmSyncRunnable.getCommandBuffer();
            System.out.println("GetRandom Tx command byte stream: " + Helpers.arrayToString(txBuffer));

            /* feed the command to Windows' TPM to obtain the response */
            TpmDeviceTbs tpmDeviceTbs = new TpmDeviceTbs();
            tpmDeviceTbs.connect();
            tpmDeviceTbs.dispatchCommand(txBuffer);
            while(!tpmDeviceTbs.responseReady());
            byte[] rxBuffer = tpmDeviceTbs.getResponse();
            System.out.println("GetRandom Rx command byte stream: " + Helpers.arrayToString(rxBuffer));

            /* process the response */
            tpmSyncRunnable.responseReady(rxBuffer);

            // wait for child thread do not terminate prematurely
            latch.await();

            Assertions.assertNull(savedException.get());
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    /**
     * - get random test
     * - use Thread.join to wait for thread completion
     */
    @Test
    public void TestOK2() {
        try {
            int threadTimeoutMs = 5000;
            AtomicReference<Exception> savedException = new AtomicReference<>();

            TpmSyncRunnable tpmSyncRunnable = new TpmSyncRunnable((tpm) -> {
                try {
                    byte[] r = tpm.GetRandom(8);
                    System.out.println("GetRandom random bytes: " + Helpers.toHex(r));
                } catch (Exception e) {
                    //e.printStackTrace();
                    savedException.set(e);
                    throw e;
                }
            }, threadTimeoutMs);
            Thread tpmSyncThread = new Thread(tpmSyncRunnable);
            tpmSyncThread.start();

            /* get TPM command */
            tpmSyncRunnable.waitForCommand(5000);
            byte[] txBuffer = tpmSyncRunnable.getCommandBuffer();
            System.out.println("GetRandom Tx command byte stream: " + Helpers.arrayToString(txBuffer));

            /* feed the command to Windows' TPM to obtain the response */
            TpmDeviceTbs tpmDeviceTbs = new TpmDeviceTbs();
            tpmDeviceTbs.connect();
            tpmDeviceTbs.dispatchCommand(txBuffer);
            while(!tpmDeviceTbs.responseReady());
            byte[] rxBuffer = tpmDeviceTbs.getResponse();
            System.out.println("GetRandom Rx command byte stream: " + Helpers.arrayToString(rxBuffer));

            /* process the response */
            tpmSyncRunnable.responseReady(rxBuffer);

            // join the child thread so parent thread will not terminate prematurely
            tpmSyncThread.join(threadTimeoutMs);

            Assertions.assertNull(savedException.get());
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    /**
     * Manually interrupt a running thread to trigger timeout error
     */
    @Test
    public void testInterrupt() {
        try {
            AtomicReference<Exception> savedException = new AtomicReference<>();

            // Setup a latch
            CountDownLatch latch = new CountDownLatch(1);

            TpmSyncRunnable tpmSyncRunnable = new TpmSyncRunnable((tpm) -> {
                try {
                    byte[] r = tpm.GetRandom(8);
                    System.out.println("GetRandom: " + Helpers.toHex(r));
                } catch (Exception e) {
                    //e.printStackTrace();
                    savedException.set(e);
                    throw e;
                } finally {
                    // release the latch
                    latch.countDown();
                }
            });
            Thread tpmSyncThread = new Thread(tpmSyncRunnable);
            tpmSyncThread.start();

            /* get TPM command */
            tpmSyncRunnable.waitForCommand(5000);
            byte[] txBuffer = tpmSyncRunnable.getCommandBuffer();
            System.out.println("GetRandom TX command byte stream: " + Helpers.arrayToString(txBuffer));

            // interrupt
            tpmSyncThread.interrupt();

            // wait for child thread do not terminate prematurely
            latch.await();

            Assertions.assertNotNull(savedException.get());
            Assertions.assertEquals(savedException.get().getMessage(), "timeout occurred, waited for TPM response.");
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    /**
     * Thread timeout test, set timeout to 1ms
     */
    @Test
    public void testResponseTimeout() {
        try {
            AtomicReference<Exception> savedException = new AtomicReference<>();

            // Setup a latch
            CountDownLatch latch = new CountDownLatch(1);

            TpmSyncRunnable tpmSyncRunnable = new TpmSyncRunnable((tpm) -> {
                try {
                    byte[] r = tpm.GetRandom(8);
                    System.out.println("GetRandom: " + Helpers.toHex(r));
                } catch (Exception e) {
                    //e.printStackTrace();
                    savedException.set(e);
                    throw e;
                } finally {
                    // release the latch
                    latch.countDown();
                }
            }, 1);
            Thread tpmSyncThread = new Thread(tpmSyncRunnable);
            tpmSyncThread.start();

            /* get TPM command */
            tpmSyncRunnable.waitForCommand(5000);
            byte[] txBuffer = tpmSyncRunnable.getCommandBuffer();
            System.out.println("GetRandom TX command byte stream: " + Helpers.arrayToString(txBuffer));

            /* wait for timeout */

            // wait for child thread do not terminate prematurely
            latch.await();

            Assertions.assertNotNull(savedException.get());
            Assertions.assertEquals(savedException.get().getMessage(), "timeout occurred, waited for TPM response.");
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    /**
     * Test TPM command timeout
     */
    @Test
    public void testCommandTimeout() {
        try {
            AtomicReference<Exception> savedException = new AtomicReference<>();

            // Setup latches
            CountDownLatch latch = new CountDownLatch(1);
            CountDownLatch delay = new CountDownLatch(1);

            TpmSyncRunnable tpmSyncRunnable = new TpmSyncRunnable((tpm) -> {
                try {
                    // introduce a delay to trigger get TPM command timeout
                    try {
                        delay.await();
                    } catch (Exception e) {}

                    byte[] r = tpm.GetRandom(8);
                    System.out.println("GetRandom: " + Helpers.toHex(r));
                } catch (Exception e) {
                    //e.printStackTrace();
                    savedException.set(e);
                    throw e;
                } finally {
                    // release the latch
                    latch.countDown();
                }
            }, 1);
            Thread tpmSyncThread = new Thread(tpmSyncRunnable);
            tpmSyncThread.start();

            /* get TPM command, expecting timeout */
            boolean isOk = tpmSyncRunnable.waitForCommand(1);
            Assertions.assertFalse(isOk);

            byte[] txBuffer = tpmSyncRunnable.getCommandBuffer();
            Assertions.assertNull(txBuffer);

            /* release delay */
            delay.countDown();

            /* re-try get TPM command */
            isOk = tpmSyncRunnable.waitForCommand(5000);
            Assertions.assertTrue(isOk);

            txBuffer = tpmSyncRunnable.getCommandBuffer();
            Assertions.assertNotNull(txBuffer);

            // wait for child thread do not terminate prematurely
            latch.await();

            Assertions.assertNotNull(savedException.get());
            Assertions.assertEquals(savedException.get().getMessage(), "timeout occurred, waited for TPM response.");
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }
}
