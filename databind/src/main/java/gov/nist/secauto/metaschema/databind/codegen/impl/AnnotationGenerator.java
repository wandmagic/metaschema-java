/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.databind.codegen.impl;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.AnnotationSpec.Builder;

import gov.nist.secauto.metaschema.core.datatype.IDataTypeAdapter;
import gov.nist.secauto.metaschema.core.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.core.datatype.markup.MarkupMultiline;
import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.node.IAssemblyNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.IDefinitionNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItemFactory;
import gov.nist.secauto.metaschema.core.model.IAssemblyDefinition;
import gov.nist.secauto.metaschema.core.model.IFlagDefinition;
import gov.nist.secauto.metaschema.core.model.IModelDefinition;
import gov.nist.secauto.metaschema.core.model.INamedInstance;
import gov.nist.secauto.metaschema.core.model.INamedModelInstanceAbsolute;
import gov.nist.secauto.metaschema.core.model.constraint.IAllowedValue;
import gov.nist.secauto.metaschema.core.model.constraint.IAllowedValuesConstraint;
import gov.nist.secauto.metaschema.core.model.constraint.ICardinalityConstraint;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraint;
import gov.nist.secauto.metaschema.core.model.constraint.IExpectConstraint;
import gov.nist.secauto.metaschema.core.model.constraint.IIndexConstraint;
import gov.nist.secauto.metaschema.core.model.constraint.IIndexHasKeyConstraint;
import gov.nist.secauto.metaschema.core.model.constraint.IKeyField;
import gov.nist.secauto.metaschema.core.model.constraint.ILet;
import gov.nist.secauto.metaschema.core.model.constraint.IMatchesConstraint;
import gov.nist.secauto.metaschema.core.model.constraint.IUniqueConstraint;
import gov.nist.secauto.metaschema.core.qname.IEnhancedQName;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.model.annotations.AllowedValue;
import gov.nist.secauto.metaschema.databind.model.annotations.AllowedValues;
import gov.nist.secauto.metaschema.databind.model.annotations.AssemblyConstraints;
import gov.nist.secauto.metaschema.databind.model.annotations.Expect;
import gov.nist.secauto.metaschema.databind.model.annotations.HasCardinality;
import gov.nist.secauto.metaschema.databind.model.annotations.Index;
import gov.nist.secauto.metaschema.databind.model.annotations.IndexHasKey;
import gov.nist.secauto.metaschema.databind.model.annotations.IsUnique;
import gov.nist.secauto.metaschema.databind.model.annotations.KeyField;
import gov.nist.secauto.metaschema.databind.model.annotations.Let;
import gov.nist.secauto.metaschema.databind.model.annotations.Matches;
import gov.nist.secauto.metaschema.databind.model.annotations.ValueConstraints;

import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A variety of utility functions for creating Module annotations.
 */
@SuppressWarnings({
    "PMD.GodClass", "PMD.CouplingBetweenObjects" // utility class
})
public final class AnnotationGenerator {
  private static final Logger LOGGER = LogManager.getLogger(AnnotationGenerator.class);

  private AnnotationGenerator() {
    // disable construction
  }

  /**
   * Get the default vale of the given member of an annotation.
   *
   * @param annotation
   *          the annotation to analyze
   * @param member
   *          the annotation member to analyze
   * @return the default value for the annotation member or {@code null} if there
   *         is not default value
   */
  public static Object getDefaultValue(Class<?> annotation, String member) {
    Method method;
    try {
      method = annotation.getDeclaredMethod(member);
    } catch (NoSuchMethodException ex) {
      throw new IllegalArgumentException(ex);
    }
    Object retval = null;
    try {
      retval = method.getDefaultValue();
    } catch (@SuppressWarnings("unused") TypeNotPresentException ex) {
      // no default value found
    }
    return retval;
  }

  private static void buildConstraint(Class<?> annotationType, AnnotationSpec.Builder annotation,
      IConstraint constraint) {
    String id = constraint.getId();
    if (id != null) {
      annotation.addMember("id", "$S", id);
    }

    String formalName = constraint.getFormalName();
    if (formalName != null) {
      annotation.addMember("formalName", "$S", formalName);
    }

    MarkupLine description = constraint.getDescription();
    if (description != null) {
      annotation.addMember("description", "$S", description.toMarkdown());
    }

    annotation.addMember("level", "$T.$L", IConstraint.Level.class, constraint.getLevel());

    String target = constraint.getTarget().getPath();
    if (!target.equals(getDefaultValue(annotationType, "target"))) {
      annotation.addMember("target", "$S", target);
    }
  }

  /**
   * Build a value constraints annotation.
   *
   * @param builder
   *          the annotation builder
   * @param definition
   *          the definition to get the value constraints for
   */
  public static void buildValueConstraints(
      @NonNull AnnotationSpec.Builder builder,
      @NonNull IFlagDefinition definition) {

    Map<IEnhancedQName, ? extends ILet> lets = definition.getLetExpressions();
    if (!lets.isEmpty() || !definition.getConstraints().isEmpty()) {
      AnnotationSpec.Builder annotation = AnnotationSpec.builder(ValueConstraints.class);
      assert annotation != null;

      applyLetAssignments(annotation, lets);
      applyAllowedValuesConstraints(annotation, definition.getAllowedValuesConstraints());
      applyIndexHasKeyConstraints(annotation, definition.getIndexHasKeyConstraints());
      applyMatchesConstraints(annotation, definition.getMatchesConstraints());
      applyExpectConstraints(annotation, definition.getExpectConstraints());

      builder.addMember("valueConstraints", "$L", annotation.build());
    }
  }

  /**
   * Build a value constraints annotation.
   *
   * @param builder
   *          the annotation builder
   * @param definition
   *          the definition to get the value constraints for
   */
  public static void buildValueConstraints(
      @NonNull AnnotationSpec.Builder builder,
      @NonNull IModelDefinition definition) {

    Map<IEnhancedQName, ? extends ILet> lets = definition.getLetExpressions();
    List<? extends IAllowedValuesConstraint> allowedValues = definition.getAllowedValuesConstraints();
    List<? extends IIndexHasKeyConstraint> indexHasKey = definition.getIndexHasKeyConstraints();
    List<? extends IMatchesConstraint> matches = definition.getMatchesConstraints();
    List<? extends IExpectConstraint> expects = definition.getExpectConstraints();

    if (!lets.isEmpty() || !allowedValues.isEmpty() || !indexHasKey.isEmpty() || !matches.isEmpty()
        || !expects.isEmpty()) {
      AnnotationSpec.Builder annotation = AnnotationSpec.builder(ValueConstraints.class);
      assert annotation != null;

      applyLetAssignments(annotation, lets);
      applyAllowedValuesConstraints(annotation, allowedValues);
      applyIndexHasKeyConstraints(annotation, indexHasKey);
      applyMatchesConstraints(annotation, matches);
      applyExpectConstraints(annotation, expects);

      builder.addMember("valueConstraints", "$L", annotation.build());
    }
  }

  /**
   * Build an assembly constraints annotation.
   *
   * @param builder
   *          the annotation builder
   * @param definition
   *          the definition to get the value constraints for
   */
  public static void buildAssemblyConstraints(
      @NonNull AnnotationSpec.Builder builder,
      @NonNull IAssemblyDefinition definition) {

    List<? extends IIndexConstraint> index = definition.getIndexConstraints();
    List<? extends IUniqueConstraint> unique = definition.getUniqueConstraints();
    List<? extends ICardinalityConstraint> cardinality = definition.getHasCardinalityConstraints();

    if (!index.isEmpty() || !unique.isEmpty() || !cardinality.isEmpty()) {
      AnnotationSpec.Builder annotation = ObjectUtils.notNull(AnnotationSpec.builder(AssemblyConstraints.class));

      applyIndexConstraints(annotation, index);
      applyUniqueConstraints(annotation, unique);
      applyHasCardinalityConstraints(definition, annotation, cardinality);

      builder.addMember("modelConstraints", "$L", annotation.build());
    }
  }

  private static void applyLetAssignments(
      @NonNull AnnotationSpec.Builder annotation,
      @NonNull Map<IEnhancedQName, ? extends ILet> lets) {
    for (ILet let : lets.values()) {
      AnnotationSpec.Builder letAnnotation = AnnotationSpec.builder(Let.class);
      letAnnotation.addMember("name", "$S", let.getName());
      letAnnotation.addMember("target", "$S", let.getValueExpression().getPath());

      MarkupMultiline remarks = let.getRemarks();
      if (remarks != null) {
        letAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }

      annotation.addMember("lets", "$L", letAnnotation.build());
    }
  }

  private static void applyAllowedValuesConstraints(
      @NonNull AnnotationSpec.Builder annotation,
      @NonNull List<? extends IAllowedValuesConstraint> constraints) {
    for (IAllowedValuesConstraint constraint : constraints) {
      assert constraint != null;
      AnnotationSpec.Builder constraintAnnotation = AnnotationSpec.builder(AllowedValues.class);
      buildConstraint(AllowedValues.class, constraintAnnotation, constraint);

      boolean isAllowedOther = constraint.isAllowedOther();
      if (isAllowedOther != (boolean) getDefaultValue(AllowedValues.class, "allowOthers")) {
        constraintAnnotation.addMember("allowOthers", "$L", isAllowedOther);
      }

      for (IAllowedValue value : constraint.getAllowedValues().values()) {
        AnnotationSpec.Builder valueAnnotation = AnnotationSpec.builder(AllowedValue.class);

        valueAnnotation.addMember("value", "$S", value.getValue());
        valueAnnotation.addMember("description", "$S", value.getDescription().toMarkdown());

        String deprecatedVersion = value.getDeprecatedVersion();
        if (deprecatedVersion != null) {
          valueAnnotation.addMember("deprecatedVersion", "$S", deprecatedVersion);
        }

        constraintAnnotation.addMember("values", "$L", valueAnnotation.build());
      }

      MarkupMultiline remarks = constraint.getRemarks();
      if (remarks != null) {
        constraintAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }
      annotation.addMember("allowedValues", "$L", constraintAnnotation.build());
    }
  }

  private static void applyIndexHasKeyConstraints(
      @NonNull AnnotationSpec.Builder annotation,
      @NonNull List<? extends IIndexHasKeyConstraint> constraints) {
    for (IIndexHasKeyConstraint constraint : constraints) {
      assert constraint != null;
      AnnotationSpec.Builder constraintAnnotation = AnnotationSpec.builder(IndexHasKey.class);
      buildConstraint(IndexHasKey.class, constraintAnnotation, constraint);

      constraintAnnotation.addMember("indexName", "$S", constraint.getIndexName());

      buildKeyFields(constraintAnnotation, constraint.getKeyFields());

      String message = constraint.getMessage();
      if (message != null) {
        constraintAnnotation.addMember("message", "$S", message);
      }

      MarkupMultiline remarks = constraint.getRemarks();
      if (remarks != null) {
        constraintAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }

      annotation.addMember("indexHasKey", "$L", constraintAnnotation.build());
    }
  }

  private static void buildKeyFields(
      @NonNull Builder constraintAnnotation,
      @NonNull List<? extends IKeyField> keyFields) {
    for (IKeyField key : keyFields) {
      assert key != null;
      AnnotationSpec.Builder keyAnnotation = AnnotationSpec.builder(KeyField.class);

      String target = key.getTarget().getPath();
      if (!target.equals(getDefaultValue(KeyField.class, "target"))) {
        keyAnnotation.addMember("target", "$S", target);
      }

      Pattern pattern = key.getPattern();
      if (pattern != null) {
        keyAnnotation.addMember("pattern", "$S", pattern.pattern());
      }

      MarkupMultiline remarks = key.getRemarks();
      if (remarks != null) {
        keyAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }

      constraintAnnotation.addMember("keyFields", "$L", keyAnnotation.build());
    }
  }

  private static void applyMatchesConstraints(
      @NonNull AnnotationSpec.Builder annotation,
      @NonNull List<? extends IMatchesConstraint> constraints) {
    for (IMatchesConstraint constraint : constraints) {
      assert constraint != null;

      AnnotationSpec.Builder constraintAnnotation = AnnotationSpec.builder(Matches.class);
      buildConstraint(Matches.class, constraintAnnotation, constraint);

      Pattern pattern = constraint.getPattern();
      if (pattern != null) {
        constraintAnnotation.addMember("pattern", "$S", pattern.pattern());
      }

      IDataTypeAdapter<?> dataType = constraint.getDataType();
      if (dataType != null) {
        constraintAnnotation.addMember("typeAdapter", "$T.class", dataType.getClass());
      }

      String message = constraint.getMessage();
      if (message != null) {
        constraintAnnotation.addMember("message", "$S", message);
      }

      MarkupMultiline remarks = constraint.getRemarks();
      if (remarks != null) {
        constraintAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }
      annotation.addMember("matches", "$L", constraintAnnotation.build());
    }
  }

  private static void applyExpectConstraints(
      @NonNull AnnotationSpec.Builder annotation,
      @NonNull List<? extends IExpectConstraint> constraints) {
    for (IExpectConstraint constraint : constraints) {
      assert constraint != null;

      AnnotationSpec.Builder constraintAnnotation = AnnotationSpec.builder(Expect.class);

      buildConstraint(Expect.class, constraintAnnotation, constraint);

      constraintAnnotation.addMember("test", "$S", constraint.getTest().getPath());

      if (constraint.getMessage() != null) {
        constraintAnnotation.addMember("message", "$S", constraint.getMessage());
      }

      MarkupMultiline remarks = constraint.getRemarks();
      if (remarks != null) {
        constraintAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }

      annotation.addMember("expect", "$L", constraintAnnotation.build());
    }
  }

  private static void applyIndexConstraints(
      @NonNull AnnotationSpec.Builder annotation,
      @NonNull List<? extends IIndexConstraint> constraints) {
    for (IIndexConstraint constraint : constraints) {
      assert constraint != null;

      AnnotationSpec.Builder constraintAnnotation = AnnotationSpec.builder(Index.class);

      buildConstraint(Index.class, constraintAnnotation, constraint);

      constraintAnnotation.addMember("name", "$S", constraint.getName());

      buildKeyFields(constraintAnnotation, constraint.getKeyFields());

      String message = constraint.getMessage();
      if (message != null) {
        constraintAnnotation.addMember("message", "$S", message);
      }

      MarkupMultiline remarks = constraint.getRemarks();
      if (remarks != null) {
        constraintAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }

      annotation.addMember("index", "$L", constraintAnnotation.build());
    }
  }

  private static void applyUniqueConstraints(
      @NonNull AnnotationSpec.Builder annotation,
      @NonNull List<? extends IUniqueConstraint> constraints) {
    for (IUniqueConstraint constraint : constraints) {
      assert constraint != null;

      AnnotationSpec.Builder constraintAnnotation = ObjectUtils.notNull(AnnotationSpec.builder(IsUnique.class));

      buildConstraint(IsUnique.class, constraintAnnotation, constraint);

      buildKeyFields(constraintAnnotation, constraint.getKeyFields());

      String message = constraint.getMessage();
      if (message != null) {
        constraintAnnotation.addMember("message", "$S", message);
      }

      MarkupMultiline remarks = constraint.getRemarks();
      if (remarks != null) {
        constraintAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }

      annotation.addMember("unique", "$L", constraintAnnotation.build());
    }
  }

  @SuppressWarnings({
      "PMD.GuardLogStatement" // guarded in outer calls
  })
  private static void checkCardinalities(
      @NonNull IAssemblyDefinition definition,
      @NonNull ICardinalityConstraint constraint,
      @NonNull ISequence<? extends IDefinitionNodeItem<?, ?>> instanceSet,
      @NonNull LogBuilder logBuilder) {

    LogBuilder warn = LOGGER.atWarn();
    for (IDefinitionNodeItem<?, ?> item : instanceSet) {
      INamedInstance instance = item.getInstance();
      if (instance instanceof INamedModelInstanceAbsolute) {
        INamedModelInstanceAbsolute modelInstance = (INamedModelInstanceAbsolute) instance;

        checkMinOccurs(definition, constraint, modelInstance, logBuilder);
        checkMaxOccurs(definition, constraint, modelInstance, logBuilder);
      } else {
        warn.log(String.format(
            "Definition '%s' has min-occurs=%d cardinality constraint targeting '%s' that is not a model instance",
            definition.getName(),
            constraint.getMinOccurs(),
            constraint.getTarget().getPath()));
      }
    }
  }

  @SuppressWarnings({
      "PMD.GuardLogStatement" // guarded in outer calls
  })
  private static void checkMinOccurs(
      @NonNull IAssemblyDefinition definition,
      @NonNull ICardinalityConstraint constraint,
      @NonNull INamedModelInstanceAbsolute modelInstance,
      @NonNull LogBuilder logBuilder) {
    Integer minOccurs = constraint.getMinOccurs();
    if (minOccurs != null) {
      if (minOccurs == modelInstance.getMinOccurs()) {
        logBuilder.log(String.format(
            "Definition '%s' has min-occurs=%d cardinality constraint targeting '%s' that is redundant with a"
                + " targeted instance named '%s' that requires min-occurs=%d",
            definition.getName(),
            minOccurs,
            constraint.getTarget().getPath(),
            modelInstance.getName(),
            modelInstance.getMinOccurs()));
      } else if (minOccurs < modelInstance.getMinOccurs()) {
        logBuilder.log(String.format(
            "Definition '%s' has min-occurs=%d cardinality constraint targeting '%s' that conflicts with a"
                + " targeted instance named '%s' that requires min-occurs=%d",
            definition.getName(),
            minOccurs,
            constraint.getTarget().getPath(),
            modelInstance.getName(),
            modelInstance.getMinOccurs()));
      }
    }
  }

  @SuppressWarnings({
      "PMD.GuardLogStatement" // guarded in outer calls
  })
  private static void checkMaxOccurs(
      @NonNull IAssemblyDefinition definition,
      @NonNull ICardinalityConstraint constraint,
      @NonNull INamedModelInstanceAbsolute modelInstance,
      @NonNull LogBuilder logBuilder) {
    Integer maxOccurs = constraint.getMaxOccurs();
    if (maxOccurs != null) {
      if (maxOccurs == modelInstance.getMaxOccurs()) {
        logBuilder.log(String.format(
            "Definition '%s' has max-occurs=%d cardinality constraint targeting '%s' that is redundant with a"
                + " targeted instance named '%s' that requires max-occurs=%d",
            definition.getName(),
            maxOccurs,
            constraint.getTarget().getPath(),
            modelInstance.getName(),
            modelInstance.getMaxOccurs()));
      } else if (maxOccurs < modelInstance.getMaxOccurs()) {
        logBuilder.log(String.format(
            "Definition '%s' has max-occurs=%d cardinality constraint targeting '%s' that conflicts with a"
                + " targeted instance named '%s' that requires max-occurs=%d",
            definition.getName(),
            maxOccurs,
            constraint.getTarget().getPath(),
            modelInstance.getName(),
            modelInstance.getMaxOccurs()));
      }
    }
  }

  private static void applyHasCardinalityConstraints(
      @NonNull IAssemblyDefinition definition,
      @NonNull AnnotationSpec.Builder annotation,
      @NonNull List<? extends ICardinalityConstraint> constraints) {

    DynamicContext dynamicContext = new DynamicContext();
    dynamicContext.disablePredicateEvaluation();

    for (ICardinalityConstraint constraint : constraints) {
      assert constraint != null;

      IAssemblyNodeItem definitionNodeItem
          = INodeItemFactory.instance().newAssemblyNodeItem(definition);

      ISequence<? extends IDefinitionNodeItem<?, ?>> instanceSet
          = constraint.matchTargets(definitionNodeItem, dynamicContext);

      if (LOGGER.isWarnEnabled()) {
        checkCardinalities(definition, constraint, instanceSet, ObjectUtils.notNull(LOGGER.atWarn()));
      }

      AnnotationSpec.Builder constraintAnnotation = AnnotationSpec.builder(HasCardinality.class);

      buildConstraint(HasCardinality.class, constraintAnnotation, constraint);

      Integer minOccurs = constraint.getMinOccurs();
      if (minOccurs != null && !minOccurs.equals(getDefaultValue(HasCardinality.class, "minOccurs"))) {
        constraintAnnotation.addMember("minOccurs", "$L", minOccurs);
      }

      Integer maxOccurs = constraint.getMaxOccurs();
      if (maxOccurs != null && !maxOccurs.equals(getDefaultValue(HasCardinality.class, "maxOccurs"))) {
        constraintAnnotation.addMember("maxOccurs", "$L", maxOccurs);
      }

      annotation.addMember("cardinality", "$L", constraintAnnotation.build());

      MarkupMultiline remarks = constraint.getRemarks();
      if (remarks != null) {
        constraintAnnotation.addMember("remarks", "$S", remarks.toMarkdown());
      }
    }
  }
}
