## Termd

An open source terminal daemon library providing terminal handling in Java.

### Features

- Telnet protocol using _Vert.x_ or _Netty_
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
