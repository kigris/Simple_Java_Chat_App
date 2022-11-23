import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Stack;

// Clase que se utiliza para agrupar mensaje, ip origen y puerto origen
class Message {
	// Campos privados
	InetAddress address;
	Integer port;
	String message;

	Message(InetAddress address_in, Integer port_in, String message_in) {
		address = address_in;
		port = port_in;
		message = message_in;
	}

	// Métodos para obtener los campos privados
	public InetAddress getAddress() {
		return address;
	}

	public Integer getPort() {
		return port;
	}

	public String getMessage() {
		return message;
	}

}

// Hilo de red que se utiliza para establecer una conexión con el cliente
class NetworkThread implements Runnable {
	// Campos privados
	Thread t;
	Socket socket; // Campo para la referencia del socket establecido
	Stack<Message> msgs; // Campo para la referencia del Stack de mensajes
	BufferedReader input; // Campo para el buffer lector
	PrintWriter output; // Campo para el buffer escritor
	ArrayList<NetworkThread> threads;// Referencia a todos los hilos activos
	ArrayList<Socket> connections; // Referencia a todas las conexiones activas

	NetworkThread(Socket socket_in, Stack<Message> msgs_in, ArrayList<NetworkThread> threads_in,
			ArrayList<Socket> connections_in) {
		t = new Thread(this);
		socket = socket_in;
		msgs = msgs_in;
		threads = threads_in;
		connections = connections_in;

		try { // Se intenta crear los bufferes
				// Se inicializan los bufferes lectores y escritores
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) { // Se captura la excepcion
			System.out.println(e);
		}
		t.start(); // Se inicia el hilo
	}

	public void join() {
		try {
			t.join();
		} catch (InterruptedException e) {
			System.out.println(e);
		}
	}

	// Método para enviar un mensaje al cliente
	public void sendMessage(String msg) {
		output.println(msg); // Se envía el mensaje a través del buffer de escritura
	}

	public void run() {
		System.out.println("Nueva conexión de " + socket.getInetAddress() + ":" + socket.getPort());
		try { // Se intenta leer del bufer de lectura
			String msgToSend;
			while (true) {
				String message = input.readLine(); // Se espera una línea enviada del cliente
				if (message == null)
					break; // Si es nula, se sale del bucle
				msgToSend = socket.getInetAddress() + ":"+ socket.getPort() + " - " + message;
				// Cuando se obtiene un mensaje se apila en el Stack de mensajes
				msgs.push(new Message(socket.getInetAddress(), socket.getPort(), msgToSend));
				System.out.println(msgToSend); // Se imprime el mensaje del cliente
			}

		} // Se captura la exepcion
		catch (IOException e) {
			System.out.println(e);
		}
		try { // Se intenta cerrar la conexión del cliente
				// Llegados aquí significa que el cliente ha cerrado la conexión o ha ocurrido
				// una excepción
			socket.close(); // Se cierra la conexión
			System.out.println("Conexión cerrada para el cliente " + socket.getInetAddress() + ":" + socket.getPort());
		} // Se captura una posible excepción
		catch (IOException e) {
			System.out.println(e);
		} finally { // Finalmente
			connections.remove(socket); // Se elimina el socket de la lista de conexiones
			threads.remove(this); // Se elimina el hilo de la lista de hilos
		}
	}
}

// Hilo que se usa para gestionar las notificaciones
class NotifyThread implements Runnable {
	Thread t;
	ArrayList<NetworkThread> threads;// Referencia a todos los hilos activos
	ArrayList<Socket> connections; // Referencia a todas las conexiones activas
	Stack<Message> msgs; // Referencia al Stack de mensajes

	NotifyThread(ArrayList<NetworkThread> threads_in, ArrayList<Socket> connections_in, Stack<Message> msgs_in) {
		t = new Thread(this);
		threads = threads_in; // Se obtiene la referencia a los hilos activos
		connections = connections_in; // Se obtiene la referencia a las conexiones activas
		msgs = msgs_in; // Se obtiene el Stack de mensajes
		t.start(); // Se inicia el hilo
	}

	public void run() {
		Message msg; // Mensaje a obtener
		while (true) { // Se ejecuta siempre
			if (!msgs.empty()) { // Si hay mensajes
				msg = msgs.pop(); // Se obtiene el último mensaje
				for (int i = 0; i < connections.size(); i++) { // Se itera sobre todas las conexiones
					// Si no se trata del mismo cliente que lo envió
					if (msg.getAddress() != connections.get(i).getInetAddress() &&
							msg.getPort() != connections.get(i).getPort()) {
						// Se envía el mensaje al cliente de la iteración actual
						threads.get(i).sendMessage(msg.getMessage());
					}
				}
			}
		}
	}
}

public class serverChat {
	public static void main(String args[]) {
		ArrayList<NetworkThread> threads = new ArrayList<>(); // Hilos de conexiones activas
		ArrayList<Socket> connections = new ArrayList<>(); // Conexiones activas
		ServerSocket server_socket; // El socket del servidor
		int port = 1500; // Puerto
		Stack<Message> msgs = new Stack<>(); // Pila de mensajes
		try { // Se intenta aceptar una conexión de un cliente
			server_socket = new ServerSocket(port); // Se inicia el socket del servidor
			System.out.println("Servidor escuchando en el puerto " + server_socket.getLocalPort());
			new NotifyThread(threads, connections, msgs); // Se crea el hilo de las notificaciones
			while (true) { // Bucle infinito del servidor
				Socket socket = server_socket.accept(); // Se obtiene la conexión con el cliente
				connections.add(socket); // Se añade la conexión en la lista de conexiones
				// Se crea y añade un hilo en la lista de hilos activos
				threads.add(new NetworkThread(socket, msgs, threads, connections));
			}
		} catch (IOException e) { // Se captura una posible excepción
			System.out.println(e);
		} finally {
			for (NetworkThread thread : threads) {
				thread.join();
			}
		}
	}
}