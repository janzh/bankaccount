package bankaccount;

import java.util.ArrayList;

public class Log {
	ArrayList<String> log;
	
	public Log() {
		log = new ArrayList<String>();
	}
	
	public String GetElement(int i){
		return log.get(i);
	}
	public void putElement(String s){
		log.add(s);
	}
}
