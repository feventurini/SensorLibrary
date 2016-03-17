package http;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class IpUtils {
	/**
	 * Questa funzione permette al server di avere sempre associato l'ip giusto
	 * nella LAN e non 127.0.0.1 come a volte restituisce
	 * {@link InetAddress#getLocalHost()}
	 *
	 * @return the first public {@link InetAddress}, or null if no one is found
	 *         or the caller doesn't have the right permissions
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public static InetAddress getCurrentIp() throws SocketException, UnknownHostException {
		InetAddress address = InetAddress.getLocalHost();
		if (!address.isLinkLocalAddress() && !address.isLoopbackAddress() && address instanceof Inet4Address)
			return address;

		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface ni = networkInterfaces.nextElement();
			Enumeration<InetAddress> nias = ni.getInetAddresses();
			while (nias.hasMoreElements()) {
				InetAddress ia = nias.nextElement();
				if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia instanceof Inet4Address)
					return ia;
			}
		}
		return null; // se non c'e un IPv4 di LAN disponibile o non si hanno i
						// permessi
	}
}
