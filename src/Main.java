import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.attribute.standard.PrinterMessageFromOperator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.omg.CORBA.SystemException;
import org.omg.CosNaming.NamingContextExtPackage.AddressHelper;

import net.sf.json.*;
import Decoder.BASE64Encoder;

/**
 *  
 * CopyRight daiminglong, this code used to config OpenV Switch's flow-table.
 * all OpenV Switchs are controlled by Floodlight SDN Controller.
 * put flow-entry (according to path.txt) & clear flow-entry included already. 
 * @since 2017-1-10
 * @author daiminglong
 * 
 */


public class Main {
	
	public static HashMap<String, Host> hostMap;
	public static HashMap<String, EdgeSwitch> edgeSwitchMap;
	public static HashMap<String, OVSSwitch> ovsSwitchMap;
	public static List<Edge> edgeList;
	
	public static String PREFIX_URL = "http://192.168.0.233:50000/";
	//public static String ADD_FLOWENTRY_REST = "wm/staticflowentrypusher/json";
	public static String ADD_FLOWENTRY_REST = "wm/staticflowpusher/json";
	public static String CLEAR_FLOWENTRY_REST = "wm/staticflowentrypusher/clear/00:00:08:00:27:14:0a:8f/json";
	public static String DEL_FLOWENTRY_REST = "wm/staticflowentrypusher/json";
	
	
	/**
	 * function used to initial the network
	 * @param hostFilePath
	 * @param switchFilePath
	 * @param topoFilePath
	 */
	public static void initNet(String hostFilePath, String switchFilePath, String topoFilePath) {
		
		hostMap = new HashMap<>();
		edgeSwitchMap = new HashMap<>();
		ovsSwitchMap = new HashMap<>();
		edgeList = new ArrayList<>();
		
		try {
			addHost(hostFilePath);
			addSwitch(switchFilePath);
			readTopo(topoFilePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * function used to add Topo's hosts
	 * @param hostFilePath
	 * @throws IOException
	 */
	public static void addHost(String hostFilePath)throws IOException {
		
		String encoding = "GBK";
        File file = new File(hostFilePath);
       
        //read the host.txt file
        if(file.exists()) {
        	InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);
        	BufferedReader bufferedReader = new BufferedReader(read);
        	String lineTxt = null;
        	while((lineTxt = bufferedReader.readLine())!=null) {
        		String[] splitResult = lineTxt.split("-");
        		Host currentHost = new Host(splitResult[0],splitResult[1],splitResult[2]);
        		hostMap.put(currentHost.getId(), currentHost);
        	}
        }
	}
	
	/**
	 * function used to add Topo's switchs
	 * @param switchHostPath
	 * @throws IOException
	 */
	public static void addSwitch(String switchFilePath)throws IOException{
		
		String encoding = "GBK";
        File file = new File(switchFilePath);
       
        //read the switch.txt file
        if(file.exists()) {
        	InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);
        	BufferedReader bufferedReader = new BufferedReader(read);
        	String lineTxt = null;
        	while((lineTxt = bufferedReader.readLine())!=null) {
        		String[] splitResult = lineTxt.split("-");
        		OVSSwitch currentSwitch = new OVSSwitch(splitResult[0],splitResult[1]);
        		ovsSwitchMap.put(currentSwitch.getId(), currentSwitch);
        	}
        }
	}
	
	/**
	 * function used to read and init the topo
	 * @param topoFilePath
	 * @throws IOException
	 */
	public static void readTopo(String topoFilePath)throws IOException{
		String encoding = "GBK";
        File file = new File(topoFilePath);
       
        //read the topo.txt file
        if(file.exists()) {
        	InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);
        	BufferedReader bufferedReader = new BufferedReader(read);
        	String lineTxt = null;
        	while((lineTxt = bufferedReader.readLine())!=null) {
        		String[] splitResult = lineTxt.split(",");
        		Edge currentEdge = new Edge(splitResult[0], splitResult[1], Integer.parseInt(splitResult[2]), Integer.parseInt(splitResult[3]));
        		edgeList.add(currentEdge);
        	}
        	
        	for(Edge e : edgeList) {
        		
        		//add edge firstNode's port Map
        		if(hostMap.containsKey(e.firstNode)) {
        			if(ovsSwitchMap.containsKey(e.secondNode)) {
        				//the secondNode is a EdgeSwitch, so add a 2HostPort to it & add this secondNode to edgeSwitchMap
        				EdgeSwitch currentEdgeSwitch = new EdgeSwitch(e.secondNode, ovsSwitchMap.get(e.secondNode).getDpId());
        				currentEdgeSwitch.add2HostPortMap(e.firstNode, e.secondNodePort);
        				Map<String, Integer> m = ovsSwitchMap.get(currentEdgeSwitch.getId()).getOvsId2PortId();
        				for(String s : m.keySet()) {
        					currentEdgeSwitch.add2SwitchPortMap(s, m.get(s));
        				}
        				edgeSwitchMap.put(currentEdgeSwitch.getId(), currentEdgeSwitch);      				
        				ovsSwitchMap.remove(currentEdgeSwitch.getId());
        			}else if(edgeSwitchMap.containsKey(e.secondNode)) {
        				edgeSwitchMap.get(e.secondNode).add2HostPortMap(e.firstNode, e.secondNodePort);
        			}else {
        				//fault
        			}
        		}else if(ovsSwitchMap.containsKey(e.firstNode)) {
        			if(ovsSwitchMap.containsKey(e.secondNode)) {
        				//these 2 node are regular OVSSwitch, add 2SwitchPort to these 2 nodes!
        				ovsSwitchMap.get(e.firstNode).add2SwitchPortMap(e.secondNode, e.firstNodePort);
        				ovsSwitchMap.get(e.secondNode).add2SwitchPortMap(e.firstNode, e.secondNodePort);
        			}else if(edgeSwitchMap.containsKey(e.secondNode)) {
        				ovsSwitchMap.get(e.firstNode).add2SwitchPortMap(e.secondNode, e.firstNodePort);
        				edgeSwitchMap.get(e.secondNode).add2SwitchPortMap(e.firstNode, e.secondNodePort);
        			}else if(hostMap.containsKey(e.secondNode)) {
        				//the secondNode is a EdgeSwitch, so add a 2HostPort to it & add this secondNode to edgeSwitchMap
        				EdgeSwitch currentEdgeSwitch = new EdgeSwitch(e.firstNode, ovsSwitchMap.get(e.firstNode).getDpId());
        				currentEdgeSwitch.add2HostPortMap(e.secondNode, e.firstNodePort);
        				Map<String, Integer> m = ovsSwitchMap.get(currentEdgeSwitch.getId()).getOvsId2PortId();
        				for(String s : m.keySet()) {
        					currentEdgeSwitch.add2SwitchPortMap(s, m.get(s));
        				}
        				edgeSwitchMap.put(currentEdgeSwitch.getId(), currentEdgeSwitch);      				
        				ovsSwitchMap.remove(currentEdgeSwitch.getId());
        			}else {
        				//fault
        			}
        		}else if(edgeSwitchMap.containsKey(e.firstNode)) {//edgeSwitchMap contain the first node
        			if(ovsSwitchMap.containsKey(e.secondNode)) {
        				//these 2 node are regular OVSSwitch, add 2SwitchPort to these 2 nodes!
        				edgeSwitchMap.get(e.firstNode).add2SwitchPortMap(e.secondNode, e.firstNodePort);
        				ovsSwitchMap.get(e.secondNode).add2SwitchPortMap(e.firstNode, e.secondNodePort);
        			}else if(edgeSwitchMap.containsKey(e.secondNode)) {
        				edgeSwitchMap.get(e.firstNode).add2SwitchPortMap(e.secondNode, e.firstNodePort);
        				edgeSwitchMap.get(e.secondNode).add2SwitchPortMap(e.firstNode, e.secondNodePort);
        			}else if(hostMap.containsKey(e.secondNode)) {
        				//the secondNode is a EdgeSwitch, so add a 2HostPort to it & add this secondNode to edgeSwitchMap
        				edgeSwitchMap.get(e.firstNode).add2HostPortMap(e.secondNode, e.firstNodePort);
        			}else {
        				//fault
        			}
        		}else {
        			//fault
        		}
        		
        	}
        }
        
	}
	
	/**
	 * function used to put flow-entry according to path.txt
	 * @param readTime
	 * @throws IOException
	 */
	public static void setFlowEntry(int readTime, int pathFileNum)throws IOException {
		
		String encoding="GBK";
        File file=new File("src/path/path" + pathFileNum + ".txt");
       
        InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);
        BufferedReader bufferedReader = new BufferedReader(read);
        String host1,host2;
        String port1,port2;
        List<Integer> switchIdList = new ArrayList();
        
        String hostTxt = null;
        String pathTxt = null;
        for(int i=0; i<readTime; i++) {
        	switchIdList.clear();
        	hostTxt = bufferedReader.readLine();
        	pathTxt = bufferedReader.readLine();
        	String[] hostArray = hostTxt.split(",");
        	host1 = hostArray[0];
        	host2 = hostArray[1];
        	port1 = hostArray[2];
        	port2 = hostArray[3];
        	String[] pathNodeArray = pathTxt.split("-");
        	
        	if(hostMap.containsKey(host1) && hostMap.containsKey(host2)) {
        		Map<String,String> map = new HashMap<String,String>();
        		for(int j=1; j<pathNodeArray.length-1; j++) {
            		if(j==1) {
            			if(edgeSwitchMap.containsKey(pathNodeArray[j])) {
            				EdgeSwitch es = edgeSwitchMap.get(pathNodeArray[j]);
            				map.put("switch", es.getDpId());  
            	            map.put("name", "flow-mod-" + es.getDpId() + "-" + String.valueOf(es.getFowEntryNum()));  
            	            map.put("cookie", "0");  
            	            map.put("priority", "1");
            	            map.put("eth_type", "0x0800");
            	            map.put("ipv4_src", hostMap.get(host1).getIP());
            	            map.put("ipv4_dst", hostMap.get(host2).getIP());
            	            map.put("ip_proto", "0x06");
            	            //map.put("tcp_src", port1);
            	            map.put("tcp_dst", port2);
            	            //map.put("src-mac", hostMap.get(host1).getMac());
            	            //map.put("dst-mac", hostMap.get(host2).getMac());
            	            //map.put("src-ip", hostMap.get(host1).getIP());
            	            //map.put("dst-ip", hostMap.get(host2).getIP());
            	            //map.put("ingress-port", "" + es.getPortIdByHostId(host1)); 
            	            map.put("active", "true");
            	            if(host2!=pathNodeArray[j]) {
               	            	map.put("actions", "output=" + es.getPortIdByOVSId(pathNodeArray[j+1]));
            	            }else {
            	            	map.put("actions","output=" + es.getPortIdByHostId(host2));
            	            }
            	             
            	            sendAddFlowEntryRequest(PREFIX_URL+ADD_FLOWENTRY_REST,map);
            	            /*JSONObject json = JSONObject.fromObject(map);  
                            System.out.println(json.toString());
                            System.out.println("-------------------");*/
    
                            map.clear();
    
            	            map.put("switch", es.getDpId());  
            	            map.put("name", "flow-mod-" + es.getDpId() + "-" + String.valueOf(es.getFowEntryNum()));  
            	            map.put("cookie", "0");  
            	            map.put("priority", "1");
            	            map.put("eth_type", "0x0800");
            	            map.put("ipv4_src", hostMap.get(host2).getIP());
            	            map.put("ipv4_dst", hostMap.get(host1).getIP());
            	            map.put("ip_proto", "0x06");
            	            //map.put("tcp_src", port2);
            	            //map.put("tcp_dst", port1);
//            	            map.put("src-ip", hostMap.get(host2).getIP());
//            	            map.put("dst-ip", hostMap.get(host1).getIP());
//            	            if(host2!=pathNodeArray[j]) {
//            	            	map.put("ingress-port", "" + es.getPortIdByOVSId(pathNodeArray[j+1]));
//            	            }else {
//            	            	map.put("ingress-port", "" + es.getPortIdByHostId(host2));
//            	            }
            	            //map.put("src-mac", hostMap.get(host2).getMac());
            	            //map.put("dst-mac", hostMap.get(host1).getMac());
            	            map.put("active", "true");
            	            map.put("actions", "output=" + es.getPortIdByHostId(host1));
            	            
            	            /*JSONObject json2 = JSONObject.fromObject(map);  
                            System.out.println(json2.toString());
                            System.out.println("-------------------");*/
                            
                            sendAddFlowEntryRequest(PREFIX_URL+ADD_FLOWENTRY_REST,map);
            			}
            		}else if(j==pathNodeArray.length-2) {
            			if(edgeSwitchMap.containsKey(pathNodeArray[j])) {
            				EdgeSwitch es = edgeSwitchMap.get(pathNodeArray[j]);
            				map.put("switch", es.getDpId());  
            	            map.put("name", "flow-mod-" + es.getDpId() + "-" + String.valueOf(es.getFowEntryNum()));  
            	            map.put("cookie", "0");  
            	            map.put("priority", "1");
            	            map.put("eth_type", "0x0800");
            	            map.put("ipv4_src", hostMap.get(host1).getIP());
            	            map.put("ipv4_dst", hostMap.get(host2).getIP());
            	            map.put("ip_proto", "0x06");
            	            //map.put("tcp_src", port1);
            	            map.put("tcp_dst", port2);
//            	            map.put("src-ip", hostMap.get(host1).getIP());
//            	            map.put("dst-ip", hostMap.get(host2).getIP());
            	            //map.put("src-mac", hostMap.get(host1).getMac());
            	            //map.put("dst-mac", hostMap.get(host2).getMac());
//            	            map.put("ingress-port", "" + es.getPortIdByOVSId(pathNodeArray[j-1])); 
            	            map.put("active", "true");
            	            map.put("actions","output=" + es.getPortIdByHostId(host2));
            	             
            	            sendAddFlowEntryRequest(PREFIX_URL+ADD_FLOWENTRY_REST,map);
            	            /*JSONObject json = JSONObject.fromObject(map);  
                            System.out.println(json.toString());
                            System.out.println("-------------------");*/
    
                            map.clear();
    
            	            map.put("switch", es.getDpId());  
            	            map.put("name", "flow-mod-" + es.getDpId() + "-" + String.valueOf(es.getFowEntryNum()));  
            	            map.put("cookie", "0");  
            	            map.put("priority", "1");
            	            map.put("eth_type", "0x0800");
            	            map.put("ipv4_src", hostMap.get(host2).getIP());
            	            map.put("ipv4_dst", hostMap.get(host1).getIP());
            	            map.put("ip_proto", "0x06");
            	            //map.put("tcp_src", port2);
            	            //map.put("tcp_dst", port1);
//            	            map.put("src-ip", hostMap.get(host2).getIP());
//            	            map.put("dst-ip", hostMap.get(host1).getIP());
            	            //map.put("src-mac", hostMap.get(host2).getMac());
            	            //map.put("dst-mac", hostMap.get(host1).getMac());
//            	            map.put("ingress-port", "" + es.getPortIdByHostId(host2));
            	            map.put("active", "true");
            	            map.put("actions", "output=" + es.getPortIdByOVSId(pathNodeArray[j-1]));
            	            
            	            /*JSONObject json2 = JSONObject.fromObject(map);  
                            System.out.println(json2.toString());
                            System.out.println("-------------------");*/
            	            
            	            sendAddFlowEntryRequest(PREFIX_URL+ADD_FLOWENTRY_REST,map);
            			}
            		}else {
            				OVSSwitch os = null;
            				if(ovsSwitchMap.containsKey(pathNodeArray[j])) {
            					os = ovsSwitchMap.get(pathNodeArray[j]);
            				}else if(edgeSwitchMap.containsKey(pathNodeArray[j])) {
            					os = edgeSwitchMap.get(pathNodeArray[j]);
            				}
            				
            				map.put("switch", os.getDpId());  
            	            map.put("name", "flow-mod-" + os.getDpId() + "-" + String.valueOf(os.getFowEntryNum()));  
            	            map.put("cookie", "0");  
            	            map.put("priority", "1");
            	            map.put("eth_type", "0x0800");
            	            map.put("ipv4_src", hostMap.get(host1).getIP());
            	            map.put("ipv4_dst", hostMap.get(host2).getIP());
            	            map.put("ip_proto", "0x06");
            	            //map.put("tcp_src", port1);
            	            map.put("tcp_dst", port2);
//            	            map.put("src-ip", hostMap.get(host1).getIP());
//            	            map.put("dst-ip", hostMap.get(host2).getIP());
            	            //map.put("src-mac", hostMap.get(host1).getMac());
            	            //map.put("dst-mac", hostMap.get(host2).getMac());
//            	            map.put("ingress-port", "" + os.getPortIdByOVSId(pathNodeArray[j-1])); 
            	            map.put("active", "true");
            	            map.put("actions", "output=" + os.getPortIdByOVSId(pathNodeArray[j+1]));
            	             
            	            sendAddFlowEntryRequest(PREFIX_URL+ADD_FLOWENTRY_REST,map);
            	            /*JSONObject json = JSONObject.fromObject(map);  
                            System.out.println(json.toString());
                            System.out.println("-------------------");*/
    
                            map.clear();
    
            	            map.put("switch", os.getDpId());  
            	            map.put("name", "flow-mod-" + os.getDpId() + "-" + String.valueOf(os.getFowEntryNum()));  
            	            map.put("cookie", "0");  
            	            map.put("priority", "1");
            	            map.put("eth_type", "0x0800");
            	            map.put("ipv4_src", hostMap.get(host2).getIP());
            	            map.put("ipv4_dst", hostMap.get(host1).getIP());
            	            map.put("ip_proto", "0x06");
            	            //map.put("tcp_src", port2);
            	            //map.put("tcp_dst", port1);
//            	            map.put("src-ip", hostMap.get(host2).getIP());
//            	            map.put("dst-ip", hostMap.get(host1).getIP());
            	            //map.put("src-mac", hostMap.get(host2).getMac());
            	            //map.put("dst-mac", hostMap.get(host1).getMac());
//            	            map.put("ingress-port", "" + os.getPortIdByOVSId(pathNodeArray[j+1]));
            	            map.put("active", "true");
            	            map.put("actions", "output=" + os.getPortIdByOVSId(pathNodeArray[j-1]));
            	            
            	            /*JSONObject json2 = JSONObject.fromObject(map);  
                            System.out.println(json2.toString());
                            System.out.println("-------------------");*/
                            sendAddFlowEntryRequest(PREFIX_URL+ADD_FLOWENTRY_REST,map);
            			}
            		}
            	}
        	}
        read.close();
        
        File flowFile = new File("src/flow-table-num.txt");
        
        FileWriter fileWriter = new FileWriter(flowFile);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        
        for(String o : ovsSwitchMap.keySet()) {
			OVSSwitch os = ovsSwitchMap.get(o);
			bufferedWriter.write(os.getDpId() + "-" + os.getTotalFlowEntryNum() + "\n");
		}
			
		for(String e : edgeSwitchMap.keySet()) {
			EdgeSwitch es = edgeSwitchMap.get(e);
			bufferedWriter.write(es.getDpId() + "-" + es.getTotalFlowEntryNum() + "\n");
		}
		bufferedWriter.close();
	}
        
	
	/**
	 * function used to add a specific flow-entry to a OpenV Switch
	 * @param urlStr
	 * @param map
	 */
	public static void sendAddFlowEntryRequest(String urlStr,Map map) {

        URL url = null;  
        
        try {  
            url = new URL(urlStr);  
        } catch (MalformedURLException e) {  
            e.printStackTrace();  
        }
        
        if (url != null) {  
            try {  
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();  
               
                urlConn.setDoInput(true);  
                urlConn.setDoOutput(true); 
                urlConn.setUseCaches(false);
                urlConn.setConnectTimeout(5 * 1000);  
                
                urlConn.setRequestMethod("POST"); 
                urlConn.setRequestProperty("Content-Type", "application/json");
                urlConn.setRequestProperty("Accept", "application/json");   
                urlConn.setRequestProperty("Charset", "UTF-8");  
                  
                DataOutputStream dos = new DataOutputStream(urlConn.getOutputStream());  
                 
                JSONObject json = JSONObject.fromObject(map);  
                System.out.println(json.toString());
                dos.writeBytes(json.toString());  
                dos.flush();  
                dos.close();  
                System.out.println(urlConn.getResponseCode());
                if (urlConn.getResponseCode() == 200) {
                	System.out.println("add flow-entry ok!");
                }else {
                	System.out.println("add flow-entry faild: " + urlConn.getResponseCode());
                } 
                urlConn.disconnect();  
  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
	}
	
	/**
	 * function used to clear all old flow-entrys in every OpenV Switch(use clear flow api)
	 * @param urlStr
	 * @return
	 */
	public static boolean clearFlowEntry(String urlStr) {
		
        URL url = null;  
        try {  
            url = new URL(urlStr);  
        } catch (MalformedURLException e) {  
            e.printStackTrace();  
        }
        
        if (url != null) {  
            try {  
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();  
                
                urlConn.setDoInput(true);  
                urlConn.setDoOutput(true); 
                urlConn.setUseCaches(false);
                urlConn.setConnectTimeout(5 * 1000);  
                
                urlConn.setRequestMethod("GET"); 
                urlConn.setRequestProperty("Content-Type", "application/json");
                urlConn.setRequestProperty("Accept", "application/json");   
                urlConn.setRequestProperty("Charset", "UTF-8");  
                    
                if (urlConn.getResponseCode() == 200) {
                	System.out.println("Clear flow-entry ok!");
                }else if(urlConn.getResponseCode() == 204) {
                	System.out.println("Clear flow-entry ok!");
                }else {
                	System.out.println("Clear flow-entry faild: " + urlConn.getResponseCode());
                } 
                urlConn.disconnect();  
  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
		return true;
	}
	
	
	/**
	 * function used to delete all old flow-entrys in every OpenV Switch(use clear flow api)
	 * @param urlStr
	 * @return
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
	public static boolean delFlowEntry() throws IOException {
		
		String encoding = "GBK";
        File file = new File("src/flow-table-num.txt");
        
		InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);
    	BufferedReader bufferedReader = new BufferedReader(read);
    	String lineTxt = null;
    	while((lineTxt = bufferedReader.readLine())!=null) {
    		String[] splitResult = lineTxt.split("-");
    		int currentFlowNum = Integer.parseInt(splitResult[1]);
    		if(currentFlowNum > 0) {
    			for(int i=1; i<=currentFlowNum; i++) {
    				Map<String, String> map = new HashMap<>();
    				map.put("name", "flow-mod-" + splitResult[0] + "-" + i);
    				sendDelFlowEntryRequest(PREFIX_URL+DEL_FLOWENTRY_REST, map);
    			}
    		}
    	}
    	read.close();
		
        return true;
	}
	
	/**
	 * function used to send a http delete request with body
	 * @param urlStr
	 * @param map
	 * @return
	 */
	public static boolean sendDelFlowEntryRequest(String urlStr, Map map) {
		
		JSONObject json = JSONObject.fromObject(map); 
		try {
	        HttpEntity entity = new StringEntity(json.toString());
	        HttpClient httpClient = new DefaultHttpClient();
	        HttpDeleteWithBody httpDeleteWithBody = new HttpDeleteWithBody(urlStr);
	        httpDeleteWithBody.setEntity(entity);

	        HttpResponse response = httpClient.execute(httpDeleteWithBody);
	        if(response.getStatusLine().toString().equals("HTTP/1.1 200 OK")) {
	        	System.out.println("delete flow ok!");
	        }else {
	        	System.out.println("delete flow error: " + response.getStatusLine());
	        }
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return true;
	}
	
	public static void main(String args[]) {
		
		initNet("src/host.txt", "src/switch.txt", "src/topo.txt");
		
		//delete all old flow-entry
//		try {
//			delFlowEntry();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		try {
			//put new flow-entry according to path.txt
			setFlowEntry(2,0);
			//setFlowEntry(1,Integer.parseInt(args[0]));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
