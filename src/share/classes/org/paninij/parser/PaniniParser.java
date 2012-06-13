package org.paninij.parser;
import com.sun.tools.javac.parser.*;

public class PaniniParser extends JavacParser {
 protected PaniniParser(ParserFactory fac,
   Lexer S,
   boolean keepDocComments,
   boolean keepLineMap,
   boolean keepEndPositions) {
 	 super(fac,S, keepDocComments, keepLineMap, keepEndPositions);
 }
 
 
}
