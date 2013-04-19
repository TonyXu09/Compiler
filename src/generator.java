
public class generator {


	public static void main (String args[]){

		String a = ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
		String word = ("sWAIN");
		for (int j= 0; j<word.length() -1; j++){
		
			for (int i = 0; i< a.length(); i++){
				System.out.println (word.substring (0, j+2) + "\t\t" + a.substring(i,i+1) + "\tID");
			}
		}	

	}
}

