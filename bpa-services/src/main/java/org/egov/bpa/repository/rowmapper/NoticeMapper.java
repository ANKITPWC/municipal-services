package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.Notice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class NoticeMapper implements ResultSetExtractor<List<Notice>> {
	
	@Autowired
	private ObjectMapper mapper;
    
	@SuppressWarnings("rawtypes")
	@Override
	public List<Notice> extractData(ResultSet rs) throws SQLException, DataAccessException {
		
		Map<String,Notice> noticeMap =  new LinkedHashMap<String,Notice>();
		
		while (rs.next()) {
			String id = rs.getString("id");
			System.out.println("id:"+id);
			Notice notice = noticeMap.get(id);
			if(notice==null) {
			Long lastModifiedTime = rs.getLong("lastModifiedTime");
			if(rs.wasNull())
				lastModifiedTime = null;
			
			AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
					.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
					.lastModifiedTime(lastModifiedTime).build();
			
			notice = Notice.builder().id(id).businessid(rs.getString("businessid")).filestoreid(rs.getString("filestoreid")).letterType(rs.getString("letterType"))
					.LetterNo(rs.getString("letterNumber")).tenantid(rs.getString("tenantid")).auditDetails(auditdetails).build();
			
			noticeMap.put(id, notice);
			}
		}
		return new ArrayList<>(noticeMap.values());
	}

}
