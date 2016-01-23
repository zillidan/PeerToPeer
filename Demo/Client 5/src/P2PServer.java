import java.net.*;
import java.io.*;
import java.util.*;
import p2pBLL.*;

public class P2PServer {

	final long chunkSize = 102400;

	int _listeningPortNumber = -1;
	int _maxClientsLimit = 0;
	String _fileToSplit = "";
	ServerSocket _listener;

	public P2PServer(int portNumber, int maxClientLimit, String fileToSplit) {
		_listeningPortNumber = portNumber;
		_maxClientsLimit = maxClientLimit;
		_fileToSplit = fileToSplit;
	}

	public static void main(String args[]) {
		ConfigManager.init();
		int serverPortNumber = Integer.parseInt(ConfigManager.getProperty("ServerPortNumber"));
		int maxClients = 5;
		String fileName = ConfigManager.getProperty("FileName");

		P2PServer server = new P2PServer(serverPortNumber, maxClients, "data/" + fileName);
		server.init();
	}

	private void init() {
		try {
			_listener = new ServerSocket(_listeningPortNumber, _maxClientsLimit);
			System.out.println("The server is running.");

			System.out.println("Splitting the file");
			FileUtility fu = new FileUtility();
			int splitCount = fu.splitFile(_fileToSplit, chunkSize);
			System.out.println("File Splited in " + splitCount + " parts");

			System.out.println("Waiting for connection");
			int clientNum = 1;
			try {
				while (true) {
					new ServerHandler(_listener.accept(), clientNum, getRandomizedSequences(clientNum, splitCount),
							splitCount, _fileToSplit).start();
					System.out.println("Client " + clientNum + " is connected!");
					clientNum++;
				}

			} catch (IOException ioException) {
				ioException.printStackTrace();
			} finally {
				if (!_listener.isClosed())
					_listener.close();
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
			System.err.println("Error setting up socket " + ioException.getMessage());
		} finally {
			try {
				if (!_listener.isClosed())
					_listener.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	private int[] getRandomizedSequences(int clientNumber, int splitCount) {
		int runner = 1;

		ArrayList<Integer> al = new ArrayList<Integer>();

		for (int i = 1; i <= splitCount; i++) {
			if (runner > _maxClientsLimit)
				runner = 1;
			if (clientNumber == runner) {
				al.add(i);
			}
			runner++;
		}

		int[] sequences = new int[al.size()];

		for (int i = 0; i < al.size(); i++) {
			sequences[i] = al.get(i).intValue();
		}

		return sequences;
	}
}
