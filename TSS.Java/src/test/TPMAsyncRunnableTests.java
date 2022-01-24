package test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tss.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Async approach is useful for REST service implementation
 * The strategy is TPM commands can be coded as usual and launch as a thread (TpmAsyncRunnable -> TpmCommandSet)
 * The running thread will be put to sleep whenever there is a pending TPM request.
 * The request can be retrieved from the thread, transfer to any TPM around the globe for processing.
 * To wake up the thread, the response from a TPM has to be fed to the thread.
 * This process continues until all TPM commands are executed or there is a timeout occurred
 */
public class TPMAsyncRunnableTests {

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

            TpmAsyncRunnable tpmAsyncRunnable = new TpmAsyncRunnable((tpm) -> {
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
            Thread tpmAsyncThread = new Thread(tpmAsyncRunnable);
            tpmAsyncThread.start();

            /* get TPM request */
            while(!tpmAsyncRunnable.txReady());
            byte[] txBuffer = tpmAsyncRunnable.getTxBuffer();
            System.out.println("GetRandom Tx command byte stream: " + Helpers.arrayToString(txBuffer));

            /* feed the request to Windows' TPM to obtain the response */
            TpmDeviceTbs tpmDeviceTbs = new TpmDeviceTbs();
            tpmDeviceTbs.connect();
            tpmDeviceTbs.dispatchCommand(txBuffer);
            while(!tpmDeviceTbs.responseReady());
            byte[] rxBuffer = tpmDeviceTbs.getResponse();
            System.out.println("GetRandom Rx command byte stream: " + Helpers.arrayToString(rxBuffer));

            /* process the response */
            tpmAsyncRunnable.rxReady(rxBuffer);

            // wait for child thread do not terminate prematurely
            latch.await();

            if (savedException.get() != null)
                throw savedException.get();
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

            TpmAsyncRunnable tpmAsyncRunnable = new TpmAsyncRunnable((tpm) -> {
                try {
                    byte[] r = tpm.GetRandom(8);
                    System.out.println("GetRandom random bytes: " + Helpers.toHex(r));
                } catch (Exception e) {
                    //e.printStackTrace();
                    savedException.set(e);
                    throw e;
                }
            }, threadTimeoutMs);
            Thread tpmAsyncThread = new Thread(tpmAsyncRunnable);
            tpmAsyncThread.start();

            /* get TPM request */
            while(!tpmAsyncRunnable.txReady());
            byte[] txBuffer = tpmAsyncRunnable.getTxBuffer();
            System.out.println("GetRandom Tx command byte stream: " + Helpers.arrayToString(txBuffer));

            /* feed the request to Windows' TPM to obtain the response */
            TpmDeviceTbs tpmDeviceTbs = new TpmDeviceTbs();
            tpmDeviceTbs.connect();
            tpmDeviceTbs.dispatchCommand(txBuffer);
            while(!tpmDeviceTbs.responseReady());
            byte[] rxBuffer = tpmDeviceTbs.getResponse();
            System.out.println("GetRandom Rx command byte stream: " + Helpers.arrayToString(rxBuffer));

            /* process the response */
            tpmAsyncRunnable.rxReady(rxBuffer);

            // join the child thread so parent thread will not terminate prematurely
            tpmAsyncThread.join(threadTimeoutMs);

            if (savedException.get() != null)
                throw savedException.get();
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    /**
     * Manually interrupt a running thread
     *
     * Test should pass with Exception message:
     * java.lang.NullPointerException: Cannot read the array length because "array" is null
     */
    @Test
    public void testInterrupt() {
        try {
            AtomicReference<Exception> savedException = new AtomicReference<>();

            // Setup a latch
            CountDownLatch latch = new CountDownLatch(1);

            TpmAsyncRunnable tpmAsyncRunnable = new TpmAsyncRunnable((tpm) -> {
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
            Thread tpmAsyncThread = new Thread(tpmAsyncRunnable);
            tpmAsyncThread.start();

            /* get TPM request */
            while(!tpmAsyncRunnable.txReady());
            byte[] txBuffer = tpmAsyncRunnable.getTxBuffer();
            System.out.println("GetRandom TX command byte stream: " + Helpers.arrayToString(txBuffer));

            // interrupt
            tpmAsyncRunnable.interrupt(tpmAsyncThread);

            // wait for child thread do not terminate prematurely
            latch.await();

            if (savedException.get() == null)
                throw new Exception("No exception found");
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    /**
     * Thread timeout test, set timeout to 1ms
     *
     * Test should pass with Exception message:
     * java.lang.NullPointerException: Cannot read the array length because "array" is null
     */
    @Test
    public void testTimeout() {
        try {
            AtomicReference<Exception> savedException = new AtomicReference<>();

            // Setup a latch
            CountDownLatch latch = new CountDownLatch(1);

            TpmAsyncRunnable tpmAsyncRunnable = new TpmAsyncRunnable((tpm) -> {
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
            Thread tpmAsyncThread = new Thread(tpmAsyncRunnable);
            tpmAsyncThread.start();

            /* get TPM request */
            while(!tpmAsyncRunnable.txReady());
            byte[] txBuffer = tpmAsyncRunnable.getTxBuffer();
            System.out.println("GetRandom TX command byte stream: " + Helpers.arrayToString(txBuffer));

            /* wait for timeout */

            // wait for child thread do not terminate prematurely
            latch.await();

            if (savedException.get() == null)
                throw new TimeoutException();
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }
}
