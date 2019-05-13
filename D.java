import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
public class D extends KeyAdapter implements ActionListener{
    JFrame frame = null;
    JTextArea label = null;
    JTextField text = null;
    String line = null;
    Dict dic;
    String d_dict =null;
    void init(String dict){
        dic = null;
        dic = new Dict(dict);
        dic.run();
    }
    D() {
        frame = new JFrame();
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout(10,10));
        text = new JTextField();
        text.requestFocus();
        text.addKeyListener(this);

        contentPane.add(text,BorderLayout.NORTH);
        label = new JTextArea(30,50);
        label.setLineWrap(true);
        JPanel p1 = new JPanel();
        p1.add(new JScrollPane(label));
        contentPane.add(p1,BorderLayout.CENTER);

        JPanel p2 =new JPanel();
        JButton btnNext = new JButton("next");
        btnNext.addActionListener(this);
        JButton btnPre= new JButton("pre");
        btnPre.addActionListener(this);
        p2.add(btnPre);
        p2.add(btnNext);
        contentPane.add(p2,BorderLayout.SOUTH);

        
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("Edit");

        JMenuItem longman = new JMenuItem("longman");
        menu.add(longman);
        longman.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                init("longman");
            }});


        JMenuItem xianya = new JMenuItem("xiangya");
        xianya.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                init("xiangya-medical");
            }});
        menu.add(xianya);
        bar.add(menu);
        frame.setJMenuBar(bar);

        frame.setTitle("Dictionary");
        frame.pack();
        //frame.setSize(800,600);
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

        String w=null;
        if (e.getActionCommand().equals("pre"))
            w = dic.findPreWord();
        if (e.getActionCommand().equals("next"))
            w = dic.findNextWord();
        setInfo(w);
    }
    public void keyTyped(KeyEvent e)
    {
        String s = text.getText();
        if (s.length() > 1){
            String w = dic.findWord(s);
            setInfo(w);
        }
    }
    public static void main (String [] args){
        D d = new D();
        d.init("longman");
    }
}
