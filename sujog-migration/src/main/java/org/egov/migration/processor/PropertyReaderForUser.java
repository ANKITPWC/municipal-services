package org.egov.migration.processor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.migration.mapper.OwnerRowMapper;
import org.egov.migration.mapper.PropertyRowMapper;
import org.egov.migration.reader.model.Owner;
import org.egov.migration.reader.model.Property;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyReaderForUser implements ItemReader<Property> {
	
	private String file;
	private String ulb;
	
	private int propertyRowIndex;
	private Iterator<Row> propertyRowIterator;
	private int skipRecord;
	
	private Map<String, Integer> propertyColMap;
	private Map<String, Integer> ownerColMap;
	
	private Map<String, List<Integer>> ownerRowMap;
	
	private Sheet ownerSheet;
	
	@Autowired
	private PropertyRowMapper propertyRowMapper;
	
	@Autowired
	private OwnerRowMapper ownerRowMapper;
	
	public PropertyReaderForUser() throws EncryptedDocumentException, IOException, Exception {
		this.propertyRowIndex = 0;
	}
	
	public PropertyReaderForUser(String file) throws EncryptedDocumentException, IOException, Exception {
		this.file = file;
		this.propertyRowIndex = 0;
	}
	
	@Value("#{jobParameters['filePath']}")
	public void setFile(final String fileName) {
		this.file = fileName;
	}
	
	public String getFile() {
		return file;
	}

	public void setSkipRecord(int skipRecord) {
		this.skipRecord = skipRecord;
	}

	@Override
	public Property read()
			throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if(this.propertyRowIndex==0) {
			readProperty();
			skipRecord();
		}
		
		if(this.propertyRowIterator.hasNext()) {
			this.propertyRowIndex++;
			return getProperty(this.propertyRowIterator.next());
		}
		
		return null;
	}
	
	private Property getProperty(Row propertyRow) {
		Property property = new Property();
		try {
			property = propertyRowMapper.mapRow(this.ulb, propertyRow, this.propertyColMap);
			log.info("Property: "+property.getPropertyId()+" reading...");
			if(MigrationUtility.isPropertyEmpty(property)) {
				return null;
			}
			List<Owner> owners = getOwner(property.getPropertyId());
			
			property.setOwners(owners);
			log.info("Property: "+property.getPropertyId()+" read successfully");
			
		} catch (Exception e) {
			String propertyId = propertyColMap.get(MigrationConst.COL_PROPERTY_ID)==null ? null : MigrationUtility.readCellValue(propertyRow.getCell(propertyColMap.get(MigrationConst.COL_PROPERTY_ID)), false);
			log.error(String.format("Some exception generated while reading property %s", property.getPropertyId()));
			MigrationUtility.addError(propertyId, "Not able to read the data. Check the data");
			MigrationUtility.addError(propertyId, e.getMessage());
		}
		
		return property;
		
	}


	private List<Owner> getOwner(String propertyId) {
		if(this.ownerRowMap.get(propertyId)==null)
			return null;
		return this.ownerRowMap.get(propertyId).stream().map(rowIndex -> ownerRowMapper.mapRow(this.ulb, ownerSheet.getRow(rowIndex), this.ownerColMap)).collect(Collectors.toList());
	}

	private void skipRecord() {
		for(int i=1; i<=this.skipRecord; i++) {
			if(propertyRowIterator.hasNext()) {
				this.propertyRowIterator.next();
				this.propertyRowIndex++;
			}
		}
	}

	private void readProperty() throws Exception,EncryptedDocumentException, IOException {
		File inputFile = new File(this.file);
		this.ulb = inputFile.getName().split("\\.")[0].toLowerCase();
		
		Workbook workbook = WorkbookFactory.create(inputFile);
		Sheet propertySheet = workbook.getSheet(MigrationConst.SHEET_PROPERTY);
		this.propertyRowIterator = propertySheet.iterator();
		updateOwnerRowMap(workbook);
		
		updatePropertyColumnMap(propertySheet.getRow(0));
	}
	
	private void updateOwnerRowMap(Workbook workbook) throws EncryptedDocumentException, IOException {
		this.ownerRowMap = new HashMap<>();
		this.ownerSheet = workbook.getSheet(MigrationConst.SHEET_OWNER);
		this.ownerSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updateOwnerColumnMap(row);
			} else {
				String propertyId = MigrationUtility.readCellValue(row.getCell(this.ownerColMap.get(MigrationConst.COL_PROPERTY_ID)), false);
				List<Integer> list = this.ownerRowMap.get(propertyId);
				if(list == null) {
					list = new ArrayList<>();
				}
				list.add(row.getRowNum());
				this.ownerRowMap.put(MigrationUtility.removeLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.ownerColMap.get(MigrationConst.COL_PROPERTY_ID)), false)), list);
			}
		});
	}
	
	private void updateOwnerColumnMap(Row row) {
		this.ownerColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.ownerColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}

	private void updatePropertyColumnMap(Row row) {
		this.propertyColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.propertyColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}
	
}
