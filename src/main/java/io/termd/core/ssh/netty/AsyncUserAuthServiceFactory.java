package io.termd.core.ssh.netty;

import org.apache.sshd.common.Service;
import org.apache.sshd.common.ServiceFactory;
import org.apache.sshd.common.session.Session;

import java.io.IOException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class AsyncUserAuthServiceFactory implements ServiceFactory {
  public static final AsyncUserAuthServiceFactory INSTANCE = new AsyncUserAuthServiceFactory();

  public AsyncUserAuthServiceFactory() {
    super();
  }

  @Override
  public String getName() {
    return "ssh-userauth";
  }

  @Override
  public Service create(Session session) throws IOException {
    return new AsyncUserAuthService(session);
  }
}