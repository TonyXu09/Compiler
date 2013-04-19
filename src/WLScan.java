import java.util.*;
import java.math.*;
import java.io.*;


/** A sample main class demonstrating the use of the Lexer.
 *  This main class just outputs each line in the input, followed by
 *  the tokens returned by the lexer for that line.
 *
 *  If a filename (or path) is supplied as the first argument, eg
 *       java Asm   src/sumOneToFive.asm
 *  this program will read from that file; if no argument is present,
 *  this program will read from standard input, whether typed directly
 *  on the keyboard or redirected at the command line from a file, as by
 *       java Asm < src/sumOneToFive.asm
 *
 *  Requires Java version 1.5
 *
 *  Minor modifications by JCBeatty, Jan 2009.
 *
 *  A very quick summary of Java language features used in this program that
 *  few graduates of CS 134 will have encountered follow.
 *
 *  (1) The definition of multiple classes in a single file. This is generally a bad idea:
 *      (a) many java tools, such as Sun's java compiler javac, locate the source for a
 *      class by looking for a file whose name matches the class name; (b) it is typically
 *      more difficulty to work with a program, especially a large program, that's defined
 *      in a single file. It is convenient for CS 241, however, because electronic submission
 *      of a program requires submitting only a single file.
 *
 *  (2) "enums" - that is, "enumerated types". In CS 134, you learned to create symbolic constants
 *      by using statements such as "static final int meaningOfTheUniverse = 42". Roughly speaking,
 *      enums are a way of asking the java compiler to create a set of such symbolic constants
 *      having distinct values. The enumerated type Kind defined in this file is a good example.
 *      However, enums are actually a special kind of class and can have constructors and methods.
 *      The enumerated type State defined much later in this file illustrates this.
 *
 *  (3) "Parametric types." This is a huge topic; Java's implementation has many warts and
 *      confusing corner cases. However, their only appearance here is in the declaration of an
 *      ArrayList of Tokens in the scan method below. [The Java class ArrayList (actually
 *      java.util.ArrayList) is Sun's version of the CS 134 ListArray class; the "List" you
 *      see here is also imported from java.util, and is analogous to (but not the same as)
 *      the List interface defined in CS 134.] So 
 *          List<Token> ret = new ArrayList<Token>();
 *      allows the compiler to take care of ensuring that you only put Tokens into ret,
 *      and to automatically cast objects you retrieve from ret into Tokens so that you don't
 *      have to.
 *
 *  (4) Nested classes and interfaces. There are examples of both below, in the definition of the
 *      class Lexer: State, Chars, AllChars and Transition are nexted class definitions; and Chars
 *      is a nested interface definition. Defining these *inside* the definition of Lexer means that
 *      they can only be used by code within Lexer, which is arguably good design if they're not
 *      *intended* to be used elsewhere.
 *
 *  (5) System.exit(0) and System.exit(1). Not surprisingly, calls to System.exit(...) cause the
 *      program to cease execution. When you run a program from a command line shell, the integer
 *      value returned by such an exit is made available to the shell. By convention, unix programs
 *      that quit normally return 0; programs that quit because they have encountered a fatal error
 *      return a non-zero value - often a non-zero error code specifying more-or-less precisely :-)
 *      exactly what error was encountered.
 *
 *  Regarding note (1): if you decide you'd REALLY prefer that each class live in its own file,
 *  it's straightforward to write a simple shell script to merge those files into a single file
 *  for submission, although care must be taken to eliminate multiple identical import statements,
 *  and Asm may be the only public class (as is already true in this file.) For an example of how
 *  to do this, see
 *                   http://jcbServer.cs.uwaterloo.ca/cs241/code/a03/merge.bash
**/
public class WLScan {

    // Execution starts here when the program is run from the command line by typing one of...
    //     java Asm < something.asm > something.mips
    //     java Asm   something.asm > something.mips
    public static final void main( String[] args ) {
        // Args contains the sequence of blank-delimited tokens supplied after the name of the class
        // containing main when a java program is executed from the command line.
        if( args.length == 0 )
            new WLScan().run( System.in );                               // System.in is an InputStream
        else {
            WLScan.exe( args[0] );
        }
    }

    // Called either from main(...) or from JUnit test_...(...) methods in TestCase subclasses.
    public static String exe( String inputFilePath ) {
        try {
            FileInputStream inStream = new FileInputStream( inputFilePath );
            return new WLScan().run( inStream );
        } catch( FileNotFoundException e ) {
            throw new Error( "Could not open file \"" + inputFilePath + "\" for reading." );
        }
    }

    // input should be either System.in or a FileInputStream attached to an input file (something.asm).
    private String run( InputStream input ) {

        Lexer   lexer = new Lexer();
        Scanner in    = new Scanner( input );

        while( in.hasNextLine() ) {
            
            String line = in.nextLine();

            // Scan the line into an array of tokens.
            Token[] tokens;
            tokens = lexer.scan( line );

            // Print the input line, followed by the tokens produced for it by the scanner.
            //System.err.println( line );
            for( int i = 0; i < tokens.length; i++ ) {
                System.out.println( tokens[i] );
            }
        }

        System.out.flush();

        // Main ignores the value returned, but the "OK" is useful if you decide to to JUnit testing;
        // run should return either a string containing "ERROR" or a string containing "OK", depending
        // on whether or not your assembler finds an error in the file it's assembly. Of course, that
        // leaves open the question of whether the MIPS code generated for a program w/o syntax errors
        // is semantically correct. You can automate testing that, too, but it takes more work since
        // you have to run the resulting *.mips file via java cs241.twoints and check its output...
        return( "OK" );
    }
}

/** The various kinds of tokens (ie values of Token.kind). */
enum Kind {
	ID,				// a string consisting of a letter (in the range a-z or A-Z) followed by zero or more letters and digits (in the range 0-9), but not equal to "wain", "int", "if", "else", "while", "println" or "return"
	NUM,			// a string consisting of a single digit (in the range 0-9) or two or more digits the first of which is not 0
	LPAREN,			// the string "("
	RPAREN,			// the string ")"
	LBRACE,			// the string "{"
	RBRACE,			// the string "}"
	RETURN,			// the string "return" (in lower case)
	IF,				// the string "if"
	ELSE,			// the string "else"
	WHILE,			// the string "while"
	PRINTLN, 		//the string "println"
	BECOMES, 		//the string "="
	WAIN, 			//the string "wain"
	INT, 			//the string "int"
	EQ, 			//the string "=="
	NE, 			//the string "!="
	LT, 			//the string "<"
	GT, 			//the string ">"
	LE, 			//the string "<="
	GE, 			//the string ">="
	PLUS, 			//the string "+"
	MINUS, 			//the string "-"
	STAR, 			//the string "*"
	SLASH, 			//the string "/"
	PCT, 			//the string "%"
	COMMA, 			//the string ","
	SEMI, 			//the string ";"
	SR,
	SRE,
	SRET,
	SRETU,
	SRETUR,
	SI,
	SIN,
	SE,
	SEL,
	SELS,
	SP,
	SPR,
	SPRI,
	SPRIN,
	SPRINT,
	SPRINTL,
	SW,
	SWA,
	SWAI,
	SWH,
	SWHI,
	SWHIL,
    WHITESPACE;     // Whitespace
}



/** The representation of a token. */
class Token {
    
    public Kind   kind;   // The kind of token.
    public String lexeme; // String representation of the actual token in the source code.

    public Token( Kind kind, String lexeme ) {
        this.kind   = kind;
        this.lexeme = lexeme;
    }

    public String toString() {
        return kind + " " + lexeme;
    }

    /** Returns an integer representation of the token. For tokens of kind
     *  INT (decimal integer constant) and HEXINT (hexadecimal integer
     *  constant), returns the integer constant. For tokens of kind
     *  REGISTER, returns the register number.
     */
    public int toInt() {
        if(      kind == Kind.INT      ) return parseLiteral( lexeme,              10, 32 );
        else {
            System.err.println( "ERROR in to-int conversion." );
            System.exit(1);
            return 0;
        }
    }
    
    private int parseLiteral( String s, int base, int bits ) {
        BigInteger x = new BigInteger( s, base );
        if( x.signum() > 0 ) {
            if( x.bitLength() > bits ) {
                System.err.println( "ERROR in parsing: constant out of range: " + s );
                System.exit(1);
            }
        } else if( x.signum() < 0 ) {
            if( x.negate().bitLength() > bits-1
                    && x.negate().subtract(new BigInteger("1")).bitLength() > bits-1 ) {
                System.err.println( "ERROR in parsing: constant out of range: " + s );
                System.exit(1);
            }
        }
        return (int) (x.longValue() & ((1L << bits) - 1));
    }
}

// Lexer -- implements a DFA that partitions an input line into a list of tokens.
// DFAs will be discussed Lectures 10, 11 and 12 and Assignment 5.
class Lexer {

    public Lexer() {
        
        CharSet whitespace    = new Chars( "\t\n\r " );
        CharSet letters       = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"           );
        CharSet lettersDigits = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" );
        CharSet digits        = new Chars( "0123456789"                                                     );
        CharSet oneToNine     = new Chars( "123456789"                                                      );
        CharSet all           = new AllChars();
        CharSet	semi		  = new Chars( ";");
        CharSet comma		  = new Chars( ",");
        CharSet	pct			  = new Chars( "%");
        CharSet slash		  = new Chars( "/");
        CharSet star		  = new Chars( "*");
        CharSet minus		  = new Chars( "-");
        CharSet plus		  = new Chars( "+");
        CharSet lessThan	  = new Chars( "<");
        CharSet greaterThan   = new Chars( ">");
        CharSet equals		  = new Chars( "=");
        CharSet lparen		  = new Chars( "(");
        CharSet rparen		  = new Chars( ")");
        CharSet lbrace		  = new Chars( "{");
        CharSet rbrace		  = new Chars( "}");
        CharSet exec          = new Chars( "!");
        CharSet zero          = new Chars( "0");
        CharSet noEIPRW		  = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdfghjklmnoqstuvxyz0123456789" );
        CharSet noE			  = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdfghijklmnopqrstuvwxyz0123456789" );
        CharSet noT           = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrsuvwxyz0123456789" );
        CharSet noU           = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstvwxyz0123456789" );
        CharSet noR			  = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqstuvwxyz0123456789" );
        CharSet noN			  = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmopqrstuvwxyz0123456789" );
        CharSet noF			  = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdeghijklmnopqrstuvwxyz0123456789" );
        CharSet noL           = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz0123456789" );
        CharSet noS           = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrtuvwxyz0123456789" );
        CharSet noI           = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghjklmnopqrstuvwxyz0123456789" );
        CharSet noA           = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZbcdefghijklmnopqrstuvwxyz0123456789" );
        CharSet noH           = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefgijklmnopqrstuvwxyz0123456789" );
        CharSet singleSlash   = new Chars( "/");
        
        
        /** The handling of whitespace is tricky. There are two things you should figure out:
         *  (a) how and why all of the characters following // are swallowed up w/o returning a token;
         *  (b) how the appearance of one or more whitespace characters causes this Lexer to cease
         *      building up an ID or keyword, which is then appended to the list of tokens found in
         *      the line, and start scanning for another token.      
        **/

        table = new Transition[] {
                new Transition( State.START,    whitespace,     State.WHITESPACE ),
                new Transition( State.START,	rbrace,			State.RBRACE	 ),
                new Transition( State.START,	lbrace,			State.LBRACE	 ),	
                new Transition( State.START,	lparen,			State.LPAREN	 ),	
                new Transition( State.START,	rparen,			State.RPAREN	 ),	
                new Transition( State.START,    equals,         State.BECOMES	 ),
                new Transition( State.START,    greaterThan,    State.GT		 ),
                new Transition( State.START,    lessThan,       State.LT         ),
                new Transition( State.START,    plus,           State.PLUS       ),
                new Transition( State.START, 	minus, 			State.MINUS      ),
                new Transition( State.START,    star,           State.STAR       ),
                new Transition( State.START,    slash,          State.SLASH      ),
                new Transition( State.START,    pct,            State.PCT        ),
                new Transition( State.START,    comma,          State.COMMA      ),
                new Transition( State.START,    semi,           State.SEMI       ),
                new Transition( State.START,    exec,           State.EXEC       ),
                new Transition( State.EXEC,     equals,         State.NE 		 ),
                new Transition( State.LT,       equals,         State.LE         ),
                new Transition( State.GT,       equals,         State.GE         ),
                new Transition( State.BECOMES,  equals,         State.EQ         ),
                new Transition( State.START,    oneToNine,      State.NUM        ),
                new Transition( State.START,    noEIPRW,        State.ID         ),
                new Transition( State.START,    zero, 		    State.ZERO       ),
                new Transition( State.NUM, 	    digits,         State.NUM        ),
                new Transition( State.ID,       lettersDigits,  State.ID         ),
                new Transition( State.START,    new Chars("r"), State.SR         ),
                new Transition( State.SR,   	new Chars("e"), State.SRE        ),
                new Transition( State.SRE,   	new Chars("t"), State.SRET       ),
                new Transition( State.SRET,   	new Chars("u"), State.SRETU      ),
                new Transition( State.SRETU,    new Chars("r"), State.SRETUR     ),
                new Transition( State.SRETUR,   new Chars("n"), State.RETURN     ),
                new Transition( State.START,    new Chars("i"), State.SI         ),
                new Transition( State.SI,       new Chars("f"), State.IF         ),
                new Transition( State.SI,       new Chars("n"), State.SIN        ),
                new Transition( State.SIN,      new Chars("t"), State.INT        ),
                new Transition( State.START,    new Chars("e"), State.SE         ),
                new Transition( State.SE,       new Chars("l"), State.SEL        ),
                new Transition( State.SEL,      new Chars("s"), State.SELS       ),
                new Transition( State.SELS,     new Chars("e"), State.ELSE       ),
                new Transition( State.START,    new Chars("p"), State.SP         ),
                new Transition( State.SP,       new Chars("r"), State.SPR        ),
                new Transition( State.SPR,      new Chars("i"), State.SPRI       ),
                new Transition( State.SPRI,     new Chars("n"), State.SPRIN      ),
                new Transition( State.SPRIN,    new Chars("t"), State.SPRINT     ),
                new Transition( State.SPRINT,   new Chars("l"), State.SPRINTL    ),
                new Transition( State.SPRINTL,  new Chars("n"), State.PRINTLN    ),
                new Transition( State.START,    new Chars("w"), State.SW         ),
                new Transition( State.SW,       new Chars("a"), State.SWA        ),
                new Transition( State.SWA,      new Chars("i"), State.SWAI       ),
                new Transition( State.SWAI,     new Chars("n"), State.WAIN       ),
                new Transition( State.SW,       new Chars("h"), State.SWH        ),
                new Transition( State.SWH,      new Chars("i"), State.SWHI       ),
                new Transition( State.SWHI,     new Chars("l"), State.SWHIL      ),
                new Transition( State.SWHIL,    new Chars("e"), State.WHILE      ),
                new Transition( State.START,    singleSlash,    State.SLASH      ),
                new Transition( State.SLASH,    singleSlash,    State.COMMENT    ),
                new Transition( State.COMMENT,  all,            State.COMMENT    ),
                new Transition( State.SR,       noE,            State.ID         ),
                new Transition( State.SRE,      noT,            State.ID         ),
                new Transition( State.SRET,     noU,            State.ID         ),
                new Transition( State.SRETU,    noR,            State.ID         ),
                new Transition( State.SRETUR,   noN,            State.ID         ),
                new Transition( State.SI,       noF,            State.ID         ),
                new Transition( State.SI,       noN,            State.ID         ),
                new Transition( State.SIN,      noT,            State.ID         ),
                new Transition( State.SE,       noL,            State.ID         ),
                new Transition( State.SEL,      noS,            State.ID         ),
                new Transition( State.SELS,     noE,            State.ID         ),
                new Transition( State.SP,       noR,            State.ID         ),
                new Transition( State.SPR,      noI,            State.ID         ),
                new Transition( State.SPRI,     noN,            State.ID         ),
        };
    }

    /** Partitions the line passed in as input into an array of tokens.
     *  The array of tokens is returned.
     */
    public Token[] scan( String input ) {

        List<Token> ret = new ArrayList<Token>();

        if( input.length() == 0 ) return new Token[0];
        int   i          = 0;
        int   startIndex = 0;
        State state      = State.START;

        while( true ) {

            Transition trans = null;

            if( i < input.length() ) trans = findTransition( state, input.charAt(i) );
            
            if( trans == null ) {
                // No more transitions possible
                if( ! state.isFinal() ) {
                    System.err.println( "ERROR in lexing after reading " + input.substring(0,i) );
                    System.exit(1);
                }
                if( state.kind != Kind.WHITESPACE ) {
                    ret.add( new Token(state.kind,input.substring(startIndex,i)) );
                }
                startIndex = i;
                state      = State.START;
                if( i >= input.length() ) break;
            } else {
                state      = trans.toState;
                i++;
            }
        }
        
        return ret.toArray( new Token[ret.size()] );
    }

    ///////////////////////////////////////////////////////////////
    // END OF PUBLIC METHODS
    ///////////////////////////////////////////////////////////////

    private Transition findTransition( State state, char c ) {
        for( int j = 0; j < table.length; j++ ) {
            Transition trans = table[j];
            if( trans.fromState == state && trans.chars.contains(c) ) {
                return trans;
            }
        }
        return null;
    }

    // Final states or those whose kind (of token) is not null, except for WHITESPACE (a special case).
    private static enum State {
    	ID(	Kind.ID	),
    	NUM( Kind.NUM ),
    	LPAREN( Kind.LPAREN ),
    	RPAREN( Kind.RPAREN ),
    	LBRACE( Kind.LBRACE ),
    	RBRACE( Kind.RBRACE ),
    	RETURN( Kind.RETURN ),
    	IF	  ( Kind.IF ),
    	ELSE  ( Kind.ELSE ),
    	WHILE ( Kind.WHILE ),
    	PRINTLN ( Kind.PRINTLN ),
    	BECOMES ( Kind.BECOMES ),
    	WAIN ( Kind.WAIN ),
    	INT ( Kind.INT ),
    	EQ( Kind.EQ ),
    	NE( Kind.NE),
    	LT( Kind.LT),
    	GT( Kind.GT),
    	LE( Kind.LE),
    	GE( Kind.GE),
    	PLUS( Kind.PLUS),
    	MINUS( Kind.MINUS),
    	STAR( Kind.STAR),
    	SLASH( Kind.SLASH),
    	PCT( Kind.PCT),
    	COMMA( Kind.COMMA),
    	SEMI( Kind.SEMI),
    	START ( null ),	//not final
    	EXEC ( null),	// not final
    	SR( Kind.ID),
    	SRE( Kind.ID),
    	SRET( Kind.ID),
    	SRETU( Kind.ID),
    	SRETUR( Kind.ID),
    	SI( Kind.ID),
    	SIN( Kind.ID),
    	SE( Kind.ID),
    	SEL( Kind.ID),
    	SELS( Kind.ID),
    	SP( Kind.ID),
    	SPR( Kind.ID),
    	SPRI( Kind.ID),
    	SPRIN( Kind.ID),
    	SPRINT( Kind.ID),
    	SPRINTL( Kind.ID),
    	SW( Kind.ID),
    	SWA( Kind.ID),
    	SWAI( Kind.ID),
    	SWH( Kind.ID),
    	SWHI( Kind.ID),
    	SWHIL( Kind.ID),	
    	ZERO ( Kind.ID),
    	COMMENT( Kind.WHITESPACE ),
        WHITESPACE( Kind.WHITESPACE );

        Kind kind;

        State( Kind kind ) {
            this.kind = kind;
        }

        boolean isFinal() {
            return kind != null;
        }
    }

    private interface CharSet {
        public boolean contains( char newC );
    }

    private class Chars implements CharSet {
        private String chars;
        public  Chars( String chars ) { this.chars = chars; }
        public  boolean contains( char newC ) {
            return chars.indexOf(newC) >= 0;
        }
    }

    private class AllChars implements CharSet {
        public boolean contains( char newC ) {
            return true;
        }
    }

    private class Transition {
        State   fromState;
        CharSet chars;
        State   toState;
        Transition( State fromState, CharSet chars, State toState ) {
            this.fromState = fromState;
            this.chars     = chars;
            this.toState   = toState;
        }
    }
    
    private Transition[] table;
}

