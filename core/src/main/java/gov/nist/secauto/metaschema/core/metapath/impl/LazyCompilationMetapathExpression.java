/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.impl;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.MetapathException;
import gov.nist.secauto.metaschema.core.metapath.StaticContext;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import edu.umd.cs.findbugs.annotations.NonNull;
import nl.talsmasoftware.lazy4j.Lazy;

/**
 * An implementation of a Metapath expression that is compiled when evaluated.
 * <p>
 * Lazy compilation may cause additional {@link MetapathException} errors at
 * evaluation time, since compilation errors are not raised until evaluation.
 */
public class LazyCompilationMetapathExpression implements IMetapathExpression {
  @NonNull
  private final String path;
  @NonNull
  private final StaticContext staticContext;
  @NonNull
  private final Lazy<IMetapathExpression> compiledMetapath;

  /**
   * Construct a new lazy-compiled Metapath expression.
   *
   * @param path
   *          the metapath expression
   * @param staticContext
   *          the static evaluation context
   */
  public LazyCompilationMetapathExpression(
      @NonNull String path,
      @NonNull StaticContext staticContext) {
    this.path = path;
    this.staticContext = staticContext;
    this.compiledMetapath = ObjectUtils.notNull(Lazy.lazy(() -> IMetapathExpression.compile(path, staticContext)));
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public StaticContext getStaticContext() {
    return staticContext;
  }

  @NonNull
  private IMetapathExpression getCompiledMetapath() {
    return ObjectUtils.notNull(compiledMetapath.get());
  }

  @Override
  public <T extends IItem> ISequence<T> evaluate(IItem focus, DynamicContext dynamicContext) {
    return getCompiledMetapath().evaluate(focus, dynamicContext);
  }
}
