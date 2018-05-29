package dto;

public class iotFirstSetupRequest {
	
	public iotFirstSetupRequest() {
		
	}
	
	public iotFirstSetupRequest(String patientID) {
		this.patientID = patientID;
	}
	
	private String patientID;
	
	public String getPatientID() {
		return patientID;
	}
	public void setPatientID(String patientID) {
		this.patientID = patientID;
	}

}
