package com.nyxus.socket;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class connectionManager {
	public void generateConnection(String URL,int port) throws IOException{
		Socket s = new Socket(URL, port);
		DataInputStream din = new DataInputStream(s.getInputStream());
		DataOutputStream dout = new DataOutputStream(s.getOutputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String str = "", str2 = "";
		while (!str.startsWith("stop")) {
			str = br.readLine();
			dout.writeUTF(str);
			dout.flush();
			str2 = din.readUTF();
			System.out.println("Server says: " + str2);
		}

		dout.close();
		s.close();
	}
	
//	public Socket generateSocket(String URL,int port) throws IOException{
//		
//		Socket socket=null;
//		try {
//			socket = new Socket(sc.getDestinationIP(), sc.getDestinationPort());
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	
//		sc.setSocket(socket);
//		Socket s = new Socket(URL, port);
//		DataInputStream din = new DataInputStream(s.getInputStream());
//		DataOutputStream dout = new DataOutputStream(s.getOutputStream());
//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//		return s;
//		
//		
//
//	}
}
