package bankaccount;

import java.awt.Dimension;


public class Main {
	public static int appIteration = 0;
	
	public static void main(String[] args){
		//CLI appInterface = new CLI();

		String[] list = cmdFormat("withdrawal(4)");
		for (String s : list){
			System.out.println(s);
		}
		
	}
	private static String[] cmdFormat(String s) {
		String[] string = new String[2];
		String[] substrings = s.split("\\(");
		string[0] = substrings[0];
		substrings = substrings[1].split("\\)");
		string[1] = substrings[0];

		return string;
	}
}
