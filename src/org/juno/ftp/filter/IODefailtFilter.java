package org.juno.ftp.filter;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.juno.ftp.core.NioSession;
import org.juno.ftp.core.TaskResource;
import org.juno.ftp.core.WORKTYPE;
import org.juno.ftp.core.FTPServer;

public class IODefailtFilter implements ChainFilter {

	private NioSession session;

	public IODefailtFilter(NioSession session) {
		this.session = session;
	}

	@Override
	public void doFilter(TaskResource taskResource) {

		WORKTYPE workType = taskResource.getWorkType();
		List<Object> params = taskResource.getParams();
		switch (workType) {
		// TODO 这里的String处理可以抽离出来集中处理
		case LIST:

			try {
				writeString(_buildOutString(params), this.session);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case GROUP_CHAT:
			try {
				writeStringToAllSessions(_buildOutString(params), FTPServer.sessionList);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case PULL:
			try {
				sendFile(params,this.session);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
	}

	private String _buildOutString(List<Object> params) {
		StringBuilder sb = new StringBuilder();
		for (Object param : params) {
			sb.append(param.toString());
			sb.append('\r');
		}
		sb.append('\n');
		return sb.toString();
	}

	private void writeString(String str, NioSession session) throws IOException {
		SocketChannel sc = session.getSocketChannel();

		/*
		 * BufferedOutputStream buffOutPut = new BufferedOutputStream(output);
		 * buffOutPut.write(str.getBytes()); buffOutPut.flush();
		 */

		try {

			ByteBuffer byteBuffer = ByteBuffer.wrap(str.getBytes());
			// byteBuffer.flip();
			sc.write(byteBuffer);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void sendFile(List<Object> params, NioSession session) throws IOException {
		if((boolean)params.get(0)) {
			params.remove(0);
			SocketChannel socketChannel = SocketChannel.open();
	        socketChannel.configureBlocking(false);
			String clientadress= ((InetSocketAddress)session.getClientAddress()).getAddress().getHostAddress();
			InetSocketAddress inetSocketAddress = new InetSocketAddress(clientadress, 6666);			
			new Thread() {
				ByteBuffer buff=null;
				public void run() {
					try {
						while(!socketChannel.connect(inetSocketAddress)) {
							socketChannel.connect(inetSocketAddress);
						}
						buff=ByteBuffer.wrap(((String)params.get(0)).getBytes());						
						buff.flip();
						socketChannel.write(buff);						
					} catch (IOException e) {
						e.printStackTrace();
					}finally {						
							try {
								if(buff!=null)
								buff.clear();
								if(socketChannel!=null)
								socketChannel.close();								
							} catch (IOException e) {
								e.printStackTrace();
							}
					}
				}
			}.start();			
		}		
		
	}

	private void writeStringToAllSessions(String str, List<NioSession> list) throws IOException {
		for (NioSession session : list) {
			writeString(str, session);
		}
	}

}
