package iot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.simpleemail.model.InvocationType;

import dto.notificationDTO;

public class LambdaIotNotificationHandler implements RequestHandler<List<notificationDTO>, Object> {

	private static final int Map = 0;
	private AWSIotClient awsIotClient;
	private AmazonDynamoDBClient amazonDynamoDBClient ;
	private GetItemRequest getTopicRequest;
	private QueryRequest getMappedPayientQuerySpec;
	private AWSLambdaClient awsLambdaClient;
	private InvokeRequest invokeRequest;
	
	private String payload = "{\n" + 
			"\"patientID\":\"PATIENT_ID\",\n" + 
			"\"message\":\"MESSAGE_CONTENT\"\n" + 
			"}";
	
	public LambdaIotNotificationHandler() {
		awsIotClient = new AWSIotClient();
		awsIotClient.withRegion(Regions.US_WEST_2);
		amazonDynamoDBClient = new AmazonDynamoDBClient();
		amazonDynamoDBClient.withRegion(Regions.US_WEST_2);
		getTopicRequest = new GetItemRequest();
		getTopicRequest.setTableName("patientThingTopicMapping");
		getMappedPayientQuerySpec = new QueryRequest();
		getMappedPayientQuerySpec.setTableName("mappedPatient");
		awsLambdaClient = new AWSLambdaClient();
		awsLambdaClient.withRegion(Regions.US_WEST_2);
		invokeRequest = new InvokeRequest();
		invokeRequest.setFunctionName("publishNotificationLambda");		
		invokeRequest.setInvocationType(com.amazonaws.services.lambda.model.InvocationType.Event);
	}
	
	public void sendNotification(String patientID,String message) {
		
		String topicName = getTopic(patientID);
		if(topicName == null){
			String mappedPatientID = getMappedPatient(patientID);
			topicName = getTopic(mappedPatientID);
		}
		if(topicName == null) {
			return;
		}
		
		// call publish lambda f
		String input = this.payload.replaceAll("PATIENT_ID", topicName);
		input = input.replaceAll("MESSAGE_CONTENT", message);
		System.out.println("Data to Publish --" + input);
		this.invokeRequest.setPayload(input);
		this.awsLambdaClient.invoke(this.invokeRequest);
		
		
		
	}
			
	public String getMappedPatient(String patientID) {
		Map<String, AttributeValue> key = new HashMap<>();
		
		AttributeValue attrib = new AttributeValue();
		attrib.setS(patientID);		
		key.put(":mappedPatientID", attrib);
		
        getMappedPayientQuerySpec
        .withKeyConditionExpression("mappedPatientID = :mappedPatientID")
        .withExpressionAttributeValues(key);
        
        
		QueryResult result = this.amazonDynamoDBClient.query(getMappedPayientQuerySpec);

		System.out.println(result.toString());
		
		if(result.getCount() != 1) {
			return null;
		}
		Map<String, AttributeValue> topic = result.getItems().get(0);
		return topic.get("patientID").getS();	
	}

	public String getTopic(String patientID) {
		Map<String, AttributeValue> key = new HashMap<>();
		AttributeValue attrib = new AttributeValue();
		attrib.setS(patientID);		
		key.put("patientID", attrib);
		getTopicRequest.setKey(key);
		GetItemResult result = this.amazonDynamoDBClient.getItem(this.getTopicRequest);

		System.out.println(result.toString());
		
		Map<String, AttributeValue> topic = result.getItem();
		if(topic == null) {
			return null;
		}
				
		return topic.get("topicName").getS();
	}
	

    @Override
    public Object handleRequest(List<notificationDTO> input, Context context) {
        context.getLogger().log("Input: " + input);

        for (notificationDTO dto: input ){
			this.sendNotification(dto.getPatientID(), dto.getNotificationMessage());
		}
        return null;
    }

}
