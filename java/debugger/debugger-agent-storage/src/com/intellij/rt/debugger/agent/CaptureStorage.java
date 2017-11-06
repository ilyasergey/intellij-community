// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.rt.debugger.agent;

import sun.misc.JavaLangAccess;
import sun.misc.SharedSecrets;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author egor
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CaptureStorage {
  private static final int MAX_STORED_STACKS = 1000;
  private static final Map<WeakReference, CapturedStack> STORAGE = new ConcurrentHashMap<WeakReference, CapturedStack>();
  private static final Deque<WeakReference> HISTORY = new ArrayDeque<WeakReference>(MAX_STORED_STACKS);

  @SuppressWarnings("SSBasedInspection")
  private static final ThreadLocal<Deque<InsertMatch>> CURRENT_STACKS = new ThreadLocal<Deque<InsertMatch>>() {
    @Override
    protected Deque<InsertMatch> initialValue() {
      return new LinkedList<InsertMatch>();
    }
  };

  private static boolean DEBUG = false;
  private static boolean ENABLED = true;

  @SuppressWarnings("unused")
  public static void capture(Object key) {
    if (!ENABLED) {
      return;
    }
    if (DEBUG) {
      System.out.println("capture - " + key);
    }
    Deque<InsertMatch> currentStacks = CURRENT_STACKS.get();
    CapturedStack stack = createCapturedStack(new Throwable(), currentStacks.isEmpty() ? null : currentStacks.getLast());
    WeakKey keyRef = new WeakKey(key);
    synchronized (HISTORY) {
      CapturedStack old = STORAGE.put(keyRef, stack);
      if (old == null) {
        if (HISTORY.size() >= MAX_STORED_STACKS) {
          STORAGE.remove(HISTORY.removeFirst());
        }
      }
      else {
        HISTORY.removeFirstOccurrence(keyRef); // must not happen often
      }
      HISTORY.addLast(keyRef);
    }
  }

  @SuppressWarnings("unused")
  public static void insertEnter(Object key) {
    if (!ENABLED) {
      return;
    }
    CapturedStack stack = STORAGE.get(new WeakKey(key));
    Deque<InsertMatch> currentStacks = CURRENT_STACKS.get();
    if (stack != null) {
      currentStacks.add(new InsertMatch(stack, getStackTraceDepth(new Throwable())));
      if (DEBUG) {
        System.out.println("insert -> " + key + ", stack saved (" + currentStacks.size() + ")");
      }
    }
    else {
      currentStacks.add(InsertMatch.EMPTY);
      if (DEBUG) {
        System.out.println("insert -> " + key + ", no stack found (" + currentStacks.size() + ")");
      }
    }
  }

  private static class WeakKey extends WeakReference {
    private final int myHashCode;

    public WeakKey(Object referent) {
      super(referent);
      myHashCode = System.identityHashCode(referent);
    }

    @Override
    public boolean equals(Object o) {
      return this == o || (o instanceof WeakKey && ((WeakKey)o).get() == get());
    }

    @Override
    public int hashCode() {
      return myHashCode;
    }
  }

  @SuppressWarnings("unused")
  public static void insertExit(Object key) {
    if (!ENABLED) {
      return;
    }
    Deque<InsertMatch> currentStacks = CURRENT_STACKS.get();
    currentStacks.removeLast();
    if (DEBUG) {
      System.out.println("insert <- " + key + ", stack removed (" + currentStacks.size() + ")");
    }
  }

  private static CapturedStack createCapturedStack(Throwable exception, InsertMatch insertMatch) {
    if (insertMatch != null && insertMatch != InsertMatch.EMPTY) {
      return new DeepCapturedStack(exception, insertMatch);
    }
    return new CapturedStack(exception);
  }

  private static class CapturedStack {
    final Throwable myException;

    private CapturedStack(Throwable exception) {
      myException = exception;
    }

    List<StackTraceElement> getStackTrace() {
      StackTraceElement[] stackTrace = myException.getStackTrace();
      return Arrays.asList(stackTrace).subList(1, stackTrace.length);
    }
  }

  private static class DeepCapturedStack extends CapturedStack {
    final InsertMatch myInsertMatch;

    public DeepCapturedStack(Throwable exception, InsertMatch insertMatch) {
      super(exception);
      myInsertMatch = insertMatch;
    }

    List<StackTraceElement> getStackTrace() {
      StackTraceElement[] stackTrace = myException.getStackTrace();
      if (myInsertMatch == null || myInsertMatch == InsertMatch.EMPTY) {
        return super.getStackTrace();
      }
      else {
        List<StackTraceElement> insertStack = myInsertMatch.myStack.getStackTrace();
        int insertPos = stackTrace.length - myInsertMatch.myDepth + 2;
        ArrayList<StackTraceElement> res = new ArrayList<StackTraceElement>(insertPos + insertStack.size() + 1);
        res.addAll(Arrays.asList(stackTrace).subList(1, insertPos));
        res.add(null);
        res.addAll(insertStack);
        return res;
      }
    }
  }

  private static class InsertMatch {
    private final CapturedStack myStack;
    private final int myDepth;

    static final InsertMatch EMPTY = new InsertMatch(null, 0);

    private InsertMatch(CapturedStack stack, int depth) {
      myStack = stack;
      myDepth = depth;
    }
  }

  // to be run from the debugger
  @SuppressWarnings("unused")
  public static Object[][] getRelatedStack(Object key) {
    CapturedStack stack = STORAGE.get(new WeakKey(key));
    if (stack == null) {
      return null;
    }
    List<StackTraceElement> stackTrace = stack.getStackTrace();
    Object[][] res = new Object[stackTrace.size()][];
    for (int i = 0; i < stackTrace.size(); i++) {
      StackTraceElement elem = stackTrace.get(i);
      if (elem == null) {
        res[i] = null;
      }
      else {
        res[i] = new Object[]{elem.getClassName(), elem.getFileName(), elem.getMethodName(), String.valueOf(elem.getLineNumber())};
      }
    }
    return res;
  }

  public static void setDebug(boolean debug) {
    DEBUG = debug;
  }

  public static void setEnabled(boolean enabled) {
    ENABLED = enabled;
  }

  private static final JavaLangAccess ourJavaLangAccess;
  static {
    JavaLangAccess access = null;
    try {
      access = SharedSecrets.getJavaLangAccess();
    }
    catch (Throwable e) {
      // java 9
    }
    ourJavaLangAccess = access;
  }
  // TODO: this is a workaround for java 9 where SharedSecrets are not available
  private static int getStackTraceDepth(Throwable exception) {
    return ourJavaLangAccess != null ? ourJavaLangAccess.getStackTraceDepth(exception) : exception.getStackTrace().length;
  }
}
