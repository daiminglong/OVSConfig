import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author daiminglong
 *
 */
public class EdgeSwitch extends OVSSwitch {
	
	private Map<String, Integer> hostId2PortId;

	public EdgeSwitch(String mId, String mDpId){
		super(mId, mDpId);
		this.hostId2PortId = new HashMap<String,Integer>();
	}
	
	public void add2HostPortMap(String hostId, int portId) {
		this.hostId2PortId.put(hostId, portId);
	}
	
	public int getPortIdByHostId(String mHostId) {
		return this.hostId2PortId.get(mHostId);
	}
	
	public Map<String, Integer> getHostId2PortId() {
		return hostId2PortId;
	}

}
