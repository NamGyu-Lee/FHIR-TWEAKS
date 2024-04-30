package ca.uhn.fhir.jpa.starter.util;

import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Performance checker.
 */
public class PerformanceChecker {

	private boolean isPrintEachEndTimer;

	private boolean isPrintTimeStack;

	private Map<String, Integer> errCounterMap;

	public PerformanceChecker(boolean isPrintEachEndTimer, boolean isPrintTimeStack){
		timeLogger = new HashMap<>();
		this.isPrintEachEndTimer = isPrintEachEndTimer;
		this.isPrintTimeStack = isPrintTimeStack;
		initTime = System.nanoTime();
		errCounterMap = new HashMap<>();
	}

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(PerformanceChecker.class);

	private long initTime;

	private long startTime;

	private Map<String, List<Double>> timeLogger;

	public void startTimer(){
		startTime = System.nanoTime();
	}

	public void endTimer(String arg){
		long endTime = System.nanoTime();
		double duration = (endTime - startTime) / 1_000_000.0;
		if(isPrintEachEndTimer){
			System.out.println("-------------------");
			System.out.println(arg + " // Execution time: " + duration + " ms");
			System.out.println("-------------------");
		}

		List<Double> timeStack;
		if(timeLogger.get(arg) == null){
			timeStack = new ArrayList<>();
		}else{
			timeStack = timeLogger.get(arg);
		}
		timeStack.add(duration);
		timeLogger.put(arg, timeStack);
	}

	public double printAllTimeStack(){
		if(isPrintTimeStack){
			for(String key : timeLogger.keySet()){
				//System.out.println("-------------------");
				System.out.println("★ " + key);
				List<Double> timeStack = timeLogger.get(key);
				for(Double dob : timeStack){
					System.out.println(dob);
				}
				//System.out.println("-------------------");
			}

			long endTime = System.nanoTime();
			double duration = (endTime - initTime) / 1_000_000.0;
			System.out.println("작업 종료. 작업 시간... : " + duration  + " ms");
			return duration;
		}
			return 0.0;
	}

	public void addErrorCounter(String errorReason){
		if(errCounterMap.keySet().contains(errorReason)){
			errCounterMap.put(errorReason , errCounterMap.get(errorReason)+1);
		}else{
			errCounterMap.put(errorReason, 1);
		}
	}

	public void exportStackToExcel(String fileName) throws IOException {
		try (Workbook workbook = new XSSFWorkbook()) { // XSSFWorkbook for .xlsx files
			Sheet sheet = workbook.createSheet("Data");

			// 헤더 생성
			Row headerRow = sheet.createRow(0);
			int columnIndex = 0;
			for (String header : timeLogger.keySet()) {
				headerRow.createCell(columnIndex).setCellValue(header);
				columnIndex++;
			}

			// 가장 긴 리스트의 길이를 찾아서 행의 수를 결정
			int maxRows = timeLogger.values().stream().mapToInt(List::size).max().orElse(0);

			// 데이터 쓰기
			for (int i = 0; i < maxRows; i++) {
				Row row = sheet.createRow(i + 1); // +1 because header row is the first
				int cellIndex = 0;
				for (List<Double> values : timeLogger.values()) {
					if (i < values.size()) {
						row.createCell(cellIndex).setCellValue(values.get(i));
					} // 값이 없는 경우 셀을 비워둡니다.
					cellIndex++;
				}
			}

			// 열 너비 자동 조정
			for (int i = 0; i < timeLogger.keySet().size(); i++) {
				sheet.autoSizeColumn(i);
			}

			// 파일로 쓰기
			try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
				workbook.write(outputStream);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
