/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.databind;

import gov.nist.secauto.metaschema.core.model.IBoundObject;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.io.BindingException;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.metaschema.databind.io.IDeserializer;
import gov.nist.secauto.metaschema.databind.io.ISerializer;
import gov.nist.secauto.metaschema.databind.io.json.DefaultJsonDeserializer;
import gov.nist.secauto.metaschema.databind.io.json.DefaultJsonSerializer;
import gov.nist.secauto.metaschema.databind.io.xml.DefaultXmlDeserializer;
import gov.nist.secauto.metaschema.databind.io.xml.DefaultXmlSerializer;
import gov.nist.secauto.metaschema.databind.io.yaml.DefaultYamlDeserializer;
import gov.nist.secauto.metaschema.databind.io.yaml.DefaultYamlSerializer;
import gov.nist.secauto.metaschema.databind.model.IBoundDefinitionModelAssembly;
import gov.nist.secauto.metaschema.databind.model.IBoundDefinitionModelComplex;
import gov.nist.secauto.metaschema.databind.model.IBoundModule;
import gov.nist.secauto.metaschema.databind.model.binding.metaschema.MetaschemaModelModule;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.talsmasoftware.lazy4j.Lazy;

/**
 * The implementation of a {@link IBindingContext} provided by this library.
 * <p>
 * This implementation caches Module information, which can dramatically improve
 * read and write performance at the cost of some memory use. Thus, using the
 * same singleton of this class across multiple I/O operations will improve
 * overall read and write performance when processing the same types of data.
 * <p>
 * Serializers and deserializers provided by this class using the
 * {@link #newSerializer(Format, Class)} and
 * {@link #newDeserializer(Format, Class)} methods will
 * <p>
 * This class is synchronized and is thread-safe.
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class DefaultBindingContext implements IBindingContext {
  private static Lazy<DefaultBindingContext> singleton = Lazy.lazy(DefaultBindingContext::new);
  @NonNull
  private final IModuleLoaderStrategy moduleLoaderStrategy;
  @NonNull
  private final Map<Class<?>, IBoundDefinitionModelComplex> boundClassToStrategyMap = new ConcurrentHashMap<>();

  /**
   * Get the singleton instance of this binding context.
   * <p>
   * Note: It is general a better practice to use a new {@link IBindingContext}
   * and reuse that instance instead of this global instance.
   *
   * @return the binding context
   * @see IBindingContext#newInstance()
   */
  @NonNull
  static DefaultBindingContext instance() {
    return ObjectUtils.notNull(singleton.get());
  }

  /**
   * Construct a new binding context.
   */
  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  public DefaultBindingContext() {
    this(new SimpleModuleLoaderStrategy());
  }

  /**
   * Construct a new binding context.
   *
   * @param strategy
   *          the behavior class to use for loading Metaschema modules
   * @since 2.0.0
   */
  public DefaultBindingContext(@NonNull IBindingContext.IModuleLoaderStrategy strategy) {
    // only allow extended classes
    moduleLoaderStrategy = strategy;
    moduleLoaderStrategy.loadModule(MetaschemaModelModule.class, this);
  }

  @Override
  @NonNull
  public IModuleLoaderStrategy getModuleLoaderStrategy() {
    return moduleLoaderStrategy;
  }

  @Override
  @NonNull
  public final IBoundModule registerModule(@NonNull Class<? extends IBoundModule> clazz) {
    return getModuleLoaderStrategy().loadModule(clazz, this);
  }

  /**
   * Get the binding matchers that are associated with this class.
   *
   * @return the list of matchers
   * @see #registerBindingMatcher(Class)
   * @see #registerBindingMatcher(IBoundDefinitionModelAssembly)
   */
  @NonNull
  protected Collection<IBindingMatcher> getBindingMatchers() {
    return getModuleLoaderStrategy().getBindingMatchers();
  }

  @Override
  public final IBoundDefinitionModelComplex registerClassBinding(IBoundDefinitionModelComplex definition) {
    Class<?> clazz = definition.getBoundClass();
    return boundClassToStrategyMap.computeIfAbsent(clazz, k -> definition);
  }

  @Override
  public final IBoundDefinitionModelComplex getBoundDefinitionForClass(@NonNull Class<? extends IBoundObject> clazz) {
    return moduleLoaderStrategy.getBoundDefinitionForClass(clazz, this);
  }

  /**
   * {@inheritDoc}
   * <p>
   * A serializer returned by this method is thread-safe.
   */
  @Override
  public <CLASS extends IBoundObject> ISerializer<CLASS> newSerializer(
      @NonNull Format format,
      @NonNull Class<CLASS> clazz) {
    Objects.requireNonNull(format, "format");
    IBoundDefinitionModelAssembly definition;
    try {
      definition = IBoundDefinitionModelAssembly.class.cast(getBoundDefinitionForClass(clazz));
    } catch (ClassCastException ex) {
      throw new IllegalStateException(
          String.format("Class '%s' is not a bound assembly.", clazz.getClass().getName()), ex);
    }
    if (definition == null) {
      throw new IllegalStateException(String.format("Class '%s' is not bound", clazz.getClass().getName()));
    }
    ISerializer<CLASS> retval;
    switch (format) {
    case JSON:
      retval = new DefaultJsonSerializer<>(definition);
      break;
    case XML:
      retval = new DefaultXmlSerializer<>(definition);
      break;
    case YAML:
      retval = new DefaultYamlSerializer<>(definition);
      break;
    default:
      throw new UnsupportedOperationException(String.format("Unsupported format '%s'", format));
    }
    return retval;
  }

  /**
   * {@inheritDoc}
   * <p>
   * A deserializer returned by this method is thread-safe.
   */
  @Override
  public <CLASS extends IBoundObject> IDeserializer<CLASS> newDeserializer(
      @NonNull Format format,
      @NonNull Class<CLASS> clazz) {
    IBoundDefinitionModelAssembly definition;
    try {
      definition = IBoundDefinitionModelAssembly.class.cast(getBoundDefinitionForClass(clazz));
    } catch (ClassCastException ex) {
      throw new IllegalStateException(
          String.format("Class '%s' is not a bound assembly.", clazz.getClass().getName()),
          ex);
    }
    if (definition == null) {
      throw new IllegalStateException(String.format("Class '%s' is not bound", clazz.getName()));
    }
    IDeserializer<CLASS> retval;
    switch (format) {
    case JSON:
      retval = new DefaultJsonDeserializer<>(definition);
      break;
    case XML:
      retval = new DefaultXmlDeserializer<>(definition);
      break;
    case YAML:
      retval = new DefaultYamlDeserializer<>(definition);
      break;
    default:
      throw new UnsupportedOperationException(String.format("Unsupported format '%s'", format));
    }

    return retval;
  }

  @Override
  public Class<? extends IBoundObject> getBoundClassForRootXmlQName(@NonNull QName rootQName) {
    Class<? extends IBoundObject> retval = null;
    for (IBindingMatcher matcher : getBindingMatchers()) {
      retval = matcher.getBoundClassForXmlQName(rootQName);
      if (retval != null) {
        break;
      }
    }
    return retval;
  }

  @Override
  public Class<? extends IBoundObject> getBoundClassForRootJsonName(@NonNull String rootName) {
    Class<? extends IBoundObject> retval = null;
    for (IBindingMatcher matcher : getBindingMatchers()) {
      retval = matcher.getBoundClassForJsonName(rootName);
      if (retval != null) {
        break;
      }
    }
    return retval;
  }

  @Override
  public <CLASS extends IBoundObject> CLASS deepCopy(@NonNull CLASS other, IBoundObject parentInstance)
      throws BindingException {
    IBoundDefinitionModelComplex definition = getBoundDefinitionForClass(other.getClass());
    if (definition == null) {
      throw new IllegalStateException(String.format("Class '%s' is not bound", other.getClass().getName()));
    }
    return ObjectUtils.asType(definition.deepCopyItem(other, parentInstance));
  }
}
