/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.schemagen;

import gov.nist.secauto.metaschema.core.configuration.IConfiguration;
import gov.nist.secauto.metaschema.core.model.IAssemblyDefinition;
import gov.nist.secauto.metaschema.core.model.IDefinition;
import gov.nist.secauto.metaschema.core.model.IModule;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.schemagen.datatype.IDatatypeManager;

import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Thsi abstract class provides a common implementation shared by all schema
 * generators.
 *
 * @param <T>
 *          the writer type
 * @param <D>
 *          the {@link IDatatypeManager} type
 * @param <S>
 *          the {@link IGenerationState} type
 */
public abstract class AbstractSchemaGenerator<
    T extends AutoCloseable,
    D extends IDatatypeManager,
    S extends AbstractGenerationState<
        T, D>>
    implements ISchemaGenerator {

  /**
   * Create a new writer to use to write the schema.
   *
   * @param out
   *          the {@link Writer} to write the schema content to
   * @return the schema writer
   * @throws SchemaGenerationException
   *           if an error occurred while creating the writer
   */
  @NonNull
  protected abstract T newWriter(@NonNull Writer out);

  /**
   * Create a new schema generation state object.
   *
   * @param module
   *          the Metaschema module to generate the schema for
   * @param schemaWriter
   *          the writer to use to write the schema
   * @param configuration
   *          the generation configuration
   * @return the schema generation state used for context and writing
   * @throws SchemaGenerationException
   *           if an error occurred while creating the generation state object
   */
  @NonNull
  protected abstract S newGenerationState(
      @NonNull IModule module,
      @NonNull T schemaWriter,
      @NonNull IConfiguration<SchemaGenerationFeature<?>> configuration);

  /**
   * Called to generate the actual schema content.
   *
   * @param generationState
   *          the generation state object
   */
  protected abstract void generateSchema(@NonNull S generationState);

  @Override
  public void generateFromModule(
      IModule metaschema,
      Writer out,
      IConfiguration<SchemaGenerationFeature<?>> configuration) {
    try {
      // avoid automatically closing streams not owned by the generator
      @SuppressWarnings({ "PMD.CloseResource", "resource" })
      T schemaWriter = newWriter(out);
      S generationState = newGenerationState(metaschema, schemaWriter, configuration);
      generateSchema(generationState);
      generationState.flushWriter();
    } catch (SchemaGenerationException ex) { // NOPMD avoid nesting same exception
      throw ex;
    } catch (Exception ex) { // NOPMD need to catch close exception
      throw new SchemaGenerationException(ex);
    }
  }

  /**
   * Determine the collection of root definitions.
   *
   * @param generationState
   *          the schema generation state used for context and writing
   * @param handler
   *          a callback to execute on each identified root definition
   * @return the list of identified root definitions
   */
  protected List<IAssemblyDefinition> analyzeDefinitions(
      @NonNull S generationState,
      @Nullable BiConsumer<ModuleIndex.DefinitionEntry, IDefinition> handler) {
    // TODO: use of handler here is confusing and introduces side effects. Consider
    // refactoring this in
    // the caller

    List<IAssemblyDefinition> rootAssemblyDefinitions = new LinkedList<>();
    for (ModuleIndex.DefinitionEntry entry : generationState.getMetaschemaIndex().getDefinitions()) {

      IDefinition definition = ObjectUtils.notNull(entry.getDefinition());
      if (definition instanceof IAssemblyDefinition && ((IAssemblyDefinition) definition).isRoot()) {
        // found root definition
        IAssemblyDefinition assemblyDefinition = (IAssemblyDefinition) definition;
        rootAssemblyDefinitions.add(assemblyDefinition);
      }

      boolean referenced = entry.isReferenced();
      if (!referenced) {
        // skip unreferenced definitions
        continue;
      }

      if (handler != null) {
        handler.accept(entry, definition);
      }
    }
    return rootAssemblyDefinitions;
  }

}
