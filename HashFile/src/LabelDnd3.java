import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.TransferHandler;


public class LabelDnd3 {
	JFrame mainFrame;
	JPanel mainPanel;
	JLabel label;
	JTextField textField;
	public LabelDnd3() {
	   mainFrame =new JFrame (   );
	   mainPanel =new JPanel ( new BorderLayout() );
	   
	   label =new JLabel ("label");
	   label.setTransferHandler( new TransferHandler("text") );
	   label.addMouseListener( new MouseAdapter(){
	   public void mousePressed( MouseEvent e ){
	     JComponent c = (JComponent)e.getSource();
	     TransferHandler handler = c.getTransferHandler();
	     handler.exportAsDrag(c,e,TransferHandler.COPY);
	    }
	   } );
	   
	   textField =new JTextField(20);
	   textField.setDragEnabled( true );
	   
	   mainPanel.add( label,BorderLayout.PAGE_START );
	   mainPanel.add( textField,BorderLayout.PAGE_END   );
	   mainFrame.getContentPane().add( mainPanel );
	   mainFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	   mainFrame.pack();
	   mainFrame.setLocationRelativeTo(null);
	   mainFrame.setVisible( true );
	}
}
