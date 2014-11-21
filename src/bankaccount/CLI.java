package bankaccount;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class CLI extends JFrame implements ReplicaListener {
	private Replica replica;
	
	private JPanel container;
	private JTextField cmdInput;
	private JLabel cmdOutput;
	private String cmdString;
	private String cmd;
	private double cmdValue;

	public CLI (int id) {
		super("Application CLI");
		replica = new Replica(this, "localhost", 8001, id);

		this.cmdInput = new JTextField(10);
		this.cmdOutput = new JLabel("SUCCESS");
		this.cmdString = "";
		this.cmd = "";
		this.cmdValue = 0;
		cmdInput.addKeyListener(new textFieldListener());
		
		
		this.container = new JPanel();
		container.add(cmdInput);
		container.add(cmdOutput);
		this.setContentPane(container);
		
		this.setPreferredSize(new Dimension(400, 200));
	    pack();
	    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	    this.setVisible(true);
		
	}
	public void setOutput(String response) {
		cmdOutput.setText(response);
	}
	
	private void cmdFormat(String s) {
		cmdString = s;
		String[] substrings = s.split("\\(");
		cmd = substrings[0];
		if(cmd == "balance"){
			return;
		}
		else if (cmd == "withdrawal" || cmd == "deposit"){
			substrings = substrings[1].split("\\)");
			cmdValue = Double.parseDouble(substrings[0]);
		}
		else {return;}
	}
	
	public class textFieldListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void keyReleased(KeyEvent e) {
			int key= e.getKeyCode();
			if(e.getSource()== cmdInput) {
				if (key == KeyEvent.VK_ENTER) {
					cmdFormat(cmdInput.getText());
					if(cmd == "balance") {
						replica.balance();
					}
					else if (cmd.equals("deposit")) {
						replica.deposit(cmdValue);
					}
					else if (cmd.equals("withdraw")){
						replica.withdraw(cmdValue);
					}
					else if (cmd.equals("fail")) {
						replica.fail();
					}
					else if (cmd.equals("unfail")) {
						replica.unfail();
					}
				}
			}
		}
		@Override
		public void keyTyped(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	public void replicaActionPerformed(ReplicaEvent e) {
		setOutput(e.getStatus().toString());
	}
}
