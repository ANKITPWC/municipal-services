package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BpaApplicationSearch;
import org.egov.bpa.web.model.BpaApprovedByApplicationSearch;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.DscDetails;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Component
public class BPAApprovedByRowMapper implements ResultSetExtractor<List<BpaApprovedByApplicationSearch>> {
	
	@Autowired
	private ObjectMapper mapper;

	@Override
	public List<BpaApprovedByApplicationSearch> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, BpaApprovedByApplicationSearch>  approvalMap = new LinkedHashMap<String, BpaApprovedByApplicationSearch>();
		while (rs.next()) {
			String id = rs.getString("id");
			BpaApprovedByApplicationSearch bpas = approvalMap.get(id);
			JsonNode buildingadditionaldetails=null;
			

			
			if(bpas == null) {
				
				
				
				Long lastModifiedTime = rs.getLong("lastModifiedTime");
				if (rs.wasNull()) {
					lastModifiedTime = null;
				}
				
				AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
						.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
						.lastModifiedTime(lastModifiedTime).build();
				
				
				DscDetails	dscDetail = DscDetails.builder().auditDetails(auditdetails).tenantId(rs.getString("tenantid"))
						.documentType(rs.getString("documenttype")).documentId(rs.getString("documentid"))
						.applicationNo(rs.getString("applicationno")).approvedBy(rs.getString("approvedby"))
						.id(id).build();
				try {
					PGobject pgObj = (PGobject) rs.getObject("additionaldetails");

					if (pgObj != null) {
						JsonNode additionalDetail = mapper.readTree(pgObj.getValue());
						dscDetail.setAdditionalDetails(additionalDetail);
					}
				} catch (Exception e) {
					throw new CustomException("PARSING ERROR",
							"The DSC Details additionalDetail json cannot be parsed");
				}
				
				try {
				PGobject pgObj = (PGobject) rs.getObject("buildingadditionaldetails");
		            if (pgObj != null) {
						buildingadditionaldetails = mapper.readTree(pgObj.getValue());	
				}
				}
				 catch (Exception e) {
						throw new CustomException("PARSING ERROR",
								"The buildingadditionaldetails json cannot be parsed");
					}
				
				bpas = BpaApprovedByApplicationSearch.builder().applicationstatus(rs.getString("applicationstatus"))
						.workflowstate(rs.getString("workflowstate")).buildingAdditionalDetails(buildingadditionaldetails)
						.dscDetails(dscDetail).build();
				
				approvalMap.put(id, bpas);
			}
			addChildrenToProperty(rs, bpas);
		}
		return new ArrayList<>(approvalMap.values());
	}

	private void addChildrenToProperty(ResultSet rs, BpaApprovedByApplicationSearch bpas) throws SQLException {
		
		String documentId = rs.getString("bpa_doc_id");
		if(documentId !=null){
		
		
		
		
		Object docDetails = null;
		if(rs.getString("doc_details") != null) {
			docDetails = new Gson().fromJson(rs.getString("doc_details").equals("{}")
					|| rs.getString("doc_details").equals("null") ? null : rs.getString("doc_details"),
					Object.class);
		}
		
		if (documentId != null) {
		Document documents = Document.builder().documentType(rs.getString("bpa_doc_documenttype"))
					.fileStoreId(rs.getString("bpa_doc_filestore"))
					.id(documentId)
					.additionalDetails(docDetails)
					.documentUid(rs.getString("documentuid")).build();
		bpas.addDocumentsItem(documents);
			
		
		
	}
		}

}
}
	
