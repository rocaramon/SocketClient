package com.nyxus.broker.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketClient {
	
	private String clientId;
	private Thread thread;
	private Socket socket;
	private ByteBuffer byteBuffer;
	private String destinationIP;
	private int destinationPort;
	//CONDICIONES NORMLAES NO NECESARIO
	//UTILIZADO POR PROSA
	private int localPort; //JSON
	private AtomicBoolean stop;
	private int retryTime; //JSON ms
	private int retryNumber; //JSON int
	private int responseTime; //JSON int


	private ClientEventManager clientEventManager;
	private ExecutorService executorService;
	
	InputStream input = null;
	OutputStream output = null;
	
	
//	private Queu
	
	public ExecutorService getExecutorService() {
		return executorService;
	}


	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}


	final Integer BYTE_BUFFER_SIZE=4096;
	
	BlockingQueue<ByteBuffer> queue = new LinkedBlockingQueue<ByteBuffer>();
//	ByteBuffer bbReader= ByteBuffer.allocate(BYTE_BUFFER_SIZE);
	ByteBuffer bbReader= ByteBuffer.allocate(BYTE_BUFFER_SIZE);

	//ENCOLAR
	public void sendData(ByteBuffer ba) {
		try {
			queue.put(ba);
		} catch (InterruptedException e) {
		System.out.println("Error en agregar a cola");
		e.printStackTrace();
		}
		//syncronuced q , bloqued q
	}
	
	
	private void mainProcess() {
		int contador=0;
		
		boolean isConnected=false;
		while(!stop.get()) {
			while(socket==null || socket.isClosed() || !socket.isConnected()) {
				initSocketCreatorAndBinding();
				
				if(!isConnected) {
					System.out.println("______________________________");
					System.out.println("Iniciando Conexion "+contador++);
					
					isConnected=initSocketConnection();
					
					if(!isConnected) {
						try {
							System.out.println("Esperando "+retryTime);
							Thread.sleep(retryTime);
						} catch (InterruptedException e) {
							System.out.println("Sleep Exception");
							e.printStackTrace();
						}
					}else {
						System.out.println("Conexion Exitosa");
						executorService.submit(()->clientEventManager.connectionReady(this));
						continue;
					}
					
				}
			}
	
			if(isConnected) {
				try {
					//Vaida si hay algo que leer en el socketr
					input = socket.getInputStream();
					output = socket.getOutputStream();
				}catch(Exception e) {
					if(!socket.isClosed()) {
						try {
							socket.close();
						} 
						catch (IOException e1) {}
					}
					clientEventManager.connectionClose(this); // SE CERRO LA CONEXION INESPERADAMENTE
				}
				
				//BLOQUE DE LECTURA
				try {
					int nBytes=input.available();
					if(nBytes > 0) {
						System.out.println("|||| Bloque de Lectura ,Existen "+nBytes+" Bytes por escribir, Si hay datos");
						//if socket has data ->thead from pool and Execute -> listener
						System.out.println("ANTES de Lectura: "+bbReader.position()+" limite: "+bbReader.limit()+"nBytes: " +nBytes);
						bbReader.put(input.readNBytes(nBytes));
						System.out.println("DESPUES de Lectura: "+bbReader.position()+" limite: "+bbReader.limit());
						bbReader.flip();
						System.out.println("FLIP de bb: "+bbReader.position()+" limite: "+bbReader.limit());
						System.out.println("|||| Fin de Bloque de Lectura |||| ");


						executorService.submit(()->clientEventManager.availableData(this,bbReader));
					}
					else {							
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
						}
//						System.out.println("No hay datos");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				//BLOQUE DE ESCRITURA
				ByteBuffer sendData = queue.peek();
				if(sendData!= null) {
					System.out.println("Mandando datos");
					byte[] arr = new byte[sendData.remaining()];
					queue.remove();
					sendData.get(arr);
					try {
						output.write(arr);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				}
			}
	}

	public void init() {
		stop = new AtomicBoolean(false);
		initThreadPool();
		initSelfThead();
	}

	private void initSelfThead() {
		socket=null;
		thread=new Thread(()->mainProcess());
		thread.start();
	}



	private void initThreadPool() {
		System.out.println("Iniciando executorService ");
		executorService = Executors.newCachedThreadPool();
		System.out.println("Exitoso executorService ");

	}

	public void initSocketCreatorAndBinding() {
		socket = new Socket();
		//binding
		if(localPort != 0) {
			try {
				System.out.println("Iniciando Binding");
				socket.bind(new InetSocketAddress((InetAddress)null,localPort));
				System.out.println("Binding Exitoso");

			} catch (IOException e) {
				System.out.println("Falla en Binding "+ e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}
	
	final private Integer WAIT_TIME =5000;
	
	public boolean initSocketConnection() {
		SocketAddress sa = new InetSocketAddress(destinationIP,destinationPort);
		try {
			socket.connect(sa,WAIT_TIME);
		}catch(SocketTimeoutException e) {
			System.out.println("Falla en conexion Timeout 5 Segundos");
			
		}catch(Exception e) {
			try {
				socket.close();
			} catch (IOException e1) {
				System.out.println("Error en Cerrar socket");
			}
			System.out.println("Falla en Conexion de socket de "+getLocalPort() +
					" a "+destinationPort);
			System.out.println("Expected true Socket closed: "+socket.isClosed());
			System.out.println(e.getLocalizedMessage());
		}
		return socket.isConnected();
	}

	
	
	public boolean isConnected() {
		return socket != null ? socket.isConnected(): false;
		
	}
	
	
	public void shutDown() throws IOException {
		stop.set(true);
		socket.close();
	}
	
	public void restart() {
		try {
			shutDown();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		init();
		
	}
	
	
	

	
	public String getClientId() {
		return clientId;
	}


	public void setClientId(String clientId) {
		this.clientId = clientId;
	}


	public Thread getThread() {
		return thread;
	}


	public void setThread(Thread thread) {
		this.thread = thread;
	}


	public Socket getSocket() {
		return socket;
	}


	public void setSocket(Socket socket) {
		this.socket = socket;
	}


	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}


	public void setByteBuffer(ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}


	public String getDestinationIP() {
		return destinationIP;
	}


	public void setDestinationIP(String destinationIP) {
		this.destinationIP = destinationIP;
	}


	public int getDestinationPort() {
		return destinationPort;
	}


	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}


	public int getLocalPort() {
		return localPort;
	}


	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}


	public AtomicBoolean getStop() {
		return stop;
	}


	public void setStop(AtomicBoolean stop) {
		this.stop = stop;
	}
	
	public ClientEventManager getClientEventManager() {
		return clientEventManager;
	}

	public void setClientEventManager(ClientEventManager clientEventManager) {
		this.clientEventManager = clientEventManager;
	}



	public int getRetryTime() {
		return retryTime;
	}


	public void setRetryTime(int retryTime) {
		this.retryTime = retryTime;
	}


	public int getRetryNumber() {
		return retryNumber;
	}


	public void setRetryNumber(int retryNumber) {
		this.retryNumber = retryNumber;
	}
	public int getResponseTime() {
		return responseTime;
	}


	public void setResponseTime(int responseTime) {
		this.responseTime = responseTime;
	}

}
