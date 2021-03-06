package com.cavan.app;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import com.cavan.java.CavanJava;
import com.cavan.java.CavanString;
import com.cavan.java.RedPacketFinder;
import com.cavan.java.TcpConnector;

public class RedPacketListener extends TcpConnector implements ClipboardOwner {

	private static final String UDP_HOSTNAME = "224.0.0.1";
	private static final int UDP_PORT = 9898;

	private MulticastSocket mUdpSocket;
	private InetSocketAddress mUdpAddress = new InetSocketAddress(UDP_HOSTNAME, UDP_PORT);

	private String mClipboardText;

	public RedPacketListener(String hostname, int port) {
		super(hostname, port);

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		if (clipboard != null) {
			clipboard.setContents(clipboard.getContents(null), this);
		}
	}

	@Override
	protected boolean onDataReceived(byte[] bytes, int length) {
		CavanJava.dLog("onDataReceived: " + new String(bytes, 0, length));
		return true;
	}

	public boolean sendTcpData(byte[] bytes, int length) {
		if (mOutStream == null) {
			return false;
		}

		try {
			mOutStream.write(bytes, 0, length);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean sendTcpData(byte[] bytes) {
		return sendTcpData(bytes, bytes.length);
	}

	public boolean sendUdpData(byte[] bytes, int length) {
		if (mUdpSocket == null) {
			try {
				mUdpSocket = new MulticastSocket();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		try {
			DatagramPacket packet = new DatagramPacket(bytes, length, mUdpAddress);
			mUdpSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean sendUdpData(byte[] bytes) {
		return sendUdpData(bytes, bytes.length);
	}

	public void sendRedPacketCode(String code) {
		CavanJava.dLog("sendRedPacketCode: " + code);

		String command = "RedPacketCode: " + code + "\n";
		byte[] bytes = command.getBytes();

		sendUdpData(bytes, bytes.length - 1);
		sendUdpData(bytes, bytes.length - 1);
		sendTcpData(bytes, bytes.length);
		sendUdpData(bytes, bytes.length - 1);
		sendUdpData(bytes, bytes.length - 1);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			try {
				if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
					String text = (String) clipboard.getData(DataFlavor.stringFlavor);
					if (text != null) {
						clipboard.setContents(new StringSelection(text), this);

						if (!text.equals(mClipboardText)) {
							mClipboardText = text;
							CavanJava.dLog("clipboard = " + text);

							RedPacketFinder finder = new RedPacketFinder();
							finder.split(text);

							for (String code : finder.getRedPacketCodes()) {
								sendRedPacketCode(code);
							}
						}
					}
				} else {
					clipboard.setContents(clipboard.getContents(null), this);
				}

				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		int port = 8864;
		String hostname = "192.168.88.88";

		if (args.length > 1) {
			hostname = args[0];
			port = Integer.parseInt(args[1]);
		} else if (args.length > 0) {
			String[] texts = args[0].split(":");
			if (texts.length > 1) {
				hostname = texts[0];
				port = Integer.parseInt(texts[1]);
			} else {
				hostname = args[0];
			}
		}

		CavanJava.dLog("hostname = " + hostname + ", port = " + port);

		RedPacketListener listener = new RedPacketListener(hostname, port);
		listener.start();

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.print("> ");
			System.out.flush();

			try {
				String line = reader.readLine();
				if (line == null) {
					break;
				}

				String code = line.replaceAll("\\W", CavanString.EMPTY_STRING);
				if (line.length() > 0) {
					if (code.equals("keeplive")) {
						listener.sendTcpData("CavanKeepAlive\n".getBytes());
					} else {
						if (code.equals("test")) {
							code = "CavanNetworkTest";
						}

						listener.sendRedPacketCode(code);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
