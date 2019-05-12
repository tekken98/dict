import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
public class D extends KeyAdapter implements ActionListener{
    JFrame frame = null;
    JTextArea label = null;
    JTextField text = null;
    String line = null;
    Dict dic;
    D() {
        dic = new Dict();
        dic.run();
        frame = new JFrame();
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout(10,10));
        text = new JTextField();
        text.requestFocus();
        text.addKeyListener(this);

        contentPane.add(text,BorderLayout.NORTH);
        JButton btnNext = new JButton("next");
        btnNext.addActionListener(this);
        contentPane.add(btnNext,BorderLayout.SOUTH);
        label = new JTextArea(50,50);
        label.setLineWrap(true);
        JPanel p1 = new JPanel();
        p1.add(new JScrollPane(label));

        contentPane.add(p1,BorderLayout.CENTER);
        
        frame.setTitle("Dictionary");
        frame.pack();
        frame.setSize(800,600);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing (WindowEvent e) {
                System.exit(0);
            }
        });
    }
    public void setInfo(String w){
        String t = dic.getCurrentWord();
        label.setText( t + "\n" +w);
    }
    public void actionPerformed(ActionEvent e){
        String w = dic.findNextWord();
        setInfo(w);
    }
    public void keyTyped(KeyEvent e)
    {
        String s = text.getText();
        System.out.print(s);
        if (s.length() > 1){
        String w = dic.findWord(s);
        setInfo(w);
        }
    }
    public static void main (String [] args){
        D d = new D();
    }
}
