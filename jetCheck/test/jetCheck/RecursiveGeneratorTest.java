package jetCheck;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author peter
 */
public class RecursiveGeneratorTest extends PropertyCheckerTestCase {
  private static final Generator<Leaf> leaves = Generator.asciiLetters().map(Leaf::new);

  private void checkShrinksToLeaf(Generator<Node> nodes) {
    PropertyFailure<?> failure = checkFails(forAllStable(nodes), tree -> !tree.toString().contains("a")).getFailure();
    assertTrue(failure.getMinimalCounterexample().getExampleValue() instanceof Leaf);
  }

  public void testShrinksToLeafWithFrequency() {
    checkShrinksToLeaf(Generator.recursive(nodes -> Generator.frequency(2, leaves,
                                                                        1, Generator.listsOf(nodes).map(Composite::new))));
  }

  public void testShrinksToLeafWithAnyOf() {
    checkShrinksToLeaf(Generator.recursive(nodes -> Generator.anyOf(leaves,
                                                                    Generator.listsOf(nodes).map(Composite::new))));
  }

  public void testShrinksToLeafDespiteWrapping() {
    checkShrinksToLeaf(Generator.recursive(nodes -> Generator.frequency(2, leaves,
                                                                        1, Generator.from(data -> Generator.listsOf(nodes).map(Composite::new).generateValue(data)))));
  }
  
  private interface Node {}
  
  private static class Leaf implements Node {
    final char c;
    Leaf(char c) { this.c = c;}
    public String toString() { return String.valueOf(c);}
  }
  
  private static class Composite implements Node {
    final List<Node> children;
    Composite(List<Node> children) { this.children = children;}
    public String toString() { return "[" + children.stream().map(Object::toString).collect(Collectors.joining("")) + "]";}
  }
}
