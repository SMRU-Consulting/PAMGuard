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
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import netTxControl_2.ClientConnectFailedException;
import netTxControl_2.NetTransmitException;
import netTxControl_2.NetworkQueuedObject;
import netTxControl_2.NetworkSendParams;
import networkTransfer.NetworkClient;
import networkTransfer.NetworkParams;
import networkTransfer.receive.NetworkReceiveParams;

public class PamMqttClient extends NetworkClient  implements MqttCallback{
	
	protected MqttAsyncClient mqttClient;
	private MqttConnectOptions mqttOptions;
	private String stationId;
	private String status;
	private String mqttConfigureError;
	public NetworkSendParams networkSendParams;
	public NetworkReceiveParams networkReceiveParams;
	
	

	public PamMqttClient(NetworkParams networkParams){
		super(networkParams);
		if(networkParams instanceof NetworkSendParams) {
			this.networkSendParams = (NetworkSendParams) networkParams;
			stationId = "pb"+networkSendParams.stationId1;
		}else {
			this.networkReceiveParams = (NetworkReceiveParams) networkParams;
			stationId = networkReceiveParams.stationName;
		}
		configureClient();
	}
	
	@Override
	public void configureClient() {
		mqttConfigureError = null;
		this.setWarning("Attempting initial configure...",1);
		if(this.mqttClient!=null) {
			try {
				this.mqttClient.close(true);
			} catch (MqttException e) {
				e.printStackTrace();
				this.setWarning("Couldn't override existing client: "+e.getMessage());
			}
		}
		try {
			if(this.networkParams.useSSL) {
				mqttClient = new MqttAsyncClient("ssl://"+this.networkParams.ipAddress+":"+this.networkParams.portNumber,stationId,new MemoryPersistence());
			}else {
				mqttClient = new MqttAsyncClient("tcp://"+this.networkParams.ipAddress+":"+this.networkParams.portNumber,stationId,new MemoryPersistence());

			}
		} catch (MqttException e) {
			e.printStackTrace();
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
		if(mqttConfigureError!=null) {
			configureClient();
			if(mqttConfigureError!=null) {
				return false;
			}
		}
		
		IMqttToken connectToken;
		
		try {
			connectToken = mqttClient.connect(mqttOptions);
		} catch (MqttSecurityException e1) {
			e1.printStackTrace();
			throw new ClientConnectFailedException(e1);
		} catch (MqttException e1) {
			e1.printStackTrace();
			throw new ClientConnectFailedException(e1);
		}
		
		try {
			connectToken.waitForCompletion(10000);
		} catch (MqttException e) {
			e.printStackTrace();
			String readableReason = "";
			if(e.getCause()==null) {
				throw new ClientConnectFailedException(e.getMessage(),e);
			}
			if(e.getCause().getMessage().equals("readHandshakeRecord")) {
				readableReason = "Broker requires certificate. Include keystore in parameters";
			}else if(e.getCause().getMessage().contains("unable to find valid certification path to requested target")) {
				readableReason = "Designated trust store does not include the required CA certificate for connection";
			}else {
				readableReason = e.getMessage();
			}
			throw new ClientConnectFailedException(readableReason,e);
		}
		
		if(!mqttClient.isConnected()) {
			Exception reason = connectToken.getException();
			Throwable cause = reason.getCause();
			String message = reason.getMessage();
			throw new ClientConnectFailedException(message);
		}

		return true;
	}

	@Override
	public void disconnect() {
		if(mqttClient==null) {
			return;
		}
		try {
			IMqttToken disconnectToken = mqttClient.disconnect();
			//disconnectToken.wait();
			//mqttClient.close();
		} catch (MqttException e) {
			e.printStackTrace();
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
	public void sendMessage(NetworkQueuedObject qo) throws NetTransmitException{
		if(this.mqttClient==null) {
			throw new NetTransmitException("Mqtt client is not initialized",new NullPointerException());
		}
		if(!this.mqttClient.isConnected()) {
			
			throw new NetTransmitException("Mqtt client is not connected", null);
			
		}
		try {
			MqttMessage message;
			if(qo.data!=null) {
				message = new MqttMessage(qo.data);
			}else {
				message = new MqttMessage(qo.jsonString.getBytes());
			}
			String type = qo.streamName;
			mqttClient.publish("APS/"+stationId+"/"+type, message);
		} catch (MqttException e) {
			throw new NetTransmitException(e);
		} 

	}

	@Override
	public void additionalClose() {
		try {
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
		status = "Mqtt lost connection. Cause: "+cause.getMessage();
		
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

	public void sendMessage(String topicExtension, String string) {
		MqttMessage message = new MqttMessage(string.getBytes());

		try {
			mqttClient.publish("APS/"+stationId+"/"+topicExtension, message);
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	
	@Override
	public boolean testClient() throws ClientConnectFailedException {
		sendMessage("test","Pamguard client test at "+Instant.now().toString());
		return true;
	}

	public static void test(NetworkParams networkParams) throws ClientConnectFailedException {
		PamMqttClient testClient = new PamMqttClient(networkParams);
		testClient.connect();
		testClient.testClient();
		testClient.disconnect();
		testClient.close();
		
	}

}
