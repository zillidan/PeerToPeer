package p2pBLL;

import java.io.*;
import java.net.*;

public class ServerHandler extends Thread {

	private Socket _connection;
	private ObjectInputStream _in;
	private ObjectOutputStream _out;
	private int _clientNumber;
	private int[] _randomizedSequenceList;
	private int _splitCount;
	private String _filePath;

	public ServerHandler(Socket connection, int clientNumber, int[] randomizedSequenceList, int splitCount, String filePath) {
		this._connection = connection;
		this._clientNumber = clientNumber;
		this._randomizedSequenceList = randomizedSequenceList;
		this._splitCount = splitCount;
		this._filePath = filePath;
	}

	@Override
	public void run() {
		try {
			_out = new ObjectOutputStream(_connection.getOutputStream());
			_in = new ObjectInputStream(_connection.getInputStream());

			int currentSeqNumber = 0;
			int index = 0;
			FileUtility fu = new FileUtility();

			_out.writeObject(_splitCount);
			_out.flush();
			_out.writeObject(_randomizedSequenceList);
			_out.flush();

			while (index < _randomizedSequenceList.length) {
				currentSeqNumber = _randomizedSequenceList[index];
				_out.writeObject(currentSeqNumber);
				_out.flush();
				_out.writeObject(_filePath.substring(_filePath.lastIndexOf('/') + 1));
				_out.flush();
				_out.writeObject(fu.getFileByteStream(_filePath + "." + currentSeqNumber));
				_out.flush();
				System.out.println("Sent file chunk no " + currentSeqNumber + " to client number " + _clientNumber);
				index++;
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
			System.out.println("Disconnect with Client " + _clientNumber);
		} finally {
			try {
				_in.close();
				_out.close();
				_connection.close();
			} catch (IOException ioException) {
				System.out.println("Disconnect with Client " + _clientNumber);
			}
		}
	}
}
