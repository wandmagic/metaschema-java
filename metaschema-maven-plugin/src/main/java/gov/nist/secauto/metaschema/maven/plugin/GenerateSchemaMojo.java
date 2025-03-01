/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.maven.plugin;

import gov.nist.secauto.metaschema.core.configuration.DefaultConfiguration;
import gov.nist.secauto.metaschema.core.configuration.IConfiguration;
import gov.nist.secauto.metaschema.core.configuration.IMutableConfiguration;
import gov.nist.secauto.metaschema.core.model.IModule;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.schemagen.ISchemaGenerator;
import gov.nist.secauto.metaschema.schemagen.SchemaGenerationFeature;
import gov.nist.secauto.metaschema.schemagen.json.JsonSchemaGenerator;
import gov.nist.secauto.metaschema.schemagen.xml.XmlSchemaGenerator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Goal which generates XML and JSON schemas for a given set of Metaschema
 * modules.
 */
@Mojo(name = "generate-schemas", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateSchemaMojo
    extends AbstractMetaschemaMojo {
  public enum SchemaFormat {
    XSD,
    JSON_SCHEMA;
  }

  @NonNull
  private static final String STALE_FILE_NAME = "generateSschemaStaleFile";

  @NonNull
  private static final XmlSchemaGenerator XML_SCHEMA_GENERATOR = new XmlSchemaGenerator();
  @NonNull
  private static final JsonSchemaGenerator JSON_SCHEMA_GENERATOR = new JsonSchemaGenerator();

  /**
   * Specifies the formats of the schemas to generate. Multiple formats can be
   * supplied and this plugin will generate a schema for each of the desired
   * formats.
   * <p>
   * A format is specified by supplying one of the following values in a
   * &lt;format&gt; subelement:
   * <ul>
   * <li><em>json</em> - Creates a JSON Schema
   * <li><em>xsd</em> - Creates an XML Schema Definition
   * </ul>
   */
  @Parameter
  private List<String> formats;

  /**
   * If enabled, definitions that are defined inline will be generated as inline
   * types. If disabled, definitions will always be generated as global types.
   */
  @Parameter(defaultValue = "true")
  @SuppressWarnings("PMD.ImmutableField")
  private boolean inlineDefinitions = true;

  /**
   * If enabled, child definitions of a choice that are defined inline will be
   * generated as inline types. If disabled, child definitions of a choice will
   * always be generated as global types. This option will only be used if
   * <code>inlineDefinitions</code> is also enabled.
   */
  @Parameter(defaultValue = "false")
  private boolean inlineChoiceDefinitions; // false;

  /**
   * Determine if inlining definitions is required.
   *
   * @return {@code true} if inlining definitions is required, or {@code false}
   *         otherwise
   */
  protected boolean isInlineDefinitions() {
    return inlineDefinitions;
  }

  /**
   * Determine if inlining choice definitions is required.
   *
   * @return {@code true} if inlining choice definitions is required, or
   *         {@code false} otherwise
   */
  protected boolean isInlineChoiceDefinitions() {
    return inlineChoiceDefinitions;
  }

  /**
   * <p>
   * Gets the last part of the stale filename.
   * </p>
   * <p>
   * The full stale filename will be generated by pre-pending
   * {@code "." + getExecution().getExecutionId()} to this staleFileName.
   *
   * @return the stale filename postfix
   */
  @Override
  protected String getStaleFileName() {
    return STALE_FILE_NAME;
  }

  /**
   * Performs schema generation using the provided Metaschema modules.
   *
   * @param modules
   *          the Metaschema modules to generate the schema for
   * @throws MojoExecutionException
   *           if an error occurred during generation
   */
  @Override
  @NonNull
  protected List<File> generate(@NonNull Set<IModule> modules) throws MojoExecutionException {
    IMutableConfiguration<SchemaGenerationFeature<?>> schemaGenerationConfig
        = new DefaultConfiguration<>();

    if (isInlineDefinitions()) {
      schemaGenerationConfig.enableFeature(SchemaGenerationFeature.INLINE_DEFINITIONS);
    } else {
      schemaGenerationConfig.disableFeature(SchemaGenerationFeature.INLINE_DEFINITIONS);
    }

    if (isInlineChoiceDefinitions()) {
      schemaGenerationConfig.enableFeature(SchemaGenerationFeature.INLINE_CHOICE_DEFINITIONS);
    } else {
      schemaGenerationConfig.disableFeature(SchemaGenerationFeature.INLINE_CHOICE_DEFINITIONS);
    }

    Set<SchemaFormat> schemaFormats;
    if (formats != null) {
      schemaFormats = ObjectUtils.notNull(EnumSet.noneOf(SchemaFormat.class));
      for (String format : formats) {
        switch (format.toLowerCase(Locale.ROOT)) {
        case "xsd":
          schemaFormats.add(SchemaFormat.XSD);
          break;
        case "json":
          schemaFormats.add(SchemaFormat.JSON_SCHEMA);
          break;
        default:
          throw new IllegalStateException("Unsupported schema format: " + format);
        }
      }
    } else {
      schemaFormats = ObjectUtils.notNull(EnumSet.allOf(SchemaFormat.class));
    }

    Path outputDirectory = ObjectUtils.notNull(getOutputDirectory().toPath());
    List<File> generatedSchemas = new LinkedList<>();
    for (IModule module : modules) {
      if (getLog().isInfoEnabled()) {
        getLog().info(String.format("Processing metaschema: %s", module.getLocation()));
      }
      if (module.getExportedRootAssemblyDefinitions().isEmpty()) {
        continue;
      }
      generatedSchemas.addAll(generateSchemas(module, schemaGenerationConfig, outputDirectory, schemaFormats));
    }
    return CollectionUtil.unmodifiableList(generatedSchemas);
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  @NonNull
  private List<File> generateSchemas(
      @NonNull IModule module,
      @NonNull IConfiguration<SchemaGenerationFeature<?>> schemaGenerationConfig,
      @NonNull Path outputDirectory,
      @NonNull Set<SchemaFormat> schemaFormats) throws MojoExecutionException {

    String shortName = module.getShortName();

    List<File> generatedSchemas = new LinkedList<>();
    if (schemaFormats.contains(SchemaFormat.XSD)) {
      try { // XML Schema
        String filename = String.format("%s_schema.xsd", shortName);
        Path xmlSchema = ObjectUtils.notNull(outputDirectory.resolve(filename));
        generateSchema(module, schemaGenerationConfig, xmlSchema, XML_SCHEMA_GENERATOR);
        generatedSchemas.add(xmlSchema.toFile());
      } catch (Exception ex) {
        throw new MojoExecutionException("Unable to generate XML schema.", ex);
      }
    }

    if (schemaFormats.contains(SchemaFormat.JSON_SCHEMA)) {
      try { // JSON Schema
        String filename = String.format("%s_schema.json", shortName);
        Path jsonSchema = ObjectUtils.notNull(outputDirectory.resolve(filename));
        generateSchema(module, schemaGenerationConfig, jsonSchema, JSON_SCHEMA_GENERATOR);
        generatedSchemas.add(jsonSchema.toFile());
      } catch (Exception ex) {
        throw new MojoExecutionException("Unable to generate JSON schema.", ex);
      }
    }
    return CollectionUtil.unmodifiableList(generatedSchemas);
  }

  private static void generateSchema(
      @NonNull IModule module,
      @NonNull IConfiguration<SchemaGenerationFeature<?>> schemaGenerationConfig,
      @NonNull Path schemaPath,
      @NonNull ISchemaGenerator generator) throws IOException {
    try (@SuppressWarnings("resource")
    Writer writer = ObjectUtils.notNull(Files.newBufferedWriter(
        schemaPath,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING))) {
      generator.generateFromModule(module, writer, schemaGenerationConfig);
    }
  }
}
