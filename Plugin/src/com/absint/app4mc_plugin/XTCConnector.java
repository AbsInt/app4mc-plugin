/*
 * The MIT License
 *
 * Copyright (c) 2018, AbsInt Angewandte Informatik GmbH
 * Author: Simon Wegener
 * Email: swegener@absint.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.absint.app4mc_plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.app4mc.amalthea.model.util.*;
import org.eclipse.app4mc.amalthea.model.InstructionsConstant;
import org.eclipse.app4mc.amalthea.model.Runnable;
import org.eclipse.app4mc.amalthea.model.RunnableItem;
import org.eclipse.app4mc.amalthea.model.SWModel;
import org.eclipse.app4mc.amalthea.model.Task;
import org.eclipse.app4mc.amalthea.model.RunnableInstructions;

import org.eclipse.app4mc.amalthea.workflow.core.Context;
import org.eclipse.app4mc.amalthea.workflow.core.WorkflowComponent;
import org.eclipse.app4mc.amalthea.workflow.core.exception.WorkflowException;

import org.eclipse.emf.common.util.EList;


// XMLMemento for writing XML files
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

public class XTCConnector extends WorkflowComponent {

	protected void checkInternal() throws WorkflowException {
		this.log.info("checkInternal: ");

		if (workingDirectory == null) {
			throw new WorkflowException("The working directory must be set before running this workflow.");
		}
		
		if (xtcLocation == null) {
			throw new WorkflowException("The location of the XTC file must be set before running this workflow.");
		}
		
		if (elfLocation == null) {
			throw new WorkflowException("The location of the binary executable (.elf file) must be set before running this workflow.");
		}
	
		if (xmlResultLocation == null) {
			throw new WorkflowException("The location of the XML result file must be set before running this workflow.");
		}
			
		if (taskPrefix == null) {
			taskPrefix = "";
		}
		
		if (runnablePrefix == null) {
			runnablePrefix = "";
		}
	}

	private EList<Runnable> runnables;
	private EList<Task> tasks;
	private String startDateTime;
	private String stopDateTime;
	
		
	@Override
	protected void runInternal(Context ctx) throws WorkflowException {
		this.log.info("runInternal: ");

		SWModel swModel = getAmaltheaModel(ctx).getSwModel();
		
		// some checking if sw model is available
		if (null == swModel) {
			throw new WorkflowException("No proper SWModel available!");
		}

		runnables = swModel.getRunnables();
		tasks = swModel.getTasks();
		
		try {
			// get current date and time
			startDateTime = new Date().toString();
			
			// write XTC file
			writeXTC(mode);
						
			// call alauncher to process the XTC file
			processXTC();
			
			// retrieve the results from the XTC file
			retrieveTimingData();
		} catch (IOException e) {
			throw new WorkflowException("IOException: " + e.getMessage());
		} catch (InterruptedException e) {
			throw new WorkflowException("InterruptedException: " + e.getMessage());
		} catch (WorkbenchException e) {
			throw new WorkflowException("WorkbenchException: " + e.getMessage());
		} finally {
			// get current date and time
			stopDateTime = new Date().toString();
		}
		
		this.log.info("runInternal: START: " + startDateTime);
		this.log.info("runInternal: STOP: " + stopDateTime);
		System.out.println("runInternal: START: " + startDateTime);
		System.out.println("runInternal: STOP: " + stopDateTime);
	}

	private String mode = "Runnables"; // FIXME
		
	public void setMode (String str) {
		mode = str;
	}
	
	private String taskPrefix;
	private String runnablePrefix;
	
	public void setTaskPrefix (String prefix) {
		taskPrefix = prefix;		
	}
	
	public void setRunnablePrefix (String prefix) {
		runnablePrefix = prefix;		
	}
	
	private String workingDirectory;
	
	public void setWorkingDirectory (String location) {
		workingDirectory = location;
	}
	
	private Double frequencyInMHz = 300.0; // FIXME
	
	public void setFrequencyInMHz (Double frequency) {
		frequencyInMHz = frequency;
	}
	
	private String xtcLocation;
	private String elfLocation;
	private String aisLocation;
	private String xmlReportLocation;
	private String xmlResultLocation;
	private String traceLocation;
		
	public void setXtcLocation (String location) {
		xtcLocation = location;
	}
	
	public void setElfLocation (String location) {
		elfLocation = location;
	}

	public void setAisLocation (String location) {
		aisLocation = location;
	}
	
	public void setXmlReportLocation (String location) {
		xmlReportLocation = location;
	}

	public void setXmlResultLocation (String location) {
		xmlResultLocation = location;
	}
	
	public void setTraceLocation (String location) {
		traceLocation = location;
	}
	
	private Set<String> ignoredRunnables = new HashSet<String>();
	private Map<String, String> mappedRunnables = new HashMap<String, String>();
	
	public void ignoreRunnable (String runnableName) {
		ignoredRunnables.add(runnableName);
	}
	
	public void mapRunnable (String sourceRunnableName, String targetRunnableName) {
		mappedRunnables.put(sourceRunnableName, targetRunnableName);
	}
	
	private static final String VENDOR_STRING = "AMALTHEA XTC Connector v0.7";
	
	private String modelName;

	public void setModelName (String name) {
		modelName = name;
	}
	
	// XTC specific stuff
	private static final String XTC_CPU_NAME = "Generic Multicore CPU";
	private String xtcRequestMode = "batch";
	private String xtcRequestType = "TimingProfiler";
	private String xtcRequestOptionCpu = "tricore";
	private String xtcRequestOptionTarget = "tc277";
	private String xtcRequestOptionTraceFormat;
	
	public void setXtcRequestMode (String mode) {
		xtcRequestMode = mode;
	}
	
	public void setXtcRequestType (String type) {
		xtcRequestType = type;
	}
	
	public void setXtcRequestTarget (String cpu, String target) {
		xtcRequestOptionCpu = cpu;
		xtcRequestOptionTarget = target;
	}
	
	public void setXtcRequestTraceFormat (String traceFormat) {
		xtcRequestOptionTraceFormat = traceFormat;
	}
	
	private void writeXTC(String mode) throws WorkflowException, IOException {
		this.log.info("Writing XTC file...");

		// create <xtc> tag
		XMLMemento xtc = XMLMemento.createWriteRoot("xtc");
		xtc.putString("xmlns", "http://www.all-times.org/xtc");
		xtc.putString("schemaLocation", "http://www.absint.com/xtc xtc-v2.6.xsd");
		xtc.putString("version", "2.6");

		// create <common> tag
		IMemento common = xtc.createChild("common");

		// create <CPU> tag
		IMemento cpu = common.createChild("CPU");
		cpu.putString("name", XTC_CPU_NAME);
		cpu.putString("id", "CPU_ID_0");

		// set .elf file
		cpu.putString("file", elfLocation);

		// do stuff either for Runnables or Tasks
		if (mode.equals("Runnables")) {
			for (int i = 0; i < runnables.size(); i++) {
	        	writeRequestForRunnable(runnables.get(i), cpu, i, null, 0);
			}
		} else {
			for (int i = 0; i < tasks.size(); i++) {
				Task task = tasks.get(i);

				writeTaskAisFile(task);
				
				writeRequestForTask(cpu, 0, task, i);
				/*
				Set<Runnable> runnablesOfTask = SoftwareUtil.getRunnableSet(task, null);
							
				// generate unique identifiers
				int id = 0;
				for (Runnable runnable : runnablesOfTask) {
					writeRequestForRunnable(runnable, cpu, ++id, task, i);
				}
				*/
			}
		}

		xtc.save(new java.io.FileWriter(xtcLocation));

		this.log.info("Finished writing '" + xtcLocation + "'.");
	}
	
	private void writeTaskAisFile(Task task) {
		String name = taskPrefix + task.getName();
		
		BufferedWriter writer = null;
        try {
            File logFile = new File(workingDirectory + "/" + name + ".ais");

            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write("# Runnables called in task '" + name + "'.\n");
            
			Set<Runnable> runnablesOfTask = SoftwareUtil.getRunnableSet(task, null);

			for (Runnable runnable : runnablesOfTask) {
				String runnableName = runnablePrefix + runnable.getName();
				writer.write("routine '" + runnableName + "' additional start;\n");
			}
                        
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
	}

	private void writeRequestForRunnable(Runnable runnable, IMemento cpu, int runnableId, Task task, int taskId) throws WorkflowException {
		this.log.info("writeRequest: ");

		String name = runnablePrefix + runnable.getName();
		
		// ignore some runnables
		if (ignoredRunnables.contains(name))
			return;
		
		// rename some runnables
		name = mappedRunnables.getOrDefault(name, name);
		
		String localAisFile = null;
		if (task != null) {
			localAisFile = task.getName() + ".ais";
		}
		
		writeRequest(name, "Task", localAisFile, cpu, runnableId, taskId);
	}
	
	private void writeRequestForTask(IMemento cpu, int runnableId, Task task, int taskId) throws WorkflowException {
		this.log.info("writeRequest: ");

		String name = taskPrefix + task.getName();
		
		writeRequest(name, "Task", name + ".ais", cpu, runnableId, taskId);
	}

	private void writeRequest(String analysisEntry, String entryType, String localAisFile, IMemento cpu, int runnableId, int taskId) throws WorkflowException {
		this.log.info("writeRequest: ");

		// create <executable> tag
		// set entry and analysis id
		IMemento executable = cpu.createChild("executable");
		executable.putString("start", analysisEntry);
		executable.putString("name", analysisEntry);
		executable.putString("type", "runnable");
		executable.putString("id", "EXEC_ID_" + taskId + "_" + runnableId);

		// create <mode> tag
		IMemento mode = executable.createChild("mode");
		mode.putString("id", "MODE_ID_" + taskId + "_" + runnableId);

		// create <description> tag
		IMemento description = mode.createChild("description");
		description.putTextData("AMALTHEA Model '" + modelName + "' - " + entryType + " '" + analysisEntry + "'");

		// create <request> tag
		// set analysis mode and analysis type
		IMemento request = mode.createChild("request");
		request.putString("mode", xtcRequestMode);
		request.putString("type", xtcRequestType);
		request.putString("vendor", VENDOR_STRING);

		// set various options
		writeXtcOptionElement(request, "a3:cpu", xtcRequestOptionCpu);
		writeXtcOptionElement(request, "a3:target", xtcRequestOptionTarget);
		writeXtcOptionElement(request, "a3:global_ais_file", aisLocation);
		writeXtcOptionElement(request, "a3:ais_file", localAisFile);
		writeXtcOptionElement(request, "a3:xml_report_file", xmlReportLocation);
		writeXtcOptionElement(request, "a3:xml_result_file", xmlResultLocation);
		writeXtcOptionElement(request, "a3:traces", traceLocation);
		writeXtcOptionElement(request, "a3:trace_format", xtcRequestOptionTraceFormat);
	}
		
	private void writeXtcOptionElement (IMemento request, String key, String value) {
		if (key != null && value != null) {
			IMemento option_cpu = request.createChild("option");
			option_cpu.putString("name", key);
			option_cpu.putString("value", value);
		}
	}
	
	private void updateRunnable(Runnable runnable, long max) {
		this.log.info("writeRequest: ");
		
		String name = runnablePrefix + runnable.getName();

		// ignore some runnables
		if (ignoredRunnables.contains(name))
			return;
		
		System.out.println("updateRunnable:" + name);
		
		EList<RunnableItem> runnableItems = runnable.getRunnableItems();
		
		// check whether some RunnableInstruction is already present
		for (int j = 0; j < runnableItems.size(); j++) {
			RunnableItem item = runnableItems.get(j);
			if (item instanceof RunnableInstructions) {
				System.out.println("Warning: There is already some timing information available for runnable '" + name + "'.");
			}
		}
		
		InstructionsConstant estimate = FactoryUtil.createInstructionConstant(max);
		RunnableInstructions timingInfo = FactoryUtil.createRunnableInstructions(estimate);
		
		CustomPropertyUtil.customPut(timingInfo, "GeneratedBy", VENDOR_STRING);
		CustomPropertyUtil.customPut(timingInfo, "GeneratedAt", startDateTime);
		
		RuntimeUtil.addRuntimeToRunnable(runnable, timingInfo);
				
		/*
		var deviation = def.createChild("deviation");
		
		var lowerBound = deviation.createChild("lowerBound");
		lowerBound.putString("xsi:type", AMALTHEA_NAMESPACE + "LongObject");
		lowerBound.putString("value", "0");
		
		var upperBound = deviation.createChild("upperBound");
		upperBound.putString("xsi:type", AMALTHEA_NAMESPACE + "LongObject");
		upperBound.putString("value", map.get(name));
		*/
	}
	
	private void retrieveTimingData() throws WorkbenchException, FileNotFoundException {
		System.out.println("Retrieving timing data...");

		// read XTC file
		XMLMemento xtc = XMLMemento.createReadRoot(new java.io.FileReader(xtcLocation));
		
		// create map for results
		HashMap<String, Long> map = new HashMap<String, Long>();
		
		// read timing information for each runnable from XTC
		IMemento[] executables = xtc.getChild("common").getChild("CPU").getChildren();
		for (int j = 0; j < executables.length; j++) {
			IMemento executable = executables[j];
			String name = executable.getString("name");
			IMemento response = executable.getChild("mode").getChild("response");
			if (response != null) {
				IMemento result = null;
				if (xtcRequestType.equals("TimingProfiler")) { // FIXME
					result = response.getChild("timing-profiler");
				} else if (xtcRequestType.equals("TimeWeaver")) {
					result = response.getChild("TimeWeaver");
				}
				String value = result.getString("value");
				String unit = result.getString("unit");
				
				Double cycles = Double.parseDouble(value);
				if (unit.equals("ns")) {
					Double cycleTimeInNS = 1000.0/frequencyInMHz;
					cycles = cycles / cycleTimeInNS;
				} else if (unit.equals("us")) {
					Double cycleTimeInUS = 1.0/frequencyInMHz;
					cycles = cycles / cycleTimeInUS;
				} else if (unit.equals("ms")) {
					Double cycleTimeInMS = 1.0/(1.0E3*frequencyInMHz);
					cycles = cycles / cycleTimeInMS;
				} else if (unit.equals("ss")) {
					Double cycleTimeInS = 1.0/(1.0E6*frequencyInMHz);
					cycles = cycles / cycleTimeInS;
				}
				cycles = Double.valueOf(Math.ceil(cycles));
				
				System.out.println("\t" + name + " " + value + " " + unit + " (" + cycles.toString() + " cycles)");
			
				map.put(name, cycles.longValue());
			} else {
				System.out.println("\t" + name + " no responce");
			}
		}
			
		// write timing information for each runnable to model
		for (int i = 0; i < runnables.size(); i++) {
			Runnable runnable = runnables.get(i);
			
			String name = runnablePrefix + runnable.getName();
			
			// ignore some runnables
			if (ignoredRunnables.contains(name))
				continue;
			
			// rename some runnables
			name = mappedRunnables.getOrDefault(name, name);
						
			Long result = map.get(name);
			if (result != null) {
				updateRunnable(runnable, result);
			}
		}
	}

	private static final String ALAUNCHER_COMMAND = "alauncher.exe";
	private String alauchnerOptions = " -j -1";

	public void setAlauchnerOptions(String options) {
		alauchnerOptions = options;
	}
	
	private void processXTC() throws IOException, InterruptedException {
		
		System.out.println("Calling TimingProfiler...");
	
		Process p = java.lang.Runtime.getRuntime().exec(ALAUNCHER_COMMAND + " " + alauchnerOptions + " " + xtcLocation);
		p.waitFor();
	}


}
