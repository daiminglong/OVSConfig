
/**
 * 
 * @author daiminglong
 *
 */
public class Host {
	private String id;
	private String IP;
	private String Mac;
	
	public Host(String mId, String mIP, String mMac) {
		this.id = mId;
		this.IP = mIP;
		this.Mac = mMac;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getIP() {
		return IP;
	}
	public void setIP(String iP) {
		IP = iP;
	}
	
	public String getMac() {
		return Mac;	
	}
	
	public void setMac(String mac) {
		Mac = mac;
	}
}
