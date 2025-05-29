package io.termd.core.ssh;

import examples.shell.Shell;
import io.termd.core.ssh.netty.NettySshTtyBootstrap;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SshTtyCommandTest {

    public static final int TIMEOUT_SECS = 5;
    public static final int REPETITIONS = 10;

    @Parameterized.Parameters
    public static Object[][] data() {
        return new Object[REPETITIONS][0];
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            // Configura il server SSH (basato su SshShellExample)
            NettySshTtyBootstrap bootstrap = new NettySshTtyBootstrap()
                    .setPort(5000)
                    .setHost("localhost");

            bootstrap.start(new Shell()).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail();
        }
    }


    @Test
    public void runCommandViaSSHTest() {
        AtomicReference<String> result = new AtomicReference<>();

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("user", "localhost", 5000)
                    .verify(TIMEOUT_SECS, TimeUnit.SECONDS)
                    .getClientSession()) {
                session.addPasswordIdentity("password");
                session.auth().verify(TIMEOUT_SECS, TimeUnit.SECONDS);
                try (
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
                ) {
                    try (ChannelShell channel = session.createShellChannel()) {
                        channel.setOut(outputStream);

                        channel.setErr(new NoCloseOutputStream(System.err));
                        channel.open().verify(TIMEOUT_SECS, TimeUnit.SECONDS);

                        OutputStream pipedIn = channel.getInvertedIn();
                        // resets all data in the output stream

                        String expectedRes = "hello world 1 w frsdfpa";
                        pipedIn.write(("echo " + expectedRes + "\n").getBytes());
                        pipedIn.flush();
                        channel.waitFor(Arrays.asList(
                                ClientChannelEvent.STDOUT_DATA,
                                ClientChannelEvent.EOF
                        ), TimeUnit.SECONDS.toMillis(2L));

                        result.set(outputStream.toString());
                        String expected = "Welcome to Term.d shell example\n" +
                                "\n" +
                                "% echo "+expectedRes+"\n" +
                                expectedRes+"\n" +
                                "% ";

                        expected = expected.replaceAll("(\r|\n)", "");
                        String actual = result.get().replaceAll("(\r|\n)", "");
                        System.out.println("Result:\n" + result.get());
                        assertEquals(expected, actual);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
