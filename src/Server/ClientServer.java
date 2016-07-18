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
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"UTF-8"));//不管三七二十一咱先把这个文件名字写了是吧
			bw.write(ss+"\n");
			bw.flush();
		}
	}
	private void outS(String ss,Socket st)throws UnsupportedEncodingException,IOException{
		if(isConnect){
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(st.getOutputStream(),"UTF-8"));//不管三七二十一咱先把这个文件名字写了是吧
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
	//以上6条跟node一毛一样
	private void upload() throws UnsupportedEncodingException, IOException{
		out("get");
		String fileName = in();
		String uuid = UUID.randomUUID().toString();
		String newFile = uuid+fileName;
		out("get");
		DataInputStream dis = new DataInputStream(s.getInputStream());
		long flength = dis.readLong();
		//文件长度
		Socket[] Nodes = MainServer.getNodes();
		//从服务器所有节点中，选择剩余容量最大的两个节点。
		if(flength<=MainServer.leftStorage){
			Socket main = Nodes[0];
			Socket basic = Nodes[1];
			outS("second",basic);
			waitGetS(basic);
			outS("up",basic);
			waitGetS(basic);
			DataOutputStream dosB = new DataOutputStream(basic.getOutputStream());
			dosB.writeLong(flength);//首先连接备份节点，备份节点能存下主节点一定能存下
			dosB.flush();
			if(waitGetS(basic)){//如果备份节点放的下，即可开始传输
				outS("second",main);
				waitGetS(main);
				outS("up",main);
				waitGetS(main);
				DataOutputStream dosA = new DataOutputStream(main.getOutputStream());
				dosA.writeLong(flength);
				dosA.flush();
				
				//传输名字
				waitGetS(main);
				outS(newFile,main);
				waitGetS(main);
				outS(newFile,basic);
				waitGetS(basic);
				
				try{
				out("get");//向客户端确认可以存储 
				byte[] inputByte = new byte[1024];     
				System.out.println("开始上传文件："+fileName);  
				double sumL = 0;
				int length;
				while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {  
					dosA.write(inputByte,0,inputByte.length);
					dosB.write(inputByte,0,inputByte.length);
					dosA.flush();
					dosB.flush();
					sumL+=length;
					System.out.println("已传输："+sumL/(flength/100)+"%");
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
				MainServer.saveProperties();//存储信息
				out(uuid);//返回uuid
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
						System.out.println("关闭main");
					}
					if(basic!=null){
						
						basic.getOutputStream().close();
						basic.close();
						System.out.println("关闭baisc");
					}
					s.close();
					System.out.println("上传失败");
				}
			}else{//如果，备份服务器都放不下
				out("false");
			}
		}
		else{//如果整个文件系统都放不下
			out("false");
		}
	}
	public void download() throws UnsupportedEncodingException, IOException{
		out("get");
		String uuid = in();
		String nowName = null;
		String fileName;
		if((fileName = MainServer.prop.getProperty(uuid)) !=null){//判断有没有这个文件
			if(fileName.equals(uuid)){//判断是否改了名
				fileName = MainServer.prop.getProperty(uuid+"oldName");
				nowName = MainServer.prop.getProperty(uuid+"nowName");
			}
			String MainIP = MainServer.prop.getProperty(uuid+"mainIP");
			int port = Integer.parseInt(MainServer.prop.getProperty(uuid+"mainPort"));
			//从本地文件中搜索
			try {
				Socket sB = new Socket(MainIP,port);
				outS("second",sB);
				waitGetS(sB);
				outS("down",sB);
				waitGetS(sB);
				outS(uuid+fileName,sB);//向节点要文件
				if(waitGetS(sB)){//找到了该文件
					out("get");
					outS("get",sB);
					DataInputStream dis = new DataInputStream(sB.getInputStream());//从节点拿
					long flength = dis.readLong();//从节点那知道文件大小
					DataOutputStream dos = new DataOutputStream(s.getOutputStream());//向客户端去
					dos.writeLong(flength);//告诉客户端
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
					//开始传输
					byte[] inputByte = new byte[1024];     
					System.out.println("开始下载文件："+fileName);  
					double sumL = 0;
					int length;
					try{
					while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {  
						dos.write(inputByte,0,inputByte.length);
						dos.flush();
						sumL+=length;
						System.out.println("已传输："+sumL/(flength/100)+"%");
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
				else{//没有这个文件GGGGG
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
						System.out.println("开始下载文件："+fileName);  
						double sumL = 0;
						int length;
						try{
						while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {  
							dos.write(inputByte,0,inputByte.length);
							dos.flush();
							sumL+=length;
							System.out.println("已传输："+sumL/(flength/100)+"%");
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
			
			
		}//如果没有文件直接返回false；
		else{
			out("false");
		}
	}
	
	public void rename() throws UnsupportedEncodingException, IOException{
		out("get");
		String fileName = in();//这个fileName是UUID我懒得改名了
		String oldName = null;
		if((oldName = MainServer.prop.getProperty(fileName))!=null){
			MainServer.prop.setProperty(fileName, fileName);//先把UUID 文件名 键值对改为 UUID UUID
			if(!oldName.equals(fileName)){
				MainServer.prop.setProperty(fileName+"oldName",oldName);//如果是第一次改名
			}
			out("get");
			String nowName = in();
			MainServer.prop.setProperty(fileName+"nowName", nowName);//设置新名字
			MainServer.saveProperties();//存储新名字
			out("get");
			if(s!=null){
				s.close();
			}
		}else{//文件不存在不能改名
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
			}//是否改名
			String mainIP = MainServer.prop.getProperty(uuid+"mainIP");
			int port = Integer.parseInt(MainServer.prop.getProperty(uuid+"mainPort"));
			Socket ss = new Socket(mainIP,port);//得到主节点
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
			//主节点结束
			for(Node nn :MainServer.NodeList){
				if(nn.getIp().equals(ss.getInetAddress().toString().substring(1))&& nn.getPort()==ss.getPort()){
					nn.upLeftStorage(Long.parseLong(MainServer.prop.getProperty(uuid+"size")));
				}
			}
			//更新一下节点的容量
			String basicIP = MainServer.prop.getProperty(fileName+"basicIP");//得到备份节点
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
				//更新容量信息
				MainServer.updateStorage();
				if(MainServer.prop.getProperty(uuid).equals(uuid)){//删除所有配置文件
					MainServer.prop.remove(uuid+"nowName");
					MainServer.prop.remove(uuid+"oldName");
				}
									
					MainServer.prop.remove(uuid);
					MainServer.prop.remove(uuid+"mainIP");
					MainServer.prop.remove(uuid+"basicIP");
					MainServer.prop.remove(uuid+"mainPort");
					MainServer.prop.remove(uuid+"basicPort");
					MainServer.prop.remove(uuid+"size");
					MainServer.saveProperties();//更新到本地
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
