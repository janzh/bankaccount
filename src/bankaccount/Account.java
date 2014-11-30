package bankaccount;

import bankaccount.ReplicaEvent.Type;

public class Account {
	double balance;

	public double getBalance() {
		return balance;
	}

	public void withdraw(double withdrawal) throws IllegalArgumentException{
		if (balance >= withdrawal){
			this.balance -= withdrawal;
		}
		else {
			throw new IllegalArgumentException("Withdrawal amount can not be higher than balance");
		}
	}
	public void deposit(double deposit) {
		this.balance += deposit;
	}
	
	public void performOperations(Log log){
		int n = log.size();
		for (int i=0; i<n; i++){
			LogEntry entry = log.getEntry(i);
			if (entry.getOperation() == Type.DEPOSIT){
				deposit(entry.getValue());
			}
			else if (entry.getOperation() == Type.WITHDRAW){
				withdraw(entry.getValue());
			}
		}
	}
	
}
