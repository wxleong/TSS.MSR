package tss;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TpmDeviceTunnelServerRunnable implements Runnable {
    private ServerSocket server;
    private TpmDevice tpmDevice;
    boolean stopServer;
    boolean stopSocket;

    public TpmDeviceTunnelServerRunnable(int port, TpmDevice tpmDevice) throws Exception {
        server = new ServerSocket(port);
        this.tpmDevice = tpmDevice;
        if (!tpmDevice.connect())
            throw new Exception("TpmDeviceTunnelServerRunnable constructor error");
        stopSocket = true;
        stopServer = false;
    }

    public void run() {
        try {
            while (!stopServer) {
                Socket socket = server.accept();
                stopSocket = false;

                try (
                    OutputStream out = socket.getOutputStream();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                ) {
                    while (!stopSocket) {
                        byte locality = in.readByte();
                        byte incoming[] = readEncapsulated(socket);
                        try {
                            tpmDevice.setLocality(Byte.toUnsignedInt(locality));
                        } catch (UnsupportedOperationException e) {
                            // not all TpmDevice support setLocality
                        }
                        tpmDevice.dispatchCommand(incoming);

                        while(!tpmDevice.responseReady());

                        byte outgoing[] = tpmDevice.getResponse();
                        writeInt(socket, outgoing.length);
                        out.write(outgoing);
                    }
                }
                socket.close();
            }
            tpmDevice.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeSocket() {
        stopSocket = true;
    }

    public void closeServer() {
        stopServer = true;
    }

    private int readInt(Socket s)
    {
        int val=-1;
        try {
            val = Helpers.netToHost(readBuf(s, 4));
        } catch (Exception e) {
            throw new TpmException("TPM IO error", e);
        }
        return val;
    }

    private void writeInt(Socket s, int val)
    {
        writeBuf(s, Helpers.hostToNet(val));
    }

    private void writeBuf(Socket s, byte[] buffer)
    {
        try
        {
            s.getOutputStream().write(buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new TpmException("TPM IO error", e);
        }
    }

    private byte[] readBuf(Socket s, int numBytes)
    {
        byte[] buf = new byte[numBytes];
        int numRead = 0;
        while(numRead<numBytes)
        {
            int sz;
            try {
                sz = s.getInputStream().read(buf, numRead, numBytes-numRead);
            } catch (IOException e) {
                throw new TpmException("TPM IO error", e);
            }
            numRead+=sz;
        }
        return buf;
    }

    private void writeEncapsulated(Socket s, byte[] buf)
    {
        writeBuf(s, Helpers.hostToNet(buf.length));
        writeBuf(s, buf);
    }

    private byte[] readEncapsulated(Socket s)
    {
        byte[] t = readBuf(s, 4);
        int sz = Helpers.netToHost(t);
        return readBuf(s, sz);
    }
}
