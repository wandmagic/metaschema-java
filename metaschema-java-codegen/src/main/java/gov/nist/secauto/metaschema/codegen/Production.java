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

package gov.nist.secauto.metaschema.codegen;

import gov.nist.secauto.metaschema.codegen.type.ITypeResolver;
import gov.nist.secauto.metaschema.model.common.IMetaschema;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Production {

  private final Map<IMetaschema, MetaschemaProduction> metaschemaToProductionMap // NOPMD - immutable
      = new HashMap<>();
  private final Map<String, PackageProduction> packageNameToProductionMap // NOPMD - immutable
      = new HashMap<>();

  public static Production of( // NOPMD - intentional name
      @NotNull IMetaschema metaschema,
      @NotNull ITypeResolver typeResolver,
      @NotNull Path targetDirectory) throws IOException {
    return of(Collections.singleton(metaschema), typeResolver, targetDirectory);
  }

  public static Production of( // NOPMD - intentional name
      @NotNull Collection<? extends IMetaschema> metaschemas,
      @NotNull ITypeResolver typeResolver,
      @NotNull Path targetDirectory) throws IOException {
    Production retval = new Production();
    for (IMetaschema metaschema : metaschemas) {
      processMetaschema(metaschema, retval, typeResolver, targetDirectory);
    }

    Map<String, PackageMetadata> packageNameToPackageMetadataMap = new HashMap<>(); // NOPMD - no concurrency
    for (MetaschemaProduction metaschemaProduction : retval.getMetaschemaProductions()) {
      String packageName = metaschemaProduction.getPackageName();

      PackageMetadata metadata = packageNameToPackageMetadataMap.get(packageName);
      if (metadata == null) {
        metadata = new PackageMetadata(metaschemaProduction); // NOPMD - intentional
        packageNameToPackageMetadataMap.put(metadata.getPackageName(), metadata);
      } else {
        metadata.addMetaschema(metaschemaProduction);
      }
    }

    for (PackageMetadata metadata : packageNameToPackageMetadataMap.values()) {
      retval.addPackage(metadata.getPackageName(), metadata.getXmlNamespace(), metadata.getMetaschemaProductions(),
          targetDirectory);
    }
    return retval;
  }

  private static void processMetaschema(@NotNull IMetaschema metaschema, @NotNull Production production,
      @NotNull ITypeResolver typeResolver, @NotNull Path targetDirectory) throws IOException {
    for (IMetaschema importedMetaschema : metaschema.getImportedMetaschemas()) {
      processMetaschema(importedMetaschema, production, typeResolver, targetDirectory);
    }

    if (production.getMetaschemaProduction(metaschema) == null) {
      production.addMetaschema(metaschema, typeResolver, targetDirectory);
    }
  }

  public MetaschemaProduction addMetaschema(@NotNull IMetaschema metaschema, @NotNull ITypeResolver typeResolver,
      @NotNull Path targetDirectory) throws IOException {
    MetaschemaProduction retval = metaschemaToProductionMap.get(metaschema);
    if (retval == null) {
      metaschemaToProductionMap.put(metaschema, new MetaschemaProduction(metaschema, typeResolver, targetDirectory));
    }

    return retval;
  }

  public PackageProduction addPackage(@NotNull String javaPackage, @NotNull URI xmlNamespace,
      @NotNull List<MetaschemaProduction> metaschemaProductions, @NotNull Path targetDirectory) throws IOException {
    PackageProduction retval = new PackageProduction(javaPackage, xmlNamespace, metaschemaProductions, targetDirectory);
    packageNameToProductionMap.put(javaPackage, retval);
    return retval;
  }

  @NotNull
  public Collection<MetaschemaProduction> getMetaschemaProductions() {
    return Collections.unmodifiableCollection(metaschemaToProductionMap.values());
  }

  protected Collection<PackageProduction> getPackageProductions() {
    return Collections.unmodifiableCollection(packageNameToProductionMap.values());
  }

  public MetaschemaProduction getMetaschemaProduction(@NotNull IMetaschema metaschema) {
    return metaschemaToProductionMap.get(metaschema);
  }

  public Stream<DefinitionProduction> getDefinitionProductionsAsStream() {
    return getMetaschemaProductions().stream()
        .flatMap(metaschema -> metaschema.getDefinitionProductions().stream());
  }

  public Stream<GeneratedClass> getGeneratedClasses() {
    return Stream.concat(
        getMetaschemaProductions().stream()
            .flatMap(metaschema -> metaschema.getGeneratedClasses()),
        getPackageProductions().stream()
            .flatMap(javaPackage -> Stream.of(javaPackage.getGeneratedClass())));
  }

  private static class PackageMetadata {
    private final String packageName;
    private final URI xmlNamespace;
    private final List<MetaschemaProduction> metaschemaProductions = new LinkedList<>();

    public PackageMetadata(@NotNull MetaschemaProduction metaschemaProduction) {
      packageName = metaschemaProduction.getPackageName();
      xmlNamespace = metaschemaProduction.getMetaschema().getXmlNamespace();
      metaschemaProductions.add(metaschemaProduction);
    }

    protected String getPackageName() {
      return packageName;
    }

    protected URI getXmlNamespace() {
      return xmlNamespace;
    }

    protected List<MetaschemaProduction> getMetaschemaProductions() {
      return metaschemaProductions;
    }

    private void addMetaschema(@NotNull MetaschemaProduction metaschemaProduction) {
      URI nextXmlNamespace = metaschemaProduction.getMetaschema().getXmlNamespace();
      if (!xmlNamespace.equals(nextXmlNamespace)) {
        String.format(
            "The package %s is associated with the XML namespaces '%s' and '%s'."
                + " A package must be associated with a single XML namespace.",
            getPackageName(), getXmlNamespace().toASCIIString(), nextXmlNamespace.toASCIIString());
      }
      metaschemaProductions.add(metaschemaProduction);
    }
  }
}