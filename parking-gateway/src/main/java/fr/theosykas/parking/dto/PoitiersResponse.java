package fr.theosykas.parking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PoitiersResponse {
	@JsonProperty("Name")
	private String NameOfParking;

	@JsonProperty("Places")
	private Integer emptySpace;
	
	@JsonProperty("Capacite")
	private Integer totalSpace;

	public PoitiersResponse() {}

	public PoitiersResponse(
		String NameOfParking,
		Integer emptySpace,
		Integer totalSpace
	) {
		this.NameOfParking = NameOfParking;
		this.emptySpace = emptySpace;
		this.totalSpace = totalSpace;
	}
}
