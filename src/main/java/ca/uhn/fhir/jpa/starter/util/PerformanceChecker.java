package ca.uhn.fhir.jpa.starter.util;

import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;

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

	public PerformanceChecker(boolean isPrintEachEndTimer, boolean isPrintTimeStack){
		timeLogger = new HashMap<>();
		this.isPrintEachEndTimer = isPrintEachEndTimer;
		this.isPrintTimeStack = isPrintTimeStack;
		initTime = System.nanoTime();
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

	public void printAllTimeStack(){
		if(isPrintTimeStack){
			for(String key : timeLogger.keySet()){
				System.out.println("-------------------");
				System.out.println(key);
				List<Double> timeStack = timeLogger.get(key);
				for(Double dob : timeStack){
					System.out.println(dob);
				}
				System.out.println("-------------------");
			}

			long endTime = System.nanoTime();
			double duration = (endTime - initTime) / 1_000_000.0;
			System.out.println("작업 종료. 작업 시간... : " + duration  + " ms");
		}
	}

}
