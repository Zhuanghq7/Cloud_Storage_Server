package zhuangh7.client;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class socket_to_Node extends Thread{
	private String filename = "";
	private boolean stop = false;
	public socket_to_Node(String s){
		filename+=s;
	}
	public static void main(String args[]){
		JFrame jf = new JFrame("server");
		jf.setSize(300, 200);
		jf.setVisible(true);
		socket_to_Node stN = new socket_to_Node("1.txt");
		Dimension screenSize =Toolkit.getDefaultToolkit().getScreenSize();
		jf.setLocation(screenSize.width/2-150,screenSize.height/2-100);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel jp = new JPanel();
	    jf.add (jp);
	    JButton button = new JButton ("上传");
	   //// button.setBounds (150-50,100-25,100,50);
	    jf.add (button);
	    button.addActionListener(new ActionListener(){
	    	@Override
	    	public void actionPerformed(ActionEvent e){
	    		stN.start();
	    	}
	    });
		jf.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) {
				stN.Stop();//炸了炸了，礼拜一问老师- -
				System.exit(0);
			}
		});
	}
	
	
	
	public void run(){
		try {
			Socket s = new Socket("127.0.0.1",2016);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"UTF-8"));//不管三七二十一咱先把这个文件名字写了是吧
			bw.write(filename+"\n");
			bw.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
			while(!br.readLine().equals("get")){
				
			}
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			File fff = new File(".");
	        String nowPath = fff.getCanonicalPath();
	        System.out.println(nowPath+"\\"+filename);
			File f = new File(nowPath+"\\"+filename);
			FileInputStream fis = new FileInputStream(f);
			byte[] sendBytes = new byte[1024];  
			int length = 0;
			double sumL = 0;
			long l = f.length();
			dos.writeLong(l);
	            while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0 && !stop) {  
	                sumL += length;    
	               // System.out.println("已传输："+((sumL/l)*100)+"%");  
	                dos.write(sendBytes, 0, length);  
	                dos.flush();  
	            } 
	            if(!stop){
	            System.out.println("传输完成");
	            }
	            else{
	            	System.out.println("传输终止");
	            }
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//@Override
	public void Stop(){
		stop = true;
	}
}
