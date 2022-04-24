package org.egov.wscalculation.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.producer.WSCalculationProducer;
import org.egov.wscalculation.repository.builder.WSCalculatorQueryBuilder;
import org.egov.wscalculation.repository.rowmapper.AnnualAdvanceRowMapper;
import org.egov.wscalculation.repository.rowmapper.DemandSchedulerRowMapper;
import org.egov.wscalculation.repository.rowmapper.InstallmentRowMapper;
import org.egov.wscalculation.repository.rowmapper.MeterReadingCurrentReadingRowMapper;
import org.egov.wscalculation.repository.rowmapper.MeterReadingRowMapper;
import org.egov.wscalculation.repository.rowmapper.WaterRowMapper;
import org.egov.wscalculation.web.models.AnnualAdvance;
import org.egov.wscalculation.web.models.AnnualAdvanceRequest;
import org.egov.wscalculation.web.models.BillSchedulerCriteria;
import org.egov.wscalculation.web.models.Installments;
import org.egov.wscalculation.web.models.MeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;
import org.egov.wscalculation.web.models.WaterConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class WSCalculationDaoImpl implements WSCalculationDao {

	@Autowired
	private WSCalculationProducer wSCalculationProducer;

	@Autowired
	private WSCalculatorQueryBuilder queryBuilder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MeterReadingRowMapper meterReadingRowMapper;
	
	@Autowired
	private MeterReadingCurrentReadingRowMapper currentMeterReadingRowMapper;
	
	@Autowired
	private DemandSchedulerRowMapper demandSchedulerRowMapper;
	
	@Autowired
	private WaterRowMapper waterRowMapper;
	
	@Autowired
	private InstallmentRowMapper installmentRowMapper;
	
	@Autowired
	private AnnualAdvanceRowMapper annualAdvanceRowMapper;
	

	@Value("${egov.meterservice.createmeterconnection}")
	private String createMeterConnection;
	
	@Value("${egov.meterservice.updatemeterconnection}")
	private String updateMeterConnection;
	
	@Value("${kafka.topic.ws.annual.advance.create}")
    private String createAnnualAdvance;

	/**
	 * 
	 * @param meterConnectionRequest
	 *            MeterConnectionRequest contains meter reading connection to be
	 *            created
	 */
	@Override
	public void saveMeterReading(MeterConnectionRequest meterConnectionRequest) {
		wSCalculationProducer.push(createMeterConnection, meterConnectionRequest);
	}
	/**
	 * 
	 * @param criteria would be meter reading criteria
	 * @return List of meter readings based on criteria
	 */
	@Override
	public List<MeterReading> searchMeterReadings(MeterReadingSearchCriteria criteria) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getSearchQueryString(criteria, preparedStatement);
		if(query == null)
			return Collections.emptyList();
		log.debug("Query: " + query);
		log.debug("Prepared Statement" + preparedStatement.toString());
		return jdbcTemplate.query(query, preparedStatement.toArray(), meterReadingRowMapper);
	}
	
	@Override
	public List<MeterReading> searchCurrentMeterReadings(MeterReadingSearchCriteria criteria) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getCurrentReadingConnectionQuery(criteria, preparedStatement);
		if (query == null)
			return Collections.emptyList();
		log.debug("Query: " + query);
		log.debug("Prepared Statement" + preparedStatement.toString());
		return jdbcTemplate.query(query, preparedStatement.toArray(), currentMeterReadingRowMapper);
	}

	/**
	 * 
	 * @param ids
	 *            of string of connection ids on which search is performed
	 * @return total number of meter reading objects if present in the table for
	 *         that particular connection ids
	 */
	@Override
	public int isMeterReadingConnectionExist(List<String> ids) {
		Set<String> connectionIds = new HashSet<>(ids);
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getNoOfMeterReadingConnectionQuery(connectionIds, preparedStatement);
		log.debug("Query: " + query);
		return jdbcTemplate.queryForObject(query, preparedStatement.toArray(), Integer.class);
	}
	
	@Override
	public ArrayList<String> searchTenantIds() {
		ArrayList<String> tenantIds = new ArrayList<>();
		String query = queryBuilder.getTenantIdConnectionQuery();
		if (query == null)
			return tenantIds;
		log.debug("Query: " + query);
		tenantIds = (ArrayList<String>) jdbcTemplate.queryForList(query, String.class);
		return tenantIds;
	}
	
	@Override
	public ArrayList<WaterConnection> searchConnectionNos(String connectionType,String tenantId) {
		ArrayList<WaterConnection> connectionNos = new ArrayList<>();
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getConnectionNumberFromWaterServicesQuery(preparedStatement,connectionType, tenantId);
		if (query == null)
			return connectionNos;
		log.info("Query: " + query);

		connectionNos = (ArrayList<WaterConnection>)jdbcTemplate.query(query,preparedStatement.toArray(),demandSchedulerRowMapper);
		return connectionNos;
	}
	
	@Override
	public List<WaterConnection> getConnectionsNoList(String tenantId, String connectionType, BillSchedulerCriteria billCriteria) {
		List<Object> preparedStatement = new ArrayList<>();
		String applicationStatus = WSCalculationConstant.WATER_CONNECTION_APP_STATUS_ACTIVATED_STRING;
		Boolean isOldApplication = Boolean.FALSE;
		List<String> wards = new ArrayList<>();
		if(billCriteria != null && billCriteria.getWards() != null) {
			wards = billCriteria.getWards().stream().filter(ward -> ward.getTenant().equalsIgnoreCase(tenantId)).flatMap(ward -> ward.getWards().stream()).map(String::trim).collect(Collectors.toList());
		}
		List<String> connectionNos = billCriteria.getConnectionNos();
		String query = queryBuilder.getConnectionNumberList(tenantId, connectionType, applicationStatus, isOldApplication, preparedStatement, wards, connectionNos);
		log.info("water " + connectionType + " connection list : " + query);
		return jdbcTemplate.query(query, preparedStatement.toArray(), demandSchedulerRowMapper);
	}

	@Override
	public List<String> getTenantId() {
		String query = queryBuilder.getDistinctTenantIds();
		log.info("Tenant Id's List Query : " + query);
		return jdbcTemplate.queryForList(query, String.class);
	}
	
	@Override
	public int isBillingPeriodExists(String connectionNo, String billingPeriod) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.isBillingPeriodExists(connectionNo, billingPeriod, preparedStatement);
		log.info("Is BillingPeriod Exits Query: " + query);
		return jdbcTemplate.queryForObject(query, preparedStatement.toArray(), Integer.class);
	}
	
	/**
	 * 
	 * @param meterConnectionRequest
	 *            MeterConnectionRequest contains meter reading connection to be
	 *            created
	 */
	@Override
	public void updateMeterReading(MeterConnectionRequest meterConnectionRequest) {
		wSCalculationProducer.push(updateMeterConnection, meterConnectionRequest);
	}
	
	@Override
	public long getConnectionCount(String tenantid, Long fromDate, Long toDate, boolean connectionWise, List<String> connectionNos){
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getCountQuery();
		preparedStatement.add(tenantid);
		preparedStatement.add(toDate);
		preparedStatement.add(fromDate);
		preparedStatement.add(toDate);
		preparedStatement.add(tenantid);

		StringBuilder queryBuilder = new StringBuilder(query);

		if(connectionWise) {
			// added for connection wise bill generation
			if(!connectionNos.isEmpty()) {
				queryBuilder.append(" and conn.connectionno in (");
				int length = connectionNos.size();
				for (int i = 0; i < length; i++) {
					queryBuilder.append(" ?");
					if (i != length - 1)
						queryBuilder.append(",");
					preparedStatement.add(connectionNos.get(i));
				}
				queryBuilder.append(")");
			}
		}

		long count = jdbcTemplate.queryForObject(queryBuilder.toString(), preparedStatement.toArray(), Integer.class);
		return count;
	}
	
	@Override
	public List<WaterConnection> getConnectionsNoList(String tenantId, String connectionType, Integer batchOffset, Integer batchsize, Long fromDate, Long toDate) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getConnectionNumberList(tenantId, connectionType, preparedStatement, batchOffset, batchsize, fromDate, toDate);
		log.info("connection " + connectionType + " connection list : " + query);
		return jdbcTemplate.query(query, preparedStatement.toArray(), waterRowMapper);
	}
	
	@Override
	public List<WaterConnection> getConnectionsNoList(String tenantId, String connectionType, Long fromDate, Long toDate, List<String> connectionNos) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getConnectionNumberList(tenantId, connectionType, preparedStatement, fromDate, toDate, connectionNos);
		log.info("connection " + connectionType + " connection list : " + query);
		return jdbcTemplate.query(query, preparedStatement.toArray(), waterRowMapper);
	}
	
	@Override
	public List<Installments> getApplicableInstallmentsByConsumerNo(String tenantId, String consumerNo) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getInstallmentByConsumerNo(tenantId, consumerNo, preparedStatement);
		log.info(" getAllInstallmentsByConsumerNo query : " + query);
		return jdbcTemplate.query(query, preparedStatement.toArray(), installmentRowMapper);
	}

	@Override
	public List<Installments> getApplicableInstallmentsByApplicationNo(String tenantId, String applicationNo) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getInstallmentByApplicationNo(tenantId, applicationNo, preparedStatement);
		log.info(" getAllInstallmentsByApplicationNo query : " + query);
		return jdbcTemplate.query(query, preparedStatement.toArray(), installmentRowMapper);
	}

	@Override
	public int getInstallmentCountByApplicationNoAndFeeType(String tenantId, String applicationNo, String feeType) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getInstallmentCountByApplicationNoAndFeeType(tenantId, applicationNo, feeType, preparedStatement);
		log.info(" getAllInstallmentsByApplicationNoAndFeeType query : " + query);
		return jdbcTemplate.queryForObject(query, preparedStatement.toArray(), Integer.class);
	}
	
	@Override
	public void saveAnnualAdvance(AnnualAdvanceRequest annualAdvanceRequests) {
		wSCalculationProducer.push(createAnnualAdvance, annualAdvanceRequests);
	}

	@Override
	public List<AnnualAdvance> getAnnualAdvance(String tenantId, String connectionNo, String finYear) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = queryBuilder.getAnnualAdvance(tenantId, connectionNo, finYear, preparedStatement);
		log.info(" Annual advance search query: " + query);
		return jdbcTemplate.query(query, preparedStatement.toArray(), annualAdvanceRowMapper);
	}
}
