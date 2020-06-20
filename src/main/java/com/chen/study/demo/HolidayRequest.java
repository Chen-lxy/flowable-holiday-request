package com.chen.study.demo;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


/**
 * 使用flowable实现请假流程
 */
public class HolidayRequest {
    public static void main(String[] args) {

        // 1.初始化processEngine流程引擎，这个对象是一个线程安全的，因此在一个应用中初始化一次即可。
        /**
         * 1.1ProcessEngineConfiguration实例创建，该实例可以配置与调整流程引擎的设置，
         * 通常使用一个XML文件创建ProcessEngineConfiguration，也可以使用编程的方式创建，
         * 所需的最小配置就是数据库连接JDBC
         * 1.2这里创建的standalone配置对象，指的是引擎是完全独立创建及使用的（而不是在spring中使用）
         * 1.3这里连接了数据库之后，程序启动后，如果需要的表不存在，会自行去创建
         */
        ProcessEngineConfiguration pec = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:mysql://localhost:3306/flowable?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8&nullCatalogMeansCurrent=true")
                .setJdbcDriver("com.mysql.jdbc.Driver")
                .setJdbcUsername("root")
                .setJdbcPassword("1234")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        ProcessEngine processEngine = pec.buildProcessEngine();

        // 2.flowable使用的是SLF4J作为内部日志框架

        // 3.流程部署，需要借助RepositoryService对象
        // 3.1 使用流程引擎processEngine获取RepositoryService对象
        RepositoryService repositoryService = processEngine.getRepositoryService();
        // 3.2 RepositoryService可以根据XML文件创建一个部署（deployment）,并调用deploy执行
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("holiday-request.bpmn20.xml")
                .deploy();

        // 4. 通过RepositoryService创建的对象ProcessDefinitionQuery对象实现
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        System.out.println("Found process definition : " + processDefinition.getName());

        // 5.启动流程实例，启动一般需要提供一些参数,这里使用runtimeService启动一个流程实例
        // 5.1 收集启动参数
        Scanner scanner = new Scanner(System.in);
        System.out.println("Who are you?");
        String employee = scanner.nextLine();
        System.out.println("How many holidays do you want to request?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());
        System.out.println("Why are you need them?");
        String description = scanner.nextLine();
        Map<String,Object> variables = new HashMap<>();
        variables.put("employee",employee);
        variables.put("nrOfHolidays",nrOfHolidays);
        variables.put("description",description);
        // 5.2 这里使用key的方式启动流程实例，key和XML中process的ID相同
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holidayRequest", variables);

        // 6.获取任务列表，需要通过TaskService创建一个TaskQuery，这里只演示查询managers组的任务
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        System.out.println("You have " + tasks.size() + "tasks");
        for (int i = 0 ; i < tasks.size() ; i ++){
            System.out.println((i + 1) + ")" + tasks.get(i).getName());
        }
        // 6.1 根据任务ID获取特定流程实例的变量
        System.out.println("Which task would you like to complete?");
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " + processVariables.get("nrOfHolidays")
                + " of holidays. Do you approve this?");

        // 7. 完场任务（实际中通常接收的是一个表单）,任务完成时要传递带有"approved"变量的map(XML中后续流程会用到)
        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        variables = new HashMap<String,Object>();
        variables.put("approved",approved);
        taskService.complete(task.getId(),variables);

        // 8.使用例是数据，flowable自动存储所有流程实例的审计数据或历史数据。这些数据可以用于创建报告
        // 深入展现组织运行的情况，瓶颈在哪里。
        // 如果希望显示实例的已经执行的时间，可以从historyService的活动历史history activities中查询到
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .list();
        for (HistoricActivityInstance activity : activities){
            System.out.println(activity.getActivityId() + " took "
                + activity.getDurationInMillis() + " milliseconds ");
        }

    }
}
