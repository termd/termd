## Termd

An open source terminal daemon library providing terminal handling in Java under ASL 2.0

[![Build Status](https://termd.ci.cloudbees.com/buildStatus/icon?job=termd-core)](https://termd.ci.cloudbees.com/job/termd-core/)

### Features

- Telnet protocol using _Vert.x 3_ or _Netty 4_
- SSH protocol using _Apache SSHD_
- Web interface using _term.js_ and _SockJS_
- Event based design
   - read events
   - window size
   - tty signals
- Readline implementation
   - extensible with plugable functions
   - multiline support
- Unicode support
- Terminfo capabilities

### Todo

- handle % stuff in parser
- ^X
- foobar@ for any foobar, not only boolean and treat it as a removal

Control flow.
Try to use the maven install plugin to install artifacts with specific dependencies.
