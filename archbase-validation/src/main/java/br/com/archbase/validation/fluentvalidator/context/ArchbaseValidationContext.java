package br.com.archbase.validation.fluentvalidator.context;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ArchbaseValidationContext {

  private static final ThreadLocal<Context> threadLocal = new ThreadLocal<>();

  private ArchbaseValidationContext() {
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
   * Context of validation
   */
  public static final class Context {

    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    private final Queue<Error> errors = new ConcurrentLinkedQueue<>();

    /**
     *
     * @param field
     * @param message
     * @param code
     * @param attemptedValue
     */
    public void addErrors(final Collection<Error> errs) {
      errs.stream().forEach(errors::add);
    }

    /**
     *
     * @param property
     * @param value
     */
    public void setProperty(final String property, final Object value) {
      if (Objects.nonNull(property)) {
        properties.put(property, value);
      }
    }

    /**
     *
     * @return
     */
    public ArchbaseValidationResult getValidationResult() {
      ArchbaseValidationContext.remove();
      return errors.isEmpty() ? ArchbaseValidationResult.ok() : ArchbaseValidationResult.fail(errors);
    }

    /**
     *
     * @param property
     * @param clazz
     * @return
     */
    public <P> P getProperty(final String property, final Class<P> clazz) {
      return clazz.cast(properties.getOrDefault(property, null));
    }

  }

}
