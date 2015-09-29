## Termd

An open source library for writing terminal applications in Java under ASL 2.0.

[![Build Status](https://termd.ci.cloudbees.com/buildStatus/icon?job=termd-core)](https://termd.ci.cloudbees.com/job/termd-core/)

### Consuming the lib

Build it yourself or consume the snapshot from [Sonatype OSS repository](https://oss.sonatype.org/content/repositories/snapshots/io/termd/termd-core/)

```
<dependency>
  <groupId>io.termd</groupId>
  <artifactId>termd-core</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

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

### Todo

- dynamic prompt
- see to use IntStream or not
- handle % stuff in parser
- foobar@ for any foobar, not only boolean and treat it as a removal

Try to use the maven install plugin to install artifacts with specific dependencies.
