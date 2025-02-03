package networkTransfer.mqttClient;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import networkTransfer.NetworkClient;
import networkTransfer.NetworkParams;
import networkTransfer.receive.NetworkReceiveParams;
import networkTransfer.send.ClientConnectFailedException;
import networkTransfer.send.NetTransmitException;
import networkTransfer.send.NetworkQueuedObject;
import networkTransfer.send.NetworkSendParams;

public class PamMqttClient extends NetworkClient  implements MqttCallback{
	
	protected MqttAsyncClient mqttClient;
	private MqttConnectOptions mqttOptions;
	private String stationId;
	private String mqttConnectionId;
	private String status;
	private String mqttConfigureError;
	public NetworkSendParams networkSendParams;
	public NetworkReceiveParams networkReceiveParams;
	private IMqttToken connectToken;
	private MqttClientPersistence persistence;
	private String serverURI;
	
	private int preconnectCounter = 0;


	public PamMqttClient(NetworkParams networkParams){
		super(networkParams);
		if(networkParams instanceof NetworkSendParams) {
			this.networkSendParams = (NetworkSendParams) networkParams;
			stationId = "pb"+networkSendParams.stationId1;
			mqttConnectionId = this.stationId+"PAM";
		}else {
			this.networkReceiveParams = (NetworkReceiveParams) networkParams;
			stationId = networkReceiveParams.stationName;
			mqttConnectionId = this.stationId;
		}
		requireReconnect = false;
		configureClient();
	}
	
	@Override
	public void configureClient() {
		mqttConfigureError = null;
		this.setWarning("Attempting initial configure...",1);
		if(this.mqttClient!=null) {
			if(connectToken!=null && !connectToken.isComplete()) {
				System.out.println("Must wait for previous instance to finish connecting before connecting again.");
				return;
			}
			try {
				this.mqttClient.close(true);
			} catch (MqttException e) {
				e.printStackTrace();
				this.setWarning("Couldn't override existing client: "+e.getMessage());
			}
		}
		
		if(this.networkParams.useSSL) {
			serverURI = "ssl://"+this.networkParams.ipAddress+":"+this.networkParams.portNumber;
		}else {
			serverURI = "tcp://"+this.networkParams.ipAddress+":"+this.networkParams.portNumber;
		}
        if(this.networkParams.persistenceDirectory!=null) {
        	persistence = new MqttDefaultFilePersistence(this.networkParams.persistenceDirectory);
        }else {
        	persistence = new MemoryPersistence();
        }
		try {
			mqttClient = new MqttAsyncClient(serverURI,mqttConnectionId,persistence);
		} catch (MqttException e) {
			System.out.println("Caught MQTT Exception attempting to initialize client. Error message: "+e.getMessage());
			mqttConfigureError = e.getMessage();
		}
		mqttOptions = new MqttConnectOptions();
		mqttOptions.setAutomaticReconnect(true);
		mqttOptions.setCleanSession(false);
		mqttOptions.setConnectionTimeout(0);
		mqttOptions.setMaxInflight(200);
		if(this.networkParams.userId!=null&&this.networkParams.password!=null&&!this.networkParams.userId.isEmpty()&&!this.networkParams.password.isEmpty()) {
			mqttOptions.setUserName(this.networkParams.userId);
			mqttOptions.setPassword(this.networkParams.password.toCharArray());

		}
		mqttClient.setCallback(this);
		if(this.networkParams.useSSL) {
			try {
				mqttOptions.setSocketFactory(this.getSSLSocketFactory());
			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | UnrecoverableKeyException e) {
				e.printStackTrace();
				mqttConfigureError = e.getMessage();
			}
		}
		
		if(mqttConfigureError!=null) {
			this.setWarning(mqttConfigureError);
		}else {
			this.removeWarning();
		}
		
	}

	@Override
	public boolean connect() throws ClientConnectFailedException{
		if(connectToken!=null && !connectToken.isComplete()) {
			System.out.println("Must wait for previous instance to finish connecting");
			return false;
		}
		
		if(mqttConfigureError!=null) {
			configureClient();
			if(mqttConfigureError!=null) {
				return false;
			}
		}
		
		try {
			persistence.open(mqttConnectionId, serverURI);
			connectToken = mqttClient.connect(mqttOptions);
		} catch (MqttSecurityException e1) {
			e1.printStackTrace();
			throw new ClientConnectFailedException(e1);
		} catch (MqttException e1) {
			e1.printStackTrace();
			throw new ClientConnectFailedException(e1);
		}

		return true;
	}

	@Override
	public void disconnect() {
		if(mqttClient==null) {
			return;
		}
		if(!this.mqttClient.isConnected()) {
			return;
		}
		try {
			IMqttToken disconnectToken = mqttClient.disconnect();
			disconnectToken.waitForCompletion(1000L);
		} catch (MqttException e) {
			try {
				if(this.mqttClient.isConnected()) {
					System.out.println("Timeout disconnecting client from broker, 1 second. Going to attempt forceful disconnection. Error: "+e.getMessage());
					mqttClient.disconnectForcibly();
				}
			} catch (MqttException e1) {
				System.out.println("Error disconnecting client forcibly. Going to proceed to closing. Error: "+e1.getMessage());
			}
		}
		try {
			mqttClient.close(true);
		} catch (MqttException e) {
			System.out.println("Mqtt client could not close. Pamguard will not function properly. Error: "+e.getMessage());
		}
	}

	@Override
	public boolean isConnected() {
		if(mqttClient==null) {
			return false;
		}
		return mqttClient.isConnected();
	}

	@Override
	public void sendNetworkQueuedObject(NetworkQueuedObject qo) throws NetTransmitException{
		
		MqttMessage message;
		if(qo.data!=null) {
			message = new MqttMessage(qo.data);
			message.setQos(1);
		}else {
			message = new MqttMessage(qo.jsonString.getBytes());
			message.setQos(1);
		}
		String type = qo.streamName;
		
		this.sendMqttMessage(type, message);

	}

	@Override
	public void additionalClose() {
		try {
			//persistence.close();
			this.mqttClient.close(true);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void notifyModelChanged(int changeType) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void connectionLost(Throwable cause) {
		status = "Disconnected";
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub
		
	}
	
	public void subscribeListener(String topic, IMqttMessageListener listener) throws MqttException {
		mqttClient.subscribe(topic, 0, listener);
	}

	public void sendStringMessage(String topicExtension, String string) throws NetTransmitException {
		MqttMessage message = new MqttMessage(string.getBytes());
		
		sendMqttMessage(topicExtension,message);
	}
	
	public void sendMqttMessage(String topicExtension, MqttMessage message) throws NetTransmitException {
		
		boolean persistenceOpened = true;;
		
		if(this.mqttClient==null) {
			throw new NetTransmitException("Mqtt client is not initialized",new NullPointerException());
		}
		
		if(this.mqttClient.isConnected()) {
			status = "Connected";
			this.removeWarning();
		}else if(this.connectToken==null) {
			status = "Disconnected";
			this.setWarning("Mqtt client is not connected with unknown cause. Messages will buffer until connection is obtained.", 1);
		}else {
			if(this.connectToken.getException()!=null) {
				status = "Disconnected";
				Exception connectException = this.connectToken.getException();
				requireReconnect = true;
				try {
					this.persistence.open(this.mqttConnectionId, this.serverURI);
					this.setWarning(connectException.getCause().getMessage()+". Messages will buffer.", 1);
				} catch (MqttPersistenceException e) {
					persistenceOpened = false;
					this.setWarning(connectException.getCause().getMessage()+". Messages will not buffer.", 2);
				}
				
			}else {
				status = "Disconnected";
				this.setWarning("Client is not connected to broker. Messages will buffer until it is.", 1);
			}
		}
		
		String topic = "APS/"+stationId+"/"+topicExtension;
		
		/*if(requireReconnect && persistenceOpened) {
			int keyIdx = 0;
				try {
				while(this.persistence.keys().hasMoreElements()) {
					String key = (String) this.persistence.keys().nextElement();
					keyIdx = Integer.valueOf(key.split("-")[1]);
				}
				String key = "s-"+(keyIdx+1);
				MqttPublish persistableMessage =  new MqttPublish(topic, message);
				this.persistence.put(key, persistableMessage);
				mqttClient.com
			} catch (MqttPersistenceException e) {
				System.out.println("Attempted to commit message to persistence directory, but failed. Error: "+e.getMessage());
			}
		}else {*/
			try {
				mqttClient.publish(topic,message.getPayload(),1,false);
			} catch (MqttException e) {
				throw new NetTransmitException(e);
			} 
		//}
	}
	
	@Override
	public boolean testClient() throws ClientConnectFailedException {
		if(this.mqttClient==null) {
			throw new ClientConnectFailedException("Could not establish client, client is null");
		}
		if(this.connectToken!=null) {
			try {
				this.setWarning("Waiting for client connection. Timeout after 10 seconds",1);
				this.connectToken.waitForCompletion(10000L);
			} catch (MqttException e) {
				this.setWarning("Client test failed. "+e.getMessage(),2);
				throw new ClientConnectFailedException("Could not connect to server after 10 seconds. Error: "+e.getMessage());
			}
		}
		if(this.mqttClient.isConnected()) {
			this.removeWarning();
			try {
				sendStringMessage("test","Pamguard client test at "+Instant.now().toString());
			} catch (NetTransmitException e) {
				throw new ClientConnectFailedException(e.getMessage());
			}
			return true;
		}
		throw new ClientConnectFailedException("Could not connect to server. Check your server parameters and your network connection.");
	}

	public static void test(NetworkParams networkParams) throws ClientConnectFailedException {
		PamMqttClient testClient = new PamMqttClient(networkParams);
		testClient.connect();
		testClient.testClient();
		testClient.disconnect();
		testClient.close();
		
	}

}
