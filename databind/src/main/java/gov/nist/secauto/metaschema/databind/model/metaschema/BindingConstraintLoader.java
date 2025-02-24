/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.databind.model.metaschema;

import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.MetapathException;
import gov.nist.secauto.metaschema.core.metapath.StaticContext;
import gov.nist.secauto.metaschema.core.model.AbstractLoader;
import gov.nist.secauto.metaschema.core.model.IAssemblyDefinition;
import gov.nist.secauto.metaschema.core.model.IConstraintLoader;
import gov.nist.secauto.metaschema.core.model.IDefinition;
import gov.nist.secauto.metaschema.core.model.IFieldDefinition;
import gov.nist.secauto.metaschema.core.model.IFlagDefinition;
import gov.nist.secauto.metaschema.core.model.IModule;
import gov.nist.secauto.metaschema.core.model.ISource;
import gov.nist.secauto.metaschema.core.model.MetaschemaException;
import gov.nist.secauto.metaschema.core.model.constraint.AbstractTargetedConstraints;
import gov.nist.secauto.metaschema.core.model.constraint.AssemblyConstraintSet;
import gov.nist.secauto.metaschema.core.model.constraint.AssemblyTargetedConstraints;
import gov.nist.secauto.metaschema.core.model.constraint.DefaultConstraintSet;
import gov.nist.secauto.metaschema.core.model.constraint.DefaultScopedContraints;
import gov.nist.secauto.metaschema.core.model.constraint.FieldTargetedConstraints;
import gov.nist.secauto.metaschema.core.model.constraint.FlagTargetedConstraints;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet;
import gov.nist.secauto.metaschema.core.model.constraint.IFeatureModelConstrained;
import gov.nist.secauto.metaschema.core.model.constraint.IModelConstrained;
import gov.nist.secauto.metaschema.core.model.constraint.IScopedContraints;
import gov.nist.secauto.metaschema.core.model.constraint.ITargetedConstraints;
import gov.nist.secauto.metaschema.core.model.constraint.IValueConstrained;
import gov.nist.secauto.metaschema.core.model.constraint.ValueConstraintSet;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.io.IBoundLoader;
import gov.nist.secauto.metaschema.databind.model.metaschema.binding.AssemblyConstraints;
import gov.nist.secauto.metaschema.databind.model.metaschema.binding.MetapathContext;
import gov.nist.secauto.metaschema.databind.model.metaschema.binding.MetaschemaMetaConstraints;
import gov.nist.secauto.metaschema.databind.model.metaschema.binding.MetaschemaMetapath;
import gov.nist.secauto.metaschema.databind.model.metaschema.binding.MetaschemaModuleConstraints;
import gov.nist.secauto.metaschema.databind.model.metaschema.impl.ConstraintBindingSupport;

import org.apache.xmlbeans.impl.values.XmlValueNotSupportedException;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import nl.talsmasoftware.lazy4j.Lazy;

/**
 * Provides methods to load a constraint set expressed in any supported
 * Metaschema format.
 * <p>
 * Loaded constraint instances are cached to avoid the need to load them for
 * every use. Any constraint set imported is also loaded and cached
 * automatically.
 */
public class BindingConstraintLoader
    extends AbstractLoader<List<IConstraintSet>>
    implements IConstraintLoader {

  @NonNull
  private final IBoundLoader loader;

  /**
   * Construct a new loader.
   *
   * @param bindingContext
   *          the Metaschema binding information used to parse constraint data
   */
  public BindingConstraintLoader(@NonNull IBindingContext bindingContext) {
    this.loader = bindingContext.newBoundLoader();
    // this.loader.enableFeature(DeserializationFeature.DESERIALIZE_VALIDATE_CONSTRAINTS);
  }

  @Override
  protected List<IConstraintSet> parseResource(@NonNull URI resource, @NonNull Deque<URI> visitedResources)
      throws IOException {

    Object constraintsDocument = loader.load(resource);

    StaticContext.Builder builder = StaticContext.builder()
        .baseUri(resource);

    builder.useWildcardWhenNamespaceNotDefaulted(true);

    List<IConstraintSet> retval;
    if (constraintsDocument instanceof MetaschemaModuleConstraints) {
      MetaschemaModuleConstraints obj = (MetaschemaModuleConstraints) constraintsDocument;

      // now check if this constraint set imports other constraint sets
      List<MetaschemaModuleConstraints.Import> imports = CollectionUtil.listOrEmpty(obj.getImports());

      @NonNull
      Set<IConstraintSet> importedConstraints;
      if (imports.isEmpty()) {
        importedConstraints = CollectionUtil.emptySet();
      } else {
        try {
          importedConstraints = new LinkedHashSet<>();
          for (MetaschemaModuleConstraints.Import imported : imports) {
            URI importedResource = imported.getHref();
            importedResource = ObjectUtils.notNull(resource.resolve(importedResource));
            importedConstraints.addAll(loadInternal(importedResource, visitedResources));
          }
        } catch (MetaschemaException ex) {
          throw new IOException(ex);
        }
      }

      CollectionUtil.listOrEmpty(obj.getNamespaceBindings()).stream()
          .forEach(binding -> builder.namespace(
              ObjectUtils.notNull(binding.getPrefix()),
              ObjectUtils.notNull(binding.getUri())));
      ISource source = ISource.externalSource(resource);

      // now create this constraint set
      retval = CollectionUtil.singletonList(new DefaultConstraintSet(
          source,
          parseScopedConstraints(obj, source),
          new LinkedHashSet<>(importedConstraints)));
    } else if (constraintsDocument instanceof MetaschemaMetaConstraints) {
      MetaschemaMetaConstraints obj = (MetaschemaMetaConstraints) constraintsDocument;

      // now check if this constraint set imports other constraint sets
      List<MetaschemaMetaConstraints.Import> imports = CollectionUtil.listOrEmpty(obj.getImports());

      retval = new LinkedList<>();
      if (!imports.isEmpty()) {
        try {
          for (MetaschemaMetaConstraints.Import imported : imports) {
            URI importedResource = imported.getHref();
            importedResource = ObjectUtils.notNull(resource.resolve(importedResource));
            retval.addAll(loadInternal(importedResource, visitedResources));
          }
        } catch (MetaschemaException ex) {
          throw new IOException(ex);
        }
      }

      CollectionUtil.listOrEmpty(obj.getNamespaceBindings()).stream()
          .forEach(binding -> builder.namespace(
              ObjectUtils.notNull(binding.getPrefix()),
              ObjectUtils.notNull(binding.getUri())));

      ISource source = ISource.externalSource(builder.build(), false);

      List<ITargetedConstraints> targetedConstraints = ObjectUtils.notNull(CollectionUtil.listOrEmpty(obj.getContexts())
          .stream()
          .flatMap(context -> parseContext(ObjectUtils.notNull(context), null, source)
              .getTargetedConstraints().stream())
          .collect(Collectors.toList()));
      retval.add(new MetaConstraintSet(source, targetedConstraints));

      retval = CollectionUtil.unmodifiableList(retval);
    } else {
      throw new UnsupportedOperationException(String.format("Unsupported constraint content '%s'.", resource));
    }
    return retval;
  }

  /**
   * Parse individual constraint definitions from the provided XMLBeans object.
   *
   * @param obj
   *          the XMLBeans object
   * @param source
   *          the source of the constraint content
   * @return the scoped constraint definitions
   */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // intentional
  @NonNull
  protected List<IScopedContraints> parseScopedConstraints(
      @NonNull MetaschemaModuleConstraints obj,
      @NonNull ISource source) {
    List<IScopedContraints> scopedConstraints = new LinkedList<>();

    for (MetaschemaModuleConstraints.Scope scope : CollectionUtil.listOrEmpty(obj.getScopes())) {
      assert scope != null;

      List<ITargetedConstraints> targetedConstraints = new LinkedList<>();
      try {
        for (IValueConstraintsBase constraintsObj : CollectionUtil.listOrEmpty(scope.getConstraints())) {
          if (constraintsObj instanceof MetaschemaModuleConstraints.Scope.Assembly) {
            targetedConstraints.add(handleScopedAssembly(
                (MetaschemaModuleConstraints.Scope.Assembly) constraintsObj,
                source));
          } else if (constraintsObj instanceof MetaschemaModuleConstraints.Scope.Field) {
            targetedConstraints.add(handleScopedField(
                (MetaschemaModuleConstraints.Scope.Field) constraintsObj,
                source));
          } else if (constraintsObj instanceof MetaschemaModuleConstraints.Scope.Flag) {
            targetedConstraints.add(handleScopedFlag(
                (MetaschemaModuleConstraints.Scope.Flag) constraintsObj,
                source));
          }
        }
      } catch (MetapathException | XmlValueNotSupportedException ex) {
        if (ex.getCause() instanceof MetapathException) {
          throw new MetapathException(
              String.format("Unable to compile a Metapath in '%s'. %s",
                  source.getSource(),
                  ex.getLocalizedMessage()),
              ex);
        }
        throw ex;
      }

      URI namespace = ObjectUtils.requireNonNull(scope.getMetaschemaNamespace());
      String shortName = ObjectUtils.requireNonNull(scope.getMetaschemaShortName());

      scopedConstraints.add(new DefaultScopedContraints(
          namespace,
          shortName,
          CollectionUtil.unmodifiableList(targetedConstraints)));
    }
    return CollectionUtil.unmodifiableList(scopedConstraints);
  }

  private static AssemblyTargetedConstraints handleScopedAssembly(
      @NonNull MetaschemaModuleConstraints.Scope.Assembly obj,
      @NonNull ISource source) {
    IModelConstrained constraints = new AssemblyConstraintSet(source);
    ConstraintBindingSupport.parse(constraints, obj, source);
    return new AssemblyTargetedConstraints(
        source,
        IMetapathExpression.lazyCompile(
            ObjectUtils.requireNonNull(obj.getTarget()),
            source.getStaticContext()),
        constraints);
  }

  private static FieldTargetedConstraints handleScopedField(
      @NonNull MetaschemaModuleConstraints.Scope.Field obj,
      @NonNull ISource source) {
    IValueConstrained constraints = new ValueConstraintSet(source);
    ConstraintBindingSupport.parse(constraints, obj, source);

    return new FieldTargetedConstraints(
        source,
        IMetapathExpression.lazyCompile(
            ObjectUtils.requireNonNull(obj.getTarget()),
            source.getStaticContext()),
        constraints);
  }

  private static FlagTargetedConstraints handleScopedFlag(
      @NonNull MetaschemaModuleConstraints.Scope.Flag obj,
      @NonNull ISource source) {
    IValueConstrained constraints = new ValueConstraintSet(source);
    ConstraintBindingSupport.parse(constraints, obj, source);

    return new FlagTargetedConstraints(
        source,
        IMetapathExpression.lazyCompile(
            ObjectUtils.requireNonNull(obj.getTarget()),
            source.getStaticContext()),
        constraints);
  }

  private Context parseContext(
      @NonNull MetapathContext contextObj,
      @Nullable Context parent,
      @NonNull ISource source) {

    List<IMetapathExpression> metapaths;
    if (parent == null) {
      metapaths = ObjectUtils.notNull(CollectionUtil.listOrEmpty(contextObj.getMetapaths()).stream()
          .map(MetaschemaMetapath::getTarget)
          .map(metapath -> IMetapathExpression.lazyCompile(
              ObjectUtils.requireNonNull(metapath),
              source.getStaticContext()))
          .collect(Collectors.toList()));
    } else {
      List<IMetapathExpression> parentMetapaths = parent.getMetapaths().stream()
          .collect(Collectors.toList());
      metapaths = ObjectUtils.notNull(CollectionUtil.listOrEmpty(contextObj.getMetapaths()).stream()
          .map(MetaschemaMetapath::getTarget)
          .flatMap(childPath -> parentMetapaths.stream()
              .map(parentPath -> parentPath.getPath() + '/' + childPath))
          .map(metapath -> IMetapathExpression.lazyCompile(
              ObjectUtils.requireNonNull(metapath),
              source.getStaticContext()))
          .collect(Collectors.toList()));
    }

    AssemblyConstraints contextConstraints = contextObj.getConstraints();
    IModelConstrained constraints = new AssemblyConstraintSet(source);
    if (contextConstraints != null) {
      ConstraintBindingSupport.parse(constraints, contextConstraints, source);
    }
    Context context = new Context(source, metapaths, constraints);

    List<Context> childContexts = ObjectUtils.notNull(CollectionUtil.listOrEmpty(contextObj.getContexts()).stream()
        .map(childObj -> parseContext(ObjectUtils.notNull(childObj), context, source))
        .collect(Collectors.toList()));

    context.addAll(childContexts);

    return context;
  }

  private static class Context {
    @NonNull
    private final List<IMetapathExpression> metapaths;
    @NonNull
    private final List<Context> childContexts = new LinkedList<>();
    @NonNull
    private final Lazy<List<ITargetedConstraints>> targetedConstraints;

    public Context(
        @NonNull ISource source,
        @NonNull List<IMetapathExpression> metapaths,
        @NonNull IModelConstrained constraints) {
      this.metapaths = metapaths;
      this.targetedConstraints = ObjectUtils.notNull(Lazy.lazy(() -> {

        Stream<ITargetedConstraints> paths = getMetapaths().stream()
            .map(metapath -> new MetaTargetedContraints(
                source,
                ObjectUtils.notNull(metapath),
                constraints));
        Stream<ITargetedConstraints> childPaths = childContexts.stream()
            .flatMap(child -> child.getTargetedConstraints().stream());

        return Stream.concat(paths, childPaths)
            .collect(Collectors.toUnmodifiableList());
      }));
    }

    @NonNull
    public List<ITargetedConstraints> getTargetedConstraints() {
      return ObjectUtils.notNull(targetedConstraints.get());
    }

    public void addAll(@NonNull Collection<Context> childContexts) {
      childContexts.addAll(childContexts);
    }

    @NonNull
    public List<IMetapathExpression> getMetapaths() {
      return metapaths;
    }
  }

  private static class MetaTargetedContraints
      extends AbstractTargetedConstraints<IModelConstrained>
      implements IFeatureModelConstrained {

    protected MetaTargetedContraints(
        @NonNull ISource source,
        @NonNull IMetapathExpression target,
        @NonNull IModelConstrained constraints) {
      super(source, target, constraints);
    }

    /**
     * Apply the constraints to the provided {@code definition}.
     * <p>
     * This will be called when a definition is found that matches the target
     * expression.
     *
     * @param definition
     *          the definition to apply the constraints to.
     */
    protected void applyTo(@NonNull IDefinition definition) {
      getLetExpressions().values().forEach(definition::addLetExpression);
      getAllowedValuesConstraints().forEach(definition::addConstraint);
      getMatchesConstraints().forEach(definition::addConstraint);
      getIndexHasKeyConstraints().forEach(definition::addConstraint);
      getExpectConstraints().forEach(definition::addConstraint);
    }

    protected void applyTo(@NonNull IAssemblyDefinition definition) {
      applyTo((IDefinition) definition);
      getIndexConstraints().forEach(definition::addConstraint);
      getUniqueConstraints().forEach(definition::addConstraint);
      getHasCardinalityConstraints().forEach(definition::addConstraint);
    }

    @Override
    public void target(IFlagDefinition definition) {
      applyTo(definition);
    }

    @Override
    public void target(IFieldDefinition definition) {
      applyTo(definition);
    }

    @Override
    public void target(IAssemblyDefinition definition) {
      applyTo(definition);
    }
  }

  private static final class MetaConstraintSet implements IConstraintSet {
    @NonNull
    private final ISource source;
    @NonNull
    private final List<ITargetedConstraints> targetedConstraints;

    private MetaConstraintSet(
        @NonNull ISource source,
        @NonNull List<ITargetedConstraints> targetedConstraints) {
      this.source = source;
      this.targetedConstraints = targetedConstraints;
    }

    @Override
    public ISource getSource() {
      return source;
    }

    @Override
    public Iterable<ITargetedConstraints> getTargetedConstraintsForModule(IModule module) {
      return targetedConstraints;
    }

    @Override
    public Collection<IConstraintSet> getImportedConstraintSets() {
      return CollectionUtil.emptyList();
    }

  }
}
