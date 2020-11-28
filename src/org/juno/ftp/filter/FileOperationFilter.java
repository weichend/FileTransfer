package org.juno.ftp.filter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.juno.ftp.core.NioSession;
import org.juno.ftp.core.TaskResource;
import org.juno.ftp.core.WORKTYPE;
import org.juno.ftp.log.LogUtil;

public class FileOperationFilter implements ChainFilter {

	private NioSession session;

	public FileOperationFilter(NioSession session) {
		this.session = session;
	}

	// 获取目录内容
	private String[] listFiles(String path) {
		File file = new File(path);
		if (!file.exists() || !file.canRead()) {

			LogUtil.info("The request folder dosenot exist");
			throw new RuntimeException("The request folder dosenot exist");

		}
		return file.list();
	}

	private boolean isExist(String filepath) {
		File file = new File(filepath);
		boolean isexist = false;
		if (!file.exists() || !file.canRead()) {
			LogUtil.info("The request file dosenot exist");
			throw new RuntimeException("The request file dosenot exist");

		} else {
			isexist = true;
		}
		return isexist;

	}

	@Override
	public void doFilter(TaskResource taskResource) {

		WORKTYPE workType = taskResource.getWorkType();

		switch (workType) {
		case LIST:
			String path = (String) taskResource.getParams().get(0);
			String[] fileNames = listFiles(path);
			taskResource.getParams().clear();
			for (String fileName : fileNames) {
				taskResource.getParams().add(fileName);
			}
		break;
		case PULL:
			String filepath = (String) taskResource.getParams().get(0);
			if (isExist(filepath)) {
				SocketChannel socketChannel = session.getSocketChannel();
				String rep = "true";
				ByteBuffer buffer = ByteBuffer.wrap(rep.getBytes());
				try {
					socketChannel.write(buffer);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (socketChannel != null)
							socketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				BufferedInputStream in = null;
				try {
					File file = new File(filepath);
					in = new BufferedInputStream(new FileInputStream(file));
					byte[] buff = new byte[1024];
					int hasread = 0;
					String str = null;
					taskResource.getParams().clear();
					taskResource.getParams().add(true);
					while ((hasread = in.read(buff)) != -1) {
						str = new String(buff, 0, hasread, "UTF-8");						
					}
					taskResource.getParams().add(str);
				} catch (IOException e) {
					e.printStackTrace();

				} finally {
					try {
						if (in != null)
							in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			} else {
				SocketChannel socketChannel = session.getSocketChannel();
				String rep = "The file does not exist;";
				ByteBuffer buffer = ByteBuffer.wrap(rep.getBytes());
				try {
					socketChannel.write(buffer);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (socketChannel != null)
							socketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				taskResource.getParams().clear();
				taskResource.getParams().add(false);
			}
			break;
		}
	}

}
