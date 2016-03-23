import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;


public class LabelDnd2 {
	JFrame mainFrame;
	JPanel mainPanel;
	JLabel label;
	JColorChooser colorChooser;
	
	public LabelDnd2() {
	   mainFrame =new JFrame (   );
	   mainPanel =new JPanel ();
	   
	   colorChooser =new JColorChooser ();
	   colorChooser.setDragEnabled( true );
	   
	   label =new JLabel (" i can accept color ");
	   label.setTransferHandler( new TransferHandler("foreground") );
	   
	   mainPanel.add( colorChooser );
	   mainPanel.add( label );
	   mainFrame.getContentPane().add( mainPanel );
	   mainFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	   mainFrame.pack();
	   mainFrame.setLocationRelativeTo(null);
	   mainFrame.setVisible( true );
	}
}
