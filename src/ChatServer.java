import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer {
	// A pre-allocated buffer for the received data
	static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

	// Decoder for incoming text -- assume UTF-8
	static private final Charset charset = Charset.forName("UTF8");
	static private final CharsetDecoder decoder = charset.newDecoder();
	static private final CharsetEncoder encoder = charset.newEncoder();

	// Data structures
	static private HashSet<SelectionKey> chatUsers = new HashSet<>();
	static private HashSet<Room> chatRooms = new HashSet<>();

	static public void main(String args[]) throws Exception {
		// Parse port from command line
		int port = Integer.parseInt(args[0]);

		try {
			// Instead of creating a ServerSocket, create a ServerSocketChannel
			ServerSocketChannel ssc = ServerSocketChannel.open();

			// Set it to non-blocking, so we can use select
			ssc.configureBlocking(false);

			// Get the Socket connected to this channel, and bind it to the
			// listening port
			ServerSocket ss = ssc.socket();
			InetSocketAddress isa = new InetSocketAddress(port);
			ss.bind(isa);

			// Create a new Selector for selecting
			Selector selector = Selector.open();

			// Register the ServerSocketChannel, so we can listen for incoming
			// connections
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("Listening on port " + port);

			while (true) {
				// See if we've had any activity -- either an incoming connection,
				// or incoming data on an existing connection
				int num = selector.select();

				// If we don't have any activity, loop around and wait again
				if (num == 0) {
					continue;
				}

				// Get the keys corresponding to the activity that has been
				// detected, and process them one by one
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				while (it.hasNext()) {
					// Get a key representing one of bits of I/O activity
					SelectionKey key = it.next();

					// What kind of activity is it?
					if (key.isAcceptable()) {

						// It's an incoming connection. Register this socket with
						// the Selector so we can listen for input on it
						Socket s = ss.accept();
						System.out.println("Got connection from " + s);

						// Make sure to make it non-blocking, so we can use a selector
						// on it.
						SocketChannel sc = s.getChannel();
						sc.configureBlocking(false);

						// Register it with the selector, for reading
						sc.register(selector, SelectionKey.OP_READ);

					} else if (key.isReadable()) {

						SocketChannel sc = null;

						try {

							// Connection from a new user
							if (key.attachment() == null) {
								key.attach(new User());
								chatUsers.add(key);
							}

							// It's incoming data on a connection -- process it
							sc = (SocketChannel) key.channel();
							// boolean ok = processInput(sc, selector); (broadcast version)
							boolean ok = processInput(key);

							// If the connection is dead, remove it from the selector
							// and close it
							if (!ok) {
								// Remove non active users
								removeChatUser(key);
								key.cancel();

								Socket s = null;
								try {
									s = sc.socket();
									System.out.println("Closing connection to " + s);
									s.close();
								} catch (IOException ie) {
									System.err.println("Error closing socket " + s + ": " + ie);
								}
							}

						} catch (IOException ie) {

							// On exception, remove this channel from the selector
							key.cancel();

							try {
								sc.close();
							} catch (IOException ie2) {
								System.out.println(ie2);
							}

							System.out.println("Closed " + sc);
						}
					}
				}

				// We remove the selected keys, because we've dealt with them.
				keys.clear();
			}
		} catch (IOException ie) {
			System.err.println(ie);
		}
	}

	// Just read the message from the socket and send it to stdout
	static private boolean processInput(SelectionKey key) throws IOException {
		// get the SocketChannel associated with key
		SocketChannel sc = (SocketChannel) key.channel();

		// Read the message to the buffer
		buffer.clear();
		sc.read(buffer);
		buffer.flip();

		// If no data, close the connection
		if (buffer.limit() == 0) {
			return false;
		}

		// Decode and print the message to server stdout
		String message = decoder.decode(buffer).toString();

		User user = (User) key.attachment();
		message = user.getMessage() + message;
		if (message.endsWith("\n")) {
			// \n
			// System.out.print("SERVER : " + message);
		} else {
			// < ctrl D > (press control d to use in linux shell)
			user.cleanMessage();
			user.addMsg(message);
			System.out.println("User message (buffering): " + user.getMessage());
			return true;
		}

		// possible command
		if (message.startsWith("/")) {

			String cmd = message.substring(1, message.length());
			// System.out.println("COMMAND : " + cmd);
			String[] tks = cmd.split("\\s");
			switch (tks[0]) {
				case ("nick"):
					String nick = message.substring(6);
					nick = nick.replace("\n", "");
					send_nick_command(nick, key);
					break;
				case ("join"):
					String room = message.substring(6);
					room = room.replace("\n", "");
					send_join_command(room, key);
					break;
				case ("leave"):
					send_leave_command(key);
					break;
				case ("bye"):
					send_bye_command(key);
					break;
				case ("priv"):
					String dest = tks[1];

					int l = tks[0].length() + 1 + 1 + 1 + tks[1].length(); // 3 <=> nr of space characters
					String privMessage = message.substring(l);

					// System.out.println("Private message to " + dest);
					// System.out.println(privMessage);
					privMessage.replace("\n", "");

					send_priv_command(dest, key, privMessage);

					break;
				default:
					// not a registered command or more than 1 '/' character
					// see notes in project assignment
					message = message.replace("\n", "");
					message = message.substring(1);

					message(key, message);
					break;
			}
		} else {
			// regular message
			message = message.replace("\n", "");
			message(key, message);
		}

		// clean the last message of user
		user.cleanMessage();
		return true;
	}

	static private void send_priv_command(String username, SelectionKey key, String message) throws IOException {
		// Search for user
		for (SelectionKey k : chatUsers) {
			User dest = (User) k.attachment();

			// found dest user in chatUsers
			if (dest.getNick().equals(username)) {

				User src = (User) key.attachment();

				send(k, "PRIVATE " + src.getNick() + " " + message);

				send(key, "PRIVATE " + src.getNick() + " " + message);
				return;
			}
		}

		// Send error message (dest user doesn't exist in chatUsers)

		send(key, "ERROR");
	}

	static private void send_bye_command(SelectionKey key) throws IOException {
		User user = (User) key.attachment();

		if (user.getChatState() == State.inside) {
			send_leave_command(key);
		}

		send(key, "BYE");

		chatUsers.remove(key);

		SocketChannel sc = (SocketChannel) key.channel();
		Socket s = null;

		try {
			s = sc.socket();
			s.close();
		} catch (IOException e) {
			//
			System.out.println("ERROR send_bye_command");
		}
	}

	static private void send_join_command(String chatRoom, SelectionKey key) throws IOException {
		User user = (User) key.attachment();

		if (user.getChatState() == State.init) {
			send(key, "ERROR");
			return;
		}
		Room auxRoom = user.getChatRoom();

		// check if user is in chatRoom
		if (auxRoom != null) {
			if (auxRoom.getName().compareTo(chatRoom) == 0) {
				send(key, "ERROR");
				return;
			}
		}

		Room newRoom = null;

		for (Room aux : chatRooms) {
			if (aux.getName().compareTo(chatRoom) == 0) {
				newRoom = aux;
				break;
			}
		}

		if (user.isInRoom()) {
			// leaves the previous room
			send_leave_command(key);
		}
		// room already exists
		if (newRoom != null) {
			user.setChatRoom(newRoom);
			newRoom.addUser(key);
		}
		// room doesn't exist
		else {
			newRoom = new Room(chatRoom);
			chatRooms.add(newRoom);
			user.setChatRoom(newRoom);
			newRoom.addUser(key);
		}
		send(key, "OK");
		status(newRoom, key, "JOINED " + user.getNick());
		user.setChatState(State.inside);
	}

	static private void send_leave_command(SelectionKey key) throws IOException {
		User user = (User) key.attachment();

		if (user.getChatState() != State.inside) {
			send(key, "ERROR");
			return;
		}

		Room room = user.getChatRoom();
		room.removeUser(key);
		user.setChatRoom(null);
		user.setChatState(State.outside);

		// Check if the room is empty (if it is eliminate it)
		if (room.isEmpty()) {
			chatRooms.remove(room);
		} else {
			status(room, key, "LEFT " + user.getNick());
		}
		send(key, "OK");
	}

	static private void removeChatUser(SelectionKey key) throws IOException {
		// handles the removal of user
		User user = (User) key.attachment();

		if (user.getChatState() == State.inside) {
			Room room = user.getChatRoom();
			room.removeUser(key);

			// if room is empty , delete it
			if (room.isEmpty()) {
				chatRooms.remove(room);
			} else {
				// print user left room message
				status(room, key, "LEFT " + user.getNick());

			}
		}

		chatUsers.remove(key);
	}

	static private void send_nick_command(String nick, SelectionKey key) throws IOException {
		User user = (User) key.attachment();

		// check if nick is not taken
		for (SelectionKey aux : chatUsers) {
			User auxChatUser = (User) aux.attachment();
			if (auxChatUser.getNick().compareTo(nick) == 0) {
				send(key, "ERROR");
				return;
			}
		}
		// check if is in any room
		if (user.getChatState() == State.inside) {
			String oldnick = user.getNick();
			String message = "NEWNICK" + " " + oldnick + " " + nick;
			Room room = user.getChatRoom();
			status(room, key, message);
		} else {
			user.setChatState(State.outside);
		}
		user.setNick(nick);
		send(key, "OK");
	}

	static private void message(SelectionKey key, String message) throws IOException {
		User user = (User) key.attachment();
		// check if user is in any room
		if (user.getChatState() != State.inside) {
			send(key, "ERROR");
			return;
		}
		message = "MESSAGE " + user.getNick() + " " + message;
		send_msg_to_room(user.getChatRoom(), message);
	}

	static private void send_msg_to_room(Room chatRoom, String msg) throws IOException {
		// send a message to all users in the room
		for (SelectionKey user : chatRoom.getUsers()) {
			send(user, msg);
		}
	}

	static private void send(SelectionKey key, String msg) throws IOException {
		msg = msg + "\n";
		SocketChannel socketCh = (SocketChannel) key.channel();
		socketCh.write(encoder.encode(CharBuffer.wrap(msg)));
	}

	static private void status(Room chatRoom, SelectionKey key, String msg) throws IOException {
		for (SelectionKey User : chatRoom.getUsers()) {
			if (User == key) {
				continue;
			}
			send(User, msg);
		}
	}
}
