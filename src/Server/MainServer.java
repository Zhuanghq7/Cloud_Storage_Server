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
	}//�洢prop�������ļ�
	
	public static void main(String[] args){
		try {
			MainServer MS = new MainServer();
			prop = new Properties();//���Լ��϶���      
			FileInputStream fis;
			fis = new FileInputStream("Server.properties");
			prop.load(fis);//�������ļ���װ�ص�Properties������    
			//maxStorage = Long.parseLong(prop.getProperty("maxStorage"));
			//leftStorage = Long.parseLong(prop.getProperty("leftStorage"));
			maxStorage = 0;
			leftStorage = 0;
			fileNumber = Integer.parseInt(prop.getProperty("fileNumber"));
			NodeNumber = Integer.parseInt(prop.getProperty("NodeNumber"));
			//��ȡprop��Ϣ����ʼ��
			
			MS.initNode(prop);
			System.out.println("��ʼ���ɹ�");
			ServerSocket ss = new ServerSocket(1234);
			while(true){
				Socket s = ss.accept();
				new ClientServer(s).start();//���߳�
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("��ʼ��������ʧ�ܣ�");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("��ʼ��������ʧ�ܣ�");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void initNode(Properties prop){//ͨ�������ļ���ʼ��������
		for(int i = 0 ;i< NodeNumber ;i++){
			String ip = prop.getProperty("Node_"+(i+1));
			int port = Integer.parseInt(prop.getProperty("Node_"+(i+1)+"_port"));
			Node temp  = new Node(ip,port);
			if(temp.isSuccess()){
				NodeList.add(temp);//����ڵ���ڣ��ҳ�ʼ���ɹ�������ӵ��б���
			}
		}
		updateStorage();//���±�������
	}
	public static void updateStorage(){//���±������洢������ʣ������
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
			return null;//�Ƿ��������ڵ�
		}
		else{
			Node main = null;
			for(Node n:NodeList){
				if(!n.isFull()){
					if(main==null){
						main = n;
					}
					else if(main.getLeftStorage()<=n.getLeftStorage()){
						main = n;//�Ҹ�����
					}
				}
			}
			Node basic = null;
			for(Node n:NodeList){
				if(!n.isFull()){
					if(n!=main){//basic��=main
						if(basic == null){
							basic = n;
						}
						else if(basic.getLeftStorage()<=n.getLeftStorage()){
							basic = n;//�Ҹ�����
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
					NodeList.remove(main);//����޷������ϣ�˵���ڵ��ڳ�������֮���Ѿ�GG��ֱ�Ӵӷ������ڵ���ɾ��
					return getNodes();//����Ѱ��
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
					NodeList.remove(basic);//����޷������ϣ�˵���ڵ��ڳ�������֮���Ѿ�GG��ֱ�Ӵӷ������ڵ���ɾ��
					return getNodes();//����Ѱ��
				}
			}
			
		}
		return null;
		// TODO Auto-generated method stub
	}
}
