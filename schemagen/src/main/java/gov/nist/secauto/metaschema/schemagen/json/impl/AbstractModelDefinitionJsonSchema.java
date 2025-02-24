/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.schemagen.json.impl;

import gov.nist.secauto.metaschema.core.metapath.StaticMetapathException;
import gov.nist.secauto.metaschema.core.model.IDefinition;
import gov.nist.secauto.metaschema.core.model.IFlagInstance;
import gov.nist.secauto.metaschema.core.model.IModelDefinition;
import gov.nist.secauto.metaschema.core.model.util.ModuleUtils;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.schemagen.IGenerationState;
import gov.nist.secauto.metaschema.schemagen.json.IDefinitionJsonSchema;
import gov.nist.secauto.metaschema.schemagen.json.IJsonGenerationState;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public abstract class AbstractModelDefinitionJsonSchema<D extends IModelDefinition>
    extends AbstractDefinitionJsonSchema<D> {
  @Nullable
  private final String jsonKeyFlagName;
  @Nullable
  private final String discriminatorProperty;
  @Nullable
  private final String discriminatorValue;
  @NonNull
  private final List<FlagInstanceJsonProperty> flagProperties;
  @NonNull
  private final IKey key;

  @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Use of final fields")
  protected AbstractModelDefinitionJsonSchema(
      @NonNull D definition,
      @Nullable String jsonKeyFlagName,
      @Nullable String discriminatorProperty,
      @Nullable String discriminatorValue) {
    super(definition);
    this.jsonKeyFlagName = jsonKeyFlagName;
    this.discriminatorProperty = discriminatorProperty;
    this.discriminatorValue = discriminatorValue;
    this.key = IKey.of(definition, jsonKeyFlagName, discriminatorProperty, discriminatorValue);

    Stream<? extends IFlagInstance> flagStream = definition.getFlagInstances().stream();

    // determine the flag instances to generate
    if (jsonKeyFlagName != null) {
      IFlagInstance jsonKeyFlag;
      try {
        jsonKeyFlag = definition.getFlagInstanceByName(
            ModuleUtils.parseFlagName(definition.getContainingModule(), jsonKeyFlagName).getIndexPosition());
      } catch (StaticMetapathException ex) {
        throw new IllegalArgumentException(ex);
      }
      if (jsonKeyFlag == null) {
        throw new IllegalArgumentException(
            String.format("The referenced json-key flag-name '%s' does not exist on definition '%s'.",
                jsonKeyFlagName,
                definition.getName()));
      }
      flagStream = flagStream.filter(instance -> !jsonKeyFlag.equals(instance));
    }

    this.flagProperties = ObjectUtils.notNull(flagStream
        .map(instance -> new FlagInstanceJsonProperty(ObjectUtils.requireNonNull(instance)))
        .collect(Collectors.toUnmodifiableList()));
  }

  @Override
  public IKey getKey() {
    return key;
  }

  protected String getJsonKeyFlagName() {
    return jsonKeyFlagName;
  }

  protected String getDiscriminatorProperty() {
    return discriminatorProperty;
  }

  protected String getDiscriminatorValue() {
    return discriminatorValue;
  }

  @Override
  protected String generateDefinitionName(IJsonGenerationState state) {
    IModelDefinition definition = getDefinition();
    StringBuilder builder = new StringBuilder();
    if (jsonKeyFlagName != null) {
      builder
          .append(IGenerationState.toCamelCase(jsonKeyFlagName))
          .append("JsonKey");
    }

    if (discriminatorProperty != null || discriminatorValue != null) {
      builder
          .append(IGenerationState.toCamelCase(ObjectUtils.requireNonNull(discriminatorProperty)))
          .append(IGenerationState.toCamelCase(ObjectUtils.requireNonNull(discriminatorValue)))
          .append("Choice");
    }
    return state.getTypeNameForDefinition(
        definition,
        builder.toString());
  }

  protected List<FlagInstanceJsonProperty> getFlagProperties() {
    return flagProperties;
  }

  @Override
  public void gatherDefinitions(
      @NonNull Map<IKey, IDefinitionJsonSchema<?>> gatheredDefinitions,
      @NonNull IJsonGenerationState state) {
    super.gatherDefinitions(gatheredDefinitions, state);

    for (FlagInstanceJsonProperty property : getFlagProperties()) {
      property.gatherDefinitions(gatheredDefinitions, state);
    }
  }

  public static class ComplexKey implements IKey {
    @NonNull
    private final IDefinition definition;
    @Nullable
    private final String jsonKeyFlagName;
    @Nullable
    private final String discriminatorProperty;
    @Nullable
    private final String discriminatorValue;

    public ComplexKey(
        @NonNull IDefinition definition,
        @Nullable String jsonKeyFlagName,
        @Nullable String discriminatorProperty,
        @Nullable String discriminatorValue) {
      this.definition = definition;
      this.jsonKeyFlagName = jsonKeyFlagName;
      this.discriminatorProperty = discriminatorProperty;
      this.discriminatorValue = discriminatorValue;
    }

    @Override
    @NonNull
    public IDefinition getDefinition() {
      return definition;
    }

    @Override
    @Nullable
    public String getJsonKeyFlagName() {
      return jsonKeyFlagName;
    }

    @Override
    public String getDiscriminatorProperty() {
      return discriminatorProperty;
    }

    @Override
    public String getDiscriminatorValue() {
      return discriminatorValue;
    }

    @Override
    public int hashCode() {
      return Objects.hash(definition, jsonKeyFlagName, discriminatorProperty, discriminatorValue);
    }

    @SuppressWarnings("PMD.OnlyOneReturn")
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof IKey)) {
        return false;
      }
      IKey other = (IKey) obj;
      return Objects.equals(definition, other.getDefinition())
          && Objects.equals(jsonKeyFlagName, other.getJsonKeyFlagName())
          && Objects.equals(discriminatorProperty, other.getDiscriminatorProperty())
          && Objects.equals(discriminatorValue, other.getDiscriminatorValue());
    }
  }
}
