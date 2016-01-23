package p2pBLL;

import java.io.*;
import java.net.*;
import java.util.*;

public class PeerHandler extends Thread {

	private String _fileName;
	private Socket _connection;
	private Hashtable<Integer, Boolean> _peerServerSequenceList;

	public PeerHandler(Socket connection, Hashtable<Integer, Boolean> peerServerSequenceList) {
		this._connection = connection;
		this._peerServerSequenceList = peerServerSequenceList;
	}

	@Override
	public void run() {
		ObjectInputStream _in = null;
		ObjectOutputStream _out = null;
		FileUtility fu = new FileUtility();
		int clientSequenceNumberToBeUploaded = 0;

		try {
			_out = new ObjectOutputStream(_connection.getOutputStream());
			_in = new ObjectInputStream(_connection.getInputStream());

			@SuppressWarnings("unused")
			int splitCount = (int) _in.readObject();
			_fileName = (String) _in.readObject();

			while (true) {
				try {
					clientSequenceNumberToBeUploaded = (int) _in.readObject();
				} catch (EOFException ex) { // Client has got all the files
											// and has disconnected
					break;
				}
				if (_peerServerSequenceList.containsKey(clientSequenceNumberToBeUploaded)) {
					_out.writeObject(true);
					_out.flush();
					_out.writeObject(
							fu.getFileByteStream("data/" + _fileName + "." + clientSequenceNumberToBeUploaded));
					_out.flush();
					System.out.println("Uploaded chunk ID " + clientSequenceNumberToBeUploaded + " to connected peer.");
				} else {
					_out.writeObject(false);
					_out.flush();
				}
			}

		} catch (IOException ioException) {
			ioException.printStackTrace();
			System.out.println("Disconnect with Client with port.");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				_in.close();
				_out.close();
				_connection.close();
			} catch (IOException ioException) {
				System.out.println("Disconnect with peer Client.");
			}
		}
	}
}
