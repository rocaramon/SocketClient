package com.nyxus.socket;

import java.net.*;
import java.io.*;

class MyServer {
	public static void main(String args[]) throws Exception {
//		initServer(3333);
		initServer(3334);

		
	}
	
	
	public static void initServer(Integer port) throws IOException {
		ServerSocket ss = new ServerSocket(port);
		Socket s = ss.accept();
		DataInputStream din = new DataInputStream(s.getInputStream());
		DataOutputStream dout = new DataOutputStream(s.getOutputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String str = "", str2 = "";
		while (!str.startsWith("stop")) {
			str = din.readUTF();
			System.out.println("client says: " + str);
//			str2 = br.readLine();
			dout.writeUTF(str2);
			dout.flush();
		}
		din.close();
		s.close();
		ss.close();
		
		
	}
}