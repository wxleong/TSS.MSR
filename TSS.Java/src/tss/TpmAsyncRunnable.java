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
            txBuffer = cmdBuf.clone();
            rxBuffer = null;
            synchronized (this) {
                try {
                    this.wait(timeout);
                    //Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    /* use interrupt to wake up the thread */
                    return;
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
            rxBuffer = respBuf.clone();
            isResponseReady = true;
            isCommandReady = false;
        }
    }
}
