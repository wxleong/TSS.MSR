package tss;

public class TpmSyncRunnable implements Runnable {

    private static final int DEFAULT_TIMEOUT = 5000;

    Tpm tpm;
    Object notifyEndingOrCommandReady;
    Object notifyResponseReady;
    Object notifyEnding;
    volatile boolean isEnded;
    volatile boolean noError;
    TpmDeviceHook tpmDeviceHook;
    TpmCommandSet tpmCommandSet;
    int timeout; // milliseconds

    public TpmSyncRunnable(TpmCommandSet tpmCommandSet, int timeout) {
        tpm = new Tpm();
        tpmDeviceHook = new TpmDeviceHook();
        tpm._setDevice(tpmDeviceHook);
        this.tpmCommandSet = tpmCommandSet;
        this.timeout = timeout;

        notifyEndingOrCommandReady = new Object();
        notifyResponseReady = new Object();
        notifyEnding = new Object();
        isEnded = false;
        noError = true;
    }

    /**
     * Wait for TPM command availability or completion
     * The calling thread stops its execution until notify()
     * @return true if command is ready or execution has completed
     *         false if timeout has occurred
     */
    public boolean waitForCommandOrEnding(int timeout) {
        synchronized (notifyEndingOrCommandReady) {
            try {
                if (!isCommandReady() && !isEnded)
                    notifyEndingOrCommandReady.wait(timeout);
            } catch (Exception e) {
            }
        }

        if (!isCommandReady() && !isEnded)
            return false; /* timeout occurred */

        return true;
    }

    public boolean waitForEnding(int timeout) {
        synchronized (notifyEnding) {
            try {
                if (!isEnded)
                    notifyEnding.wait(timeout);
            } catch (Exception e) {
            }
        }
        if (!isEnded)
            return false; /* timeout occurred */

        return true;
    }

    public boolean isEnded() {
        return isEnded;
    }

    /**
     * This flag is meaningful only if isEnded is set
     * @return
     */
    public boolean isEndedOk() { return noError; }

    @Override
    public void run() {
        try {
            tpmCommandSet.run(tpm);
        } catch (Exception e) {
            noError = false;
            //throw e;
        } finally {
            synchronized (notifyEnding) {
                synchronized (notifyEndingOrCommandReady) {
                    isEnded = true;
                    notifyEnding.notify();
                    notifyEndingOrCommandReady.notify();
                }
            }
        }
    }

    public boolean isCommandReady() {
        return tpmDeviceHook.isCommandReady;
    }

    public byte[] getCommandBuffer() {
        return tpmDeviceHook.cmdBuffer;
    }

    /**
     * Provide TPM response so the TpmCommandSet may continue to process
     * @param respBuf
     */
    public void responseReady(byte[] respBuf) {
        synchronized (notifyResponseReady) {
            tpmDeviceHook.setResponseBuffer(respBuf);
            notifyResponseReady.notify();
        }
    }

    public void interrupt() {
        synchronized (notifyResponseReady) {
            notifyResponseReady.notify();
        }
    }

    public interface TpmCommandSet {
        void run(Tpm tpm);
    }

    class TpmDeviceHook extends TpmDevice {

        byte[] cmdBuffer;
        byte[] rspBuffer;
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

        /**
         * Reaches here when TPM command is available from
         * processing TpmCommandSet
         * @param cmdBuf  TPM command buffer
         */
        @Override
        public void dispatchCommand(byte[] cmdBuf) {

            /**
             * .clone() has an unknown bug where cmdBuffer intermittently becomes null...
             * Avoid .clone() for now.
             *
             * To reproduce this issue, swap the following code with the commented code and
             * run all the tests in TPMSyncRunnableTests concurrently, notice some
             * tests will fail.
             */
            //cmdBuffer = cmdBuf.clone();
            cmdBuffer = new byte[cmdBuf.length];
            System.arraycopy(cmdBuf, 0, cmdBuffer, 0, cmdBuf.length);

            rspBuffer = null;
            isResponseReady = false;

            if (notifyEndingOrCommandReady != null) {
                synchronized (notifyEndingOrCommandReady) {
                    isCommandReady = true;
                    notifyEndingOrCommandReady.notify();
                }
            } else {
                isCommandReady = true;
            }

            synchronized (notifyResponseReady) {
                try {
                    if (!isResponseReady)
                        notifyResponseReady.wait(timeout);
                } catch (InterruptedException e) {
                    /* use interrupt to wake up the thread */
                } catch (Exception e) {
                    throw new TpmException(e.getMessage());
                }
            }

            if (!isResponseReady)
                throw new TpmException("timeout occurred, waited for TPM response.");
        }

        @Override
        public byte[] getResponse() {
            return rspBuffer;
        }

        @Override
        public boolean responseReady() {
            return isResponseReady;
        }

        @Override
        public void close() {

        }

        public void setResponseBuffer(byte[] respBuf) {
            //rspBuffer = respBuf.clone(); // bug, check above
            rspBuffer = new byte[respBuf.length];
            System.arraycopy(respBuf, 0, rspBuffer, 0, respBuf.length);
            isResponseReady = true;
            isCommandReady = false;
        }
    }
}
