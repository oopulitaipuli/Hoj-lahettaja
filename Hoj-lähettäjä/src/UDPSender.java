import java.io.*;
import java.net.*;

public class UDPSender{
	private static int[] sum;				//Array for saving sums
	private static int[] count;				//Array for saving counts
	public static final int PORT = 3126;	//UDP target port
	private static int sumPort = 6000;		//Starting value of ports for adders
	
	public static void main(String[] args) throws Exception {
		System.out.println("Enter TCP port");											//
		BufferedReader TCPPort = new BufferedReader(new InputStreamReader(System.in));	//Read user input
		String port = TCPPort.readLine();												//
		int tcpPort = Integer.parseInt(port);				//TCP port for communication between main and server
	
		InetAddress targetAddr = InetAddress.getLocalHost();	//Target address
		int targetPort=PORT;
		DatagramSocket socket = new DatagramSocket();
		byte[] data = port.getBytes();
		DatagramPacket packet = new DatagramPacket(data,data.length,targetAddr,targetPort);
		socket.send(packet);							//Send TCP port number
		socket.close();
	
		ServerSocket ss = new ServerSocket(tcpPort);
		ss.setSoTimeout(5000);
		int t = 0; //Value received from server
		Socket cs = connectTCP(ss);
		if (cs == null){							//If there was no TCP connection
			System.out.println("No connection");
			ss.close();
			System.exit(-1);
		}
		cs.setSoTimeout(5000);
		InputStream is = cs.getInputStream();
		OutputStream os = cs.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		ObjectInputStream ois = new ObjectInputStream(is);
		try{
			t = ois.readInt();						//Receive number of adders
			System.out.println(t);
		} catch (SocketTimeoutException e){			//If no number received within 5 seconds, send -1
			oos.writeInt(-1);
			oos.flush();
		}
		sum = new int[t];							//Set size of sum array
		count = new int[t];							//Set size of count array
		for (int i = 0; i < t; i++){
			oos.writeInt(sumPort + i);					//Ports for adders
			oos.flush();								//Send the ports
			new Adder(sumPort + i, i, sum, count).start();	//Start adders
		}
		cs.setSoTimeout(60000);
		try{
			while(true){						//Keep streams open until 0 received from server
				int input = ois.readInt();
				if(input == 0){					//If 0 sent by server, start closing streams and socket
					break;
				} else if (input == 1){			//If 1 sent by server(sum of all adders)...
					int totalSum = 0;
					for (int i = 0; i < t; i++){
						totalSum += sum[i];		//...calculate total sum of values in each index
					}
					oos.writeInt(totalSum);
					oos.flush();
				} else if (input == 2){			//If 2 sent by server(which adder has biggest sum...
					int biggest = 0;
					for (int i = 1; i < t; i++){
						if (sum[i] > sum[biggest]){	//...compare all values in sum array..
							biggest = i;			//...and get index of the biggest one
						}
					}
					oos.writeInt(biggest+1);		//Add one to index to get ordinal number of corresponding thread
					oos.flush();
				} else if (input == 3){				//If 3 sent by server(Total count of all numbers sent)...
					int totalCount = 0;
					for (int i = 0; i < t; i++){
						totalCount += count[i];		//...calculate total of values in each index
					}
					oos.writeInt(totalCount);
					oos.flush();
				} else {
					oos.writeInt(-1);
					oos.flush();
				}
			}
		}catch (SocketTimeoutException e){  //If no query within a minute, close app
			System.out.println("Timed out");
			close(oos, ois, os, is, cs);
		}
		close(oos, ois, os, is, cs);
		
	} //main
		
		public static Socket connectTCP(ServerSocket ss){
			for(int i = 1; i <= 5; i++){					//5 connection attempts
				try{
					Socket s=ss.accept();
					return s;								//If connection is made, return the socket
				} catch (SocketTimeoutException e){
					System.out.println("Connection attempt number " + i + " failed");
				} catch (IOException e){
					System.out.println("Connection failed");
				}
			}
			return null;									//If fifth attempt fails, return null
		} //connectTCP
		
		public static void close(ObjectOutputStream oos, ObjectInputStream ois, OutputStream os, InputStream is, Socket s){
			try{
				oos.close();
				ois.close();
				os.close();
				is.close();
				s.close();
			} catch (IOException e){}
		} //close
		
		static class Adder extends Thread{
			public final int sumPort;						//Port for adder
			private int num;								//Index for saving values in array
			private int input;								//Number sent by the server
			private int[] sumResult;						//Array for saving sum
			private int[] countResult;						//Array for saving count
			
			public Adder(int t, int i, int[] a, int[] b){
				sumPort = t;
				num = i;
				sumResult = a;
				countResult = b;
			}
			
			@Override
			public void run(){
				try{
					ServerSocket ss = new ServerSocket(sumPort);
					Socket s = ss.accept();
					InputStream i = s.getInputStream();
					ObjectInputStream oi = new ObjectInputStream(i);
					
					while(true){							//Run until 0 received from server
						input = oi.readInt();				//Read number sent by the server
						if (input == 0){
							break;							//If server sends 0, start closing thread
						} else{
							sumResult[num] += input;		//Add number sent by the server to the array in the index specified for the thread
							countResult[num] ++;			//Increase count by one in the specified index
						}
						
					}
					oi.close();					//
					i.close();					//
					s.close();					//
					ss.close();					//End thread
				} catch (IOException e){}
			}
		} //Adder
		
		
		
	}