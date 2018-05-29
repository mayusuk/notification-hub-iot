package iot;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import dto.iotMapRequest;

public class LambdaIotMapPatientHandler implements RequestHandler<iotMapRequest, Object> {

	
	private PutItemRequest mappPatientRequest;
	private AmazonDynamoDBClient amazonDynamoDBClient ;
	
	
	
	public LambdaIotMapPatientHandler() {
		amazonDynamoDBClient = new AmazonDynamoDBClient();
		amazonDynamoDBClient.withRegion(Regions.US_WEST_2); 
		mappPatientRequest = new PutItemRequest();
		mappPatientRequest.setTableName("mappedPatient");
	}
	
	public void mappPatients(String existingPatientID, String newPatientID) {
		AttributeValue patientIDattributeValue = new AttributeValue();
		patientIDattributeValue.setS(existingPatientID);
		
		Map<String, AttributeValue> patientID = new HashMap<>() ;
		patientID.put("patientID", patientIDattributeValue);
		mappPatientRequest.setItem(patientID);
		
		AttributeValue newPatientIDattributeValue = new AttributeValue();
		newPatientIDattributeValue.setS(newPatientID);
		mappPatientRequest.addItemEntry("mappedPatientID", newPatientIDattributeValue);
		
		amazonDynamoDBClient.putItem(mappPatientRequest);		
	}
	

    @Override
    public Object handleRequest(iotMapRequest input, Context context) {
        context.getLogger().log("Input: " + input);

        
        String thingName = input.getExistingPatientID();
		String newPatientID = input.getMappedPatientID();
		
		this.mappPatients(thingName,newPatientID);
		
		return "Success";
    }

}
