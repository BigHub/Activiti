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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 启动类
 */
public class DemoMan {

    private static final Logger logger = LoggerFactory.getLogger(DemoMan.class);

    public static void main(String[] args) throws ParseException {
        logger.info("启动程序");

        // 1. 创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        // 2. 部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        // 3. 启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        // 4. 处理流程任务
        processTask(processEngine, processInstance);

        logger.info("结束程序");
    }

    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            logger.info("待处理任务数 [{}]", list.size());
            for (Task task : list) {
                logger.info("待处理任务 [{}]", task.getName());

                Map<String, Object> variables = getMap(processEngine, scanner, task);

                taskService.complete(task.getId(), variables);

                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId()).singleResult();
            }
        }
        scanner.close();
    }


    private static Map<String, Object> getMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        Map<String, Object> variables = new HashMap<>();

        for (FormProperty p : formProperties) {
            String line = null;
            if (p.getType() instanceof StringFormType) {
                logger.info("请输入 {} ?", p.getName());
                line = scanner.nextLine();
                variables.put(p.getId(), line);
            } else if (p.getType() instanceof DateFormType) {
                logger.info("请输入 {} ? 格式(yyyy-MM-dd)", p.getName());
                line = scanner.nextLine();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(line);
                variables.put(p.getId(), date);
            } else {
                logger.info("类型暂不支持 {}", p.getType());
            }
            logger.info("您输入的内容是 [{}]", line);
        }
        return variables;
    }


    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        logger.info("启动流程 [{}]", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deployment = repositoryService.createDeployment();
        deployment.addClasspathResource("double-approve.bpmn20.xml");
        Deployment deploy = deployment.deploy();
        String deployId = deploy.getId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
        logger.info("流程定义文件 [{}], 流程ID [{}]", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = processEngine.VERSION;
        logger.info("流程引擎名称 [{}]，版本 [{}]", name, version);
        return processEngine;
    }
}
