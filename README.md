# APP4MC plugin

This repository contains a plugin for the [Eclipse APP4MC](https://projects.eclipse.org/projects/technology.app4mc) framework.
It implements a prototypical coupling between AMALTHEA meta models and [XML Timing Cookies (XTC)](https://www.absint.com/xtc/).
The plugin has been implemented as a Java class extending the WorkflowComponent in the APP4MC framework.
The component can be used inside scripted workflows in the EASE framework. An example for such an usage is shown below:

    var xtc = new XTCConnector()
    xtc.setWorkingDirectory(WORKING_DIRECTORY);
    xtc.setModelName(MODEL_NAME)
    xtc.ignoreRunnable("Runnable_3");
    xtc.mapRunnable("Runnable_0","Mangeled_Name_0");
    xtc.setXtcLocation(XTC_FILE_LOCATION);
    xtc.setXmlReportLocation(XML_REPORT_FILE_LOCATION);
    xtc.setElfLocation(EXECUTABLE_FILE_LOCATION);
    xtc.setAisLocation(AIS_FILE_LOCATION);
    xtc.setTraceLocation(TRACE_FILE_LOCATION);
    xtc.setXtcRequestType(XTC_REQUEST_TYPE);
    xtc.setXtcRequestTarget(XTC_REQUEST_CPU, XTC_REQUEST_TARGET);
    xtc.setXtcRequestTraceFormat(XTC_REQUEST_TRACE_FORMAT);
    xtc.setFrequencyInMHz(300.0);
    xtc.run(ctx)

When the workflow is executed, it iterates over all `Runnables` in the AMALTHEA model and generates an analysis request for each of them.
The resulting XTC file is then processed by AbsInt's tool launcher.
After that, the XTC file contains a response for each successful analysis.
As a last step, these responses are parsed by the workflow component and written back into the AMALTHEA model such that each runnable in the model contains a `Ticks` element storing the timing analysis result.

## Possible configuration options of the `XTCConnector` workflow component

| OPTION | POSSIBLE VALUES | NOTE |
| ------ | --------------- | ---- |
| ignoreRunnable | String | The name of a runnable for which no analysis shall be performed. Can be used multiple times.
| mapRunnable | (String, String) | Maps the name of a runnable in the model to its name in the binary executable. Can be used multiple times.
| setAisLocation | String | The path to the AIS file. Corresponds to the request option a3:global_ais_file.
| setElfLocation | String | The path to the ELF file. Corresponds to the file attribute of the CPU tag in the XTC file.
| setFrequencyInMHz | Float | The frequency of the CPU. Is used to convert results in physical time units into CPU cycles.
| setModelName | String | The model name is used in the description tags of the generated XTC file.
| setTraceLocation | String | The path to the trace file. Corresponds to the XTC request option a3:traces.
| setWorkingDirectory | String | The directory where temporary files are stored.
| setXmlReportLocation | String | The path where the XML report file shall be stored. Corresponds to the XTC request option a3:xml_report_file.
| setXtcLocation | String | The path where the XTC file shall be stored.
| setXtcRequestTarget | ("tricore", "Generic TriCore v1.6.x") | Corresponds to the XTC request options a3:cpu and a3:target.
| setXtcRequestTraceFormat | "NexusLauterbachExport" | Corresponds to the XTC request option a3:trace_format.
| setXtcRequestType | "TimingProfiler", "TimeWeaver", or "WCET" | Corresponds to the type attribute of an XTC request.

## Elements of the AMALTHEA SW model used in the `XTCConnector` workflow component

| AMALTHEA ELEMENT    | NOTE |
| ------------------- | ---- |
| Runnables           | An analysis is performed for each runnable in the software model.
| Ticks               | This element is used to represent the execution time of a runnable. It is appended to the list of runnable items of a runnable in case of a successful timing analysis.
| CustomProperties    | The custom properties *GeneratedBy* and *GeneratedAt* are added to the `Ticks` in order to mark the origin of the timing analysis results.

## Versions

The plugin supports AMALTHEA versions [`v0.8.2`](https://github.com/AbsInt/app4mc-plugin/tree/amalthea_v0.8.2) and [`v0.9.3`](https://github.com/AbsInt/app4mc-plugin/tree/amalthea_v0.9.3).
