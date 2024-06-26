package br.com.archbase.validation.fluentvalidator.context;

import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public final class ArchbaseProcessorContext {

  private static final ThreadLocal<Context> threadLocal = new ThreadLocal<>();

  private ArchbaseProcessorContext() {
    super();
  }

  /**
   *
   * @return
   */
  public static Context get() {
    if (Objects.isNull(threadLocal.get())) {
      threadLocal.set(new Context());
    }
    return threadLocal.get();
  }

  /**
   *
   */
  public static void remove() {
    threadLocal.remove();
  }

  /**
   * Context of processor
   */
  public static final class Context {

    private final Deque<AtomicInteger> stackCounter = new ConcurrentLinkedDeque<>();

    public void create() {
      stackCounter.push(new AtomicInteger(0));
    }

    public void remove() {
      if (!stackCounter.isEmpty()) {
        stackCounter.pop();
      }
    }

    public void inc() {
      if (!stackCounter.isEmpty()) {
        stackCounter.peek().incrementAndGet();
      }
    }

    public Integer get() {
      return stackCounter.isEmpty() ? 0 : stackCounter.peek().get();
    }

  }

}
