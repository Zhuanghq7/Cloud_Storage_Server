package Server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.swing.JOptionPane;

public class ClientServer extends Thread{
	private Socket s;
	private boolean isConnect = false;
	public ClientServer(Socket s){
		this.s = s;
		isConnect = true;
	}
	private void out(String ss) throws UnsupportedEncodingException, IOException{
		if(isConnect){
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"UTF-8"));//�������߶�ʮһ���Ȱ�����ļ�����д���ǰ�
			bw.write(ss+"\n");
			bw.flush();
		}
	}
	private void outS(String ss,Socket st)throws UnsupportedEncodingException,IOException{
		if(isConnect){
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(st.getOutputStream(),"UTF-8"));//�������߶�ʮһ���Ȱ�����ļ�����д���ǰ�
			bw.write(ss+"\n");
			bw.flush();
		}
	}
	private String inS(Socket st) throws IOException{
		if(isConnect){
			BufferedReader br = new BufferedReader(new InputStreamReader(st.getInputStream(),"UTF-8"));
			return br.readLine();
		}
		return null;
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
	private boolean waitGetS(Socket s) throws UnsupportedEncodingException, IOException{
		String temp = inS(s);
		if(temp.equals("get"))
			return true;
		return false;
	}
	//����6����nodeһëһ��
	private void upload() throws UnsupportedEncodingException, IOException{
		out("get");
		String fileName = in();
		String uuid = UUID.randomUUID().toString();
		String newFile = uuid+fileName;
		out("get");
		DataInputStream dis = new DataInputStream(s.getInputStream());
		long flength = dis.readLong();
		//�ļ�����
		Socket[] Nodes = MainServer.getNodes();
		//�ӷ��������нڵ��У�ѡ��ʣ���������������ڵ㡣
		if(flength<=MainServer.leftStorage){
			Socket main = Nodes[0];
			Socket basic = Nodes[1];
			outS("second",basic);
			waitGetS(basic);
			outS("up",basic);
			waitGetS(basic);
			DataOutputStream dosB = new DataOutputStream(basic.getOutputStream());
			dosB.writeLong(flength);//�������ӱ��ݽڵ㣬���ݽڵ��ܴ������ڵ�һ���ܴ���
			dosB.flush();
			if(waitGetS(basic)){//������ݽڵ�ŵ��£����ɿ�ʼ����
				outS("second",main);
				waitGetS(main);
				outS("up",main);
				waitGetS(main);
				DataOutputStream dosA = new DataOutputStream(main.getOutputStream());
				dosA.writeLong(flength);
				dosA.flush();
				
				//��������
				waitGetS(main);
				outS(newFile,main);
				waitGetS(main);
				outS(newFile,basic);
				waitGetS(basic);
				
				try{
				out("get");//��ͻ���ȷ�Ͽ��Դ洢 
				byte[] inputByte = new byte[1024];     
				System.out.println("��ʼ�ϴ��ļ���"+fileName);  
				double sumL = 0;
				int length;
				while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {  
					dosA.write(inputByte,0,inputByte.length);
					dosB.write(inputByte,0,inputByte.length);
					dosA.flush();
					dosB.flush();
					sumL+=length;
					System.out.println("�Ѵ��䣺"+sumL/(flength/100)+"%");
					if(sumL>=flength){
						//System.out.println("1");
						waitGetS(main);
						waitGetS(basic);
						break;
					}  
				}  
				for(Node nn :MainServer.NodeList){
					if(nn.getIp().equals(main.getInetAddress())&& nn.getPort()==main.getPort()){
						nn.downLeftStorage(flength);
					}
					if(nn.getIp().equals(basic.getInetAddress())&&nn.getPort()==basic.getPort()){
						nn.downLeftStorage(flength);
					}
				}
				MainServer.prop.setProperty(uuid, fileName);
				MainServer.prop.setProperty(uuid+"mainIP",main.getInetAddress().toString().substring(1));
				MainServer.prop.setProperty(uuid+"mainPort",""+main.getPort());
				MainServer.prop.setProperty(uuid+"basicIP",basic.getInetAddress().toString().substring(1));
				MainServer.prop.setProperty(uuid+"basicPort",""+basic.getPort());
				MainServer.prop.setProperty(uuid+"size", ""+flength);
				MainServer.prop.setProperty("fileNumber", ""+(Integer.parseInt(MainServer.prop.getProperty("fileNumber"))+1));
				MainServer.updateStorage();
				MainServer.saveProperties();//�洢��Ϣ
				out(uuid);//����uuid
				waitGet();
				if(main!=null){
					main.getOutputStream().close();
					main.close();
				}
				if(basic!=null){
					basic.getOutputStream().close();
					basic.close();
				}
				}catch(Exception e){
					if(main!=null){
						main.getOutputStream().close();
						main.close();
						System.out.println("�ر�main");
					}
					if(basic!=null){
						
						basic.getOutputStream().close();
						basic.close();
						System.out.println("�ر�baisc");
					}
					s.close();
					System.out.println("�ϴ�ʧ��");
				}
			}else{//��������ݷ��������Ų���
				out("false");
			}
		}
		else{//��������ļ�ϵͳ���Ų���
			out("false");
		}
	}
	public void download() throws UnsupportedEncodingException, IOException{
		out("get");
		String uuid = in();
		String nowName = null;
		String fileName;
		if((fileName = MainServer.prop.getProperty(uuid)) !=null){//�ж���û������ļ�
			if(fileName.equals(uuid)){//�ж��Ƿ������
				fileName = MainServer.prop.getProperty(uuid+"oldName");
				nowName = MainServer.prop.getProperty(uuid+"nowName");
			}
			String MainIP = MainServer.prop.getProperty(uuid+"mainIP");
			int port = Integer.parseInt(MainServer.prop.getProperty(uuid+"mainPort"));
			//�ӱ����ļ�������
			try {
				Socket sB = new Socket(MainIP,port);
				outS("second",sB);
				waitGetS(sB);
				outS("down",sB);
				waitGetS(sB);
				outS(uuid+fileName,sB);//��ڵ�Ҫ�ļ�
				if(waitGetS(sB)){//�ҵ��˸��ļ�
					out("get");
					outS("get",sB);
					DataInputStream dis = new DataInputStream(sB.getInputStream());//�ӽڵ���
					long flength = dis.readLong();//�ӽڵ���֪���ļ���С
					DataOutputStream dos = new DataOutputStream(s.getOutputStream());//��ͻ���ȥ
					dos.writeLong(flength);//���߿ͻ���
					dos.flush();
					waitGet();//*********
					if(nowName!=null){
						out(nowName);
					}
					else{
						out(fileName);
					}
					waitGet();
					outS("get",sB);
					//��ʼ����
					byte[] inputByte = new byte[1024];     
					System.out.println("��ʼ�����ļ���"+fileName);  
					double sumL = 0;
					int length;
					try{
					while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {  
						dos.write(inputByte,0,inputByte.length);
						dos.flush();
						sumL+=length;
						System.out.println("�Ѵ��䣺"+sumL/(flength/100)+"%");
						if(sumL>=flength){
							//System.out.println("1");
							break;
						}  
					}  
					}catch(Exception e3){
						if(sB!=null){
							sB.close();
						}
						if(s!=null){
							s.close();
						}
					}
				}
				else{//û������ļ�GGGGG
					out("false");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				try{
					String BasicIP = MainServer.prop.getProperty(uuid+"basicIP");
					port = Integer.parseInt(MainServer.prop.getProperty(uuid+"basicPort"));
					Socket sB = new Socket(BasicIP,port);
					outS("second",sB);
					waitGetS(sB);
					outS("down",sB);
					waitGetS(sB);
					outS(uuid+fileName,sB);
					if(waitGetS(sB)){
						out("get");
						DataInputStream dis = new DataInputStream(sB.getInputStream());
						long flength = dis.readLong();
						DataOutputStream dos = new DataOutputStream(s.getOutputStream());
						outS("get",sB);
						dos.writeLong(flength);
						dos.flush();
						waitGet();
						if(nowName!=null){
							out(nowName);
						}
						else{
							out(fileName);
						}
						waitGet();
						outS("get",sB);
						
						byte[] inputByte = new byte[1024];     
						System.out.println("��ʼ�����ļ���"+fileName);  
						double sumL = 0;
						int length;
						try{
						while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {  
							dos.write(inputByte,0,inputByte.length);
							dos.flush();
							sumL+=length;
							System.out.println("�Ѵ��䣺"+sumL/(flength/100)+"%");
							if(sumL>=flength){
								//System.out.println("1");
								break;
							}  
						}  
						}catch(Exception e3){
							if(sB!=null){
								sB.close();
							}if(s!=null){
								s.close();
							}
						}
					}
					else{
						out("false");
					}
				}catch(Exception e2){
					out("false");
				}
				e.printStackTrace();
			}
			
			
		}//���û���ļ�ֱ�ӷ���false��
		else{
			out("false");
		}
	}
	
	public void rename() throws UnsupportedEncodingException, IOException{
		out("get");
		String fileName = in();//���fileName��UUID�����ø�����
		String oldName = null;
		if((oldName = MainServer.prop.getProperty(fileName))!=null){
			MainServer.prop.setProperty(fileName, fileName);//�Ȱ�UUID �ļ��� ��ֵ�Ը�Ϊ UUID UUID
			if(!oldName.equals(fileName)){
				MainServer.prop.setProperty(fileName+"oldName",oldName);//����ǵ�һ�θ���
			}
			out("get");
			String nowName = in();
			MainServer.prop.setProperty(fileName+"nowName", nowName);//����������
			MainServer.saveProperties();//�洢������
			out("get");
			if(s!=null){
				s.close();
			}
		}else{//�ļ������ڲ��ܸ���
			out("false");
		}
	}
	
	public void delete() throws UnsupportedEncodingException, IOException{
		out("get");
		String fileName = in();
		String uuid = fileName;
		String nowName = null;
		String oldName = null;
		if((nowName = MainServer.prop.getProperty(uuid))!=null){
			
			if(nowName.equals(uuid)){
				nowName = MainServer.prop.getProperty(uuid+"nowName");
				oldName = MainServer.prop.getProperty(uuid+"oldName");
			}//�Ƿ����
			String mainIP = MainServer.prop.getProperty(uuid+"mainIP");
			int port = Integer.parseInt(MainServer.prop.getProperty(uuid+"mainPort"));
			Socket ss = new Socket(mainIP,port);//�õ����ڵ�
			outS("second",ss);
			waitGetS(ss);
			outS("delete",ss);
			waitGetS(ss);
			String f = uuid+nowName;
			if(oldName!=null){
				f = uuid+oldName;
			}
			outS(f,ss);
			waitGetS(ss);
			if(ss!=null){
				ss.close();
			}
			//���ڵ����
			for(Node nn :MainServer.NodeList){
				if(nn.getIp().equals(ss.getInetAddress().toString().substring(1))&& nn.getPort()==ss.getPort()){
					nn.upLeftStorage(Long.parseLong(MainServer.prop.getProperty(uuid+"size")));
				}
			}
			//����һ�½ڵ������
			String basicIP = MainServer.prop.getProperty(fileName+"basicIP");//�õ����ݽڵ�
			port = Integer.parseInt(MainServer.prop.getProperty(fileName+"basicPort"));
			try{
				Socket sss = new Socket(basicIP,port);
				outS("second",sss);
				waitGetS(sss);
				outS("delete",sss);
				waitGetS(sss);
				String ff = uuid+nowName;
				if(MainServer.prop.getProperty(fileName).equals(fileName)){
					ff = uuid+MainServer.prop.getProperty(uuid+"oldName");
				}
				outS(ff,sss);
				waitGetS(sss);
				if(sss!=null){
					sss.close();
				}
				for(Node nn :MainServer.NodeList){
					if(nn.getIp().equals(sss.getInetAddress().toString().substring(1))&& nn.getPort()==sss.getPort()){
						nn.upLeftStorage(Long.parseLong(MainServer.prop.getProperty(uuid+"size")));
					}
				}
				//����������Ϣ
				MainServer.updateStorage();
				if(MainServer.prop.getProperty(uuid).equals(uuid)){//ɾ�����������ļ�
					MainServer.prop.remove(uuid+"nowName");
					MainServer.prop.remove(uuid+"oldName");
				}
									
					MainServer.prop.remove(uuid);
					MainServer.prop.remove(uuid+"mainIP");
					MainServer.prop.remove(uuid+"basicIP");
					MainServer.prop.remove(uuid+"mainPort");
					MainServer.prop.remove(uuid+"basicPort");
					MainServer.prop.remove(uuid+"size");
					MainServer.saveProperties();//���µ�����
				}catch(Exception ee){
					if(s!=null){
						s.close();
					}
				}
			out("get");
		}
		else{
			out("false");
		}
	}
	@Override
	public void run(){
		try {
			String fun = in();
			switch(fun){
			case "up":
				upload();
				break;
			case "down":
				download();
				break;
			case "rename":
				rename();
				break;
			case "delete":
				delete();
				break;
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
