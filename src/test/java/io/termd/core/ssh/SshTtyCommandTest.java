package io.termd.core.ssh;

import examples.shell.Shell;
import io.termd.core.ssh.netty.NettySshTtyBootstrap;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SshTtyCommandTest {

    public static final int TIMEOUT_SECS = 5;
    public static final int REPETITIONS = 2;

    @Parameterized.Parameters
    public static Object[][] data() {
        return new Object[REPETITIONS][0];
    }


    @Test
    public void runCommandViaSSHTest() {
        // Configura il server SSH (basato su SshShellExample)
        NettySshTtyBootstrap bootstrap = new NettySshTtyBootstrap()
                .setPort(5000)
                .setHost("localhost");
        try {

            bootstrap.start(new Shell()).get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            fail(e.getMessage());
        }


        String result = "";

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

                        StringBuilder expected = new StringBuilder();
                        expected.append("Welcome to Term.d shell example\n")
                                .append("\n")
                                .append("% ");

                        String expectedString = "";

                        // resets all data in the output stream
                        for (int i = 0; i < REPETITIONS; i++) {

                            String expectedRes = "hello world " + (i + 1);
                            pipedIn.write(("echo " + expectedRes + "\n").getBytes());
                            pipedIn.flush();
                            channel.waitFor(Arrays.asList(
                                    ClientChannelEvent.STDOUT_DATA,
                                    ClientChannelEvent.EOF
                            ), TimeUnit.SECONDS.toMillis(2L));

                            result = outputStream.toString();
                            expected.append("echo ").append(expectedRes).append("\n").append(expectedRes).append("\n").append("% ");

                            expectedString = expected.toString().replaceAll("(\r|\n)", "");
                            String actual = result.replaceAll("(\r|\n)", "");

                            assertEquals(expectedString, actual);
                        }
                    }
                }
            }

            bootstrap.stop();
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }
}
