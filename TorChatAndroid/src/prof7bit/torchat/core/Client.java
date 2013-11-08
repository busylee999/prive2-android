package prof7bit.torchat.core;

import java.io.IOException;

import prof7bit.reactor.ListenPort;
import prof7bit.reactor.ListenPortHandler;
import prof7bit.reactor.Reactor;
import prof7bit.reactor.TCP;
import prof7bit.reactor.TCPHandler;
import android.util.Log;

public class Client implements ListenPortHandler, ConnectionHandler {
	final static String LOG_TAG = "Client";
	final static String ONION_DOMAIN = ".onion";

	private ClientHandler clientHandler;
	private Reactor reactor;
	private ListenPort listenPort;

	private String mMyOnionAddress = "gnlkmtgnk134lmrw34nkrw";
	private String mMyRandomString = "213543857986565313";

	public Client(ClientHandler clientHandler, int port) throws IOException {
		this.clientHandler = clientHandler;
		this.reactor = new Reactor();
		this.listenPort = new ListenPort(reactor, this);
		this.listenPort.listen(port);
	}

	public void close() throws InterruptedException {
		this.reactor.close();
	}

	@Override
	public TCPHandler onAccept(TCP tcp) {
		Log.i(LOG_TAG, "new connection was accepted");
		Connection c = new Connection(tcp, this);
		return c;
	}

	public void startConnection(String onionAddress) throws IOException {
		Log.i(LOG_TAG, "start connection");
		Connection c;
		c = new Connection(new Reactor(), onionAddress + ONION_DOMAIN, 11009,
				this);

	}

	// TODO change logic
	@Override
	public void onPingReceived(Msg_ping msg) {
		Log.i(LOG_TAG,
				"ping " + msg.getOnionAddress() + " " + msg.getRandomString());
		clientHandler.onStartHandshake(msg.getOnionAddress(),
				msg.getRandomString());
		Connection connection = msg.getConnection();
		if (connection.type == Connection.Type.INCOMING) {
			/*
			 * if it is incoming connection handshake is starting since this
			 * moment send ping pong TODO send status, version
			 */

			// set handshake state to start
			connection.handshakeState = Connection.HandshakeState.START;

			// send message "ping"
			Msg_ping msgPing = new Msg_ping(connection);
			msgPing.setOnionAddress(mMyOnionAddress);
			msgPing.setRandomString(mMyRandomString);
			connection.sendMessage(msgPing);

			// send message 'pong"
			Msg_pong msgPong = new Msg_pong(connection);
			msgPong.setRandomString(msg.getRandomString());
			connection.sendMessage(msgPong);

		} else if (connection.type == Connection.Type.OUTCOMING) {
			/*
			 * if it is outcoming connection handshake not change need to send
			 * pong TODO may these packets is not enough
			 */

			// send "pong"
			Msg_pong msgPong = new Msg_pong(connection);
			msgPong.setRandomString(msg.getRandomString());
			connection.sendMessage(msgPong);
		} else
			Log.w(LOG_TAG, "undefined connection type");

	}

	@Override
	public void onPongReceived(Msg_pong msg) {
		Log.i(LOG_TAG, "pong " + msg.getRandomString());
		// check is random string is my random string
		if (msg.getRandomString().equals(mMyRandomString)) {
			/*
			 * if it is my string handshake is complete need to notify client
			 * handler if it is outgoing connection need to send pong
			 */
			clientHandler.onHandshakeComplete();

			Connection connection = msg.getConnection();

			// set handshake state to success
			connection.handshakeState = Connection.HandshakeState.SUCCESS;

			// if outcoming
			if (connection.type == Connection.Type.OUTCOMING) {
				// send "pong"
				Msg_pong msgPong = new Msg_pong(connection);
				msgPong.setRandomString(msg.getRandomString());
				connection.sendMessage(msgPong);
			}

		} else {
			Log.e(LOG_TAG, "string is not my string");
			clientHandler.onHandshakeAbort("string is not my string");
		}
	}

	@Override
	public void onMessageReceived(Msg_message msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDisconnect(String reason) {
		// TODO implement
	}

	/**
	 * This function will be called then connection will be established
	 * 
	 */
	@Override
	public void onConnect(Connection connection) {
		startHandshake(connection);
	}

	protected void startHandshake(Connection connection) {
		// set handshake state to start
		connection.handshakeState = Connection.HandshakeState.START;

		// send ping foor notify recepient of starting handshake
		Msg_ping msgPing = new Msg_ping(connection);
		msgPing.setOnionAddress(mMyOnionAddress);
		msgPing.setRandomString(mMyRandomString);
		connection.sendMessage(msgPing);
	}
}