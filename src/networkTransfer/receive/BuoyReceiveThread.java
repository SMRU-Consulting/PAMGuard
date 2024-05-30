package networkTransfer.receive;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import networkTransfer.NetworkObject;
import networkTransfer.NetworkReceiverInterface;

public class BuoyReceiveThread {
	
	private String buoyId;

	public BuoyReceiveThread(String buoyId) {
		this.buoyId = buoyId;
		
	}

	public void newMessage(String topic, MqttMessage message) {
		boolean isStatus = topic.contains("status");
		if(!isStatus) {
			
			InputStream inStream = new ByteArrayInputStream(message.getPayload());
			DataInputStream dis = new DataInputStream(inStream);
			
			
			try {
				NetworkObject receivedObject = NetworkReceiverInterface.readNetworkObject(dis, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			//NetworkObject returnedData = networkDataUser.interpretData
		}
		
	}

}
