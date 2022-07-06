/*
 * Portions of this software was developed by employees of the National Institute
 * of Standards and Technology (NIST), an agency of the Federal Government and is
 * being made available as a public service. Pursuant to title 17 United States
 * Code Section 105, works of NIST employees are not subject to copyright
 * protection in the United States. This software may be subject to foreign
 * copyright. Permission in the United States and in foreign countries, to the
 * extent that NIST may hold copyright, to use, copy, modify, create derivative
 * works, and distribute this software and its documentation without fee is hereby
 * granted on a non-exclusive basis, provided that this notice and disclaimer
 * of warranty appears in all copies.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER
 * EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY
 * THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND FREEDOM FROM
 * INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL CONFORM TO THE
 * SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL BE ERROR FREE.  IN NO EVENT
 * SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT,
 * INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, RESULTING FROM,
 * OR IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY,
 * CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR
 * PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT
 * OF THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */

package gov.nist.secauto.metaschema.model;

import gov.nist.secauto.metaschema.model.common.MetaschemaException;
import gov.nist.secauto.metaschema.model.common.util.CollectionUtil;
import gov.nist.secauto.metaschema.model.common.util.ObjectUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public abstract class AbstractLoader<T> {
  private static final Logger LOGGER = LogManager.getLogger(ConstraintLoader.class);

  @NotNull
  private final Map<@NotNull URI, T> cache = new LinkedHashMap<>(); // NOPMD - intentional

  /**
   * Retrieve the set of loaded resources.
   * 
   * @return the set of loaded resources
   */
  @SuppressWarnings("null")
  @NotNull
  public Collection<@NotNull T> getLoadedConstraintSets() {
    return CollectionUtil.unmodifiableCollection(cache.values());
  }

  /**
   * Retrieve a mapping of resource URIs to the associated loaded resource.
   * 
   * @return the mapping
   */
  @NotNull
  protected Map<@NotNull URI, T> getCachedEntries() {
    return CollectionUtil.unmodifiableMap(cache);
  }

  /**
   * Load a resource from the specified URI.
   * 
   * @param resource
   *          the resource to load
   * @return the loaded instance for the specified resource
   * @throws MetaschemaException
   *           if an error occurred while processing the resource
   * @throws IOException
   *           if an error occurred parsing the resource
   */
  @NotNull
  public T load(@NotNull URI resource) throws MetaschemaException, IOException {
    if (!resource.isAbsolute()) {
      throw new IllegalArgumentException(String.format("The URI '%s' must be absolute.", resource.toString()));
    }
    return loadInternal(resource, new Stack<>());
  }

  /**
   * Load a resource from the specified path.
   * 
   * @param path
   *          the resource to load
   * @return the loaded instance for the specified resource
   * @throws MetaschemaException
   *           if an error occurred while processing the resource
   * @throws IOException
   *           if an error occurred parsing the resource
   */
  @NotNull
  public T load(@NotNull Path path) throws MetaschemaException, IOException {
    return loadInternal(ObjectUtils.notNull(path.toUri()), new Stack<>());
  }

  /**
   * Load a resource from the specified file.
   * 
   * @param file
   *          the resource to load
   * @return the loaded instance for the specified resource
   * @throws MetaschemaException
   *           if an error occurred while processing the resource
   * @throws IOException
   *           if an error occurred parsing the resource
   */
  @NotNull
  public T load(@NotNull File file) throws MetaschemaException, IOException {
    return loadInternal(ObjectUtils.notNull(file.toURI()), new Stack<>());
  }

  /**
   * Loads a resource from the specified URL.
   * 
   * @param url
   *          the URL to load the resource from
   * @return the loaded instance for the specified resource
   * @throws MetaschemaException
   *           if an error occurred while processing the resource
   * @throws IOException
   *           if an error occurred parsing the resource
   */
  @NotNull
  public T load(@NotNull URL url) throws MetaschemaException, IOException {
    try {
      URI resource = url.toURI();
      return loadInternal(ObjectUtils.notNull(resource), new Stack<>());
    } catch (URISyntaxException ex) {
      // this should not happen
      LOGGER.error("Invalid url", ex);
      throw new IOException(ex);
    }
  }

  /**
   * Loads a resource from the provided URI.
   * <p>
   * If the resource imports other resources, the provided {@code visitedResources} can be used to
   * track circular imports. This is useful when this method recurses into included resources.
   * <p>
   * Previously loaded resources are provided by the cache. This method will add the resource to the
   * cache after all imported resources have been loaded.
   * 
   * @param resource
   *          the resource to load
   * @param visitedResources
   *          a LIFO queue representing previously visited resources in an import chain
   * @return the loaded resource
   * @throws MetaschemaException
   *           if an error occurred while processing the resource
   * @throws MalformedURLException
   *           if the provided URI is malformed
   * @throws IOException
   *           if an error occurred parsing the resource
   */
  @NotNull
  protected T loadInternal(@NotNull URI resource, @NotNull Stack<@NotNull URI> visitedResources)
      throws MetaschemaException, MalformedURLException, IOException {
    // first check if the current resource has been visited to prevent cycles
    if (visitedResources.contains(resource)) {
      throw new MetaschemaException("Cycle detected in metaschema includes for '" + resource + "'. Call stack: '"
          + visitedResources.stream().map(n -> n.toString()).collect(Collectors.joining(",")));
    }

    T retval = cache.get(resource);
    if (retval == null) {
      LOGGER.info("Loading metaschema '{}'", resource);

      try {
        visitedResources.push(resource);
        retval = parseResource(resource, visitedResources);
      } finally {
        visitedResources.pop();
      }
      cache.put(resource, retval);
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Found metaschema in cache '{}'", resource);
      }
    }
    return ObjectUtils.notNull(retval);
  }

  protected abstract T parseResource(@NotNull URI resource, @NotNull Stack<@NotNull URI> visitedResources)
      throws IOException;

}