package io.termd.core.ssh;

import org.apache.sshd.common.io.IoServiceFactoryFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettySshdTestBase extends Assert {

  @Before
  public void setNettyServer() {
//        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    System.setProperty(IoServiceFactoryFactory.class.getName(), TestIoServiceFactoryFactory.class.getName());
  }

  @After
  public void unsetNettyServer() {
    System.clearProperty(IoServiceFactoryFactory.class.getName());
  }
}
