/*
 * Copyright 2016 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.http.websocket;

import io.termd.core.http.websocket.client.Client;
import io.termd.core.http.websocket.server.TermServer;
import io.termd.core.util.MockProcess;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ExecuteCommandWithVerboseOutput {

  private static final String TEST_COMMAND = "java -cp ./target/test-classes/ " + MockProcess.class.getName();
  //private static final String TEST_COMMAND_PARAMS = "100 300 long";//OK
  //private static final String TEST_COMMAND_PARAMS = "1000 30 long";//OK
  //private static final String TEST_COMMAND_PARAMS = "1000 3 long"; //FAIL with Xmx64m; started with Xmx512m using ~250M
  //private static final String TEST_COMMAND_PARAMS = "1000 0 long"; //FAIL with Xmx64m; started with Xmx512m using ~250M
  private static final String TEST_COMMAND_PARAMS = "5000 0 long"; //high cpu load, extremely slow processing; started with Xmx512m, using ~400M

  public static void main(String[] args) throws Exception {
    String testCommandParams;
    if (args.length == 0) {
      testCommandParams = TEST_COMMAND_PARAMS;
    } else {
      testCommandParams = Arrays.asList(args).stream().collect(Collectors.joining(" "));
    }

    ExecuteCommandWithVerboseOutput test = new ExecuteCommandWithVerboseOutput();
    test.executeCommandAndReceiveResponse(testCommandParams);
  }

  public void executeCommandAndReceiveResponse(String testCommandParams) throws Exception {
    TermServer termServer = TermServer.start();

    String processSocketUrl = "http://" + Configurations.HOST + ":" + termServer.getPort() +  Configurations.TERM_PATH;
    StringBuilder responseData = new StringBuilder();
    Consumer<String> responseConsumer = (data) -> {
      //responseData.append(data);
      System.out.print(data);
    };
    Client client = Client.connectCommandExecutingClient(processSocketUrl, Optional.of(responseConsumer));

    Client.executeRemoteCommand(client, TEST_COMMAND + " " + testCommandParams);

    //Wait.forCondition(() -> responseData.toString().contains(MockProcess.FINAL_MESSAGE), 300, ChronoUnit.SECONDS);
    //termServer.stop();

  }
}
