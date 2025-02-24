/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.core.metapath.cst.path;

import gov.nist.secauto.metaschema.core.metapath.item.node.IDefinitionNodeItem;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A wildcard matcher that matches any local name in a specific namespace.
 * <p>
 * This matcher implements the namespace:* pattern where it matches any node
 * whose namespace exactly matches the specified namespace, regardless of the
 * local name.
 */
class MatchAnyLocalName implements IWildcardMatcher {
  @NonNull
  private final String namespace;

  /**
   * Construct the matcher using the provided namespace for matching.
   *
   * @param namespace
   *          the namespace used to match nodes
   */
  public MatchAnyLocalName(@NonNull String namespace) {
    this.namespace = namespace;
  }

  @Override
  public boolean test(IDefinitionNodeItem<?, ?> item) {
    return namespace.equals(item.getQName().getNamespace());
  }

  @Override
  public String toString() {
    return namespace + ":*";
  }
}
