/*
 * Copyright 2014 Julien Viet
 *
 * Julien Viet licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package io.modsh.core.telnet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TestBase {

  private volatile Throwable throwable;
  private CountDownLatch latch;
  private boolean testCompleteCalled;
  private boolean awaitCalled;

  protected void testComplete() {
    if (testCompleteCalled) {
      throw new IllegalStateException("testComplete() already invoked");
    }
    testCompleteCalled = true;
    latch.countDown();
  }

  protected AssertionError failure(String msg) {
    return new AssertionError(msg);
  }

  protected AssertionError failure(String msg, Throwable cause) {
    AssertionError afe = new AssertionError(msg);
    afe.initCause(cause);
    return afe;
  }

  protected AssertionError failure(Throwable cause) {
    if (cause instanceof AssertionError) {
      return (AssertionError) cause;
    } else {
      AssertionError ae = new AssertionError();
      ae.initCause(cause);
      return ae;
    }
  }

  protected void await() {
    if (awaitCalled) {
      throw new IllegalStateException("await() already invoked");
    }
    boolean ok;
    try {
      awaitCalled = true;
      ok = latch.await(2, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      throw failure("Test thread was interrupted", e);
    }
    if (!ok) {
      throw failure("Test timed out");
    } else {
      if (throwable != null) {
        throw failure(throwable);
      }
    }
  }

  private void handleThrowable(Throwable t) {
    throwable = t;
    latch.countDown();
    if (t instanceof AssertionError) {
      throw (AssertionError)t;
    }
  }

  protected void assertTrue(boolean condition) {
    try {
      Assert.assertTrue(condition);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  protected void assertFalse(boolean condition) {
    try {
      Assert.assertFalse(condition);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  protected void assertEquals(Object expected, Object actual) {
    try {
      Assert.assertEquals(expected, actual);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  @Before
  public void beforeTest() {
    latch = new CountDownLatch(1);
    throwable = null;
    testCompleteCalled = false;
    awaitCalled = false;
  }

  @After
  public void afterTest() {
    if (!testCompleteCalled && !awaitCalled && throwable != null) {
      throw new IllegalStateException("You either forget to call testComplete() or forgot to await() for an asynchronous test");
    }
  }


}
