
import java.net.*;
import java.io.*;
import java.util.*;
import p2pBLL.*;

public class P2PClient {

	final String localhostIP = "127.0.0.1";
	final long chunkSize = 102400;

	int _serverPortNumber = -1;
	int _peerServerPortNumber = -1;
	int _peerClientPortNumber = -1;
	Socket _clientSocket;
	ServerSocket _peerUploadSocketListener;
	Socket _peerDownloadSocket;
	Hashtable<Integer, Boolean> _downloadedSequenceList;

	public P2PClient(int serverPortNumber, int uploadPeerPortNumber, int downloadPeerPortNumber) {
		_serverPortNumber = serverPortNumber;
		_peerServerPortNumber = uploadPeerPortNumber;
		_peerClientPortNumber = downloadPeerPortNumber;
		_downloadedSequenceList = new Hashtable<Integer, Boolean>();
	}

	public static void main(String args[]) {
		ConfigManager.init();

		String clientNumber = args[0];

		int serverPortNumber = Integer.parseInt(ConfigManager.getProperty("ServerPortNumber"));
		int peerListenerPortNumber = Integer.parseInt(ConfigManager.getProperty("PeerListenerPortClient" + clientNumber));
		int peerClientPortNumber = Integer.parseInt(ConfigManager.getProperty("PeerClientPortClient" + clientNumber));

		P2PClient client = new P2PClient(serverPortNumber, peerListenerPortNumber, peerClientPortNumber);
		client.init();
	}

	void init() {
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		FileUtility fu = new FileUtility();
		String fileName = "";
		int splitCount = 0;

		// Take data from server

		try {
			_clientSocket = new Socket(localhostIP, _serverPortNumber);
			System.out.println("Connected to localhost in port " + _serverPortNumber);

			out = new ObjectOutputStream(_clientSocket.getOutputStream());
			in = new ObjectInputStream(_clientSocket.getInputStream());
			splitCount = (int) in.readObject();
			int[] mySequenceList = (int[]) in.readObject();

			for (int i = 0; i < mySequenceList.length; i++) {
				int currentSequenceNumber = (int) in.readObject();
				fileName = (String) in.readObject();
				byte[] byteStream = (byte[]) in.readObject();

				fu.setFileByteStream("data/" + fileName + "." + currentSequenceNumber, byteStream);
				_downloadedSequenceList.put(currentSequenceNumber, true);
				PrintWriter chunkListFile = new PrintWriter(new FileWriter("Chunk List.txt", true));
				chunkListFile.println(currentSequenceNumber);
				chunkListFile.flush();
				chunkListFile.close();
				System.out.println("Received file chunk with ID " + currentSequenceNumber + " from server");
			}

		} catch (ConnectException e) {
			System.out.println("Connection refused. You need to initiate a server first.");
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found");
		} catch (UnknownHostException unknownHost) {
			System.out.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			try {
				in.close();
				out.close();
				_clientSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		// Now make it a peer server
		Thread peerServerThread = new Thread() {
			@Override
			public void run() {
				try {
					_peerUploadSocketListener = new ServerSocket(_peerServerPortNumber, 1);
					System.out.println("This peer is now hosting its set of files.");

					System.out.println("Waiting for connection");

					PeerHandler peerAsServer = new PeerHandler(_peerUploadSocketListener.accept(),
							_downloadedSequenceList);
					peerAsServer.start();
					System.out.println("A peer client is connected!");

				} catch (IOException ioException) {
					ioException.printStackTrace();
					System.err.println("Error setting up socket " + ioException.getMessage());
				} finally {
					try {
						if (_peerUploadSocketListener != null && !_peerUploadSocketListener.isClosed())
							_peerUploadSocketListener.close();
					} catch (IOException ioException) {
						ioException.printStackTrace();
					}
				}
			}
		};

		peerServerThread.start();

		// Now make it a client
		try {
			boolean retry = true;
			int retryCount = 1;
			while (retry) {
				try {
					_peerDownloadSocket = new Socket(localhostIP, _peerClientPortNumber);
					break;
				} catch (ConnectException e) {
					System.out.println("Connection refused. Peer at port " + _peerClientPortNumber
							+ " acting as peer server not initiated. Retrying...");
					retryCount++;
					if (retryCount > 300) {
						retry = false;
						break;
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}

			if (_peerDownloadSocket == null || !_peerDownloadSocket.isConnected()) {
				System.out.println("Could not conect to peer at port " + _peerClientPortNumber);
				return;
			}

			System.out.println("Connected to peer at port " + _peerClientPortNumber);

			out = new ObjectOutputStream(_peerDownloadSocket.getOutputStream());
			in = new ObjectInputStream(_peerDownloadSocket.getInputStream());

			Hashtable<Integer, Boolean> allSequences = new Hashtable<Integer, Boolean>();
			boolean isSequenceAvailable = false;

			for (int i = 1; i <= splitCount; i++) {
				allSequences.put(i, true);
			}

			out.writeObject(splitCount);
			out.flush();
			out.writeObject(fileName);
			out.flush();

			while (true) {
				for (int key : allSequences.keySet()) {
					if (_downloadedSequenceList.containsKey(key))
						continue;
					// System.out.println("Check if peer server has chunk ID " +
					// key);
					out.writeObject(key);
					out.flush();
					isSequenceAvailable = (boolean) in.readObject();
					if (!isSequenceAvailable) {
						// System.out.println("Chunk ID " + key + " is not
						// available now, I will retry again");
						continue;
					}
					byte[] byteStream = (byte[]) in.readObject();

					fu.setFileByteStream("data/" + fileName + "." + key, byteStream);
					_downloadedSequenceList.put(key, true);
					PrintWriter chunkListFile = new PrintWriter(new FileWriter("Chunk List.txt", true));
					chunkListFile.println(key);
					chunkListFile.flush();
					chunkListFile.close();
					System.out.println(
							"Received file chunk with sequence " + key + " from peer " + _peerClientPortNumber);
				}

				if (_downloadedSequenceList.size() == splitCount)
					break;
			}

			// Merge
			System.out.println("Merging files");
			fu.mergeFile("data/" + fileName, splitCount);

			System.out.println("Task completed");
			System.in.read();

		} catch (ClassNotFoundException e) {
			System.out.println("Class not found");
		} catch (UnknownHostException unknownHost) {
			System.out.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
				if (_peerDownloadSocket != null && !_peerDownloadSocket.isClosed())
					_peerDownloadSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
}
