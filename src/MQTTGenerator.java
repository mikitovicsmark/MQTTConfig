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
import Allocation.RunningInstance;
import Allocation.Subscriber;
import Allocation.Topic;
import Allocation.YakinduSM;

public class MQTTGenerator {

        public static void main(String[] args) {

                EMFModelLoad loader = new EMFModelLoad();
                CPS cps = loader.load();

                System.out.println(cps);

                for (Iterator<Hardware> hardwareIterator = cps.getHardwares().iterator(); hardwareIterator.hasNext();) {
                        Hardware hw = hardwareIterator.next();
                        for (Iterator<RunningInstance> instanceIterator = hw.getInstances().iterator(); instanceIterator.hasNext();) {
	                        RunningInstance runningInstance = instanceIterator.next();
	                        generateRunner(runningInstance, cps.getBroker());
                        }
                }
        }
        
        public static void generateRunner(RunningInstance runningInstance, String broker) {
        	PrintWriter out = null;
        	try {
        		YakinduSM yakinduSM = runningInstance.getYakindu();
        		String smName = yakinduSM.getName();
				out = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(smName + "Runner.txt")), "UTF-8"));
				
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
					out.println(indent(2) + "MQTT" + smName + " client" + clientId + " = new MQTT" + smName + "(broker, " + runningInstance.getClientId() + ", persistence);");
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
					 
					 for(Topic topic : topics) {
						 out.println(indent(3) + "client" + clientId + ".subscribe(" + topic.getName() + ", qos);");
					 }
					 out.println(indent(3) + "MQTT" + smName + " client" + clientId + " = new MQTT" + smName + "(broker, " + runningInstance.getClientId() + ", persistence);");
					 out.println(indent(3) + "client" + clientId + ".init();");
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
