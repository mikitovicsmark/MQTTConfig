import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import Allocation.CPS;
import Allocation.Hardware;
import Allocation.InEvent;
import Allocation.OutEvent;
import Allocation.RunningInstance;
import Allocation.Subscriber;
import Allocation.Topic;
import Allocation.YakinduSM;

public class MQTTGenerator {

        public static void main(String[] args) {

                EMFModelLoad loader = new EMFModelLoad();
                CPS cps = loader.load();

                for (Iterator<Hardware> hardwareIterator = cps.getHardwares().iterator(); hardwareIterator.hasNext();) {
                        Hardware hw = hardwareIterator.next();
                        for (Iterator<RunningInstance> instanceIterator = hw.getInstances().iterator(); instanceIterator.hasNext();) {
	                        RunningInstance runningInstance = instanceIterator.next();
	                        generateRunner(runningInstance, cps.getBroker());
	                        generateMQTTClass(runningInstance.getYakindu());
                        }
                }
        }
        
        private static void generateMQTTClass(YakinduSM yakindu) {
        	PrintWriter out = null;
        	List<Topic> topics = new ArrayList<>();
			
			for (Iterator<InEvent> inEventIterator = yakindu.getInEvents().iterator(); inEventIterator.hasNext();) {
				 	InEvent inEvent = inEventIterator.next();
				 	Topic topic = inEvent.getTopic();
				 	if (!topics.contains(topic)) {
				 		topics.add(topic);
				 	}
			}
			for (Iterator<OutEvent> inEventIterator = yakindu.getOutEvents().iterator(); inEventIterator.hasNext();) {
				 	OutEvent outEvent = inEventIterator.next();
				 	Topic topic = outEvent.getTopic();
				 	if (!topics.contains(topic)) {
				 		topics.add(topic);
				 	}
			}
        	try {
            	out = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("MQTT" + yakindu.getName() + ".java")), "UTF-8"));
        		out.println("import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;\r\n" + 
        				"import org.eclipse.paho.client.mqttv3.MqttCallback;\r\n" + 
        				"import org.eclipse.paho.client.mqttv3.MqttClient;\r\n" + 
        				"import org.eclipse.paho.client.mqttv3.MqttConnectOptions;\r\n" + 
        				"import org.eclipse.paho.client.mqttv3.MqttException;\r\n" + 
        				"import org.eclipse.paho.client.mqttv3.MqttMessage;\r\n" + 
        				"import org.eclipse.paho.client.mqttv3.MqttPersistenceException;\r\n" + 
        				"import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;\r\n" + 
        				"import org.yakindu.scr.RuntimeService;");
        		if (yakindu.isTimer()) {
        			out.println("import org.yakindu.scr.TimerService;");
        		}
        		out.println("import org.yakindu.scr." + yakindu.getName().toLowerCase() + yakindu.getName() + "Statemachine;\n" );
        		out.println("public class MQTT" + yakindu.getName() + " implements MqttCallback{\r\n" + 
        				indent(1) + yakindu.getName() + "Statemachine statemachine;\r\n" + 
        				indent(1) + "MqttClient myClient;\r\n" + 
        				indent(1) + "MqttConnectOptions connOpts;\r\n" + 
        				indent(1) + "boolean started = false;\r\n\n");

        		out.println("	public MQTT" + yakindu.getName() + "(String broker, String clientId, MemoryPersistence persistence) {\r\n" + 
        				"		 try {\r\n" + 
        				"			statemachine = new " + yakindu.getName() + "Statemachine();\r\n" + 
        				"			myClient = new MqttClient(broker, clientId, persistence);\r\n" + 
        				"			myClient.setCallback(this);\r\n" + 
        				"			connOpts = new MqttConnectOptions();\r\n" + 
        				"			\r\n" + 
        				"		    connOpts.setCleanSession(true);\r\n" + 
        				"		    connOpts.setKeepAliveInterval(30);\r\n" + 
        				"		    \r\n" + 
        				"		    myClient.connect(connOpts);\r\n" + 
        				"		} catch (MqttException e) {\r\n" + 
        				"			e.printStackTrace();\r\n" + 
        				"		}\r\n" + 
        				"	}");

        		out.println("	public void init() {\r\n" + 
        				"		if (!started) {\r\n");
        		if(yakindu.isTimer()) {
        			out.println("statemachine.setTimer(new TimerService());\r\n");
        		}
        		if(yakindu.isObserver()) {
	   				 for (Iterator<OutEvent> outEventIterator = yakindu.getOutEvents().iterator(); outEventIterator.hasNext();) {
						 	OutEvent outEvent = outEventIterator.next();
						 	Topic topic = outEvent.getTopic();
						 	out.println(indent(2) + "statemachine.getSCI" + outEvent.getInterface() + "().getListeners().add(new SCI" + outEvent.getInterface() + "Listener() {");
						 	out.println("				public void on" + outEvent.getName() + "Raised() {\r\n" + 
						 			"					String topic = \"" + topic + "\";\r\n" + 
						 			"					String content = \"" + outEvent.getMessage() + "\";\r\n" + 
						 			"					MqttMessage message = new MqttMessage(content.getBytes());\r\n" + 
						 			"					try {\r\n" + 
						 			"						myClient.publish(topic, message);\r\n" + 
						 			"					} catch (MqttPersistenceException e) {\r\n" + 
						 			"						e.printStackTrace();\r\n" + 
						 			"					} catch (MqttException e) {\r\n" + 
						 			"						e.printStackTrace();\r\n" + 
						 			"					}\r\n" + 
						 			"				}");
						 	out.println(indent(2) + "});");
					 }
        		}
        		out.println(
        				"			statemachine.init();\r\n" + 
        				"			statemachine.enter();\r\n" + 
        				"			RuntimeService.getInstance().registerStatemachine(statemachine, 100);\r\n" + 
        				"			started = true;\r\n" + 
        				"		}\r\n" + 
        				"	}");
        		
				 for (Iterator<InEvent> inEventIterator = yakindu.getInEvents().iterator(); inEventIterator.hasNext();) {
					 	InEvent inEvent = inEventIterator.next();
					 	Topic topic = inEvent.getTopic();
					 	if (topic == null) {
					 		out.println("	public void " + inEvent.getName() + "() {\r\n" + 
					 				"		statemachine.getSCI" + inEvent.getInterface() + "().raise" + inEvent.getName() + "();\r\n" + 
					 				"		statemachine.runCycle();\r\n" + 
					 				"	}");
					 	}
				 }
        		
        		out.println(indent(1) + "public void connectionLost(Throwable arg0) {\r\n" + 
        				indent(2) + "System.out.println(\"Connection lost.\");\r\n" + 
        				indent(1) + "}\r\n" + 
        				"\r\n" + 
        				indent(1) + "public void deliveryComplete(IMqttDeliveryToken arg0) {\r\n" + 
        				indent(2) + "System.out.println(\"Delivery complete.\");\r\n" + 
        				indent(1) + "}\r\n" + 
        				"\r\n" + 
        				indent(1) + "public void messageArrived(String topic, MqttMessage msg) throws Exception {\r\n" + 
        				indent(2) + "System.out.println(\"Got message: Topic: \" + topic + \"\\n\\tMessage: \" + new String(msg.getPayload()));\r\n" + 
        				indent(1) + "}\r\n" + 
        				"\r\n" + 
        				indent(1) + "public void subscribe(String topic, int qos) throws MqttException {\r\n" + 
        				indent(2) + "myClient.subscribe(topic, qos);\r\n" + 
        				indent(1) + "}\r\n" + 
        				"\r\n" + 
        				indent(1) + "public void publish(String topic, MqttMessage message) throws MqttPersistenceException, MqttException {\r\n" + 
        				indent(2) + "myClient.publish(topic, message);\r\n" + 
        				indent(1) + "}\r\n" + 
        				"\r\n" + 
        				indent(1) + "public void disconnect() throws MqttException {\r\n" + 
        				indent(2) + "myClient.disconnect();\r\n" + 
        				indent(1) + "}\r\n"
        				+ "}");
        	} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (out != null) {
					out.flush();
					out.close();
				}
			}
			
		}

		public static void generateRunner(RunningInstance runningInstance, String broker) {
        	PrintWriter out = null;
        	try {
        		YakinduSM yakinduSM = runningInstance.getYakindu();
        		String smName = yakinduSM.getName();
				out = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(smName + "Runner.java")), "UTF-8"));
				
				Subscriber subscriber = runningInstance.getSubscriber();
				if (subscriber != null) {
					out.println("import org.eclipse.paho.client.mqttv3.MqttMessage;");
				}
				out.println("import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;\n");
				out.println("public class " + smName + "Runner {\n");
				out.println(indent(1) + "public static void main(String[] args) throws InterruptedException {\n");
				if (subscriber != null) {
					out.println(indent(2) + "int qos             = 2;");
				}
				out.println(indent(2) + "String broker       = \"" + broker + "\"");
				out.println(indent(2) + "String clientId     = \"" + runningInstance.getClientId() + "\"");
				out.println(indent(2) + "MemoryPersistence persistence = new MemoryPersistence();\n");
				
				int clientId = 1;
				if (runningInstance.getPublisher() != null) {
					out.println(indent(2) + "MQTT" + smName + " client" + clientId + " = new MQTT" + smName + "(broker, clientId, persistence);");
					out.println(indent(2) + "client" + clientId + ".init();");
					clientId++;
				}
				if (subscriber != null) {
					List<Topic> topics = new ArrayList<>();
					
					out.println(indent(2) + "try {");
					 for (Iterator<InEvent> inEventIterator = subscriber.getInevent().iterator(); inEventIterator.hasNext();) {
						 	InEvent inEvent = inEventIterator.next();
						 	Topic topic = inEvent.getTopic();
						 	if (!topics.contains(topic)) {
						 		topics.add(topic);
						 	}
					 }
					 out.println(indent(3) + "MQTT" + smName + " client" + clientId + " = new MQTT" + smName + "(broker, clientId, persistence);");
					 out.println(indent(3) + "client" + clientId + ".init();");
					 for(Topic topic : topics) {
						 out.println(indent(3) + "client" + clientId + ".subscribe(\"" + topic.getName() + "\", qos);");
					 }
					 out.println(indent(2) + "} catch(MqttException me) {");
					 out.println(indent(3) + "System.out.println(\"msg \"+me.getMessage());");
					 out.println(indent(3) + "System.out.println(\"loc \"+me.getLocalizedMessage());");
					 out.println(indent(3) + "System.out.println(\"cause \"+me.getCause());");
					 out.println(indent(3) + "System.out.println(\"excep \"+me);");
					 out.println(indent(3) + "me.printStackTrace();");
					 out.println(indent(2) + "}");
				}
				out.println(indent(1) + "}");
				out.println("}");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (out != null) {
					out.flush();
					out.close();
				}
			}
        }
        
        public static String indent(int count) {
        	String result = "";
        	for (int i=0; i<count; i++) {
        		result += "\t";
        	}
			return result;
        }
}
