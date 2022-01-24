package tss;
import javax.net.SocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;

public class TpmDeviceSSLTunnelClient extends TpmDevice
{
    protected Socket CommandSocket = null;
    String hostName;
    int port;

    boolean responsePending;
    int currentLocality;

    public TpmDeviceSSLTunnelClient(String hostName, int port)
    {
        init(hostName, port);
    }
    

    void init(String hostName, int port)
    {
        this.hostName = hostName;
        this.port = port;
    }

    @Override
    public boolean connect()
    {
        try {
            SocketFactory socketFactory = SSLSocketFactory.getDefault();
            CommandSocket = socketFactory.createSocket(hostName, port);

            SSLSocket sslSocket = (SSLSocket) CommandSocket;
            sslSocket.setEnabledCipherSuites(
                    new String[] { "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256" });
            sslSocket.setEnabledProtocols(
                    new String[] { "TLSv1.3" });

            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            sslSocket.setSSLParameters(sslParams);

        } catch (Exception e) {
            if (CommandSocket != null)
                try { CommandSocket.close(); } catch (IOException ioe) {}
            System.err.println("Failed to connect to the TPM at " + hostName + ":" + 
                               port + ": " +  e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void close()
    {
        if (CommandSocket != null) {
            try { CommandSocket.close(); } catch (IOException ioe) {}
            CommandSocket = null;
        }
    }

    @Override
    public void dispatchCommand(byte[] commandBuffer) 
    {
        writeBuf(CommandSocket, new byte[] {(byte) currentLocality});
        writeInt(CommandSocket, commandBuffer.length);
        try {
            CommandSocket.getOutputStream().write(commandBuffer);
            responsePending = true;
        } catch (IOException e) {
            throw new TpmException("Error sending data to the TPM", e);
        }
    }
    
    @Override
    public byte[] getResponse()
    {
        if(!responsePending)
        {
            throw new TpmException("Cannot getResponse() without a prior dispatchCommand()");
        }
        responsePending = false;
        byte[] outBuf = readEncapsulated(CommandSocket);
        return outBuf;
    }
    
    @Override
    public boolean responseReady()
    {
        if(!responsePending)
        {
            throw new TpmException("Cannot responseReady() without a prior dispatchCommand()");
        }
        int available;
        try {
            available = CommandSocket.getInputStream().available();
        } catch (IOException e) {
            throw new TpmException("Error getting data from the TPM", e);
        }
        return (available>0);
    }

    @Override
    public void setLocality(int locality)  
    {
        currentLocality = locality;
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
