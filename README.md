## Termd

An open source terminal daemon library providing terminal handling in Java under ASL 2.0

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

- Telnet protocol using _Vert.x 3_ or _Netty 4_
- SSH protocol using _Apache SSHD_
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

### What is not Termd

- not a shell
- not an arg parser
- not a command framework

### Todo

- see to use IntStream or not
- handle % stuff in parser
- ^X
- foobar@ for any foobar, not only boolean and treat it as a removal

Control flow.
Try to use the maven install plugin to install artifacts with specific dependencies.
