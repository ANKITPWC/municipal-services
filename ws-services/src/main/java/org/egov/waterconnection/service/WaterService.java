package org.egov.waterconnection.service;

import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.waterconnection.web.models.SearchCriteria;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.WaterConnectionResponse;

public interface WaterService {

	List<WaterConnection> createWaterConnection(WaterConnectionRequest waterConnectionRequest);

	List<WaterConnection> search(SearchCriteria criteria, RequestInfo requestInfo);
	
	List<WaterConnection> updateWaterConnection(WaterConnectionRequest waterConnectionRequest);

	List<WaterConnection> migrateWaterConnection(WaterConnectionRequest waterConnectionRequest);

	List<WaterConnection> migrateSewerageConnection(WaterConnectionRequest waterConnectionRequest);
	
	WaterConnectionResponse planeSearch(SearchCriteria criteria, RequestInfo requestInfo);

}
