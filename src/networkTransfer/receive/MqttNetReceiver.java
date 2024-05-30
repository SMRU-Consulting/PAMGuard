package networkTransfer.receive;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;
import netTxControl_2.ClientConnectFailedException;
import networkTransfer.NetworkObject;
import networkTransfer.NetworkReceiverInterface;
import networkTransfer.mqttClient.PamMqttClient;
import nidaqdev.networkdaq.NetworkAudioInterpreter;

public class MqttNetReceiver extends PamMqttClient implements NetworkReceiverInterface,NetworkDataUser{

	private NetworkReceiveParams netParams;
	private NetworkReceiver netReceiver;
	private NetworkAudioInterpreter networkAudioInterpreter;
	private BuoyStatusDataBlock buoyStatusDataBlock;
	
	
	public MqttNetReceiver(NetworkReceiveParams netRxParams, NetworkReceiver netReceiver) {
		super(netRxParams);
		this.netParams = netRxParams;
		this.netReceiver = netReceiver;
		buoyStatusDataBlock = this.netReceiver.getBuoyStatusDataBlock();
	}

	@Override
	public void stopConnectionThread() {

		
	}

	@Override
	public int getNDataIn() {

		return 0;
	}

	@Override
	public int getNPacketsIn() {

		return 0;
	}

	@Override
	public void runReceiver() {
		this.configureClient();
		try {
			this.connect();
		} catch (ClientConnectFailedException e) {
			e.printStackTrace();
		}
		
		try {
			this.subscribeListener(netParams.baseTopic+"/#", new BaseListener());
		} catch (MqttException e) {
			e.printStackTrace();
		}
		
	}
	
	private class BaseListener implements IMqttMessageListener{

		HashMap<String,BuoyReceiveThread> buoyReceivers;
		
		public BaseListener() {
			buoyReceivers = new HashMap<String,BuoyReceiveThread>();
		}
		
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			String buoyId = topic.split("/")[1];
			if(!buoyReceivers.containsKey(buoyId)) {
				buoyReceivers.put(buoyId, new BuoyReceiveThread(buoyId));
			}
			
			buoyReceivers.get(buoyId).newMessage(topic,message);
			
			
		}
		
	}

	@Override
	public void socketClosed(NetworkReceiveThread networkReceiveThread) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public NetworkObject interpretData(NetworkObject receivedObject) {
		BuoyStatusDataUnit buoyStatusDataUnit = netReceiver.findBuoyStatusDataUnit(receivedObject.getBuoyId1(), receivedObject.getBuoyId2(), true);
		buoyStatusDataUnit.setSocket(receivedObject.getSocket());
	
		NetworkObject retObject = null;
		switch(receivedObject.getDataType1()) {
		case NetworkReceiver.NET_PAM_DATA:
			retObject = netReceiver.interpretPamData(receivedObject, buoyStatusDataUnit);
			break;
		case NetworkReceiver.NET_PAM_COMMAND:
			retObject = netReceiver.interpretPamCommand(receivedObject, buoyStatusDataUnit);
			break;
		case NetworkReceiver.NET_AUDIO_DATA:
			if (networkAudioInterpreter == null) {
				networkAudioInterpreter = new NetworkAudioInterpreter(netReceiver);
			}
			networkAudioInterpreter.interpretData(receivedObject, buoyStatusDataUnit);
		}
		for (NetworkDataUser extraUser : netReceiver.getExtraDataUsers()) {
			retObject = extraUser.interpretData(receivedObject);
		}
		buoyStatusDataBlock.updatePamData(buoyStatusDataUnit, PamCalendar.getTimeInMillis());
		return retObject;
	}

	
	@Override
	public void newReceivedDataUnit(BuoyStatusDataUnit buoyStatusDataUnit, PamDataUnit dataUnit) {
		// TODO Auto-generated method stub
		
	}

}
