/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.maven.plugin;

import gov.nist.secauto.metaschema.core.model.IModule;
import gov.nist.secauto.metaschema.core.model.MetaschemaException;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.codegen.JavaGenerator;
import gov.nist.secauto.metaschema.databind.codegen.config.DefaultBindingConfiguration;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Goal which generates Java source files for a given set of Metaschema modules.
 */
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateSourcesMojo
    extends AbstractMetaschemaMojo {
  private static final String STALE_FILE_NAME = "generateSourcesStaleFile";

  /**
   * A set of binding configurations.
   */
  @Parameter
  protected File[] configs;

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
   * Retrieve a list of binding configurations.
   *
   * @return the collection of binding configurations
   */
  protected List<File> getConfigs() {
    List<File> retval;
    if (configs == null) {
      retval = Collections.emptyList();
    } else {
      retval = Arrays.asList(configs);
    }
    return retval;
  }

  /**
   * Generate the Java source files for the provided Metaschemas.
   *
   * @param modules
   *          the collection of Metaschema modules to generate sources for
   * @throws MojoExecutionException
   *           if an error occurred while generating sources
   */
  protected void generate(@NonNull Set<IModule> modules) throws MojoExecutionException {
    DefaultBindingConfiguration bindingConfiguration = new DefaultBindingConfiguration();
    for (File config : getConfigs()) {
      try {
        if (getLog().isInfoEnabled()) {
          getLog().info("Loading binding configuration: " + config.getPath());
        }
        bindingConfiguration.load(config);
      } catch (IOException ex) {
        throw new MojoExecutionException(
            String.format("Unable to load binding configuration from '%s'.", config.getPath()), ex);
      }
    }

    try {
      if (getLog().isInfoEnabled()) {
        getLog().info("Generating Java classes in: " + getOutputDirectory().getPath());
      }
      JavaGenerator.generate(modules, ObjectUtils.notNull(getOutputDirectory().toPath()),
          bindingConfiguration);
    } catch (IOException ex) {
      throw new MojoExecutionException("Creation of Java classes failed.", ex);
    }
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  @Override
  public void execute() throws MojoExecutionException {
    File staleFile = getStaleFile();
    try {
      staleFile = ObjectUtils.notNull(staleFile.getCanonicalFile());
    } catch (IOException ex) {
      if (getLog().isWarnEnabled()) {
        getLog().warn("Unable to resolve canonical path to stale file. Treating it as not existing.", ex);
      }
    }

    boolean generate;
    if (shouldExecutionBeSkipped()) {
      if (getLog().isDebugEnabled()) {
        getLog().debug(String.format("Source file generation is configured to be skipped. Skipping."));
      }
      generate = false;
    } else if (staleFile.exists()) {
      generate = isGenerationRequired();
    } else {
      if (getLog().isInfoEnabled()) {
        getLog().info(String.format("Stale file '%s' doesn't exist! Generating source files.", staleFile.getPath()));
      }
      generate = true;
    }

    if (generate) {
      performGeneration();
      createStaleFile(staleFile);

      // for m2e
      getBuildContext().refresh(getOutputDirectory());
    }

    // add generated sources to Maven
    try {
      getMavenProject().addCompileSourceRoot(getOutputDirectory().getCanonicalFile().getPath());
    } catch (IOException ex) {
      throw new MojoExecutionException("Unable to add output directory to maven sources.", ex);
    }
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private void performGeneration() throws MojoExecutionException {
    File outputDir = getOutputDirectory();
    if (getLog().isDebugEnabled()) {
      getLog().debug(String.format("Using outputDirectory: %s", outputDir.getPath()));
    }

    if (!outputDir.exists() && !outputDir.mkdirs()) {
      throw new MojoExecutionException("Unable to create output directory: " + outputDir);
    }

    IBindingContext bindingContext;
    try {
      bindingContext = newBindingContext();
    } catch (MetaschemaException | IOException ex) {
      throw new MojoExecutionException("Failed to create the binding context", ex);
    }

    // generate Java sources based on provided metaschema sources
    Set<IModule> modules;
    try {
      modules = getModulesToGenerateFor(bindingContext);
    } catch (Exception ex) {
      throw new MojoExecutionException("Loading of metaschema modules failed", ex);
    }

    generate(modules);
  }
}
