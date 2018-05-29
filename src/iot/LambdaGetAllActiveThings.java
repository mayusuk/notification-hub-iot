package iot;

import java.util.ArrayList;	
import java.util.List;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ListThingsResult;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import dto.thingResult;

public class LambdaGetAllActiveThings implements RequestHandler<Object, Object> {

	private AWSIotClient awsIotClient;
	
	
	public LambdaGetAllActiveThings() {
			awsIotClient = new AWSIotClient();
			awsIotClient.withRegion(Regions.US_WEST_2);
	}
	
	public List<thingResult> getThings(){
		
		ListThingsRequest listThingsRequest = new ListThingsRequest();
		listThingsRequest.setThingTypeName("patient");
		ListThingsResult things = this.awsIotClient.listThings(listThingsRequest);
		
		List<thingResult>  thingResults = new ArrayList<>();
		
		for(ThingAttribute attribute : things.getThings()) {
			thingResults.add(new thingResult(attribute.getThingName()));
		}
        
		return thingResults;
	}
	

    @Override
    public Object handleRequest(Object input, Context context) {
        context.getLogger().log("Input: " + input);
        
        return this.getThings();
		
    }

}
