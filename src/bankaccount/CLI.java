package bankaccount;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class CLI extends JFrame{
	private Replica replica;
	
	private JPanel container;
	private JTextField cmdInput;
	private JLabel cmdOutput;
	private String cmdString;
	private String cmd;
	private double cmdValue;

	public CLI () {
		super("Application CLI");
		replica = new Replica(this);

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
						replica.balance(cmdValue);
					}
					else if(cmd == "deposit") {
						replica.deposit(cmdValue);
					}
					else if(cmd == "withdraw"){
						
					}
				}
			}
		}
		@Override
		public void keyTyped(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	}
}

