@ECHO OFF
echo Starting
javac -d "bin" src/p2pBLL/ConfigManager.java

javac -d "bin" src/p2pBLL/FileUtility.java
javac -d "bin" -cp "src" src/p2pBLL/PeerHandler.java
javac -d "bin" -cp "src" src/p2pBLL/ServerHandler.java
javac -d "bin" -cp "src" src/P2PClient.java

java -cp "bin" P2PClient 3

