package Server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Node {
	private String name = null;
	private String ip;
	private int port;
	private long maxStorage = 0;
	private long leftStorage = 0;
	private Socket s = null;
	private boolean full = false;
	private boolean isConnect = false;
	private boolean initSuccess = false;
	public String getIp(){
		return ip;
	}
	public int getPort(){
		return port;
	}
	public long getMaxStorage(){
		return maxStorage;
	}
	public long getLeftStorage(){
		return leftStorage;
	}
	public void upLeftStorage(long s){
		leftStorage +=s;
	}
	public void downLeftStorage(long s){
		leftStorage-=s;
	}
	public boolean isSuccess(){
		return initSuccess;
	}
	public boolean isFull(){
		return full;
	}
	public String getName(){
		return name;
	}
	private void out(String ss) throws UnsupportedEncodingException, IOException{
		if(isConnect){
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"UTF-8"));//不管三七二十一咱先把这个文件名字写了是吧
			bw.write(ss+"\n");
			bw.flush();
		}
	}
	private String in() throws UnsupportedEncodingException, IOException{
		if(isConnect){
			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
			return br.readLine();
		}
		return null;
	}
	private boolean waitGet() throws UnsupportedEncodingException, IOException{
		String temp = in();
		if(temp.equals("get"))
			return true;
		return false;
	}
	public Node(String ip,int port){
		try {
			this.ip = ip;
			this.port = port;
			s = new Socket(ip,port);
			isConnect = true;
			out("first");
			name = in();
			out("get");
			DataInputStream dis = new DataInputStream(s.getInputStream());
			maxStorage = dis.readLong();
			out("get");
			leftStorage = dis.readLong();
			out("get");
			if(leftStorage >= maxStorage){
				full = true;
			}
			if(s!=null){
				s.close();
			}
			isConnect = false;
			initSuccess = true;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
