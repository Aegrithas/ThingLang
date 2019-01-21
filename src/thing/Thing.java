package thing;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.antlr.v4.runtime.*;

final class Thing {
  
  private Thing() {}
  
  public static void main(String[] args) {
    var files = new ArrayList<ThingParser.FileContext>();
    var errorCount = 0;
    if (args.length == 0) {
        System.err.println("expected input files");
        System.exit(1);
    }
    for (var file : args) try {
      var input = CharStreams.fromFileName(file);
      var lexer = new ThingLexer(input);
      var tokens = new CommonTokenStream(lexer);
      var parser = new ThingParser(tokens);
      files.add(parser.file());
      errorCount += parser.getNumberOfSyntaxErrors();
    } catch (IOException e) {
      System.err.println("Error reading file '" + file + "': " + e.getMessage());
      System.exit(1);
    }
    if (errorCount != 0) {
      System.err.println(errorCount + " errors");
      System.exit(1);
    }
    try {
      var translator = new ASTTranslator();
      var printer = new ASTPrinter();
      var evaluator = new ASTEvaluator();
      //for (var file : files) printer.visit(file.accept(translator));
      for (var file : files) file.accept(translator).accept(evaluator);
    } catch (AST.JumpNode.Exception e) {
      if (e.type == AST.JumpNode.Type.THROW) System.err.println("uncaught exception: " + e.value);
      else System.err.println("Exception: '" + e.type.name().toLowerCase() + "' outside of valid context");
      System.exit(1);
    } catch (SpreadException e) {
      System.err.println("Exception: 'spread' outside of valid context");
      System.exit(1);
    } catch (RuntimeException e) {
      var message = e.getMessage();
      System.err.println(e.getClass().getSimpleName() + (message == null ? "" : ": " + message));
      System.exit(1);
    }
  }
  
}
