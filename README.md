## Termd

An open source library for writing terminal applications in Java under ASL 2.0.

[![Build Status](https://termd.ci.cloudbees.com/buildStatus/icon?job=termd-core)](https://termd.ci.cloudbees.com/job/termd-core/)

### Consuming the lib

Add this dependency to your build.

```
<dependency>
  <groupId>io.termd</groupId>
  <artifactId>termd-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

Snapshots are available from [Sonatype OSS repository](https://oss.sonatype.org/content/repositories/snapshots/io/termd/termd-core/)

### Features

- Telnet/SSH using _Netty 4_ or [_Vert.x 3_](https://github.com/vert-x3/vertx-shell)
- Web interface using _term.js_ and _SockJS_/_Websocket_
- Event based design
   - read events
   - window size
   - tty signals
- Readline implementation
   - extensible with plugable functions
   - multi-line support
   - multi-byte char support
   - multi-cell char support
- Unicode support
- Terminfo capabilities

### Supported protocols

#### Telnet

Termd provides its own implementation of Telnet written on top of Netty 4.

#### SSH

Termd provides an implementation of [Apache SSHD](http://mina.apache.org/sshd-project/) backed by Netty 4.

#### Websocket

Termd in a web page using the [term.js](https://github.com/chjj/term.js/) client library and Netty websockets.

### FAQ

- what is not Termd ?
  - not a shell
  - not an arg parser
  - not a command framework
- why async style ?
  - keyboard, ctrl-c, etc... event processing is easy to program against
- why would I use Termd ?
  - you want to write a terminal application easily
  - you want to support various protocols like SSH, telnet or a web interface
  - you don't care about the technical details

### Examples

#### Events

A simple examples showing TTY events.

The actual [example](src/examples/java/examples/events/EventsExample.java), runs with
[Telnet](src/examples/java/examples/events/TelnetEventsExample.java),
[SSH](src/examples/java/examples/events/SshEventsExample.java),
[Websocket](src/examples/java/examples/events/WebsocketEventsExample.java).

#### Readline

A simple examples showing how to use Readline.

The actual [example](src/examples/java/examples/readline/ReadlineExample.java), runs with
[Telnet](src/examples/java/examples/readline/TelnetReadlineExample.java),
[SSH](src/examples/java/examples/readline/SshReadlineExample.java),
[Websocket](src/examples/java/examples/readline/WebsocketReadlineExample.java).

#### Readline function

A simple examples showing how to extend Readline with a custom function that reverse the line content.

The actual [example](src/examples/java/examples/readlinefunction/ReadlineFunctionExample.java), runs with
[Telnet](src/examples/java/examples/readlinefunction/TelnetReadlineFunctionExample.java),
[SSH](src/examples/java/examples/readlinefunction/SshReadlineFunctionExample.java),
[Websocket](src/examples/java/examples/readlinefunction/WebsocketReadlineFunctionExample.java).

#### Shell

A simple shell example giving an overview of TTY interactions.

The actual [example](src/examples/java/examples/shell/Shell.java), run with
[Telnet](src/examples/java/examples/shell/TelnetShellExample.java),
[SSH](src/examples/java/examples/shell/SshShellExample.java),
[Websocket](src/examples/java/examples/shell/WebsocketShellExample.java).

#### Screencast

Broadcast the desktop to the client, focusing on pushing data to the TTY.

The actual [example](src/examples/java/examples/screencast/Screencaster.java), run with
[Telnet](src/examples/java/examples/screencast/TelnetScreencastingExample.java),
[SSH](src/examples/java/examples/screencast/SshScreencastingExample.java),
[Websocket](src/examples/examples/java/screencast/WebsocketScreencastingExample.java).

#### Snake

The popular Snake game on the event loop.

The actual [example](src/examples/java/examples/snake/SnakeGame.java), run with
[Telnet](src/examples/java/examples/screencast/TelnetScreencastingExample.java),
[SSH](src/examples/java/examples/screencast/SshScreencastingExample.java),
[Websocket](src/examples/java/examples/screencast/WebsocketScreencastingExample.java).

#### Plasma

A funny demo effect using unicode chars.

The actual [example](src/examples/java/examples/plasma/Plasma.java), run with
[Telnet](src/examples/java/examples/screencast/TelnetPlasmaExample.java),
[SSH](src/examples/java/examples/screencast/SshPlasmaExample.java),
[Websocket](src/examples/java/examples/screencast/WebsocketPlasmaExample.java).

#### Ptybridge

The PTY bridge is a bridge to native process, use with caution for security reasons, run with
[Telnet](src/examples/java/examples/ptybridge/TelnetPtyBridgeExample.java),
[SSH](src/examples/java/examples/ptybridge/SshPtyBridgeExample.java),
[Websocket](src/examples/java/examples/ptybridge/WebsocketPtyBridgeExample.java).

#### Telnet

A simple telnet example that shows Telnet options negociation.

### Todo

- dynamic prompt
- see to use IntStream or not
- handle % stuff in parser
- foobar@ for any foobar, not only boolean and treat it as a removal

Try to use the maven install plugin to install artifacts with specific dependencies.
