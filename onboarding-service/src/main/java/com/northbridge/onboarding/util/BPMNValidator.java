package com.northbridge.onboarding.util;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

/**
 * Validates BPMN files for syntax errors and structural issues.
 * Checks for:
 * - Valid XML structure
 * - Required BPMN elements (process, start event, end event)
 * - Referenced elements exist
 * - Sequence flow connectivity
 */
@Slf4j
public class BPMNValidator {

    public static class BPMNValidationResult {
        public final boolean isValid;
        public final List<String> errors;
        public final List<String> warnings;
        public final Map<String, Object> metadata;

        public BPMNValidationResult(boolean isValid, List<String> errors, List<String> warnings, Map<String, Object> metadata) {
            this.isValid = isValid;
            this.errors = errors;
            this.warnings = warnings;
            this.metadata = metadata;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== BPMN Validation Report ===\n");
            sb.append("Status: ").append(isValid ? "✓ VALID" : "✗ INVALID").append("\n");

            if (!metadata.isEmpty()) {
                sb.append("\n--- Process Metadata ---\n");
                metadata.forEach((key, value) -> sb.append(String.format("  %s: %s\n", key, value)));
            }

            if (!errors.isEmpty()) {
                sb.append("\n--- Errors (").append(errors.size()).append(") ---\n");
                errors.forEach(e -> sb.append("  �� ").append(e).append("\n"));
            }

            if (!warnings.isEmpty()) {
                sb.append("\n--- Warnings (").append(warnings.size()).append(") ---\n");
                warnings.forEach(w -> sb.append("  ⚠ ").append(w).append("\n"));
            }

            if (errors.isEmpty() && warnings.isEmpty()) {
                sb.append("\nNo errors or warnings found.\n");
            }

            return sb.toString();
        }
    }

    /**
     * Validates a BPMN file from an input stream
     */
    public static BPMNValidationResult validate(InputStream bpmnInputStream) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.ACCESS_EXTERNAL_DTD, false);
            factory.setFeature(XMLConstants.ACCESS_EXTERNAL_SCHEMA, false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    warnings.add("Line " + e.getLineNumber() + ": " + e.getMessage());
                }

                @Override
                public void error(SAXParseException e) {
                    errors.add("Line " + e.getLineNumber() + ": " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    errors.add("FATAL at line " + e.getLineNumber() + ": " + e.getMessage());
                    throw e;
                }
            });

            Document doc = builder.parse(bpmnInputStream);

            // Extract metadata
            Element root = doc.getDocumentElement();

            if (!"definitions".equals(root.getLocalName())) {
                errors.add("Root element must be 'bpmn:definitions', found: " + root.getTagName());
                return new BPMNValidationResult(false, errors, warnings, metadata);
            }

            String targetNamespace = root.getAttribute("targetNamespace");
            metadata.put("Target Namespace", targetNamespace.isEmpty() ? "Not defined" : targetNamespace);

            String exporter = root.getAttribute("exporter");
            String exporterVersion = root.getAttribute("exporterVersion");
            if (!exporter.isEmpty() || !exporterVersion.isEmpty()) {
                metadata.put("Exporter", exporter + " v" + exporterVersion);
            }

            String executionPlatform = root.getAttributeNS("http://camunda.com/schema/modeler/1.0", "executionPlatform");
            String executionPlatformVersion = root.getAttributeNS("http://camunda.com/schema/modeler/1.0", "executionPlatformVersion");
            if (!executionPlatform.isEmpty()) {
                metadata.put("Execution Platform", executionPlatform + " v" + executionPlatformVersion);
            }

            // Get all processes
            NodeList processes = doc.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process");

            if (processes.getLength() == 0) {
                errors.add("No BPMN process definitions found");
                return new BPMNValidationResult(false, errors, warnings, metadata);
            }

            metadata.put("Number of Processes", processes.getLength());

            for (int i = 0; i < processes.getLength(); i++) {
                Element process = (Element) processes.item(i);
                String processId = process.getAttribute("id");
                String processName = process.getAttribute("name");
                boolean isExecutable = "true".equals(process.getAttribute("isExecutable"));

                metadata.put("Process " + (i + 1) + " ID", processId);
                metadata.put("Process " + (i + 1) + " Name", processName.isEmpty() ? "(unnamed)" : processName);
                metadata.put("Process " + (i + 1) + " Executable", isExecutable);

                validateProcess(process, errors, warnings);
            }

        } catch (Exception e) {
            errors.add("Failed to parse BPMN: " + e.getMessage());
            log.error("BPMN validation failed", e);
        }

        return new BPMNValidationResult(errors.isEmpty(), errors, warnings, metadata);
    }

    private static void validateProcess(Element process, List<String> errors, List<String> warnings) {
        String processId = process.getAttribute("id");

        // Check for start events
        NodeList startEvents = process.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "startEvent");
        if (startEvents.getLength() == 0) {
            warnings.add("Process '" + processId + "' has no start event");
        } else {
            for (int i = 0; i < startEvents.getLength(); i++) {
                Element startEvent = (Element) startEvents.item(i);
                String eventId = startEvent.getAttribute("id");
                String eventName = startEvent.getAttribute("name");
                log.debug("Start Event: {} ({})", eventName.isEmpty() ? eventId : eventName, eventId);
            }
        }

        // Check for end events
        NodeList endEvents = process.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "endEvent");
        if (endEvents.getLength() == 0) {
            warnings.add("Process '" + processId + "' has no end event");
        } else {
            for (int i = 0; i < endEvents.getLength(); i++) {
                Element endEvent = (Element) endEvents.item(i);
                String eventId = endEvent.getAttribute("id");
                String eventName = endEvent.getAttribute("name");
                log.debug("End Event: {} ({})", eventName.isEmpty() ? eventId : eventName, eventId);
            }
        }

        // Check for tasks
        NodeList tasks = process.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "serviceTask");
        metadata(process).put("Service Tasks", tasks.getLength());

        NodeList userTasks = process.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "userTask");
        metadata(process).put("User Tasks", userTasks.getLength());

        // Check for gateways
        NodeList gateways = process.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "exclusiveGateway");
        NodeList eventGateways = process.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "eventBasedGateway");
        metadata(process).put("Exclusive Gateways", gateways.getLength());
        metadata(process).put("Event-Based Gateways", eventGateways.getLength());

        // Validate sequence flows
        validateSequenceFlows(process, errors, warnings);
    }

    private static void validateSequenceFlows(Element process, List<String> errors, List<String> warnings) {
        NodeList sequenceFlows = process.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "sequenceFlow");
        Map<String, String> elementIds = new HashMap<>();

        // Build a map of all element IDs in the process
        NodeList allElements = process.getChildNodes();
        for (int i = 0; i < allElements.getLength(); i++) {
            if (allElements.item(i) instanceof Element) {
                Element elem = (Element) allElements.item(i);
                String id = elem.getAttribute("id");
                if (!id.isEmpty()) {
                    elementIds.put(id, elem.getLocalName());
                }
            }
        }

        for (int i = 0; i < sequenceFlows.getLength(); i++) {
            Element flow = (Element) sequenceFlows.item(i);
            String sourceRef = flow.getAttribute("sourceRef");
            String targetRef = flow.getAttribute("targetRef");

            if (!elementIds.containsKey(sourceRef)) {
                errors.add("Sequence flow '" + flow.getAttribute("id") + "' references unknown source: " + sourceRef);
            }
            if (!elementIds.containsKey(targetRef)) {
                errors.add("Sequence flow '" + flow.getAttribute("id") + "' references unknown target: " + targetRef);
            }
        }
    }

    private static Map<String, Object> metadata(Element process) {
        return new HashMap<>();
    }
}

