
package gov.nist.secauto.metaschema.docsgen.explode;

import static org.junit.jupiter.api.Assertions.fail;

import gov.nist.secauto.metaschema.model.MetaschemaLoader;
import gov.nist.secauto.metaschema.model.common.IMetaschema;
import gov.nist.secauto.metaschema.model.common.MetaschemaException;
import gov.nist.secauto.metaschema.model.common.constraint.IAllowedValue;
import gov.nist.secauto.metaschema.model.common.constraint.IAllowedValuesConstraint;
import gov.nist.secauto.metaschema.model.common.metapath.DynamicContext;
import gov.nist.secauto.metaschema.model.common.metapath.StaticContext;
import gov.nist.secauto.metaschema.model.common.metapath.item.ICycledAssemblyNodeItem;
import gov.nist.secauto.metaschema.model.common.metapath.item.IDefinitionNodeItem;
import gov.nist.secauto.metaschema.model.common.metapath.item.INodeItemFactory;
import gov.nist.secauto.metaschema.model.common.util.CustomCollectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

class ExplosionVisitorTest {

  @Test
  void test() throws MetaschemaException, IOException {
    MetaschemaLoader loader = new MetaschemaLoader();
    IMetaschema metaschema = loader.load(URI.create(
        "https://raw.githubusercontent.com/usnistgov/OSCAL/v1.0.4/src/metaschema/oscal_complete_metaschema.xml"));

    ExplosionVisitor visitor = new ExplosionVisitor();

    INodeItemFactory factory = INodeItemFactory.instance();

    DynamicContext dynamicContext = new StaticContext()
        .newDynamicContext()
        .disablePredicateEvaluation();

    List<@NotNull ? extends IAssemblyModelElement> rootAssemblies = metaschema.getExportedAssemblyDefinitions().stream()
        .filter(modelItem -> modelItem.isRoot())
        .map(root -> factory.newAssemblyNodeItem(root, metaschema.getLocation()))
        .map(rootItem -> (IAssemblyModelElement) visitor.visit(rootItem, dynamicContext))
        .collect(Collectors.toUnmodifiableList());
    
    Visitor outputVisitor = new Visitor();
    rootAssemblies.forEach(root -> outputVisitor.visitAssembly(root, 0));
    fail("Not yet implemented");
  }

  private class Visitor implements IModelElementVisitor<Void, @NotNull Integer> {
    void visitFlags(@NotNull IModelElement element, Integer depth) {
      int newDepth = ++depth;
      for (IModelElement flag : element.getFlags()) {
        flag.accept(this, newDepth);
      }
    }

    void visitModelItems(@NotNull IModelElement element, Integer depth) {
      int newDepth = ++depth;
      for (IModelElement flag : element.getModelItems()) {
        flag.accept(this, newDepth);
      }
    }

    @Override
    public Void visitAssembly(@NotNull IAssemblyModelElement element, Integer depth) {
      outputNode("assembly", element, depth);
      visitFlags(element, depth);
      visitModelItems(element, depth);
      return null;
    }

    @Override
    public Void visitField(@NotNull IFieldModelElement element, Integer depth) {
      outputNode("field", element, depth);
      visitFlags(element, depth);
      return null;
    }

    @Override
    public Void visitFlag(@NotNull IFlagModelElement element, Integer depth) {
      outputNode("flag", element, depth);
      return null;
    }

    private void outputNode(@NotNull String type, @NotNull IModelElement element, @NotNull Integer depth) {
      IDefinitionNodeItem nodeItem = element.getNodeItem();
      StringBuffer buffer = new StringBuffer();
      buffer
          .append("  ".repeat(depth))
          .append(type);
      
      if (element.getNodeItem() instanceof ICycledAssemblyNodeItem) {
        buffer.append(" (cycle}");
      }

      buffer
          .append(": ")
          .append(nodeItem.getName());
//          .append(' ')
//          .append(Objects.hashCode(nodeItem.getInstance()))
//          .append(' ')
//          .append(Objects.hashCode(nodeItem.getDefinition()));
      
      String values = element.getConstraints().stream()
          .filter(constraint -> (constraint instanceof IAllowedValuesConstraint))
          .map(constraint -> (IAllowedValuesConstraint)constraint)
          .flatMap(constraint -> constraint.getAllowedValues().values().stream())
          .map(IAllowedValue::getValue)
          .sorted()
          .distinct()
          .collect(CustomCollectors.joiningWithOxfordComma("or"));
      if (!values.isBlank()) {
        buffer
          .append(" values: ")
          .append(values);
      }
      System.out.println(buffer);
    }

  }
}
