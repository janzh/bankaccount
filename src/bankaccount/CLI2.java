package bankaccount;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import bankaccount.ReplicaEvent.Type;
import bankaccount.log.LogEntry;

public class CLI2 extends JPanel implements ActionListener, ReplicaListener {
	
	protected JTextField textField;
    protected JTextArea textArea;
    protected JLabel label, result;
    protected Replica replica;
	
	public CLI2(Replica replica){	
		super(new GridBagLayout());
		this.replica = replica;
		
		textField = new JTextField(20);
		textField.addActionListener(this);
		
		textArea = new JTextArea(30,20);
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea);
		
		label = new JLabel("#"+replica.getId()+" Status: ");
		result = new JLabel("Result: ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		c.fill = GridBagConstraints.HORIZONTAL;
		add(textField, c);
		add(label, c);
		add(result, c);
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		
		add(scrollPane, c);
		
		//Create and set up the window.
        JFrame frame = new JFrame("CLI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
 
        //Add contents to the window.
        container.add(this);	
        
        frame.add(container);
 
        //Display the window.
        frame.pack();
        frame.setTitle("Port: "+replica.getLocationData().getPort());
        frame.setVisible(true);
	}
	
	String cmd;
	double cmdValue;
	
	private void cmdFormat(String s) {
		String[] substrings = s.split("\\(");
		cmd = substrings[0];
		if (cmd.equals("withdraw") || cmd.equals("deposit")){
			substrings = substrings[1].split("\\)");
			cmdValue = Double.parseDouble(substrings[0]);
		}
		else {return;}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String text = textField.getText();
//        textArea.append(text + "\n");
        textField.selectAll();
 
        //Make sure the new text is visible, even if there
        //was a selection in the text area.
        textArea.setCaretPosition(textArea.getDocument().getLength());
        
        //if (text.equals("deposit")) replica.deposit(500);
        cmdFormat(text);
        if(cmd.equals("balance")) {
			replica.balance();
		}
		else if(cmd.equals("deposit")) {
			replica.deposit(cmdValue);
		}
		else if(cmd.equals("withdraw")) {
			replica.withdraw(cmdValue);
		}
		else if(cmd.equals("fail")) {
			replica.fail();
		}
		else if(cmd.equals("unfail")) {
			replica.unfail();
		}
		else if(cmd.equals("print")) {
			updateLogTextArea();
		}
	}

	@Override
	public void replicaActionPerformed(ReplicaEvent e) {
		if (e.getType() == Type.RECEIVE){
//			textArea.append("Received message: "+e.getMessage()+"\n");
		}
		else if (e.getType() == Type.BALANCE){
			//setStatus("");
			//textArea.append(e.getValue() + "\n");
			setResult(e.getValue());
		} else {
			setStatus(e.getStatus().toString());
		}
	}
	
	private void updateLogTextArea(){
		ArrayList<LogEntry> log = replica.getLog().getLogList();
		textArea.setText("");
		for (LogEntry entry : log){
			textArea.append(entry.getOperation().toString()+": "+entry.getValue() + "\n");
		}
	}
	
	private void setStatus(String s){
		label.setText("#"+replica.getId()+"Status: "+s);
	}
	
	private void setResult(double d){
		result.setText("Result: "+d);
	}

}