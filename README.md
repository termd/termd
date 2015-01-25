## Termd

An open source terminal daemon library providing terminal handling in Java.

### Features

- Telnet protocol using Vert.x or Netty
- SSH protocol using Apache SSHD
- Event based design
- Extensible readline implementation
- Unicode support

### Todo

Key handler and default key handler in event handler just print stuff in the console.
Setting a key handler replaces the default handler and do something with them.
As fucking simple as that.

The Event stuff should be moved into the EventHandler and it should receive key events

Control flow.