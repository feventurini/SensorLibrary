import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IpUtils {
	/**
	 * Questa funzione permette al server di avere sempre associato l'ip giusto
	 * nella LAN e non 127.0.0.1 come a volte restituisce
	 * {@link InetAddress#getLocalHost()}
	 * 
	 * @return
	 * @throws SocketException 
	 */
	public static InetAddress getCurrentIp() throws SocketException {
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
			Enumeration<InetAddress> nias = ni.getInetAddresses();
			while (nias.hasMoreElements()) {
				InetAddress ia = (InetAddress) nias.nextElement();
				if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia instanceof Inet4Address) {
					return ia;
				}
			}
		}
		return null; // se non c'e un IPv4 di LAN disponibile
	}
}
