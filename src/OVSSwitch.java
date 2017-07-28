import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author daiminglong
 *
 */
public class OVSSwitch {
	
	protected String id;
	protected String dpId;
	protected int flowEntryNum;
	protected int portNum = 0;
	
	protected Map<String, Integer> ovsId2PortId;
	

	public OVSSwitch(String mId, String mDpId) {
		this.id = mId;
		this.dpId = mDpId;
		this.flowEntryNum = 0;
		this.ovsId2PortId = new HashMap<String,Integer>();
	}
	
	public void add2SwitchPortMap(String ovsId, int portId) {
		this.ovsId2PortId.put(ovsId, portId);
		this.portNum++;
	}
	
	public int getPortIdByOVSId(String mOVSId) {
		return this.ovsId2PortId.get(mOVSId);
	}
	
	public int getFowEntryNum() {
		this.flowEntryNum++;
		return flowEntryNum;
	}

	public void setFowEntryNum(int fowEntryNum) {
		this.flowEntryNum = fowEntryNum;
	}

	public int getPortNum() {
		return portNum;
	}

	public void setPortNum(int portNum) {
		this.portNum = portNum;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDpId() {
		return dpId;
	}

	public void setDpId(String dpId) {
		this.dpId = dpId;
	}
	
	public Map<String, Integer> getOvsId2PortId() {
		return ovsId2PortId;
	}
	
	public int getTotalFlowEntryNum() {
		return flowEntryNum;
	}

}
