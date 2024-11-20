package io.termd.core.ssh;

import io.termd.core.TestBase;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class SshSession {

    ClientSession session;
    ChannelShell channel;
    InputStream in;
    OutputStream out;

    private String termType;

    public String termType() {
        return termType;
    }

    public SshSession termType(String termType) {
        this.termType = termType;
        return this;
    }

    public void disconnect() throws IOException {
        session.disconnect(0, "bye");
    }

    public SshSession write(byte[] bytes) throws IOException {
        out.write(bytes);
        out.flush();
        return this;
    }

    public byte[] read(int len) throws IOException {
        byte[] buf = new byte[len];
        while (len > 0) {
            int count = in.read(buf, buf.length - len, len);
            if (count == -1) {
                throw new AssertionError("Could not read enough");
            }
            len -= count;
        }
        return buf;
    }

    public boolean isClosed() {
        return channel.isClosed();
    }

    public int exitStatus() {
        return channel.getExitStatus();
    }

    public boolean checkDisconnect() {
        try {
            return in != null && in.read() == -1;
        } catch (IOException e) {
            throw TestBase.failure(e);
        }
    }

    public void connect() throws Exception {
        org.apache.sshd.client.SshClient client = org.apache.sshd.client.SshClient.setUpDefaultClient();
        client.start();
        ClientSession sess = client
                .connect("whatever", "localhost", 5000)
                .verify()
                .getSession();
        sess.addPasswordIdentity("whocares");
        sess.auth().verify(TimeUnit.SECONDS.toMillis(5000));
        session = sess;
        channel = session.createShellChannel();
        if (termType != null) {
            channel.setPtyType(termType);
        }
        PipedInputStream p1 = new PipedInputStream();
        PipedOutputStream p2 = new PipedOutputStream(p1);
        PipedInputStream p3 = new PipedInputStream();
        PipedOutputStream p4 = new PipedOutputStream(p3);
        channel.setIn(p1);
        channel.setOut(p4);
        channel.open().verify();
        in = p3;
        out = p2;
    }

    public void close() {
        if (out != null) {
            try { out.close(); } catch (Exception ignore) {}
        }
        if (in != null) {
            try { in.close(); } catch (Exception ignore) {}
        }
        if (channel != null) {
//      try { channel.disconnect(); } catch (Exception ignore) {}
        }
        if (session != null) {
//      try { session.disconnect(); } catch (Exception ignore) {}
        }
    }

}
