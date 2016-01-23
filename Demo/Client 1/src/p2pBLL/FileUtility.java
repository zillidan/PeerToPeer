package p2pBLL;

import java.io.*;

public class FileUtility {

	public long getFileSize(String filePath) {
		try {
			FileInputStream inputFile = new FileInputStream(filePath);
			long fileSize = inputFile.getChannel().size();
			inputFile.close();

			return fileSize;
		} catch (IOException ex) {
			ex.printStackTrace();
			return -1;
		}
	}

	public byte[] getFileByteStream(String filePath) {
		try {
			File f = new File(filePath);
			int retryCount = 0;
			while (true) {
				if (f.exists())
					break;
				else {
					try {
						Thread.sleep(1000);
						retryCount++;
						if (retryCount > 50) {
							break;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			FileInputStream inputFile = new FileInputStream(filePath);
			byte[] byteStream = new byte[(int) (inputFile.getChannel().size())];
			inputFile.read(byteStream);
			inputFile.close();

			return byteStream;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public void setFileByteStream(String filePath, byte[] byteStream) {
		try {
			FileOutputStream outputFile = new FileOutputStream(filePath);
			outputFile.write(byteStream);
			outputFile.flush();
			outputFile.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public int splitFile(String filePath, long chunkSize) {
		FileInputStream inputFile = null;
		FileOutputStream outputFile = null;

		try {
			inputFile = new FileInputStream(filePath);
			byte[] bytes = new byte[(int) chunkSize];
			int index = 1;
			int chunkLength = 0;

			while ((chunkLength = inputFile.read(bytes)) != -1) {
				outputFile = new FileOutputStream(filePath + "." + index);
				outputFile.write(bytes, 0, chunkLength);
				outputFile.flush();
				outputFile.close();
				index++;
			}
			inputFile.close();
			return --index;

		} catch (IOException ioException) {
			ioException.printStackTrace();
			return -1;
		}
	}

	public void mergeFile(String filePath, int splitCount) {
		FileInputStream inputFile = null;
		FileOutputStream outputFile = null;

		try {
			outputFile = new FileOutputStream(filePath);

			for (int i = 1; i <= splitCount; i++) {
				inputFile = new FileInputStream(filePath + "." + i);
				byte[] fullBuffer = new byte[(int) (inputFile.getChannel().size())];
				inputFile.read(fullBuffer);
				inputFile.close();

				outputFile.write(fullBuffer);

				// Delete the split file
				// File fileToDelete = new File(filePath + "." + i);
				// fileToDelete.delete();
			}

			outputFile.flush();
			outputFile.close();

		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

}
