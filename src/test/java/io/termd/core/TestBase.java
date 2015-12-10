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
package io.termd.core;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TestBase {

  private static final int ctrl(char c) {
    return c - 64;
  }

  protected static final int[] CTRL_A_KEY = { ctrl('A') };
  protected static final int[] CTRL_C_KEY = { ctrl('C') };
  protected static final int[] CTRL_D_KEY = { ctrl('D') };
  protected static final int[] CTRL_E_KEY = { ctrl('E') };
  protected static final int[] UP_KEY = { 27, '[', 'A' };
  protected static final int[] DOWN_KEY = { 27, '[', 'B' };
  protected static final int[] FORWARD_KEY = { 27, '[', 'C' };
  protected static final int[] FORWARD_WORD_SEQ = { 27, 'f' };
  protected static final int[] BACKWARD_KEY = { 27, '[', 'D' };
  protected static final int[] BACKWARD_WORD_SEQ = { 27, 'b' };
  protected static final int[] BACKWARD_DELETE_KEY = { 8 };
  protected static final int[] KILL_LINE = { ctrl('K') };
  protected static final int[] META_BACKSPACE = { 27, 127 };

  private volatile Throwable throwable;
  private CountDownLatch latch;
  private Exception testCompleteCalled;
  private boolean awaitCalled;

  protected void testComplete() {
    if (testCompleteCalled != null) {
      throw new IllegalStateException("testComplete() already invoked", testCompleteCalled);
    }
    testCompleteCalled = new Exception();
    latch.countDown();
  }

  public static AssertionError failure(String msg) {
    return new AssertionError(msg);
  }

  public static AssertionError failure(String msg, Throwable cause) {
    AssertionError afe = new AssertionError(msg);
    afe.initCause(cause);
    return afe;
  }

  public static AssertionError failure(Throwable cause) {
    if (cause instanceof AssertionError) {
      return (AssertionError) cause;
    } else {
      AssertionError ae = new AssertionError();
      ae.initCause(cause);
      return ae;
    }
  }

  protected void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(10, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      throw failure(e);
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

  public void assertNull(Object object) {
    try {
      Assert.assertNull(object);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  public <T> T assertNotNull(T object) {
    try {
      Assert.assertNotNull(object);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
    return object;
  }

  public void fail(Throwable message) {
    try {
      Assert.fail(message.getMessage());
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  public void fail(String message) {
    try {
      Assert.fail(message);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  public void assertTrue(boolean condition) {
    try {
      Assert.assertTrue(condition);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  public void assertTrue(String msg, boolean condition) {
    try {
      Assert.assertTrue(msg, condition);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  public void assertFalse(boolean condition) {
    try {
      Assert.assertFalse(condition);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  public void assertEquals(Object expected, Object actual) {
    try {
      Assert.assertEquals(expected, actual);
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  public void assertEquals(int[] expected, int[] actual) {
    try {
      Assert.assertTrue("Was expecting " + Arrays.toString(expected) + " to be equals to " + Arrays.toString(actual), Arrays.equals(expected, actual));
    } catch (AssertionError e) {
      handleThrowable(e);
    }
  }

  public void awaitLatch(CountDownLatch latch) throws InterruptedException {
    assertTrue(latch.await(10, TimeUnit.SECONDS));
  }

  @Before
  public void beforeTest() {
    latch = new CountDownLatch(1);
    throwable = null;
    testCompleteCalled = null;
    awaitCalled = false;
  }

  @After
  public void afterTest() {
    if (testCompleteCalled == null && !awaitCalled && throwable != null) {
      throw new IllegalStateException("You either forget to call testComplete() or forgot to await() for an asynchronous test");
    }
  }
}
