package org.egov.bpa.web.model;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Notice {
	
	@JsonProperty("id")
	private String id = null;
	
	
	@JsonProperty("businessid")
	private String businessid = null;
	
	@JsonProperty("LetterNo")
	private String LetterNo = null;
	
	@JsonProperty("filestoreid")
	private String filestoreid = null;
	
	@JsonProperty("letterType")
	private String letterType = null;
	
	@JsonProperty("tenantid")
	private String tenantid = null;
	
	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;
	
	
	


}
