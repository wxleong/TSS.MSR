package test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tss.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

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

            /* Use Windows' TPM */
            TpmDeviceTbs tpmDeviceTbs = new TpmDeviceTbs();
            tpmDeviceTbs.connect();
            tpmDeviceTbs.dispatchCommand(txBuffer);
            while(!tpmDeviceTbs.responseReady());
            byte[] rxBuffer = tpmDeviceTbs.getResponse();
            System.out.println("GetRandom Rx command byte stream: " + Helpers.arrayToString(rxBuffer));

            /* get TPM response */
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

            /* Use Windows' TPM */
            TpmDeviceTbs tpmDeviceTbs = new TpmDeviceTbs();
            tpmDeviceTbs.connect();
            tpmDeviceTbs.dispatchCommand(txBuffer);
            while(!tpmDeviceTbs.responseReady());
            byte[] rxBuffer = tpmDeviceTbs.getResponse();
            System.out.println("GetRandom Rx command byte stream: " + Helpers.arrayToString(rxBuffer));

            /* get TPM response */
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
     * Manually interrupt the running thread
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
