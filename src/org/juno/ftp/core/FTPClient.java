package org.juno.ftp.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.juno.ftp.com.PropertiesUtil;

public class FTPClient {

	private final String HOST; // 服务器的ip
	private final int PORT; // 服务器端口
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private BufferedOutputStream bufferedOutput;
	private Scanner scan;
	private String _inputline;
	CRLFLineReader lineReader;
	ExecutorService executor;

	public FTPClient() {
		HOST = PropertiesUtil.getProperty("ftp.server.host");
		PORT = Integer.parseInt(PropertiesUtil.getProperty("ftp.server.port"));
		executor = Executors.newCachedThreadPool();
	}

	public void connect() throws IOException {

		try {
			socket = new Socket(HOST, PORT);
			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();
			bufferedOutput = new BufferedOutputStream(outputStream);
			lineReader = new CRLFLineReader(new InputStreamReader(inputStream, "UTF-8"));
			String response = lineReader.readLine();
			System.out.println(response);

			// executor.submit(new Worker());
			Thread thread = new Thread(new Worker());
			thread.start();

			startInput();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 启动输入
	private void startInput() throws IOException {
		this.scan = new Scanner(System.in);
		while (scan.hasNext()) {
			_inputline = scan.nextLine();
			_inputline = ClientStringBuilder.stringBuilder(_inputline);
			bufferedOutput.write(_inputline.getBytes());
			bufferedOutput.flush();
			if(pullrequest(_inputline)) {
				new Thread(new pullWorker(_inputline)).start();
			}
		}
	}

	private boolean pullrequest(String request) throws IOException {
		boolean flag=false;
		if (request.startsWith("$pull ")) {
			if(lineReader.readLine().equals("true"))
			flag=true;
		}
		return flag;			
	}
	class pullWorker implements Runnable{
		InputStream in = null;
		ServerSocket serversocket = null;
		Socket socket = null;
		FileOutputStream out=null;
		String request;
		public pullWorker(String request) {
			this.request=request;
		}
		public void run() {
			try {
				serversocket = new ServerSocket(6666);
				socket = serversocket.accept();
				in = socket.getInputStream();
				File file = new File("D:\\"+request.substring(6, request.length()));
				if (!file.exists()) {
					file.createNewFile();
				}
				out = new FileOutputStream(file);
				byte[] buff = new byte[1024];
				int hasread = 0;
				String str = null;
				if ((hasread = in.read(buff)) != -1) {
					str = new String(buff, 0, hasread, "UTF-8");
				}
				out.write(str.getBytes());

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
					if (out != null) {
						out.close();
					}
					if (socket != null) {
						socket.close();
					}
					if (serversocket != null) {
						serversocket.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}		
	}

	class Worker implements Runnable {

		byte[] buff = new byte[1024];
		private String response=null;

		@Override
		public void run() {
			while (true) {
				try {
					// inputStream.read(buff);
					response = lineReader.readLine();
					System.out.println(response);
				} catch (IOException e) {

					e.printStackTrace();
				}

			}
		}

	}

}
