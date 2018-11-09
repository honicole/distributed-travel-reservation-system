package Client;

import java.net.Socket;

public class PerformanceAnalyzer extends TCPClient {

	public PerformanceAnalyzer(Socket socket) throws Exception {
		super(socket);
	}

	public static void main(String[] args) {
		
		TCPClient performanceAnalyzer = null;
		try {
			performanceAnalyzer = new PerformanceAnalyzer(null);
		} catch (Exception e) {
		   
		}
		performanceAnalyzer.runCommand();

	    // Set the security policy
	    if (System.getSecurityManager() == null) {
	      System.setSecurityManager(new SecurityManager());
	    }

	}

}
