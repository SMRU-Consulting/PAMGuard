package networkTransfer;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import networkTransfer.mqttClient.PamMqttClient;
import networkTransfer.send.ClientConnectFailedException;
import networkTransfer.send.NetTransmitException;
import networkTransfer.send.NetworkQueuedObject;
import networkTransfer.send.NetworkSendParams;
import networkTransfer.send.NetworkSender;
import warnings.PamWarning;
import warnings.WarningSystem;

public abstract class NetworkClient {
	
	protected NetworkParams networkParams;
	
	PamWarning sendWarning;
	
	public NetworkClient(NetworkParams netParams) {
		this.networkParams = netParams;
		sendWarning = new PamWarning("Network Send Error","Warn!",0);
	}
	
	public abstract void configureClient();

	public abstract boolean connect() throws ClientConnectFailedException;
	
	public abstract void disconnect();
	
	public abstract boolean isConnected();
	
	public abstract void sendMessage(NetworkQueuedObject qo) throws NetTransmitException;
	
	public void close() {
		//disconnect();
		additionalClose();
	}
	
	public abstract void additionalClose();

	public abstract void notifyModelChanged(int changeType);

	public abstract String getStatus();

	public abstract boolean testClient() throws ClientConnectFailedException;
	
	public SSLSocketFactory getSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
		
		
		if(this.networkParams.useSystemTrustStore) {
			System.setProperty( "Djavax.net.ssl.trustStoreType", "WINDOWS-ROOT");
			SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
			sslContext.init(null, null, new SecureRandom());
			return sslContext.getSocketFactory();
		}
		
		SSLContext context = SSLContext.getInstance("TLSv1.3");
		
		KeyManager[] keys = null;
		
		if(this.networkParams.keyStorePath!=null) {
			FileInputStream keyFile = new FileInputStream(this.networkParams.keyStorePath);
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(keyFile, this.networkParams.keyStorePassword.toCharArray());
			keyFile.close();
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, this.networkParams.keyStorePassword.toCharArray());
			keys = kmf.getKeyManagers();
		}
		

		KeyStore trustStore =  KeyStore.getInstance(KeyStore.getDefaultType());
		FileInputStream trustFile = new FileInputStream(this.networkParams.trustStorePath);
		trustStore.load(trustFile,this.networkParams.trustStorePassword.toCharArray());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);
		
		context.init(keys, tmf.getTrustManagers(), new SecureRandom());
		SSLContext.setDefault(context);
		
		return context.getSocketFactory();
	}
	
	public void setWarning(String message) {
		setWarning(message,2);
	}
	
	public void setWarning(String message, int level) {
		if(message==null) {
			WarningSystem.getWarningSystem().removeWarning(sendWarning);
		}else {
			sendWarning.setWarningMessage(message);
			sendWarning.setWarnignLevel(level);
			WarningSystem.getWarningSystem().addWarning(sendWarning);
		}
	}
	
	public void removeWarning() {
		WarningSystem.getWarningSystem().removeWarning(sendWarning);
		
	}

	public void updateParams(NetworkSendParams networkSendParams2) {
		this.networkParams = networkSendParams2;
	}
	
}
