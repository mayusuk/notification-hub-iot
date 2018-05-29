package dto;

public class iotMapRequest {

	private String existingPatientID;
	private String mappedPatientID;
	

	public String getExistingPatientID() {
		return existingPatientID;
	}
	public void setExistingPatientID(String existingPatientID) {
		this.existingPatientID = existingPatientID;
	}
	public String getMappedPatientID() {
		return mappedPatientID;
	}
	public void setMappedPatientID(String mappedPatientID) {
		this.mappedPatientID = mappedPatientID;
	}
}
