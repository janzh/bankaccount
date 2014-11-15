package bankaccount;

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
	
}
