/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.databind.codegen.typeinfo;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import gov.nist.secauto.metaschema.core.model.IChoiceGroupInstance;
import gov.nist.secauto.metaschema.core.model.IGroupable;
import gov.nist.secauto.metaschema.core.model.IModelDefinition;
import gov.nist.secauto.metaschema.core.model.INamedModelInstanceGrouped;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.codegen.typeinfo.def.IAssemblyDefinitionTypeInfo;
import gov.nist.secauto.metaschema.databind.model.annotations.BoundChoiceGroup;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;

public class ChoiceGroupTypeInfoImpl
    extends AbstractModelInstanceTypeInfo<IChoiceGroupInstance>
    implements IChoiceGroupTypeInfo {

  /**
   * Create a type information object describing a choice group instance.
   *
   * @param instance
   *          the choice group instance to generate type information for
   * @param parent
   *          the type information for the parent assembly definition containing
   *          the choice group
   */
  public ChoiceGroupTypeInfoImpl(
      @NonNull IChoiceGroupInstance instance,
      @NonNull IAssemblyDefinitionTypeInfo parent) {
    super(instance, parent);
  }

  @Override
  public TypeName getJavaItemType() {
    return getParentTypeInfo().getTypeResolver().getClassName(getInstance());
  }

  @Override
  protected AnnotationSpec.Builder newBindingAnnotation() {
    return ObjectUtils.notNull(AnnotationSpec.builder(BoundChoiceGroup.class));
  }

  @SuppressWarnings({ "PMD.UseConcurrentHashMap", "PMD.NPathComplexity", "PMD.CyclomaticComplexity" })
  @Override
  public Set<IModelDefinition> buildBindingAnnotation(
      TypeSpec.Builder typeBuilder,
      FieldSpec.Builder fieldBuilder,
      AnnotationSpec.Builder annotation) {
    IChoiceGroupInstance choiceGroup = getInstance();

    String discriminator = choiceGroup.getJsonDiscriminatorProperty();
    if (!IChoiceGroupInstance.DEFAULT_JSON_DISCRIMINATOR_PROPERTY_NAME.equals(discriminator)) {
      annotation.addMember("discriminator", "$S", discriminator);
    }

    int minOccurs = choiceGroup.getMinOccurs();
    if (minOccurs != IGroupable.DEFAULT_GROUP_AS_MIN_OCCURS) {
      annotation.addMember("minOccurs", "$L", minOccurs);
    }

    int maxOccurs = choiceGroup.getMaxOccurs();
    if (maxOccurs != IGroupable.DEFAULT_GROUP_AS_MAX_OCCURS) {
      annotation.addMember("maxOccurs", "$L", maxOccurs);
    }

    if (maxOccurs == -1 || maxOccurs > 1) {
      // requires a group-as
      annotation.addMember("groupAs", "$L", generateGroupAsAnnotation().build());
    }

    String jsonKeyName = choiceGroup.getJsonKeyFlagInstanceName();
    if (jsonKeyName != null) {
      annotation.addMember("jsonKey", "$S", jsonKeyName);
    }

    Set<IModelDefinition> retval = new LinkedHashSet<>();

    IAssemblyDefinitionTypeInfo parentTypeInfo = getParentTypeInfo();
    ITypeResolver typeResolver = parentTypeInfo.getTypeResolver();

    Map<ClassName, List<INamedModelInstanceGrouped>> referencedDefinitions = new LinkedHashMap<>();
    Collection<? extends INamedModelInstanceGrouped> modelInstances = getInstance().getNamedModelInstances();
    for (INamedModelInstanceGrouped modelInstance : modelInstances) {
      ClassName className = typeResolver.getClassName(modelInstance.getDefinition());
      List<INamedModelInstanceGrouped> instances = referencedDefinitions.get(className);
      if (instances == null) {
        instances = new LinkedList<>(); // NOPMD needed
        referencedDefinitions.put(className, instances);
      }
      instances.add(modelInstance);
    }

    for (INamedModelInstanceGrouped modelInstance : modelInstances) {
      assert modelInstance != null;
      IGroupedNamedModelInstanceTypeInfo instanceTypeInfo = typeResolver.getTypeInfo(modelInstance, this);

      ClassName className = typeResolver.getClassName(modelInstance.getDefinition());
      retval.addAll(instanceTypeInfo.generateMemberAnnotation(
          annotation,
          typeBuilder,
          referencedDefinitions.get(className).size() > 1));
    }
    return retval;
  }

}
