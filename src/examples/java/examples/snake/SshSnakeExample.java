/*
 * Copyright 2015 Julien Viet
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

package examples.snake;

import io.termd.core.ssh.netty.NettySshTtyBootstrap;

import java.util.concurrent.TimeUnit;

public class SshSnakeExample {

  public synchronized static void main(String[] args) throws Exception {
    NettySshTtyBootstrap bootstrap = new NettySshTtyBootstrap().
        setPort(5000).
        setHost("localhost");
    bootstrap.start(new SnakeGame()).get(10, TimeUnit.SECONDS);
    System.out.println("SSH started on localhost:5000");
    SshSnakeExample.class.wait();
  }
}
