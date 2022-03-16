package tss;

public class TpmSyncRunnable implements Runnable {

    private static final int DEFAULT_TIMEOUT = 5000;

    Tpm tpm;
    Object notifyTarget;
    TpmDeviceHook tpmDeviceHook;
    TpmCommandSet tpmCommandSet;
    int timeout; // milliseconds

    public TpmSyncRunnable(TpmCommandSet tpmCommandSet, int timeout) {
        tpm = new Tpm();
        tpmDeviceHook = new TpmDeviceHook();
        tpm._setDevice(tpmDeviceHook);
        this.tpmCommandSet = tpmCommandSet;
        this.timeout = timeout;
        notifyTarget = this;
    }

    /**
     * Wait for TPM command availability
     * The calling thread stops its execution until notify()
     * @return
     */
    public boolean waitForCommand(int timeout) {
        synchronized (notifyTarget) {
            try {
                if (!isCommandReady())
                    notifyTarget.wait(timeout);
            } catch (Exception e) {
            }
        }

        if (!isCommandReady())
            return false; /* timeout occurred */

        return true;
    }

    public TpmSyncRunnable(TpmCommandSet tpmCommandSet) {
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

    public boolean isCommandReady() {
        return tpmDeviceHook.isCommandReady;
    }

    public byte[] getCommandBuffer() {
        return tpmDeviceHook.cmdBuffer;
    }

    /**
     * Provide TPM response so the TpmCommandSet may continue to process
     * @param cmdBuf
     */
    public void responseReady(byte[] cmdBuf) {
        synchronized (tpmDeviceHook) {
            tpmDeviceHook.setResponseBuffer(cmdBuf);
            tpmDeviceHook.notify();
        }
    }

    public interface TpmCommandSet {
        void run(Tpm tpm);
    }

    class TpmDeviceHook extends TpmDevice {

        byte[] cmdBuffer;
        byte[] rspBuffer;
        boolean isCommandReady;
        boolean isResponseReady;

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

            if (notifyTarget != null) {
                synchronized (notifyTarget) {
                    isCommandReady = true;
                    notifyTarget.notify();
                }
            } else {
                isCommandReady = true;
            }

            synchronized (this) {
                try {
                    if (!isResponseReady)
                        this.wait(timeout);
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
