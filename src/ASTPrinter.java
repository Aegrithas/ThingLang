package thing;

import java.lang.reflect.Modifier;

import static java.lang.System.out;
import static thing.AST.*;

class ASTPrinter {
  
  private int tab = 0;
  
  public void visit(AST ast) {
    var type = ast.getClass();
    out.println(type.getSimpleName());
    tab++;
    var first = true;
    for (var field : type.getDeclaredFields()) try {
      if (Modifier.isStatic(field.getModifiers())) continue;
      var fieldType = field.getType();
      if (Iterable.class.isAssignableFrom(fieldType)) {
        var i = 0;
        var name = field.getName();
        for (var value : (Iterable) field.get(ast)) {
          first = false;
          printTab();
          out.print(name);
          out.print('.');
          out.print(i++);
          out.print(": ");
          if (value instanceof AST) visit((AST) value);
          else if (value instanceof String) out.println('"' + (String) value + '"');
          else out.println(value);
        }
      } else {
        first = false;
        printTab();
        out.print(field.getName());
        out.print(": ");
        var value = field.get(ast);
        if (value instanceof AST) visit((AST) value);
        else if (value instanceof String) out.println('"' + (String) value + '"');
        else out.println(value);
      }
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      System.exit(1);
    }
    if (first) {
      printTab();
      out.println("none");
    }
    tab--;
  }
  
  private void printTab() {
    if (tab > 0) for (var i = tab; i --> 0;) out.print("| ");
  }
  
}