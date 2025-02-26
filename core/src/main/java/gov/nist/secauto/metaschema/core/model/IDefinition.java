/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.model;

import gov.nist.secauto.metaschema.core.model.constraint.IFeatureValueConstrained;
import gov.nist.secauto.metaschema.core.qname.IEnhancedQName;

import java.util.Locale;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public interface IDefinition extends INamedModelElement, IAttributable, IFeatureValueConstrained {
  /**
   * Describes the visibility of a definition to other modules.
   */
  enum ModuleScope {
    /**
     * The definition is scoped to only the defining module.
     */
    PRIVATE,
    /**
     * The definition is scoped to its defining module and any importing module.
     */
    PUBLIC;
  }

  @NonNull
  ModuleScope DEFAULT_MODULE_SCOPE = ModuleScope.PUBLIC;

  /**
   * Retrieve the definition's scope within the context of its defining module.
   *
   * @return the module scope
   */
  @NonNull
  default ModuleScope getModuleScope() {
    return ModuleScope.PRIVATE;
  }

  /**
   * The qualified name for the definition.
   * <p>
   * This name is the combination of the definition's namespace, which is the
   * module's namespace, and the definition's name.
   *
   * @return the definition's qualified name
   */
  @NonNull
  IEnhancedQName getDefinitionQName();

  /**
   * Determine if the definition is defined inline, meaning the definition is
   * declared where it is used.
   * <p>
   * If this method returns {@code false}, then {@link #getInlineInstance()} must
   * return {@code null}.
   *
   * @return {@code true} if the definition is declared inline or {@code false} if
   *         the definition is able to be globally referenced
   * @see #getInlineInstance()
   */
  default boolean isInline() {
    return getInlineInstance() != null;
  }

  /**
   * If {@link #isInline()} is {@code true}, return the instance the definition is
   * inlined for.
   * <p>
   * If this method returns {@code null}, then {@link #getInlineInstance()} must
   * return {@code false}.
   *
   * @return the instance or {@code null} otherwise
   * @see #isInline()
   */
  INamedInstance getInlineInstance();

  /**
   * Generates a coordinate string for the provided information element
   * definition.
   *
   * A coordinate consists of the element's:
   * <ul>
   * <li>containing Metaschema's short name
   * <li>model type
   * <li>name
   * <li>hash code
   * </ul>
   *
   * @return the coordinate
   */
  @SuppressWarnings("null")
  @Override
  default String toCoordinates() {
    return String.format("%s:%s-definition:%s(%d)",
        getContainingModule().getShortName(),
        getModelType().toString().toUpperCase(Locale.ROOT),
        getName(),
        hashCode());
  }

  /**
   * Get the resource location information for the provided item, if known.
   *
   * @param itemValue
   *          the item to get the location information for
   *
   * @return the resource location information, or {@code null} if not known
   */
  @Nullable
  default IResourceLocation getLocation(@NonNull Object itemValue) {
    return itemValue instanceof IBoundObject ? ((IBoundObject) itemValue).getMetaschemaData() : null;
  }
}
