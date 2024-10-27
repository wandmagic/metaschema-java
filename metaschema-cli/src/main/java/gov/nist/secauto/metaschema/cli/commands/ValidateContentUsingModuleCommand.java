/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.metaschema.cli.commands;

import gov.nist.secauto.metaschema.cli.processor.CLIProcessor.CallingContext;
import gov.nist.secauto.metaschema.cli.processor.command.ICommandExecutor;
import gov.nist.secauto.metaschema.core.configuration.DefaultConfiguration;
import gov.nist.secauto.metaschema.core.configuration.IMutableConfiguration;
import gov.nist.secauto.metaschema.core.model.IModule;
import gov.nist.secauto.metaschema.core.model.MetaschemaException;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet;
import gov.nist.secauto.metaschema.core.model.validation.JsonSchemaContentValidator;
import gov.nist.secauto.metaschema.core.model.validation.XmlSchemaContentValidator;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.IBindingContext.ISchemaValidationProvider;
import gov.nist.secauto.metaschema.schemagen.ISchemaGenerator;
import gov.nist.secauto.metaschema.schemagen.ISchemaGenerator.SchemaFormat;
import gov.nist.secauto.metaschema.schemagen.SchemaGenerationFeature;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.transform.stream.StreamSource;

import edu.umd.cs.findbugs.annotations.NonNull;

public class ValidateContentUsingModuleCommand
    extends AbstractValidateContentCommand {
  @NonNull
  private static final String COMMAND = "validate-content";

  @Override
  public String getName() {
    return COMMAND;
  }

  @Override
  public String getDescription() {
    return "Verify that the provided resource is well-formed and valid to the provided Module-based model.";
  }

  @Override
  public Collection<? extends Option> gatherOptions() {
    Collection<? extends Option> orig = super.gatherOptions();

    List<Option> retval = new ArrayList<>(orig.size() + 1);
    retval.addAll(orig);
    retval.add(MetaschemaCommands.METASCHEMA_OPTION);

    return CollectionUtil.unmodifiableCollection(retval);
  }

  @Override
  public ICommandExecutor newExecutor(CallingContext callingContext, CommandLine commandLine) {
    return new OscalCommandExecutor(callingContext, commandLine);
  }

  private final class OscalCommandExecutor
      extends AbstractValidationCommandExecutor {

    private OscalCommandExecutor(
        @NonNull CallingContext callingContext,
        @NonNull CommandLine commandLine) {
      super(callingContext, commandLine);
    }

    @Override
    protected IBindingContext getBindingContext(@NonNull Set<IConstraintSet> constraintSets)
        throws MetaschemaException, IOException {
      return MetaschemaCommands.newBindingContextWithDynamicCompilation(constraintSets);
    }

    @Override
    protected IModule getModule(
        CommandLine commandLine,
        IBindingContext bindingContext) throws IOException, MetaschemaException {

      URI cwd = ObjectUtils.notNull(Paths.get("").toAbsolutePath().toUri());

      IModule module;
      try {
        module = MetaschemaCommands.handleModule(commandLine, cwd, bindingContext);
      } catch (URISyntaxException ex) {
        throw new IOException(String.format("Cannot load module as '%s' is not a valid file or URL.", ex.getInput()),
            ex);
      }
      return module;
    }

    @Override
    protected ISchemaValidationProvider getSchemaValidationProvider(
        IModule module,
        CommandLine commandLine,
        IBindingContext bindingContext) {
      return new ModuleValidationProvider(module);
    }

  }

  private static final class ModuleValidationProvider implements ISchemaValidationProvider {
    @NonNull
    private final IModule module;

    public ModuleValidationProvider(@NonNull IModule module) {
      this.module = module;
    }

    @Override
    public XmlSchemaContentValidator getXmlSchemas(
        @NonNull URL targetResource,
        @NonNull IBindingContext bindingContext) throws IOException, SAXException {
      IMutableConfiguration<SchemaGenerationFeature<?>> configuration = new DefaultConfiguration<>();

      try (StringWriter writer = new StringWriter()) {
        ISchemaGenerator.generateSchema(module, writer, SchemaFormat.XML, configuration);
        try (Reader reader = new StringReader(writer.toString())) {
          return new XmlSchemaContentValidator(
              ObjectUtils.notNull(List.of(new StreamSource(reader))));
        }
      }
    }

    @Override
    public JsonSchemaContentValidator getJsonSchema(
        @NonNull JSONObject json,
        @NonNull IBindingContext bindingContext) throws IOException {
      IMutableConfiguration<SchemaGenerationFeature<?>> configuration = new DefaultConfiguration<>();

      try (StringWriter writer = new StringWriter()) {
        ISchemaGenerator.generateSchema(module, writer, SchemaFormat.JSON, configuration);
        return new JsonSchemaContentValidator(
            new JSONObject(new JSONTokener(writer.toString())));
      }
    }
  }
}
