package Server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Vector;

public class MainServer {
	public static long maxStorage = 0;
	public static long leftStorage = 0;
	public static int fileNumber = 0;
	public static int NodeNumber = 0;
	public static Vector<Node> NodeList = new Vector<Node>();
	public static Properties prop;
	
	public static void saveProperties(){
		try {
			FileOutputStream fos = new FileOutputStream("Server.properties");
			prop.store(fos, null);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}//存储prop到本地文件
	
	public static void main(String[] args){
		try {
			MainServer MS = new MainServer();
			prop = new Properties();//属性集合对象      
			FileInputStream fis;
			fis = new FileInputStream("Server.properties");
			prop.load(fis);//将属性文件流装载到Properties对象中    
			//maxStorage = Long.parseLong(prop.getProperty("maxStorage"));
			//leftStorage = Long.parseLong(prop.getProperty("leftStorage"));
			maxStorage = 0;
			leftStorage = 0;
			fileNumber = Integer.parseInt(prop.getProperty("fileNumber"));
			NodeNumber = Integer.parseInt(prop.getProperty("NodeNumber"));
			//读取prop信息并初始化
			
			MS.initNode(prop);
			System.out.println("初始化成功");
			ServerSocket ss = new ServerSocket(1234);
			while(true){
				Socket s = ss.accept();
				new ClientServer(s).start();//新线程
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("初始化服务器失败！");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("初始化服务器失败！");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void initNode(Properties prop){//通过配置文件初始化服务器
		for(int i = 0 ;i< NodeNumber ;i++){
			String ip = prop.getProperty("Node_"+(i+1));
			int port = Integer.parseInt(prop.getProperty("Node_"+(i+1)+"_port"));
			Node temp  = new Node(ip,port);
			if(temp.isSuccess()){
				NodeList.add(temp);//如果节点存在，且初始化成功，则添加到列表中
			}
		}
		updateStorage();//更新本地容量
	}
	public static void updateStorage(){//更新本地最大存储容量跟剩余容量
		for(Node n:NodeList){
			maxStorage+=n.getMaxStorage();
			leftStorage+=n.getLeftStorage();
		}
		prop.setProperty("maxStorage", ""+maxStorage);
		prop.setProperty("leftStorage", ""+leftStorage);
		saveProperties();
	}
	public static Socket[] getNodes() {
		if(NodeList.size()<=1){
			return null;//是否有两个节点
		}
		else{
			Node main = null;
			for(Node n:NodeList){
				if(!n.isFull()){
					if(main==null){
						main = n;
					}
					else if(main.getLeftStorage()<=n.getLeftStorage()){
						main = n;//找个最大的
					}
				}
			}
			Node basic = null;
			for(Node n:NodeList){
				if(!n.isFull()){
					if(n!=main){//basic！=main
						if(basic == null){
							basic = n;
						}
						else if(basic.getLeftStorage()<=n.getLeftStorage()){
							basic = n;//找个最大的
						}
					}
				}
			}
			if(main!=null && basic !=null){
				Socket[] result = new Socket[2];
				try {
					Socket Main = new Socket(main.getIp(),main.getPort());
					result[0] = Main;
				} catch (Exception e) {
					NodeList.remove(main);//如果无法连接上，说明节点在初次连接之后已经GG，直接从服务器节点中删除
					return getNodes();//重新寻找
				}	
				try{
					Socket Basic = new Socket(basic.getIp(),basic.getPort());
					result[1] = Basic;
					return result;
				}catch(Exception e){
					if(result[0]!=null){
						try {
							result[0].close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					NodeList.remove(basic);//如果无法连接上，说明节点在初次连接之后已经GG，直接从服务器节点中删除
					return getNodes();//重新寻找
				}
			}
			
		}
		return null;
		// TODO Auto-generated method stub
	}
}
