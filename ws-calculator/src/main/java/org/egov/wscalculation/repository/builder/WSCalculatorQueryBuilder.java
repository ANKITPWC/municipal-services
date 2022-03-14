package org.egov.wscalculation.repository.builder;

import java.util.List;
import java.util.Set;

import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class WSCalculatorQueryBuilder {

	@Autowired
	private WSCalculationConfiguration config;

	private static final String Offset_Limit_String = "OFFSET ? LIMIT ?";
	private final static String Query = "SELECT mr.id, mr.connectionNo as connectionId, mr.billingPeriod, mr.meterStatus, mr.lastReading, mr.lastReadingDate, mr.currentReading,"
			+ " mr.currentReadingDate, mr.createdBy as mr_createdBy, mr.tenantid, mr.lastModifiedBy as mr_lastModifiedBy,"
			+ " mr.createdTime as mr_createdTime, mr.lastModifiedTime as mr_lastModifiedTime FROM eg_ws_meterreading mr";

	private final static String noOfConnectionSearchQuery = "SELECT count(*) FROM eg_ws_meterreading WHERE";
    
	private final static String noOfConnectionSearchQueryForCurrentMeterReading= "select mr.currentReading from eg_ws_meterreading mr";
	
	private final static String tenantIdWaterConnectionSearchQuery ="select DISTINCT tenantid from eg_ws_connection";
	
	private final static String connectionNoWaterConnectionSearchQuery = "SELECT conn.connectionNo as conn_no FROM eg_ws_service wc INNER JOIN eg_ws_connection conn ON wc.connection_id = conn.id";
	
	private static final String connectionNoListQuery = "SELECT distinct conn.connectionno, ws.connectionexecutiondate FROM eg_ws_connection conn INNER JOIN eg_ws_service ws ON conn.id = ws.connection_id";

	private static final String distinctTenantIdsCriteria = "SELECT distinct(tenantid) FROM eg_ws_connection ws";

	private  static final String countQuery = "select count(distinct(conn.connectionno)) from eg_ws_connection conn inner join eg_ws_service wc on conn.id=wc.connection_id where conn.tenantid = ? and conn.applicationstatus = 'CONNECTION_ACTIVATED' and conn.isoldapplication = false and wc.connectiontype ='Non Metered' and conn.connectionno is not null and wc.connectionexecutiondate <= ? and conn.connectionno not in (select distinct(consumercode) from egbs_demand_v1 dmd where (dmd.taxperiodfrom >= ? and dmd.taxperiodto <= ?) and businessservice = 'WS' and tenantid = ?)";

	private static String holderSelectValues = "connectionholder.tenantid as holdertenantid, connectionholder.connectionid as holderapplicationId, userid, connectionholder.status as holderstatus, isprimaryholder, connectionholdertype, holdershippercentage, connectionholder.relationship as holderrelationship, connectionholder.createdby as holdercreatedby, connectionholder.createdtime as holdercreatedtime, connectionholder.lastmodifiedby as holderlastmodifiedby, connectionholder.lastmodifiedtime as holderlastmodifiedtime";

	private static final String INNER_JOIN_STRING = "INNER JOIN";

	private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";
	
	private static final String WATER_SEARCH_QUERY = "SELECT conn.*, wc.*, document.*, plumber.*, wc.connectionCategory, wc.connectionType, wc.waterSource, wc.usagecategory,"
			+ " wc.meterId, wc.meterInstallationDate, wc.pipeSize, wc.noOfTaps, wc.proposedPipeSize, wc.proposedTaps, wc.connection_id as connection_Id, wc.connectionExecutionDate, wc.initialmeterreading, wc.appCreatedDate,"
			+ " wc.detailsprovidedby, wc.estimationfileStoreId , wc.sanctionfileStoreId , wc.estimationLetterDate,"
			+ " wc.connectionfacility, wc.noofwaterclosets, wc.nooftoilets,"
			+ " conn.id as conn_id, conn.tenantid, conn.applicationNo, conn.applicationStatus, conn.status, conn.connectionNo, conn.oldConnectionNo, conn.property_id, conn.roadcuttingarea,"
			+ " conn.action, conn.adhocpenalty, conn.adhocrebate, conn.adhocpenaltyreason, conn.applicationType, conn.dateEffectiveFrom,"
			+ " conn.adhocpenaltycomment, conn.adhocrebatereason, conn.adhocrebatecomment, conn.createdBy as ws_createdBy, conn.lastModifiedBy as ws_lastModifiedBy,"
			+ " conn.createdTime as ws_createdTime, conn.lastModifiedTime as ws_lastModifiedTime,conn.additionaldetails, "
			+ " conn.locality, conn.isoldapplication, conn.roadtype, document.id as doc_Id, document.documenttype, document.filestoreid, document.active as doc_active, plumber.id as plumber_id,"
			+ " plumber.name as plumber_name, plumber.licenseno, roadcuttingInfo.id as roadcutting_id, roadcuttingInfo.roadtype as roadcutting_roadtype, roadcuttingInfo.roadcuttingarea as roadcutting_roadcuttingarea, roadcuttingInfo.roadcuttingarea as roadcutting_roadcuttingarea,"
			+ " roadcuttingInfo.active as roadcutting_active, plumber.mobilenumber as plumber_mobileNumber, plumber.gender as plumber_gender, plumber.fatherorhusbandname, plumber.correspondenceaddress,"
			+ " plumber.relationship, " + holderSelectValues
			+ " FROM eg_ws_connection conn "
			+  INNER_JOIN_STRING
			+" eg_ws_service wc ON wc.connection_id = conn.id"
			+  LEFT_OUTER_JOIN_STRING
			+ "eg_ws_applicationdocument document ON document.wsid = conn.id"
			+  LEFT_OUTER_JOIN_STRING
			+ "eg_ws_plumberinfo plumber ON plumber.wsid = conn.id"
			+  LEFT_OUTER_JOIN_STRING
			+ "eg_ws_connectionholder connectionholder ON connectionholder.connectionid = conn.id"
			+  LEFT_OUTER_JOIN_STRING
			+ "eg_ws_roadcuttinginfo roadcuttingInfo ON roadcuttingInfo.wsid = conn.id ";

	private static final String GET_INSTALLMENT_DETAILS_BY_CONSUMER_NO = "select ewi.* from eg_ws_installment ewi inner join (select feetype, min(installmentno) as installmentNo from eg_ws_installment where consumerno = ? and demandid is null group by feetype) ewi2 on ewi2.feetype = ewi.feetype and ewi2.installmentNo = ewi.installmentno where ewi.tenantid = ? and ewi.consumerno = ? and demandid is null";

	private static final String NO_OF_INSTALLMENT_PRESENT_BY_APPLICATIONNO_AND_FEETYPE = "SELECT count(*) FROM eg_ws_installment ewi ";

	private static final String GET_INSTALLMENT_DETAILS_BY_APPLICATION_NO = "select * from eg_ws_installment ewi ";
	
	public String getDistinctTenantIds() {
		return distinctTenantIdsCriteria;
	}
	/**
	 * 
	 * @param criteria
	 *            would be meter reading criteria
	 * @param preparedStatement Prepared SQL Statement
	 * @return Query for given criteria
	 */
	public String getSearchQueryString(MeterReadingSearchCriteria criteria, List<Object> preparedStatement) {
		if(criteria.isEmpty()){return  null;}
		StringBuilder query = new StringBuilder(Query);
		if(!StringUtils.isEmpty(criteria.getTenantId())){
			addClauseIfRequired(preparedStatement, query);
			query.append(" mr.tenantid= ? ");
			preparedStatement.add(criteria.getTenantId());
		}
		if (!CollectionUtils.isEmpty(criteria.getConnectionNos())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" mr.connectionNo IN (").append(createQuery(criteria.getConnectionNos())).append(" )");
			addToPreparedStatement(preparedStatement, criteria.getConnectionNos());
		}
		addOrderBy(query);
		return addPaginationWrapper(query, preparedStatement, criteria);
	}

	private String createQuery(Set<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}

	private void addToPreparedStatement(List<Object> preparedStatement, Set<String> ids) {
		preparedStatement.addAll(ids);
	}

	private void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
		if (values.isEmpty())
			queryString.append(" WHERE ");
		else {
			queryString.append(" AND");
		}
	}

	private String addPaginationWrapper(StringBuilder query, List<Object> preparedStmtList,
			MeterReadingSearchCriteria criteria) {
		query.append(" ").append(Offset_Limit_String);
		Integer limit = config.getMeterReadingDefaultLimit();
		Integer offset = config.getMeterReadingDefaultOffset();

		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMeterReadingDefaultLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMeterReadingDefaultLimit())
			limit = config.getMeterReadingDefaultLimit();

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		preparedStmtList.add(offset);
		preparedStmtList.add(limit + offset);
		return query.toString();
	}

	public String getNoOfMeterReadingConnectionQuery(Set<String> connectionIds, List<Object> preparedStatement) {
		StringBuilder query = new StringBuilder(noOfConnectionSearchQuery);
		query.append(" id in (").append(createQuery(connectionIds)).append(" )");
		addToPreparedStatement(preparedStatement, connectionIds);
		return query.toString();
	}
	
	public String getCurrentReadingConnectionQuery(MeterReadingSearchCriteria criteria,
			List<Object> preparedStatement) {
		if(criteria.isEmpty()){return null;}
		StringBuilder query = new StringBuilder(noOfConnectionSearchQueryForCurrentMeterReading);
		if(!StringUtils.isEmpty(criteria.getTenantId())){
			addClauseIfRequired(preparedStatement, query);
			query.append(" mr.tenantid= ? ");
			preparedStatement.add(criteria.getTenantId());
		}
		if (!CollectionUtils.isEmpty(criteria.getConnectionNos())) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" mr.connectionNo IN (").append(createQuery(criteria.getConnectionNos())).append(" )");
			addToPreparedStatement(preparedStatement, criteria.getConnectionNos());
		}
		query.append(" ORDER BY mr.currentReadingDate DESC LIMIT 1");
		return query.toString();
	}
	
	public String getTenantIdConnectionQuery() {
		return tenantIdWaterConnectionSearchQuery;
	}
	
	private void addOrderBy(StringBuilder query) {
		query.append(" ORDER BY mr.currentReadingDate DESC");
	}
	
	public String getConnectionNumberFromWaterServicesQuery(List<Object> preparedStatement, String connectionType,
			String tenentId) {
		StringBuilder query = new StringBuilder(connectionNoWaterConnectionSearchQuery);
		if (!StringUtils.isEmpty(connectionType)) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" wc.connectionType = ? ");
			preparedStatement.add(connectionType);
		}

		if (!StringUtils.isEmpty(tenentId)) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.tenantId = ? ");
			preparedStatement.add(tenentId);
		}
		return query.toString();

	}
	
	
	public String getConnectionNumberList(String tenantId, String connectionType, String applicationStatus, Boolean isOldApplication,
			List<Object> preparedStatement, List<String> wards, List<String> connectionNos) {
		StringBuilder query = new StringBuilder(connectionNoListQuery);

		// Add connection type
		addClauseIfRequired(preparedStatement, query);
		query.append(" ws.connectiontype = ? ");
		preparedStatement.add(connectionType);
		
		// add tenantid
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.tenantid = ? ");
		preparedStatement.add(tenantId);
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.applicationstatus = ? ");
		preparedStatement.add(applicationStatus);
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.isoldapplication = ? ");
		preparedStatement.add(isOldApplication);
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.connectionno is not null");
		
//		if(!wards.isEmpty()) {
//			addClauseIfRequired(preparedStatement, query);
//			query.append(" conn.additionaldetails ->> 'ward' in (");
//			int wardCount=0;
//			for (String ward : wards) {
//				if(wardCount==0)
//					query.append("?");
//				else
//					query.append(",?");
//				preparedStatement.add(ward);
//				wardCount++;
//			}
//			query.append(")");
//		}
		
		if(!connectionNos.isEmpty()) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.connectionno in (");
			int length = connectionNos.size();
			for (int i = 0; i < length; i++) {
				query.append(" ?");
				if (i != length - 1)
					query.append(",");
				preparedStatement.add(connectionNos.get(i));
			}
			query.append(")");
		}
		return query.toString();
		
	}
	
	public String isBillingPeriodExists(String connectionNo, String billingPeriod, List<Object> preparedStatement) {
		StringBuilder query = new StringBuilder(noOfConnectionSearchQuery);
		query.append(" connectionNo = ? ");
		preparedStatement.add(connectionNo);
		addClauseIfRequired(preparedStatement, query);
		query.append(" billingPeriod = ? ");
		preparedStatement.add(billingPeriod);
		return query.toString();
	}
	
	public String getSearchLatestQueryString(MeterReadingSearchCriteria criteria, List<Object> preparedStatement) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getCountQuery() {
		return countQuery;
	}
	
	public String getConnectionNumberList(String tenantId, String connectionType, List<Object> preparedStatement, Integer batchOffset, Integer batchsize, Long fromDate, Long toDate) {
		//StringBuilder query = new StringBuilder(connectionNoListQuery);

		StringBuilder query = new StringBuilder(WATER_SEARCH_QUERY);

		// add tenantid
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.tenantid = ? ");
		preparedStatement.add(tenantId);
		
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.applicationstatus = 'CONNECTION_ACTIVATED'");
		
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.isoldapplication = false");
		
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.connectionno is not null");
		
		addClauseIfRequired(preparedStatement, query);
		query.append(" wc.connectiontype = ? ");
		preparedStatement.add(connectionType);
		
		// remove the connection which was created after billing period
		addClauseIfRequired(preparedStatement, query);
		query.append(" wc.connectionexecutiondate <= ?");
		preparedStatement.add(toDate);
		
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.connectionno NOT IN (select distinct(consumercode) from egbs_demand_v1 dmd where (dmd.taxperiodfrom >= ? and dmd.taxperiodto <= ?) and businessservice = 'WS' and tenantid=?)");
		preparedStatement.add(fromDate);
		preparedStatement.add(toDate);
		preparedStatement.add(tenantId);

//		addClauseIfRequired(preparedStatement, query);
		String orderbyClause = " and conn.connectionno in (select ewc.connectionno from eg_ws_connection ewc inner join eg_ws_service ews on ews.connection_id = ewc.id where ewc.tenantid = ? and ewc.applicationstatus = 'CONNECTION_ACTIVATED' and ewc.isoldapplication = false and ewc.connectionno is not null and ews.connectiontype = ? and ews.connectionexecutiondate <= ? order by ewc.connectionno offset ? limit ?)";
		preparedStatement.add(tenantId);
		preparedStatement.add(connectionType);
		preparedStatement.add(toDate);
		preparedStatement.add(batchOffset);
		preparedStatement.add(batchsize);
		query.append(orderbyClause);
		
		return query.toString();
		
	}
	
	public String getConnectionNumberList(String tenantId, String connectionType, List<Object> preparedStatement, Long fromDate, Long toDate, List<String> connectionNos) {
		//StringBuilder query = new StringBuilder(connectionNoListQuery);

		StringBuilder query = new StringBuilder(WATER_SEARCH_QUERY);

		// add tenantid
		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.tenantid = ? ");
		preparedStatement.add(tenantId);

		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.applicationstatus = 'CONNECTION_ACTIVATED'");

		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.isoldapplication = false");

		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.connectionno is not null");

		addClauseIfRequired(preparedStatement, query);
		query.append(" wc.connectiontype = ? ");
		preparedStatement.add(connectionType);

		// remove the connection which was created after billing period
		addClauseIfRequired(preparedStatement, query);
		query.append(" wc.connectionexecutiondate <= ?");
		preparedStatement.add(toDate);

		// added for connection wise bill generation
		if(!connectionNos.isEmpty()) {
			addClauseIfRequired(preparedStatement, query);
			query.append(" conn.connectionno in (");
			int length = connectionNos.size();
			for (int i = 0; i < length; i++) {
				query.append(" ?");
				if (i != length - 1)
					query.append(",");
				preparedStatement.add(connectionNos.get(i));
			}
			query.append(")");
		}

		addClauseIfRequired(preparedStatement, query);
		query.append(" conn.connectionno NOT IN (select distinct(consumercode) from egbs_demand_v1 dmd where (dmd.taxperiodfrom >= ? and dmd.taxperiodto <= ?) and businessservice = 'WS' and tenantid=?)");
		preparedStatement.add(fromDate);
		preparedStatement.add(toDate);
		preparedStatement.add(tenantId);

		return query.toString();

	}
	
	public String getInstallmentByConsumerNo(String tenantId, String consumerNo, List<Object> preparedStatement) {
		StringBuilder query = new StringBuilder(GET_INSTALLMENT_DETAILS_BY_CONSUMER_NO);
		preparedStatement.add(consumerNo);
		preparedStatement.add(tenantId);
		preparedStatement.add(consumerNo);
		return query.toString();
	}

	public String getInstallmentCountByApplicationNoAndFeeType(String tenantId, String applicationNo, String feeType, List<Object> preparedStatement) {
		StringBuilder query = new StringBuilder(NO_OF_INSTALLMENT_PRESENT_BY_APPLICATIONNO_AND_FEETYPE);
		if(!StringUtils.isEmpty(tenantId)){
			addClauseIfRequired(preparedStatement, query);
			query.append(" ewi.tenantId= ? ");
			preparedStatement.add(tenantId);
		}
		if(!StringUtils.isEmpty(applicationNo)){
			addClauseIfRequired(preparedStatement, query);
			query.append(" ewi.applicationno= ? ");
			preparedStatement.add(applicationNo);
		}
		if(!StringUtils.isEmpty(feeType)){
			addClauseIfRequired(preparedStatement, query);
			query.append(" ewi.feetype= ? ");
			preparedStatement.add(feeType);
		}

		return query.toString();
	}

	public String getInstallmentByApplicationNo(String tenantId, String applicationNo, List<Object> preparedStatement) {
		StringBuilder query = new StringBuilder(GET_INSTALLMENT_DETAILS_BY_APPLICATION_NO);

		addClauseIfRequired(preparedStatement, query);
		query.append(" tenantid = ? ");
		preparedStatement.add(tenantId);

		addClauseIfRequired(preparedStatement, query);
		query.append(" applicationno = ? ");
		preparedStatement.add(applicationNo);

		addClauseIfRequired(preparedStatement, query);
		query.append(" installmentno = 1 ");

		return query.toString();
  }

}
