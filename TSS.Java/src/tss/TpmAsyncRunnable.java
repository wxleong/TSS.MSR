package tss;

import java.util.concurrent.TimeoutException;

public class TpmAsyncRunnable implements Runnable {

    private static final int DEFAULT_TIMEOUT = 5000;

    Tpm tpm;
    TpmDeviceHook tpmDeviceHook;
    TpmCommandSet tpmCommandSet;
    int timeout; // milliseconds

    public TpmAsyncRunnable(TpmCommandSet tpmCommandSet, int timeout) {
        tpm = new Tpm();
        tpmDeviceHook = new TpmDeviceHook();
        tpm._setDevice(tpmDeviceHook);
        this.tpmCommandSet = tpmCommandSet;
        this.timeout = timeout;
    }

    public TpmAsyncRunnable(TpmCommandSet tpmCommandSet) {
        this(tpmCommandSet, DEFAULT_TIMEOUT);
    }

    @Override
    public void run() {
        try {
            tpmCommandSet.run(tpm);
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean txReady() {
        return tpmDeviceHook.isCommandReady;
    }

    public byte[] getTxBuffer() {
        return tpmDeviceHook.txBuffer;
    }

    public void rxReady(byte[] cmdBuf) {
        tpmDeviceHook.setRxBuffer(cmdBuf);
        synchronized (tpmDeviceHook) {
            tpmDeviceHook.notify();
        }
    }

    public void interrupt(Thread thread) {
        thread.interrupt();
    }

    public interface TpmCommandSet {
        void run(Tpm tpm);
    }

    class TpmDeviceHook extends TpmDevice {

        byte[] txBuffer;
        byte[] rxBuffer;
        volatile boolean isCommandReady;
        volatile boolean isResponseReady;

        public TpmDeviceHook() {
            isCommandReady = false;
            isResponseReady = false;
        }

        @Override
        public boolean connect() {
            return true;
        }

        @Override
        public void dispatchCommand(byte[] cmdBuf) {
            isCommandReady = true;
            isResponseReady = false;

            /**
             * .clone() has an unknown bug where txBuffer intermittently becomes null...
             * Avoid .clone() for now.
             *
             * To reproduce this issue, swap the following code with the commented code and
             * run all the tests in TPMAsyncRunnableTests concurrently, notice some
             * tests will fail.
             */
            //txBuffer = cmdBuf.clone();
            txBuffer = new byte[cmdBuf.length];
            System.arraycopy(cmdBuf, 0, txBuffer, 0, cmdBuf.length);

            rxBuffer = null;
            synchronized (this) {
                try {
                    this.wait(timeout);
                } catch (InterruptedException e) {
                    /* use interrupt to wake up the thread */
                }
            }
        }

        @Override
        public byte[] getResponse() {
            return rxBuffer;
        }

        @Override
        public boolean responseReady() {
            return isResponseReady;
        }

        @Override
        public void close() {

        }

        public void setRxBuffer(byte[] respBuf) {
            //rxBuffer = respBuf.clone(); // bug, check above
            rxBuffer = new byte[respBuf.length];
            System.arraycopy(respBuf, 0, rxBuffer, 0, respBuf.length);
            isResponseReady = true;
            isCommandReady = false;
        }
    }
}
