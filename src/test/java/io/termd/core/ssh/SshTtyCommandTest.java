package io.termd.core.ssh;

import examples.shell.Shell;
import io.termd.core.ssh.netty.NettySshTtyBootstrap;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.fail;

public class SshTtyCommandTest {

    public static final int TIMEOUT_SECS = 5;
    public static final int REPETITIONS = 1;


    @Test
    public void runCommandViaSSHTest() throws InterruptedException {

        try {
            // Configura il server SSH (basato su SshShellExample)
            NettySshTtyBootstrap bootstrap = new NettySshTtyBootstrap()
                    .setPort(5000)
                    .setHost("localhost");

            bootstrap.start(new Shell()).get(10, TimeUnit.SECONDS);
            System.out.println("SSH server started on localhost:5000");
        } catch (Exception e) {
            fail();
        }

        AtomicReference<String> result = new AtomicReference<>();

        // Avvia client SSH in un thread separato
        Thread clientThread = new Thread(() -> {
            try (SshClient client = SshClient.setUpDefaultClient()) {
                client.start();

                try (ClientSession session = client.connect("user", "localhost", 5000)
                        .verify(TIMEOUT_SECS, TimeUnit.SECONDS)
                        .getClientSession()) {
                    session.addPasswordIdentity("password");
                    session.auth().verify(TIMEOUT_SECS, TimeUnit.SECONDS);
                    byte[] output = new byte[16];
                    try (
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(output)
                    ) {
                        try (ChannelShell channel = session.createShellChannel()) {
                            channel.setIn(inputStream);
                            channel.setOut(outputStream);

                            channel.setErr(new NoCloseOutputStream(System.err));
                            channel.open().verify(TIMEOUT_SECS, TimeUnit.SECONDS);

                            for (int i = 0; i < REPETITIONS; i++) {
                                String expectedOutput = "hello world " + i;

                                OutputStream pipedIn = channel.getInvertedIn();
                                pipedIn.write(("echo " + expectedOutput).getBytes());
                                pipedIn.flush();

                                Thread.sleep(200); // dai tempo alla risposta
                                result.set(outputStream.toString());

                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(1000);
        clientThread.start();
        clientThread.join();

        System.out.println("Result:\n"+ result.get());

    }
}
