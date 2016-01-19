

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.rmi.RemoteException;

import discovery.Provider;

public class TestUser {

	public static void main(String[] args) {
		int registryPort = 1099;
		String serviceName = "Server";
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				System.in));

		if (args.length < 1 || args.length > 2) {
			System.out.println("Usage: ClientRMI registryHost [registryPort]");
			System.exit(1);
		}

		String registryHost = args[0];
		if (args.length == 2) {
			try {
				registryPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out
						.println("Usage: ClientRMI registryHost [registryPort]");
				System.exit(2);
			}
			if (registryPort < 1024 || registryPort > 65535) {
				System.out.println("Port out of range");
				System.exit(3);
			}
		}

		// Connessione al servizio RMI remoto
		try {
			String completeName = "//" + registryHost + ":" + registryPort
					+ "/" + serviceName;
			Provider provider = (Provider) Naming.lookup(completeName);
			System.out.println("ClientRMI: Servizio \"" + serviceName
					+ "\" connesso");

			String richiesta;

			while (true) {
				System.out.printf("Che vuoi fare?\n1\tmetodo1\n2\tmetodo2\n^D\tper uscire\n");
				richiesta = stdIn.readLine();
				if(richiesta==null) {
					System.out.println("Ciao, alla prossima");
					break;
				} else if (richiesta.equals("1")) {
					// Lettura dell'input da stdIn
					try {
						// Invocazione di serverRMI.metodo1
						provider.find("cucina", "temperatura");
						// Stampa del risultato
					} catch (RemoteException re) {
						re.printStackTrace();
					}
				} else if (richiesta.equals("2")) {
					// Lettura dell'input da stdIn
					try {
						// Invocazione di serverRMI.metodo2
						provider.find("cucina", null);
						// Stampa del risultato
					} catch (RemoteException re) {
						re.printStackTrace();
					}
				} else
					System.out.println("Servizio non disponibile");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(4);
		}
	}
}
