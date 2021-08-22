package org.egov.migration.business.controller;

import java.io.IOException;

import javax.validation.Valid;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.egov.migration.common.model.MigrationRequest;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.processor.WnsMigrationJobExecutionListner;
import org.egov.migration.service.WnsService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WnsBatchTriggerController {
	
	@Autowired
    public JobBuilderFactory jobBuilderFactory;
	
	@Autowired
    JobLauncher jobLauncher;
	
	@Autowired
	@Qualifier("stepWnsMigrate")
	Step stepWnsMigrate;
	
	@Autowired
	PropertiesData properties;
	
	@Autowired
	RecordStatistic recordStatistic;
	
	@Autowired
	WnsService wnsService;
	
	@Autowired
	WnsMigrationJobExecutionListner wnsMigrationJobExecutionListner;
	
	@PostMapping("/wns-migrate/run")
	public void runWnsMigration(@RequestBody @Valid MigrationRequest request) throws InvalidFormatException, IOException {
		properties.setAuthToken(request.getAuthToken());
		
		// Scanning of folder
		String fileName = "puri.xlsx";
		String file = properties.getWnsDataFileDirectory().concat("\\").concat(fileName);
        try {
        	recordStatistic.getErrorRecords().clear();
        	recordStatistic.getSuccessRecords().clear();
        	
        	Job job = jobBuilderFactory.get("firstBatchJob")
        			.incrementer(new RunIdIncrementer())
        			.listener(wnsMigrationJobExecutionListner)
        			.flow(stepWnsMigrate).end().build();
        	
        	JobParameters jobParameters = new JobParametersBuilder()
        			.addLong("time", System.currentTimeMillis())
        			.addString("filePath", file)
        			.addString("fileName", fileName)
        			.toJobParameters();
        	
			jobLauncher.run(job, jobParameters);
			wnsService.writeExecutionTime();
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			e.printStackTrace();
		}
	}
}
