import java.util.*;

/**
 * Starter code for CS241 assignments 9 and 10 for Fall 2008.
 * 
 * Based on Scheme code by Gord Cormack. Java translation by Ondrej Lhotak.
 * 
 * Version 20081105.1
 *
 */
public class WLGen {
    Scanner in = new Scanner(System.in);

    // The set of terminal symbols in the WL grammar.
    Set<String> terminals = new HashSet<String>(Arrays.asList("BOF", "BECOMES", 
         "COMMA", "ELSE", "EOF", "EQ", "GE", "GT", "ID", "IF", "INT", "LBRACE", 
         "LE", "LPAREN", "LT", "MINUS", "NE", "NUM", "PCT", "PLUS", "PRINTLN",
         "RBRACE", "RETURN", "RPAREN", "SEMI", "SLASH", "STAR", "WAIN", "WHILE"));

    List<String> symbols;
    Map<String, Integer> symbolTable = new HashMap<String, Integer>();
    int symbolCounter = 0;
    int numWhileCounter = 0;
    int numIfCounter = 0;

    // Data structure for storing the parse tree.
    public class Tree {
        List<String> rule;

        ArrayList<Tree> children = new ArrayList<Tree>();

        // Does this node's rule match otherRule?
        boolean matches(String otherRule) {
            return tokenize(otherRule).equals(rule);
        }
    }

    // Divide a string into a list of tokens.
    List<String> tokenize(String line) {
        List<String> ret = new ArrayList<String>();
        Scanner sc = new Scanner(line);
        while (sc.hasNext()) {
            ret.add(sc.next());
        }
        return ret;
    }

    
    // Read and return wli parse tree
    Tree readParse(String lhs) {
        String line = in.nextLine();
        List<String> tokens = tokenize(line);
        Tree ret = new Tree();
        ret.rule = tokens;
        if (!terminals.contains(lhs)) {
            Scanner sc = new Scanner(line);
            sc.next(); // discard lhs
            while (sc.hasNext()) {
                String s = sc.next();
                ret.children.add(readParse(s));
            }
        }
        return ret;
    }

    // Compute symbols defined in t
    List<String> genSymbols(Tree t) {
        if (t.matches("S BOF procedure EOF")) {
            // recurse on procedure
            return genSymbols(t.children.get(1));
        } else if (t.matches("procedure INT WAIN LPAREN dcl COMMA dcl RPAREN LBRACE dcls statements RETURN expr SEMI RBRACE")) {
            List<String> ret = new ArrayList<String>();
            // recurse on dcl and dcl
            ret.addAll(genSymbols(t.children.get(3)));
            ret.addAll(genSymbols(t.children.get(5)));
            ret.addAll(genSymbols(t.children.get(8)));
            return ret;
        } else if (t.matches("dcls dcls dcl BECOMES NUM SEMI")){
        	List<String> ret = new ArrayList<String>();
        	ret.addAll(genSymbols(t.children.get(0)));
        	ret.addAll(genSymbols(t.children.get(1)));
        	return ret;
        } else if (t.matches("dcls")){
        	List<String> ret = new ArrayList<String>();
        	return ret;
        }
        else if (t.matches("dcl INT ID")) {
            // recurse on ID
            return genSymbols(t.children.get(1));
        } else if (t.rule.get(0).equals("ID")) {
            List<String> ret = new ArrayList<String>();
            ret.add(t.rule.get(1));
            if (!symbolTable.containsKey(t.rule.get(1))){
            	symbolCounter+=4;
            	symbolTable.put(t.rule.get(1), symbolCounter);
            } else {
            	bail ( "ID " + t.rule.get(1) + " is already declared");
            }
            return ret;
        } else {
            bail("unrecognized rule " + t.rule);
            return null;
        }
    }
    
    // Print an error message and exit the program.
    void bail(String msg) {
        System.err.println("ERROR: " + msg);
        System.exit(0);
    }
    
    //gen println procedure
    String genPrintln(){
    	String ret ="";
    	ret += "printLine:\n";
    	ret += "add $6, $3, $0\n"; // $6 is the value to be printed 
    	ret += "slt $7, $3, $0\n"; //$7 = 1 iff $1 < 0, else $7 = 0
        ret += "beq $7, $0, IfDone\n";
        ret += "lis $5\n";
        ret += ".word 0x0000002d\n"; // put minus sign in $5
        ret += "sw $5, 0($28)\n"; // print negative sign
        ret += "sub $6, $0, $6\n"; //$6 = 0 - $6
        ret += "IfDone:\n";
        ret += "add $20, $30, $0\n"; //$20 = $30
        
        // calculate all the digits and save them onto the stack
        // they are calculated in reverse order.  Popping them off the stack
        // to print will put them in the forward order.
        ret += "Loop:\n";
        ret += "divu $6, $10\n"; // $6 / 10
        ret += "mfhi $15\n"; // move remainder to $15
        ret += "sw $15, -4($20)\n"; //mem[$20] = $10
        ret += "mflo $6\n"; // move quotient to $6
        ret += "sub $20, $20, $4\n";
        ret += "bne $6, $0, Loop\n"; // if $6 is not 0, loop back

        
        // use second Loop to print the digits in the right order
        ret += "Loop2:\n";
        ret += "lw $15, 0($20)\n"; //$8 = mem[$20]
        ret += "add $15, $15, $16\n"; //calculate the ascii value of the digit
        ret += "sw $15, 0($28)\n"; //print the character in $15
        ret += "add $20, $20, $4\n"; //$20 = $20 + 4
        ret += "bne $20, $30, Loop2\n"; //jump the loop
        ret += "sw $10, 0($28)\n"; // print char new line
        ret += "jr$31\n";

        return ret;
    }
    
    String genDeclaration( String name, String num){
    	String ret = "";
    	int loc = symbolTable.get(name);
    	ret += "lis $3\n";
    	ret += ".word " + num + "\n";
        ret += "sw $3, " + loc + "($29)\n";
    	return ret;
    }
    
    String genInitilization( ){
    	String ret = "";
    	ret += "sw $1, 4($29)\n";
    	ret += "sw $2, 8($29)\n";
    	return ret;
    }

    String genPush(){
    	String ret = "";
    	ret += "push:\n";
    	ret += "sw $3, -4($30)\n";
    	ret += "sub $30, $30, $4\n";
    	ret += "jr $31\n";
    	return ret;
    }
    
    // generate for add
    String genAdd(){
    	String ret = "";
    	ret += "add:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "add $3, $6, $5\n";
    	ret += "sw $3, -4($30)\n";
    	ret += "sub $30, $30, $4\n";
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for sub
    String genSub(){
    	String ret = "";
    	ret += "sub:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "sub $3, $6, $5\n";
    	ret += "sw $3, -4($30)\n";
    	ret += "sub $30, $30, $4\n";
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for star
    String genStar(){
    	String ret = "";
    	ret += "star:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "mult $5, $6\n";
    	ret += "mflo $3\n";
    	ret += "sw $3, -4($30)\n";
    	ret += "sub $30, $30, $4\n";
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for slash
    String genSlash(){
    	String ret = "";
    	ret += "slash:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "div $6, $5\n";
    	ret += "mflo $3\n";
    	ret += "sw $3, -4($30)\n";
    	ret += "sub $30, $30, $4\n";
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for pct
    String genPct(){
    	String ret = "";
    	ret += "pct:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "div $6, $5\n";
    	ret += "mfhi $3\n";
    	ret += "sw $3, -4($30)\n";
    	ret += "sub $30, $30, $4\n";
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for LT
    String genLT(){
    	String ret = "";
    	ret += "LT:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "slt $3, $6, $5\n"; // if a is less than b, then $22 is 1, branch if 0
    	ret += "jr $31\n";
    	return ret;
    }
    
    String genEQ(){
    	String ret = "";
    	ret += "EQ:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "sub $5, $5, $6\n"; // if they are equal, then $3 is 0, branch when not equal
    	ret += "add $3, $0, $0\n"; //set register 3 as 0, assume $5 is not 0
    	ret += "bne $0, $5, 1\n"; //if $5 is not 0, skip next line
    	ret += "add $3, $0, $11\n"; //set register 3 to 1
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for NE
    String genNE(){
    	String ret = "";
    	ret += "NE:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "sub $3, $5, $6\n"; // if they are equal, then $3 is 0, branch when equal
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for LE
    String genLE(){
    	String ret = "";
    	ret += "LE:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "slt $7, $5, $6\n"; // if b is less than a, then $7 is 1
    	ret += "sub $3, $7, $11\n"; // move $7 - 1 to $3
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for GE
    String genGE(){
    	String ret = "";
    	ret += "GE:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "slt $7, $6, $5\n"; // if a is less than b, then $7 is 1, branch when 1
    	ret += "sub $3, $7, $11\n"; // move $7 - 1 to $3
    	ret += "jr $31\n";
    	return ret;
    }
    
    //generate for GT
    String genGT(){
    	String ret = "";
    	ret += "GT:\n";
    	ret += "lw $5, 0($30)\n"; //$5 contains b
    	ret += "lw $6, 4($30)\n"; //$6 contains a
    	ret += "add $30, $30, $8\n"; //move stack pointer
    	ret += "slt $3, $5, $6\n"; // if b is less than a, then $7 is 1
    	ret += "jr $31\n";
    	return ret;
    }
    
    /* Generate the code for the parse tree t.
     * $4 is always 4
     * $11 is always 1
     * $8 is always 8
     * $28 is always 0xffff000c for printing
     * $16 is 48, for ASCII offset
     * $17 to store word println to call subroutine
     * $12 to store LT
     * $14 to store EQ
     * $18 to store LE
     * $22 to store NE
     * $13 to store GE
     * $9 to store GT
     * $19 to call push subroutine
     * $21 to call add
     * $23 to call sub
     * $24 to call star
     * $26 to call slash
     * $27 to call pct
     * $25 is a copy of $31
    */
    String genCode(Tree t) {
        if (t.matches("S BOF procedure EOF")) {
        	String ret = "";
        	ret += "lis $4\n";
        	ret += ".word 4\n";
        	ret += "lis $11\n";
        	ret += ".word 1\n";
        	ret += "lis $8\n";
        	ret += ".word 8\n";
        	ret += "lis $28\n"; // for printing output
        	ret += ".word 0xffff000c\n";
        	ret += "lis $16\n"; // for ascii offset 
        	ret += ".word 48\n";
        	ret += "lis $10\n"; 
        	ret += ".word 10\n";
        	ret += "lis $17\n"; 
        	ret += ".word printLine\n";
        	ret += "lis $9\n";
        	ret += ".word GT\n";
        	ret += "lis $19\n";
        	ret += ".word push\n";
        	ret += "lis $21\n";
        	ret += ".word add\n";
        	ret += "lis $23\n";
        	ret += ".word sub\n";
        	ret += "lis $24\n";
        	ret += ".word star\n";
        	ret += "lis $26\n";
        	ret += ".word slash\n";
        	ret += "lis $27\n";
        	ret += ".word pct\n";
        	ret += "lis $12\n";
        	ret += ".word LT\n";
        	ret += "lis $14\n";
        	ret += ".word EQ\n";
        	ret += "lis $18\n";
        	ret += ".word LE\n";
        	ret += "lis $22\n";
        	ret += ".word NE\n";
        	ret += "lis $13\n";
        	ret += ".word GE\n";
        	ret += "add $25, $31, $0\n"; // save $31 in register 25
            return ret + genCode(t.children.get(1)) + "jr $25\n" + genPrintln() + genPush() + genAdd() + genSub() + genStar() + genSlash() + genPct()
            	+ genLT() + genEQ() + genNE() + genLE() + genGE() + genGT();
        } else if (t
                .matches("procedure INT WAIN LPAREN dcl COMMA dcl RPAREN LBRACE dcls statements RETURN expr SEMI RBRACE")) {
            return genInitilization() + genCode(t.children.get(8)) + genCode(t.children.get(9)) + genCode(t.children.get(11));
        } else if (t.matches("expr term")) {
            return genCode(t.children.get(0));
        } else if (t.matches("statements statements statement")){
        	return genCode(t.children.get(0)) + genCode(t.children.get(1));
        } else if (t.matches("dcls dcls dcl BECOMES NUM SEMI")){
        	
        	// pass in name of variable and the number it should be declared to
        	return genCode(t.children.get(0)) + genDeclaration( t.children.get(1).children.get(1).rule.get(1), t.children.get(3).rule.get(1));
        } else if (t.matches("dcl INT ID")){
        	return genCode(t.children.get(1));
        } else if (t.matches("dcls")){
        	return "";
        } else if (t.matches("statement ID BECOMES expr SEMI")){
        	String ret = "";
        	int loc = symbolTable.get(t.children.get(0).rule.get(1));
        	ret += genCode(t.children.get(2));
        	ret += "sw $3, " + loc + "($29)\n";
        	return ret;
        } else if (t.matches("statements")){
        	return "";
        } else if (t.matches("statement PRINTLN LPAREN expr RPAREN SEMI")){
        	String ret = "";
        	ret += "jalr $17\n";
        	return genCode ( t.children.get(2)) + ret;
        } else if (t.matches("statement IF LPAREN test RPAREN LBRACE statements RBRACE ELSE LBRACE statements RBRACE")){
        	numIfCounter ++;
        	int thisIfCounter = numIfCounter;
        	String ret = "";
        	ret += genCode (t.children.get(2));
        	ret += "beq $3, $0, else" + thisIfCounter + "\n";
        	ret += genCode(t.children.get(5));
        	ret += "beq $0, $0, endIf" + thisIfCounter + "\n";
        	ret += "else" + thisIfCounter + ":\n";
        	ret += genCode(t.children.get(9));
        	ret += "endIf" + thisIfCounter + ":\n";
        	return ret;
        } else if (t.matches("statement WHILE LPAREN test RPAREN LBRACE statements RBRACE")){
        	//recurse on test and on statements
        	numWhileCounter++;
        	int thisWhileCounter = numWhileCounter;
        	String ret = "";
        	ret += "swh" + thisWhileCounter + ":\n";
        	ret += genCode (t.children.get(2));
        	ret += "beq $0, $3, ewh" + numWhileCounter + "\n";
        	ret += genCode(t.children.get(5));
        	ret += "beq $0, $0, swh" + thisWhileCounter + "\n";
        	ret += "ewh" + thisWhileCounter + ":\n";
        	return ret;
        } else if (t.matches("test expr LT expr")){
        	String ret = "";
        	ret += genCode (t.children.get(0)); //get code for expr for test
        	ret += genCode (t.children.get(2)); //get code for other expr for test
        	ret += "jalr $12\n"; // call genLT
        	return ret;
        } else if (t.matches("test expr EQ expr")){
        	String ret = "";
        	ret += genCode (t.children.get(0)); //get code for expr for test
        	ret += genCode (t.children.get(2)); //get code for other expr for test
        	ret += "jalr $14\n"; // call pop 2
        	return ret;
        } else if (t.matches("test expr NE expr")){
        	String ret = "";
        	ret += genCode (t.children.get(0)); //get code for expr for test
        	ret += genCode (t.children.get(2)); //get code for other expr for test
        	ret += "jalr $22\n"; // call pop 2
        	return ret;
        } else if (t.matches("test expr LE expr")){
        	String ret = "";
        	ret += genCode (t.children.get(0)); //get code for expr for test
        	ret += genCode (t.children.get(2)); //get code for other expr for test
        	ret += "jalr $18\n";
        	return ret;
        } else if (t.matches("test expr GE expr")){
        	String ret = "";
        	ret += genCode (t.children.get(0)); //get code for expr for test
        	ret += genCode (t.children.get(2)); //get code for other expr for test
        	ret += "jalr $13\n"; // call pop 2
        	return ret;
        } else if (t.matches("test expr GT expr")){
        	String ret = "";
        	ret += genCode (t.children.get(0)); //get code for expr for test
        	ret += genCode (t.children.get(2)); //get code for other expr for test
        	ret += "jalr $9\n"; // call GT
        	return ret;
        } else if (t.matches("expr expr PLUS term")){
        	String ret = "";
        	ret += "jalr $21\n";
        	return genCode( t.children.get(0)) + genCode ( t.children.get(2)) + ret;
        } else if (t.matches("expr expr MINUS term")){
        	String ret = "";
        	ret += "jalr $23\n";
        	return genCode( t.children.get(0)) + genCode ( t.children.get(2)) + ret;
        } else if (t.matches("term term STAR factor")){
        	String ret = "";
        	ret += "jalr $24\n";
        	return genCode( t.children.get(0)) + genCode ( t.children.get(2)) + ret;
        } else if (t.matches("term term SLASH factor")){
        	String ret = "";
        	ret += "jalr $26\n";
        	return genCode( t.children.get(0)) + genCode ( t.children.get(2)) + ret;
        } else if (t.matches("term term PCT factor")){
        	String ret = "";
        	ret += "jalr $27\n";
        	return genCode( t.children.get(0)) + genCode ( t.children.get(2)) + ret;
        } else if (t.matches("term factor")) {
            return genCode(t.children.get(0));
        } else if (t.matches("factor ID")) {
            return genCode(t.children.get(0));
        } else if (t.matches("factor NUM")) {
            return genCode(t.children.get(0));
        } else if (t.matches("factor LPAREN expr RPAREN")){
        	return genCode(t.children.get(1));
        } else if (t.rule.get(0).equals("ID")) {
            String name = t.rule.get(1);
        	String ret = "";
            if ( !symbolTable.containsKey(name)){
            	bail ( "ID " + name + " was not declared");
            } else {
            	int counter = symbolTable.get(name);
            	ret += "lw $3, " + counter + "($29)\n";
            	ret += "jalr $19\n";
            	return ret;
            }
            return null;
        } else if (t.rule.get(0).equals("NUM")) {
        	String number = t.rule.get(1);
        	String ret = "";
        	ret += "lis $3\n";
        	ret += ".word " + number + "\n";
        	ret += "jalr $19\n";
        	return ret;
        } else {
            bail("unrecognized rule " + t.rule);
            return null;
        }
    }

    String secondPass( String input){
    	String ret = "";
    	StringTokenizer st = new StringTokenizer(input, "\n"); 
    	Map<String, String> declarations = new HashMap<String, String>();
    	
    	while( st.hasMoreTokens() ){
    		String line = st.nextToken();
    		
    		if ( line.equals("lis $3")){
    			
    		} else {
        		ret += line + "\n";
    		}
    	}
    	
    	return ret;
    }
    
    // Main program
    public static final void main(String args[]) {
        new WLGen().go();
    }

    public void go() {
        Tree parseTree = readParse("S");
        symbols = genSymbols(parseTree);
        String firstPassResult = genCode(parseTree);
        System.out.print( secondPass( firstPassResult ));
    }
}
