package bankaccount;

public class DecideMessage extends Message {
	double value;
	
	public DecideMessage(double value){
		this.value = value;
	}
	
	public double getValue(){
		return value;
	}
}
