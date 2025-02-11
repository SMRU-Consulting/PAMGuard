package networkTransfer.receive;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import networkTransfer.NetworkObject;
import networkTransfer.NetworkReceiverInterface;

public class MqttReceiveThread {
	
	private String buoyId;
	private NetworkDataUser dataUser;
	private Timer heartBeatTimer;
	private boolean isAlive;

	public MqttReceiveThread(String buoyId, NetworkDataUser dataUser) {
		this.buoyId = buoyId;
		this.dataUser = dataUser;
		startHeartbeatTimer();
	}

	public void newMessage(String topic, MqttMessage message) {
		
		heartBeatTimer.cancel();
		isAlive = true;
		startHeartbeatTimer();
		
		boolean isPamData = topic.contains("pamData");
		if(isPamData) {
			
			InputStream inStream = new ByteArrayInputStream(message.getPayload());
			DataInputStream dis = new DataInputStream(inStream);
			int headInt=0;
			try {
				headInt = dis.readInt();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			if(headInt!=NetworkReceiveThread.HEADID) {
				return;
			}
			
			NetworkObject receivedObject;
			
			try {
				receivedObject = NetworkReceiverInterface.readNetworkObject(dis, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			
			
			dataUser.interpretData(receivedObject);
		}
		
	}
	
	private void startHeartbeatTimer() {
		heartBeatTimer = new Timer();
		heartBeatTimer.schedule(new HeartbeatTimerTask(), 60*1000);
	}
	
	private class HeartbeatTimerTask extends TimerTask{

		@Override
		public void run() {
			isAlive = false;
			
		}
		
	}

	public boolean isAlive() {
		return isAlive;
	}

}
