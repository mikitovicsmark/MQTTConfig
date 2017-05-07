package generator;

import static org.junit.Assert.*;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yakindu.scr.light.LightStatemachine.State;

import generated.MQTTControlPanel;
import generated.MQTTLight;
import generated.MQTTLightSensor;
import generated.MQTTMovementDetector;
import generated.MQTTSprinkler;

public class TestRunner {

	MQTTControlPanel controlClient;
	MQTTLightSensor lightSensorClient;
	MQTTMovementDetector movementDetectorClient;
	MQTTSprinkler sprinklerClient;
	MQTTLight lightClient;
	
    @Before public void initialize() {
		String broker       = "tcp://localhost:1883";
    	
		String controlClientId     = "ControlPanelClient";
		MemoryPersistence persistence2 = new MemoryPersistence();
		controlClient = new MQTTControlPanel(broker, controlClientId, persistence2);
		controlClient.init();
		
		String lightSensorClientId     = "LightSensorClient";
		MemoryPersistence persistence3 = new MemoryPersistence();
		lightSensorClient = new MQTTLightSensor(broker, lightSensorClientId, persistence3);
		lightSensorClient.init();
		
		String movementDetectorClientId     = "MovementDetectorClient";
		MemoryPersistence persistence4 = new MemoryPersistence();
		movementDetectorClient = new MQTTMovementDetector(broker, movementDetectorClientId, persistence4);
		movementDetectorClient.init();
		
		String sprinklerClientId     = "SprinklerClient";
		MemoryPersistence persistence5 = new MemoryPersistence();
		
		int qos             = 2;
		String lightClientId     = "LightClient";
		MemoryPersistence persistence1 = new MemoryPersistence();
		
		try {
			sprinklerClient = new MQTTSprinkler(broker, sprinklerClientId, persistence5);
			sprinklerClient.init();
			sprinklerClient.subscribe("ControlPanel", qos);
			
			lightClient = new MQTTLight(broker, lightClientId, persistence1);
			lightClient.init();
			lightClient.subscribe("Light", qos);
			lightClient.subscribe("Movement", qos);
			lightClient.subscribe("ControlPanel", qos);
		} catch(MqttException me) {
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}   
    }
	
    @After
    public void afterTest() {
    	try {
			controlClient.disconnect();
	    	lightSensorClient.disconnect();
	    	movementDetectorClient.disconnect();
	    	sprinklerClient.disconnect();
	    	lightClient.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
    }
    
	@Test
	public void testLightOn() throws InterruptedException {
		lightSensorClient.onDark();
		movementDetectorClient.onDetect();
		Thread.sleep(200);
		assertTrue(lightClient.statemachine.isStateActive(State.main_region_On));
	}

	@Test
	public void testLightCases() throws InterruptedException {
		//Switch On, all true
		controlClient.switchLight(); // Switch on, bright, no movement
		Thread.sleep(200);
		assertTrue(lightClient.statemachine.isStateActive(State.main_region_On));
		
		lightSensorClient.onDark(); // Switch on, dark, no movement
		Thread.sleep(200);
		assertTrue(lightClient.statemachine.isStateActive(State.main_region_On));
		
		movementDetectorClient.onDetect(); // Switch on, dark, movement
		Thread.sleep(200);
		assertTrue(lightClient.statemachine.isStateActive(State.main_region_On));
			
		lightSensorClient.onBright(); // Switch on, bright, movement
		Thread.sleep(200);
		assertTrue(lightClient.statemachine.isStateActive(State.main_region_On));
		
		Thread.sleep(11000);
		
		//Switch Off, only one true
		controlClient.switchLight(); // Switch off, bright, no movement
		Thread.sleep(200);
		assertFalse(lightClient.statemachine.isStateActive(State.main_region_On));

		lightSensorClient.onDark(); // Switch off, dark, no movement
		Thread.sleep(200);
		assertFalse(lightClient.statemachine.isStateActive(State.main_region_On));
		
		movementDetectorClient.onDetect(); // Switch off, dark, movement
		Thread.sleep(200);
		assertTrue(lightClient.statemachine.isStateActive(State.main_region_On));
			
		lightSensorClient.onBright(); // Switch off, bright, movement
		Thread.sleep(200);
		assertFalse(lightClient.statemachine.isStateActive(State.main_region_On));
	}
	
	@Test
	public void testSprinklerManual() throws InterruptedException {
		controlClient.switchSprinkler();
		Thread.sleep(200);
		assertTrue(sprinklerClient.statemachine.isStateActive(org.yakindu.scr.sprinkler.SprinklerStatemachine.State.main_region_On));
		
		Thread.sleep(5200);
		assertTrue(sprinklerClient.statemachine.isStateActive(org.yakindu.scr.sprinkler.SprinklerStatemachine.State.main_region_On));
		
		controlClient.switchSprinkler();
		Thread.sleep(200);
		assertFalse(sprinklerClient.statemachine.isStateActive(org.yakindu.scr.sprinkler.SprinklerStatemachine.State.main_region_On));
	}
	
	@Test
	public void testSprinklerAuto() throws InterruptedException {
		Thread.sleep(10200);
		assertTrue(sprinklerClient.statemachine.isStateActive(org.yakindu.scr.sprinkler.SprinklerStatemachine.State.main_region_On));
		
		Thread.sleep(5200);
		assertFalse(sprinklerClient.statemachine.isStateActive(org.yakindu.scr.sprinkler.SprinklerStatemachine.State.main_region_On));
	}
	
}
