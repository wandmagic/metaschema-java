/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.databind.model.metaschema.impl;

import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItemFactory;
import gov.nist.secauto.metaschema.core.model.IAssemblyDefinition;
import gov.nist.secauto.metaschema.core.model.IAssemblyInstanceGrouped;
import gov.nist.secauto.metaschema.core.model.IChoiceGroupInstance;
import gov.nist.secauto.metaschema.core.model.IContainerModelSupport;
import gov.nist.secauto.metaschema.core.model.IFieldDefinition;
import gov.nist.secauto.metaschema.core.model.IFieldInstanceGrouped;
import gov.nist.secauto.metaschema.core.model.IModule;
import gov.nist.secauto.metaschema.core.model.INamedModelInstanceGrouped;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelChoiceGroup;
import gov.nist.secauto.metaschema.databind.model.IBoundInstanceModelGroupedAssembly;
import gov.nist.secauto.metaschema.databind.model.metaschema.binding.AssemblyModel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class ChoiceGroupModelContainerSupport
    implements IContainerModelSupport<
        INamedModelInstanceGrouped,
        INamedModelInstanceGrouped,
        IFieldInstanceGrouped,
        IAssemblyInstanceGrouped> {
  @NonNull
  private final Map<QName, INamedModelInstanceGrouped> namedModelInstances;
  @NonNull
  private final Map<QName, IFieldInstanceGrouped> fieldInstances;
  @NonNull
  private final Map<QName, IAssemblyInstanceGrouped> assemblyInstances;

  @SuppressWarnings("PMD.ShortMethodName")
  public static IContainerModelSupport<
      INamedModelInstanceGrouped,
      INamedModelInstanceGrouped,
      IFieldInstanceGrouped,
      IAssemblyInstanceGrouped> of(
          @Nullable AssemblyModel.ChoiceGroup binding,
          @NonNull IBoundInstanceModelGroupedAssembly bindingInstance,
          @NonNull IChoiceGroupInstance parent,
          @NonNull INodeItemFactory nodeItemFactory) {
    List<Object> instances;
    return binding == null || (instances = binding.getChoices()) == null || instances.isEmpty()
        ? IContainerModelSupport.empty()
        : new ChoiceGroupModelContainerSupport(
            binding,
            bindingInstance,
            parent,
            nodeItemFactory);
  }

  /**
   * Construct a new assembly model container.
   *
   * @param binding
   *          the choice model object bound to a Java class
   * @param bindingInstance
   *          the Metaschema module instance for the bound model object
   * @param parent
   *          the assembly definition containing this container
   * @param nodeItemFactory
   *          the node item factory used to generate child nodes
   */
  @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseConcurrentHashMap", "PMD.PrematureDeclaration" })
  @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Use of final fields")
  public ChoiceGroupModelContainerSupport(
      @NonNull AssemblyModel.ChoiceGroup binding,
      @NonNull IBoundInstanceModelGroupedAssembly bindingInstance,
      @NonNull IChoiceGroupInstance parent,
      @NonNull INodeItemFactory nodeItemFactory) {

    // create temporary collections to store the child binding objects
    final Map<QName, INamedModelInstanceGrouped> namedModelInstances = new LinkedHashMap<>();
    final Map<QName, IFieldInstanceGrouped> fieldInstances = new LinkedHashMap<>();
    final Map<QName, IAssemblyInstanceGrouped> assemblyInstances = new LinkedHashMap<>();

    // create counters to track child positions
    int assemblyReferencePosition = 0;
    int assemblyInlineDefinitionPosition = 0;
    int fieldReferencePosition = 0;
    int fieldInlineDefinitionPosition = 0;

    // TODO: make "instances" a constant
    IBoundInstanceModelChoiceGroup instance = ObjectUtils.requireNonNull(
        bindingInstance.getDefinition().getChoiceGroupInstanceByName("choices"));
    for (Object obj : ObjectUtils.notNull(binding.getChoices())) {
      assert obj != null;

      IBoundInstanceModelGroupedAssembly objInstance
          = (IBoundInstanceModelGroupedAssembly) instance.getItemInstance(obj);

      if (obj instanceof AssemblyModel.ChoiceGroup.Assembly) {
        IAssemblyInstanceGrouped assembly = newInstance(
            (AssemblyModel.ChoiceGroup.Assembly) obj,
            objInstance,
            assemblyReferencePosition++,
            parent);
        addInstance(assembly, namedModelInstances, assemblyInstances);
      } else if (obj instanceof AssemblyModel.ChoiceGroup.DefineAssembly) {
        IAssemblyInstanceGrouped assembly = new InstanceModelGroupedAssemblyInline(
            (AssemblyModel.ChoiceGroup.DefineAssembly) obj,
            objInstance,
            assemblyInlineDefinitionPosition++,
            parent,
            nodeItemFactory);
        addInstance(assembly, namedModelInstances, assemblyInstances);
      } else if (obj instanceof AssemblyModel.ChoiceGroup.Field) {
        IFieldInstanceGrouped field = newInstance(
            (AssemblyModel.ChoiceGroup.Field) obj,
            objInstance,
            fieldReferencePosition++,
            parent);
        addInstance(field, namedModelInstances, fieldInstances);
      } else if (obj instanceof AssemblyModel.ChoiceGroup.DefineField) {
        IFieldInstanceGrouped field = new InstanceModelGroupedFieldInline(
            (AssemblyModel.ChoiceGroup.DefineField) obj,
            objInstance,
            fieldInlineDefinitionPosition++,
            parent);
        addInstance(field, namedModelInstances, fieldInstances);
      } else {
        throw new UnsupportedOperationException(
            String.format("Unknown choice group model instance class: %s", obj.getClass()));
      }
    }

    this.namedModelInstances = namedModelInstances.isEmpty()
        ? CollectionUtil.emptyMap()
        : CollectionUtil.unmodifiableMap(namedModelInstances);
    this.fieldInstances = fieldInstances.isEmpty()
        ? CollectionUtil.emptyMap()
        : CollectionUtil.unmodifiableMap(fieldInstances);
    this.assemblyInstances = assemblyInstances.isEmpty()
        ? CollectionUtil.emptyMap()
        : CollectionUtil.unmodifiableMap(assemblyInstances);
  }

  protected static void addInstance(
      @NonNull IAssemblyInstanceGrouped assembly,
      @NonNull Map<QName, INamedModelInstanceGrouped> namedModelInstances,
      @NonNull Map<QName, IAssemblyInstanceGrouped> assemblyInstances) {
    QName effectiveName = assembly.getXmlQName();
    namedModelInstances.put(effectiveName, assembly);
    assemblyInstances.put(effectiveName, assembly);
  }

  protected static void addInstance(
      @NonNull IFieldInstanceGrouped field,
      @NonNull Map<QName, INamedModelInstanceGrouped> namedModelInstances,
      @NonNull Map<QName, IFieldInstanceGrouped> fieldInstances) {
    QName effectiveName = field.getXmlQName();
    namedModelInstances.put(effectiveName, field);
    fieldInstances.put(effectiveName, field);
  }

  @NonNull
  protected static IAssemblyInstanceGrouped newInstance(
      @NonNull AssemblyModel.ChoiceGroup.Assembly obj,
      @NonNull IBoundInstanceModelGroupedAssembly objInstance,
      int position,
      @NonNull IChoiceGroupInstance parent) {
    IAssemblyDefinition owningDefinition = parent.getOwningDefinition();
    IModule module = owningDefinition.getContainingModule();

    QName name = parent.getContainingModule().toModelQName(ObjectUtils.requireNonNull(obj.getRef()));
    IAssemblyDefinition definition = module.getScopedAssemblyDefinitionByName(name);

    if (definition == null) {
      throw new IllegalStateException(
          String.format("Unable to resolve assembly reference '%s' in definition '%s' in module '%s'",
              name,
              owningDefinition.getName(),
              module.getShortName()));
    }
    return new InstanceModelGroupedAssemblyReference(obj, objInstance, position, definition, parent);
  }

  @NonNull
  protected static IFieldInstanceGrouped newInstance(
      @NonNull AssemblyModel.ChoiceGroup.Field obj,
      @NonNull IBoundInstanceModelGroupedAssembly objInstance,
      int position,
      @NonNull IChoiceGroupInstance parent) {
    IAssemblyDefinition owningDefinition = parent.getOwningDefinition();
    IModule module = owningDefinition.getContainingModule();

    QName name = parent.getContainingModule().toModelQName(ObjectUtils.requireNonNull(obj.getRef()));
    IFieldDefinition definition = module.getScopedFieldDefinitionByName(name);
    if (definition == null) {
      throw new IllegalStateException(
          String.format("Unable to resolve field reference '%s' in definition '%s' in module '%s'",
              name,
              owningDefinition.getName(),
              module.getShortName()));
    }
    return new InstanceModelGroupedFieldReference(obj, objInstance, position, definition, parent);
  }

  @SuppressWarnings("null")
  @Override
  public Collection<INamedModelInstanceGrouped> getModelInstances() {
    return namedModelInstances.values();
  }

  @Override
  public Map<QName, INamedModelInstanceGrouped> getNamedModelInstanceMap() {
    return namedModelInstances;
  }

  @Override
  public Map<QName, IFieldInstanceGrouped> getFieldInstanceMap() {
    return fieldInstances;
  }

  @Override
  public Map<QName, IAssemblyInstanceGrouped> getAssemblyInstanceMap() {
    return assemblyInstances;
  }
}
