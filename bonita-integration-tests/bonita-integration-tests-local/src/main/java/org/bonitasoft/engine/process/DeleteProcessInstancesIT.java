package org.bonitasoft.engine.process;

import static java.util.Arrays.asList;
import static org.bonitasoft.engine.data.instance.api.DataInstanceContainer.ACTIVITY_INSTANCE;
import static org.bonitasoft.engine.data.instance.api.DataInstanceContainer.PROCESS_INSTANCE;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.assertj.core.api.SoftAssertions;
import org.bonitasoft.engine.bpm.bar.BarResource;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.bar.actorMapping.Actor;
import org.bonitasoft.engine.bpm.bar.actorMapping.ActorMapping;
import org.bonitasoft.engine.bpm.connector.ConnectorEvent;
import org.bonitasoft.engine.bpm.contract.Type;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.core.contract.data.SContractDataNotFoundException;
import org.bonitasoft.engine.core.process.instance.model.archive.SAConnectorInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.SAFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.SAProcessInstance;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.InvalidExpressionException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.test.BuildTestUtil;
import org.bonitasoft.engine.test.CommonAPILocalIT;
import org.junit.Test;

/**
 * Verify that api methods delete process instances and all its elements
 */
public class DeleteProcessInstancesIT extends CommonAPILocalIT {

    @Test
    public void should_delete_complete_archived_process_instances() throws Exception {
        loginOnDefaultTenantWithDefaultTechnicalUser();
        User user = createUser("deleteProcessInstanceIT", "bpm");
        ProcessDefinitionBuilder mainProcessBuilder = new ProcessDefinitionBuilder()
                .createNewInstance("mainProcess", "1.0");
        mainProcessBuilder.addContract().addInput("simpleInput1", Type.TEXT, "a simple input");
        mainProcessBuilder.addActor("actor");
        mainProcessBuilder.addUserTask("userTask1", "actor").addContract().addInput("simpleInputTask", Type.TEXT, "a simple task input");
        ActorMapping actorMapping = new ActorMapping();
        Actor actor = new Actor("actor");
        actorMapping.addActor(actor);
        actor.addUser("deleteProcessInstanceIT");
        ProcessDefinition mainProcess = deployAndEnableProcess(barWithConnector(mainProcessBuilder
                .addDocumentDefinition("myDoc").addInitialValue(docValueExpr())
                .addStartEvent("start1")
                .addConnector("connector1", "myConnector", "1.0", ConnectorEvent.ON_ENTER)
                .addAutomaticTask("autoWithConnector").addConnector("connector1", "myConnector", "1.0", ConnectorEvent.ON_ENTER)
                .addAutomaticTask("autoWithData").addShortTextData("activityData", s("activityDataValue"))
                .addCallActivity("call1", s("subProcess"), s("1.0"))
                .addCallActivity("call2", s("subProcess"), s("2.0"))
                .getProcess()).setActorMapping(actorMapping).done());
        ProcessDefinition sub1 = getProcessAPI().deployAndEnableProcess(barWithConnector(new ProcessDefinitionBuilder()
                .createNewInstance("subProcess", "1.0")
                .addDocumentDefinition("myDoc").addInitialValue(docValueExpr())
                .addStartEvent("start1")
                .addConnector("connector1", "myConnector", "1.0", ConnectorEvent.ON_ENTER)
                .addAutomaticTask("autoWithConnector").addConnector("connector1", "myConnector", "1.0", ConnectorEvent.ON_ENTER)
                .addAutomaticTask("autoWithData").addShortTextData("activityData", s("activityDataValue"))
                .addCallActivity("sub2", s("subProcess"), s("2.0")).getProcess()).done());

        ProcessDefinition sub2 = getProcessAPI().deployAndEnableProcess(barWithConnector(new ProcessDefinitionBuilder()
                .createNewInstance("subProcess", "2.0")
                .addDocumentDefinition("myDoc").addInitialValue(docValueExpr())
                .addStartEvent("start1")
                .addConnector("connector1", "myConnector", "1.0", ConnectorEvent.ON_ENTER)
                .addAutomaticTask("autoWithConnector").addConnector("connector1", "myConnector", "1.0", ConnectorEvent.ON_ENTER)
                .addAutomaticTask("autoWithData").addShortTextData("activityData", s("activityDataValue")).getProcess()).done());


        List<Long> processInstances = new ArrayList<>();
        List<Long> userTaskInstances = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            long id = getProcessAPI().startProcessWithInputs(mainProcess.getId(), Collections.singletonMap("simpleInput1", "singleInputValue")).getId();
            long userTask1 = waitForUserTask(id, "userTask1");
            userTaskInstances.add(userTask1);
            getProcessAPI().assignUserTask(userTask1, user.getId());
            getProcessAPI().executeUserTask(userTask1, Collections.singletonMap("simpleInputTask", "simpleInputTaskValue"));
            waitForProcessToFinish(id);
            processInstances.add(id);
        }
        final List<SAFlowNodeInstance> allArchFlowNodesBeforeDelete = getTenantAccessor().getUserTransactionService().executeInTransaction(this::searchAllArchFlowNodes);
        final List<SAProcessInstance> allArchProcessInstancesBeforeDelete = getTenantAccessor().getUserTransactionService().executeInTransaction(this::searchAllArchProcessInstances);

        getProcessAPI().deleteArchivedProcessInstancesInAllStates(processInstances);

        getTenantAccessor().getUserTransactionService().executeInTransaction((Callable<Void>) () -> {
            for (Long userTaskInstance : userTaskInstances) {
                try {
                    getTenantAccessor().getContractDataService().getArchivedUserTaskDataValue(userTaskInstance, "simpleInputTask");
                    fail("should have deleted archived contract data on activity instance");
                } catch (SContractDataNotFoundException e) {
                    //ok
                }
            }
            for (Long processInstance : processInstances) {
                try {
                    getTenantAccessor().getContractDataService().getArchivedProcessDataValue(processInstance, "simpleInput1");
                    fail("should have deleted archived contract data on process instance");
                } catch (SContractDataNotFoundException e) {
                    //ok
                }
            }
            SoftAssertions.assertSoftly((soft) -> {
                try {
                    soft.assertThat(searchAllArchProcessInstances()).isEmpty();
                    soft.assertThat(searchAllArchFlowNodes()).isEmpty();
                    soft.assertThat(getTenantAccessor().getCommentService().searchArchivedComments(new QueryOptions(0, 1000))).isEmpty();
                    soft.assertThat(getTenantAccessor().getConnectorInstanceService().searchArchivedConnectorInstance(new QueryOptions(0, 100, SAConnectorInstance.class, null, null), getTenantAccessor().getReadPersistenceService())).isEmpty();
                    soft.assertThat(getTenantAccessor().getDocumentService().getNumberOfArchivedDocuments(new QueryOptions(0, 100))).isEqualTo(0);
                    for (SAFlowNodeInstance flowNodeInstance : allArchFlowNodesBeforeDelete) {
                        soft.assertThat(getTenantAccessor().getDataInstanceService().getLocalSADataInstances(flowNodeInstance.getSourceObjectId(), ACTIVITY_INSTANCE.toString(), 0, 1)).isEmpty();
                    }
                    for (SAProcessInstance processInstance : allArchProcessInstancesBeforeDelete) {
                        soft.assertThat(getTenantAccessor().getDataInstanceService().getLocalSADataInstances(processInstance.getSourceObjectId(), PROCESS_INSTANCE.toString(), 0, 1)).isEmpty();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            return null;
        });
        disableAndDeleteProcess(asList(mainProcess, sub1, sub2));
    }

    private List<SAProcessInstance> searchAllArchProcessInstances() throws SBonitaReadException {
        return getTenantAccessor().getProcessInstanceService().searchArchivedProcessInstances(new QueryOptions(0, 1000));
    }

    private List<SAFlowNodeInstance> searchAllArchFlowNodes() throws org.bonitasoft.engine.persistence.SBonitaReadException {
        return getTenantAccessor().getActivityInstanceService().searchArchivedFlowNodeInstances(SAFlowNodeInstance.class, new QueryOptions(0, 1000));
    }

    private Expression docValueExpr() throws InvalidExpressionException {
        return new ExpressionBuilder().createGroovyScriptExpression("docValue",
                "new org.bonitasoft.engine.bpm.document.DocumentValue(\"hello3\".getBytes(),\"plain/text\",\"file1.txt\")",
                DocumentValue.class.getName());
    }

    private BusinessArchiveBuilder barWithConnector(DesignProcessDefinition process) throws Exception {
        byte[] connectorImplementationFile = BuildTestUtil.buildConnectorImplementationFile("myConnector", "1.0", "impl1", "1.0", AddCommentConnector.class.getName());
        return new BusinessArchiveBuilder().createNewBusinessArchive()
                .setProcessDefinition(process)
                .addConnectorImplementation(new BarResource("connector.impl", connectorImplementationFile));
    }

    private Expression s(String s) throws InvalidExpressionException {
        return new ExpressionBuilder().createConstantStringExpression(s);
    }

}
