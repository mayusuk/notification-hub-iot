package iot;

import java.util.stream.Collectors;	

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.AttachThingPrincipalRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.amazonaws.services.iot.model.CreatePolicyRequest;
import com.amazonaws.services.iot.model.CreatePolicyResult;
import com.amazonaws.services.iot.model.CreateThingRequest;
import com.amazonaws.services.iot.model.GetPolicyRequest;
import com.amazonaws.services.iot.model.ListPoliciesRequest;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ListThingsResult;
import com.amazonaws.services.iot.model.Policy;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import dto.iotFirstSetupRequest;

public class LambdaFirstSetupHandler implements RequestHandler<iotFirstSetupRequest, Object> {

	private AWSIotClient awsIotClient;
	private AmazonDynamoDBClient amazonDynamoDBClient ;
	private PutItemRequest iotKeyCertsPutItemRequest;
	private PutItemRequest iotPatientThingTopicMappingRequest;
	private PutItemRequest mappPatientRequest;
	
	private String TEMP_POLICY_TEMPLATE = "{\r\n" + 
			"  \"Version\": \"2012-10-17\",\r\n" + 
			"  \"Statement\": [\r\n" + 
			"    {\r\n" + 
			"      \"Effect\": \"Allow\",\r\n" + 
			"      \"Action\": [\r\n" + 
			"        \"iot:Subscribe\",\r\n" + 
			"        \"iot:Publish\"\r\n" + 
			"      ],\r\n" + 
			"      \"Resource\": \"arn:aws:iot:us-west-2:013261620024:topic/TEMP_TOPIC\"\r\n" + 
			"    },\r\n" + 
			"    {\r\n" + 
			"      \"Effect\": \"Allow\",\r\n" + 
			"      \"Action\": [\r\n" + 
			"        \"iot:Subscribe\"\r\n" + 
			"      ],\r\n" + 
			"      \"Resource\": \"arn:aws:iot:us-west-2:013261620024:topic/MAIN_TOPIC\"\r\n" + 
			"    }\r\n" + 
			"  ]\r\n" + 
			"}";
	
	
	public LambdaFirstSetupHandler() {
		
		awsIotClient = new AWSIotClient();
		awsIotClient.withRegion(Regions.US_WEST_2);
		amazonDynamoDBClient = new AmazonDynamoDBClient();
		amazonDynamoDBClient.withRegion(Regions.US_WEST_2);  
		iotKeyCertsPutItemRequest = new PutItemRequest();
		iotKeyCertsPutItemRequest.setTableName("iotKeyCerts");	
		iotPatientThingTopicMappingRequest = new PutItemRequest();
		iotPatientThingTopicMappingRequest.setTableName("patientThingTopicMapping");
		mappPatientRequest = new PutItemRequest();
		mappPatientRequest.setTableName("mappedPatient");
	}
	
	public void mappPatients(String existingPatientID, String newPatientID) {
		AttributeValue patientIDattributeValue = new AttributeValue();
		patientIDattributeValue.setS(existingPatientID);
		mappPatientRequest.addItemEntry("patientID", patientIDattributeValue);
		
		AttributeValue newPatientIDattributeValue = new AttributeValue();
		newPatientIDattributeValue.setS(newPatientID);
		mappPatientRequest.addItemEntry("mappedPatientID", newPatientIDattributeValue);
		
		amazonDynamoDBClient.putItem(mappPatientRequest);
		
	}

	public void mapThingAndTopic(String thingname){
		AttributeValue patientIDattributeValue = new AttributeValue();
		patientIDattributeValue.setS(thingname);
		iotPatientThingTopicMappingRequest.addItemEntry("patientID", patientIDattributeValue);
		
		AttributeValue topicattributeValue = new AttributeValue();
		topicattributeValue.setS(thingname + "_MAIN");
		iotPatientThingTopicMappingRequest.addItemEntry("topicName", topicattributeValue);
		
		amazonDynamoDBClient.putItem(iotPatientThingTopicMappingRequest);
	}
	
	public ListThingsResult getThings(){
		
		ListThingsRequest listThingsRequest = new ListThingsRequest();
		listThingsRequest.setThingTypeName("patient");
		ListThingsResult things = this.awsIotClient.listThings(listThingsRequest);
        
		return things;
	}
	
	public boolean checkThingExists(String thingName) {
		java.util.List<ThingAttribute> result= this.getThings().getThings().stream().filter(thing -> thing.getThingName().equals(thingName)).collect(Collectors.toList());
		if( result != null && !result.isEmpty()){
			return true;
		}
		return false;
	}
		
	public String createPatientThing(String thingName) {
		
		CreateThingRequest createThingRequest = new CreateThingRequest();
		createThingRequest.setThingName(thingName);
		createThingRequest.setThingTypeName("patient");
		return this.awsIotClient.createThing(createThingRequest).getThingName();
		
	}
	
	public void getPolicies() {
		GetPolicyRequest getPolicyRequest = new GetPolicyRequest();
		getPolicyRequest.setPolicyName("testing");
		System.out.println(this.awsIotClient.getPolicy(getPolicyRequest).getPolicyDocument());
	}
	
	public java.util.List<Policy> listPolicies() {
		ListPoliciesRequest listPoliciesRequest = new ListPoliciesRequest();
		return this.awsIotClient.listPolicies(listPoliciesRequest).getPolicies();
	}
	
	public CreatePolicyResult createPolicy(String thingName) {
		CreatePolicyRequest createPolicyRequest = new CreatePolicyRequest();
		createPolicyRequest.setPolicyName(thingName + "_POLICY");
		String template = TEMP_POLICY_TEMPLATE.replaceAll("MAIN_TOPIC", thingName + "_MAIN");
		template = template.replaceAll("TEMP_TOPIC", thingName + "_TEMP");
		createPolicyRequest.setPolicyDocument(template);
		
        return this.awsIotClient.createPolicy(createPolicyRequest);
	}
	
	public CreateKeysAndCertificateResult createCertificate() {
		
		CreateKeysAndCertificateRequest createKeysAndCertificateRequest = new  CreateKeysAndCertificateRequest();
	
		return this.awsIotClient.createKeysAndCertificate(createKeysAndCertificateRequest);

	}
	
	public void attachPolicyToCert(String policyName, String certName) {
		AttachPrincipalPolicyRequest attachPrincipalPolicyRequest = new AttachPrincipalPolicyRequest();
		attachPrincipalPolicyRequest.setPolicyName(policyName);
		attachPrincipalPolicyRequest.setPrincipal(certName);
		this.awsIotClient.attachPrincipalPolicy(attachPrincipalPolicyRequest);
	}
	
	public void attachCertToThing(String thingName, String certName){
		AttachThingPrincipalRequest attachThingPrincipalRequest = new AttachThingPrincipalRequest();
		attachThingPrincipalRequest.setPrincipal(certName);
		attachThingPrincipalRequest.setThingName(thingName);
		this.awsIotClient.attachThingPrincipal(attachThingPrincipalRequest);
	}
	
	public void storeKeyCert(CreateKeysAndCertificateResult result , String patientId) {
		
		PutItemRequest itemRequest = iotKeyCertsPutItemRequest;
		AttributeValue patientIDattributeValue = new AttributeValue();
		patientIDattributeValue.setS(patientId);
		itemRequest.addItemEntry("patientID", patientIDattributeValue);
		
		AttributeValue certArnattributeValue = new AttributeValue();
		certArnattributeValue.setS(result.getCertificateArn());
		itemRequest.addItemEntry("certArn", certArnattributeValue);
		
		AttributeValue certIDattributeValue = new AttributeValue();
		certIDattributeValue.setS(result.getCertificateId());
		itemRequest.addItemEntry("certID", certIDattributeValue);
		
		AttributeValue certPEMattributeValue = new AttributeValue();
		certPEMattributeValue.setS(result.getCertificatePem());
		itemRequest.addItemEntry("certPEM", certPEMattributeValue);
		
		AttributeValue privatekeyattributeValue = new AttributeValue();
		privatekeyattributeValue.setS(result.getKeyPair().getPrivateKey());
		itemRequest.addItemEntry("privatekey", privatekeyattributeValue);
		
		AttributeValue publicKeyattributeValue = new AttributeValue();
		publicKeyattributeValue.setS(result.getKeyPair().getPublicKey());
		itemRequest.addItemEntry("publicKey", publicKeyattributeValue);
		
		this.amazonDynamoDBClient.putItem(itemRequest);
		
	}

	
    @Override
    public Object handleRequest(iotFirstSetupRequest input, Context context) {
        context.getLogger().log("Input: " + input);

        String thingName = input.getPatientID();
        
		if(!this.checkThingExists(thingName)){
			System.out.println(this.createPatientThing(thingName));
			this.mapThingAndTopic(thingName);
			CreatePolicyResult policyResult = this.createPolicy(thingName);
			CreateKeysAndCertificateResult  keyAndCert = this.createCertificate();
			this.storeKeyCert(keyAndCert, thingName);
			this.attachPolicyToCert(policyResult.getPolicyName(), keyAndCert.getCertificateArn());
			this.attachCertToThing(thingName, keyAndCert.getCertificateArn());
		}
		
		
		return "Success";
    }

}
