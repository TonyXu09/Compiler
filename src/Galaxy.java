import java.util.*;

/**
 * A simple class to read a .cfg file and print the left-canonical
 * derivation without leading or trailing spaces.
 *
 * @version 071024.0
 */
public class Galaxy {
  public static void main(String[] args) {
    Scanner in = new Scanner(System.in);
    skipGrammar(in);
    printDerivation(in);
  }

  /**
   * Skip the grammar part of the input.
   *
   * @param in the scanner for reading input
   */
  private static void skipGrammar(Scanner in) {
    assert(in.hasNextInt());

    // read the number of terminals and move to the next line
    int numTerm = in.nextInt();
    in.nextLine();

    // skip the lines containing the terminals
    for (int i = 0; i < numTerm; i++) {
      in.nextLine();
    }

    // read the number of non-terminals and move to the next line
    int numNonTerm = in.nextInt();
    in.nextLine();

    // skip the lines containing the non-terminals
    for (int i = 0; i < numNonTerm; i++) {
      in.nextLine();
    }

    // skip the line containing the start symbol
    in.nextLine();

    // read the number of rules and move to the next line
    int numRules = in.nextInt();
    in.nextLine();

    // skip the lines containing the production rules
    for (int i = 0; i < numRules; i++) {
      in.nextLine();
    }
  }

  /**
   * Prints the derivation with whitespace trimmed.
   *
   * @param in the scanner for reading input
   */
  private static void printDerivation(Scanner in) {
	  String line = "";
	  if (in.hasNextLine()){
		  line = in.nextLine();
	  }
    while (in.hasNextLine()) {
    	String termToReplace = "";
    	String nextLine = "";
    	if ( in.hasNext() ) {
    		termToReplace = in.next();
    	}
    	if ( in.hasNextLine() ){
    		nextLine = in.nextLine();
    		line = line.replaceFirst(termToReplace, nextLine);
    	}   
    }
	line = line.replaceAll("id", "42");
	String[] tokens = line.split( " " );
	List<String> list = new ArrayList<String>();
	
	for ( int i = 0; i < tokens.length; i++){
		if ( tokens[i].equals( "42" ) || tokens[i].equals( "-") 
				|| tokens[i].equals( "(") || tokens[i].equals( ")")){ 
			list.add( tokens[i]);
		}
	}
	
	int j =0;
	List<Integer> lLoc = new ArrayList<Integer>();
	List<Integer> rLoc = new ArrayList<Integer>();
	
	// find the locations of the brackets
	while ( j < list.size() ){
		if ( list.get(j).equals( "(" )){
			lLoc.add(j);
		}else if ( list.get(j).equals( ")" )){
			rLoc.add(j);
		}
		j++;
	}
	
	Stack<String> s = new Stack<String>();
	int sum = 0;
	int numParen = rLoc.size();
	
	for ( int i = 0; i < numParen ; i++){
		int loc = rLoc.get(i);
		while ( !list.get(loc).equals("(") ){
			loc--;
		}
//		System.out.println( " i is" + i );
		for ( int k = rLoc.get(i) - 1; k > loc; k--){
//			System.out.println(k);
			if ( !list.get(k).equals("-") ){
				s.push(list.get(k));
			}
			list.set(k, "0");
		}
		list.set( rLoc.get(i), "0");
		list.set( loc, String.valueOf( calculateSum ( s) ) );
	}
	
	j = list.size() - 1;
	
	// find the locations of the brackets
	while ( j >= 0 ){
		if ( !list.get(j).equals("-")){
			s.push(list.get(j));
		}
		j--;
	}
	
	sum = calculateSum( s);
	
	System.out.println( sum );
//	System.out.println( "stack is " + s.toString() );
//	System.out.println( lLoc.toString());
//	System.out.println( rLoc.toString());
//	System.out.println( list.toString());
	//System.out.println( (2 - numId) * 42 );
  }
  
  private static int calculateSum( Stack<String> s){
//	  System.out.println( "stack is " + s.toString() );
	  
	  int result = 0;
	  
	  if ( !s.empty()){
		  result = Integer.valueOf( s.pop());
	  }
	  while ( !s.empty()){
		  result -= Integer.valueOf( s.pop() );
	  }
	  return result;
  }
}
