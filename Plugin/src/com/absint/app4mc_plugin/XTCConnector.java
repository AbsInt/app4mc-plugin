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
		
		if (xtcLocation == null) {
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
	
	private String mode = "Runnables"; // FIXME
	
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
			if (mode == "Runnables") {
				writeXTCForRunnables();
			} else {
				writeXTCForTasks();
			}
			
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
	
	private String xtcLocation;
	private String elfLocation;
	private String aisLocation;
	private String xmlReportLocation;
	private String xmlResultLocation;
		
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
	
	private Set<String> ignoredRunnables = new HashSet<String>();
	private Map<String, String> mappedRunnables = new HashMap<String, String>();
	
	public void ignoreRunnable (String runnableName) {
		ignoredRunnables.add(runnableName);
	}
	
	public void mapRunnable (String sourceRunnableName, String targetRunnableName) {
		mappedRunnables.put(sourceRunnableName, targetRunnableName);
	}
	
	private static final String VENDOR_STRING = "AMALTHEA XTC Connector v0.5";
	
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
	
	private void writeXTCForRunnables() throws WorkflowException, IOException {
		this.log.info("writeXTC: ");

		this.log.info("Writing XTC file...");

		// create <xtc> tag
		XMLMemento xtc = XMLMemento.createWriteRoot("xtc");
		xtc.putString("xmlns", "http://www.all-times.org/xtc");
		xtc.putString("schemaLocation", "http://www.absint.com/xtc xtc-v2.4.xsd");
		xtc.putString("version", "2.4");

		// create <common> tag
		IMemento common = xtc.createChild("common");

		// create <CPU> tag
		IMemento cpu = common.createChild("CPU");
		cpu.putString("name", XTC_CPU_NAME);
		cpu.putString("id", "CPU_ID_0");

		// set .elf file
		cpu.putString("file", elfLocation);

		
		for (int i = 0; i < runnables.size(); i++) {
        	writeRequestForRunnable(runnables.get(i), cpu, i, null, 0);
		}

		xtc.save(new java.io.FileWriter(xtcLocation));

		this.log.info("Finished writing '" + xtcLocation + "'.");
		
	}

	private void writeXTCForTasks() throws WorkflowException, IOException {
		this.log.info("writeXTC: ");

		this.log.info("Writing XTC file...");
		// create <xtc> tag
		XMLMemento xtc = XMLMemento.createWriteRoot("xtc");
		xtc.putString("xmlns", "http://www.all-times.org/xtc");
		xtc.putString("schemaLocation", "http://www.absint.com/xtc xtc-v2.4.xsd");
		xtc.putString("version", "2.4");

		// create <common> tag
		IMemento common = xtc.createChild("common");

		// create <CPU> tag
		IMemento cpu = common.createChild("CPU");
		cpu.putString("name", XTC_CPU_NAME);
		cpu.putString("id", "CPU_ID_0");

		// set .elf file
		cpu.putString("file", elfLocation);

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

		xtc.save(new FileWriter(xtcLocation));

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
		HashMap<String, String> map = new HashMap<String, String>();
		
		// read timing information for each runnable from XTC
		IMemento[] executables = xtc.getChild("common").getChild("CPU").getChildren();
		for (int j = 0; j < executables.length; j++) {
			IMemento executable = executables[j];
			String name = executable.getString("name");
			IMemento response = executable.getChild("mode").getChild("response");
			if (response != null) {
				IMemento result = response.getChild("timing-profiler");
				String value = result.getString("value");
				String unit = result.getString("unit");
			
				System.out.println("\t" + name + " " + value + " " + unit);
			
				map.put(name, value);
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
						
			String result = map.get(name);
			if (result != null) {
			   	updateRunnable(runnable, Long.parseLong(result));
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
