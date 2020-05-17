package ${package};

import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DemoMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoMain.class);

    public static void main(String[] args) {
        LOGGER.info("启动我们的程序");

        // 创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        // 部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        // 启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        // 处理流程任务
        processTask(processEngine, processInstance);

        LOGGER.info("结束我们的程序");
    }

    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()){
            TaskService taskService = processEngine.getTaskService();
            List<Task> taskList = taskService.createTaskQuery().list();
            LOGGER.info("待处理任务数 [{}]", taskList.size());
            taskList.forEach(task -> {
                LOGGER.info("待处理任务 [{}]", task.getName());
                Map<String, Object> variables = getStringObjectMap(processEngine, scanner, task);
                taskService.complete(task.getId(), variables);
            });

            processInstance = processEngine.getRuntimeService()
                    .createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();;
        }
        scanner.close();
    }

    private static Map<String, Object> getStringObjectMap(ProcessEngine processEngine, Scanner scanner, Task task) {
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        Map<String, Object> variables = new HashMap<>();
        formProperties.forEach(formProperty -> {
            String line = null;
            if (formProperty.getType() instanceof StringFormType) {
                LOGGER.info("请输入 {} ?", formProperty.getName());
                line = scanner.nextLine();
                variables.put(formProperty.getId(), line);
            }else if (formProperty.getType() instanceof DateFormType){
                LOGGER.info("请输入 {} ? 格式(yyyy-MM-dd)", formProperty.getName());
                line = scanner.nextLine();
                LocalDateTime parsedDate = LocalDateTime.parse(line, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                variables.put(formProperty.getId(), Date.from(parsedDate.atZone(ZoneId.systemDefault()).toInstant()));
            }else {
                LOGGER.info("类型暂不支持 [{}]", formProperty.getType());
            }
            LOGGER.info("您输入的内容是 [{}]", line);
        });
        return variables;
    }

    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        LOGGER.info("启动流程 [{}]", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        LOGGER.info("流程定义文件 [{}], 流程ID [{}]", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = processEngine.VERSION;
        LOGGER.info("流程引擎名称[{}]，版本[{}].", name, version);
        return processEngine;
    }
}
