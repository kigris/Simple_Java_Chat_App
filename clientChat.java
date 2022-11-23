import java.net.*;
import java.io.*;

// Hilo que escucha por los mensajes entrantes
class ListenThread implements Runnable {
	BufferedReader inputFromServer; // Referencia al buffer del servidor
	Thread t;

	public ListenThread(BufferedReader reader) {
		t = new Thread(this);
		inputFromServer = reader; // Se obtiene el buffer del servidor
		t.start(); // Se inicia el hilo
	}

	public void run() {
		try { // Se intenta leer del servidor
			// Mientras el buffer esté listo
			while (inputFromServer.ready()) {
				// Se intenta leer del buffer del servidor
				String message = inputFromServer.readLine();
				if (message != null) { // Si hay mensaje que envió
					System.out.println(message); // Se imprime
				}
			}
		} catch (IOException e) { // Se capturan excepciones
			System.out.println(e);
		}
	}

	public void stop(){
		try{
			inputFromServer.close();
		} catch(Exception e) {
			System.out.println(e);
		}
		Thread.currentThread().interrupt();;
	}
}

public class clientChat {
	public static void main(String[] args) {
		int port = 1500;
		ListenThread listenThread;
		String server = "localhost";
		Socket socket = null;
		String msgToSend; // mensaje a ser enviado
		BufferedReader input; // buffer lector
		PrintWriter output; // buffer escritor
		int ERROR = 1;

		// Se leen argumentos
		if (args.length == 2) { // Si hay dos argumentos
			server = args[0]; // El primero es la dirección del servidor
			try { // Se intenta obtener el puerto con el segundo argumento
				port = Integer.parseInt(args[1]); // Se convierte a entero
			} catch (Exception e) { // Se captura excepción
				System.out.println("Error al intentar obtener el puerto");
				System.out.println("Se ha establecido por defecto el puerto 1500");
			}
		}

		try { // Se intenta conectar con el servidor
			System.out.print("...Estableciendo conexión con el servidor...");
			socket = new Socket(server, port);
			System.out.println("Conectado con el servidor " +
					socket.getInetAddress() +
					":" + socket.getPort());
		} catch (UnknownHostException e) { // Se captura excepción de host desconocido
			System.out.println(e);
			System.exit(ERROR);
		} catch (IOException e) { // Se captura excepción de IO
			System.out.println(e);
			System.exit(ERROR);
		}

		try { // Se intentan abrir los buffers
				// Se abre un buffer de lectura de la consola del cliente
			InputStreamReader inputStreamer = new InputStreamReader(System.in);
			input = new BufferedReader(inputStreamer);
			// Se abre un buffer de escritura contra el servidor
			output = new PrintWriter(socket.getOutputStream(), true);
			// Se abre un buffer de lectura del servidor, para los mensajes entrantes
			// Se crea el hilo de escucha con un buffer de lectura del servidor, para
			// mensajes entrantes
			listenThread = new ListenThread(new BufferedReader(new InputStreamReader(socket.getInputStream())));
			// Se obtienen mensajes del cliente y se transmiten al servidor
			while (true) {
				msgToSend = input.readLine(); // Se obtiene el mensaje a enviar
				if(msgToSend == null){
					// Se detiene el hilo de escucha
					listenThread.stop();
					// Se sale del bucle
					break;					
				}
				output.println(msgToSend); // Se envía el mensaje al servidor
			}
		} catch (IOException e) { // Se captura excepción de IO
			System.out.println(e);
		}
		try { // Se intenta cerrar la conexión
			socket.close();
			System.out.println("Conexión cerrada con éxito!");
		} catch (IOException e) { // Se captura excepción de IO
			System.out.println(e);
		}
	}
}
