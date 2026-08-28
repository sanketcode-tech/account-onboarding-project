# Phase 4 — BPMN design & deploy to Camunda 8 SaaS

Checklist
- [ ] Model the BPMN diagram `current-account-onboarding.bpmn` in Camunda Desktop Modeler
- [ ] Include elements: Start, Service Tasks, User Tasks, Message Catch Events, Exclusive Gateway, Boundary Timers
- [ ] Ensure correlation key is `applicationId` for all message events
- [ ] Add boundary timer events: Signing Ceremony (48 hours), Provisioning Approval (24 hours)
- [ ] Deploy BPMN manually from Modeler to Camunda 8 SaaS (manual deploy for iteration)
- [ ] Verify process in Operate; claim/complete tasks in Tasklist
- [ ] Add BPMN file to `docs/bpmn/current-account-onboarding.bpmn` (placeholder)

Goal
Design the exact BPMN flow for Current Account Onboarding and deploy it to Camunda 8 SaaS so the Zeebe engine hosts the process model. We'll iterate manually in the Modeler, then configure `onboarding-service` to optionally deploy programmatically.

Deliverables
- BPMN file `docs/bpmn/current-account-onboarding.bpmn` (author in Camunda Modeler)
- Deployment notes and the process id/key used for starting instances
- Documentation of message names and variable payload shapes for correlation (use `applicationId`)

Implementation notes
- Use Message Throw/Catch Event semantics for the Kafka-driven waits (offer accepted, document signed).
- Service Tasks for automated steps should be implemented as Zeebe jobs (external job workers) in `onboarding-service` during Phase 5.
- Do not programmatically deploy until process is stable; manual deployment from Modeler is preferred for iteration.

Code wiring status
- The consumer in `application-service` is already wired to call a workflow service when an application event is received. See:

```
application-service/src/main/java/com/northbridge/application/service/KafkaConsumerListener.java
application-service/src/main/java/com/northbridge/application/service/WorkflowServiceImpl.java
```

`WorkflowServiceImpl` is currently a safe placeholder: it checks for `CAMUNDA_*` env vars and logs guidance. Replace the TODO in that implementation with the Zeebe client call to create process instances once you have deployed the BPMN model.

Manual Deploy (Modeler)
1. Open `current-account-onboarding.bpmn` in Camunda Desktop Modeler
2. Set execution platform to Camunda 8
3. Click Deploy and enter your Camunda SaaS Client ID/Secret, Cluster ID, and Region
4. Confirm deployment in Camunda Operate

Verify
- Start a test instance from Camunda Console or via `onboarding-service` API (once implemented)
- Confirm User Tasks appear in Tasklist (Signing Ceremony, Provisioning Approval)
- Confirm timers escalate when not actioned (you can shorten timers for testing)

Programmatic deployment (optional)
- `onboarding-service` includes a `ProcessDeployer` which can auto-deploy BPMN files from `classpath:/bpmn` at startup.
- To enable programmatic deployment set `process.deployer.enabled=true` and ensure CAMUNDA_* env vars are set in your `.env`.
- Example: run `onboarding-service` with `-Dprocess.deployer.enabled=true` or set the environment variable.

Notes
- Keep process variables minimal and clear: `applicationId`, `customerId`, `offerId`, `documentId`, `accountId`, `status`.
- Document all message names and expected JSON structure for Kafka events in `docs/bpmn/README.md`.

