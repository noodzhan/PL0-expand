package sys;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;  
  
//实现接口ActionListener  
public class UI implements ActionListener {  
  
    JFrame jf;  
    JPanel jpanel;  
    JPanel jpanelB;
    JButton jb1, jb2, jb3;  
    JTextArea jta = null;  
    JTextArea jta1=null;
    JTextArea jta2=null;
    
    JScrollPane jscrollPane;
    JScrollPane jscrollPane1;
    JScrollPane jscrollPane2;
    
  
	static boolean button1=false ;
	static boolean button2=false;
    static boolean button3=false;
    
    
    

    public UI() {
		
		//下面是实现UI
		jf = new JFrame("PL0");  
        Container contentPane = jf.getContentPane();  
        contentPane.setLayout(new BorderLayout());  
        
        
        jta = new JTextArea(10, 15);  
        jta.setTabSize(4);  
        jta.setFont(new Font("标楷体", Font.BOLD, 16));  
        jta.setLineWrap(true);// 激活自动换行功能  
        jta.setWrapStyleWord(true);// 激活断行不断字功能  
        //jta.setBackground(Color.pink);  
  
        jta1=new JTextArea();
        jta1 = new JTextArea(10, 15);  
        jta1.setTabSize(4);  
        jta1.setFont(new Font("标楷体", Font.BOLD, 16));  
        jta1.setLineWrap(true);// 激活自动换行功能  
        jta1.setWrapStyleWord(true);// 激活断行不断字功能  
        
        
        jta2=new JTextArea();
        jta2 = new JTextArea(10, 15);  
        jta2.setTabSize(4);  
        jta2.setFont(new Font("标楷体", Font.BOLD, 16));  
        jta2.setLineWrap(true);// 激活自动换行功能  
        jta2.setWrapStyleWord(true);// 激活断行不断字功能  
        
        jpanelB=new JPanel(new GridLayout(0,3));
        
        
        
        
        jscrollPane = new JScrollPane(jta);  
        jscrollPane1 = new JScrollPane(jta1);
        jscrollPane2 = new JScrollPane(jta2);  
        
        jpanel = new JPanel();  
        jpanel.setLayout(new GridLayout(7, 0));  
  
       
        
        
        
        jb1 = new JButton("编译");
        //jb1.setPreferredSize(new Dimension(40,30));
        jb1.addActionListener(this);  
        jb2 = new JButton("生成中间代码"); 
        //jb2.setPreferredSize(new Dimension(40,30));
        jb2.addActionListener(this);  
        jb3 = new JButton("运行程序");  
        //jb3.setPreferredSize(new Dimension(40,30));
        jb3.addActionListener(this);  
  
        jpanel.add(new JLabel());
        jpanel.add(jb1);
        jpanel.add(new JLabel());
        jpanel.add(jb2); 
        jpanel.add(new JLabel());
        jpanel.add(jb3);  
        jpanel.add(new JLabel());
  
        //contentPane.add(jscrollPane);
        //contentPane.add(jscrollPane1);
        //contentPane.add(jscrollPane2);
        
        jpanelB.add(jscrollPane);
        jpanelB.add(jscrollPane1);
        jpanelB.add(jscrollPane2);
  
        
        contentPane.add(jpanelB,BorderLayout.CENTER);
        
        contentPane.add(jpanel,BorderLayout.EAST);  
        
        JPanel jp2=new JPanel(new GridLayout(1,3));
        jp2.add(new JLabel("PL0代码"));
        jp2.add(new JLabel("中间代码"));
        jp2.add(new JLabel("运行结果"));
        
        contentPane.add(jp2,BorderLayout.NORTH);
  
        jf.setSize(1400, 800);  
        jf.setLocation(100,100);  
        jf.setVisible(true);  
  
        jf.addWindowListener(new WindowAdapter() {  
            public void windowClosing(WindowEvent e) {  
                System.exit(0);  
            }  
        });  
		
		
	}
    public void back_control() {
    	
    	
    	String fname = "";
		BufferedReader fin;
		
		try {
			
			fname = "wo.txt";
			
			fin = new BufferedReader(new FileReader(fname), 4096);

			
			PL0.tableswitch=true;
			PL0.listswitch=true;
			
			PL0.fa1 = new PrintStream("fa1.tmp");
			
			
			// 构造编译器并初始化
			PL0 pl0 = new PL0(fin);
			
			if (pl0.compile()) {
				// 如果成功编译则接着解释运行
				PL0.fa2 = new PrintStream("fa2.tmp");
				pl0.interp.interpret();
				PL0.fa2.close();
			} else {
				System.out.print("Errors in pl/0 program");
				jta2.setText("Errors in pl/0 program");
			}
			
		} catch (IOException e1) {
			System.out.println("Can't open file!");
			jta2.setText("Can't open file!");
		}

		System.out.println();
		
		
    	
    	
    }
    
    
    static boolean delFile(File file) {
        if (!file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                delFile(f);
            }
        }
        return file.delete();
    }
    
  
    public static void main(String s[]) {
    	new UI();
    }
      
    
    // 覆盖接口ActionListener的方法actionPerformed  
    public void actionPerformed(ActionEvent e) {  
        if (e.getSource() == jb1) {  
        	
        	
        	
        	this.delFile(new File("wo.txt"));
        	this.delFile(new File("fa.tmp"));
        	this.delFile(new File("fa1.tmp"));
        	this.delFile(new File("fa2.tmp"));
        	this.delFile(new File("fas.tmp"));
        	
        	jta1.setText("");
        	jta2.setText("");
        	
        	String filename="wo.txt";
        	File f=new File(filename);
			FileWriter fw=null;
			System.out.println("编译点击");
			try {
				
				fw=new FileWriter(f);
				fw.write(this.jta.getText());	
				
			}catch(Exception e1) {
				e1.printStackTrace();
			}finally {
				try {
					fw.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			back_control();//执行控制部分
			
		
        	
        } else if (e.getSource() == jb2) {  
        	File f=null;
			BufferedReader br=null;
			try {
				
				br=new BufferedReader(new FileReader("fa.tmp"));
				String allcon="";
				String line=null;
				while((line=br.readLine())!=null) {
					allcon+=line+"\r\n";
				}
				this.jta1.setText(allcon);
			}catch(Exception e1) {
				e1.printStackTrace();
				jta2.setText("无代码");
			}finally {
				try {
					br.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
        } else if (e.getSource() == jb3) {  
        	File f=null;
			BufferedReader br=null;
			try {
				
				br=new BufferedReader(new FileReader("fa2.tmp"));
				String allcon="";
				String line=null;
				while((line=br.readLine())!=null) {
					allcon+=line+"\r\n";
				}
				this.jta2.setText(allcon);
			}catch(Exception e1) {
				e1.printStackTrace();
			}finally {
				try {
					br.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
        }  
    }   
}  

