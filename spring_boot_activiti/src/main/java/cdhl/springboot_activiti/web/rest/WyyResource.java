package cdhl.springboot_activiti.web.rest;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import cdhl.springboot_activiti.domain.erm.RoleUser;
import cdhl.springboot_activiti.service.erm.UserService;
import cdhl.springboot_activiti.utils.JumpActivityCmd;

/**
 * 流程管理控制器
 */
@RestController
@RequestMapping("/api/workflow")
public class WyyResource {
    @Autowired
    protected UserService userService;
    @Autowired
    protected RepositoryService repositoryService;
    @Autowired
    protected IdentityService identityService;
    @Autowired
    protected RuntimeService runtimeService;
    @Autowired
    protected TaskService taskService;
    @Autowired
    protected HistoryService historyService;
    @Autowired
    protected ManagementService managementService;

    private static Log log = LogFactory.getLog(WyyResource.class);
    public ProcessInstance startProcess(String userId,String key, String businesskey, Map map) {
		identityService.setAuthenticatedUserId(userId);
		return runtimeService.startProcessInstanceByKey(key, businesskey, map);
	}
    
    /**
     * 代办任务
     *
     * @return
     */
    @RequestMapping(value = "/testput/{modelId}",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> testput(@PathVariable String modelId,@RequestBody MultiValueMap<String, String> values) throws URISyntaxException {

        System.out.println("modelId:"+modelId);

        Iterator<String> iter=values.keySet().iterator();
        while (iter.hasNext()){
            String text=iter.next();
            System.out.println("key:"+text);
            List<String> l=values.get(text);
            for (String s : l){
                System.out.println("value:"+s);
            }
            System.out.println("---------------");
        }

//        ModelSaveRestResource

        Map map=new HashMap<>();

        map.put("success",true);
        return ResponseEntity.ok(map);
    }


    /**
     * 代办任务
     *
     * @return
     */
    @RequestMapping(value = "/mywork",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> mywork() throws URISyntaxException {
        List list=new ArrayList<>();

        String userId=userService.getUserWithAuthorities().getId().toString();
        List<Task> taskList = taskService.createTaskQuery().taskCandidateOrAssigned(userId).list();
        for (Task task : taskList) {
            Map map=new HashMap<>();

            map.put("id",task.getId());
            map.put("name",task.getName());
            map.put("definitionId",task.getProcessDefinitionId());
            map.put("instanceId",task.getProcessInstanceId());
            map.put("definitionKey",task.getTaskDefinitionKey());
            map.put("assignee",task.getAssignee());
            map.put("createTime",task.getCreateTime());

            list.add(map);
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 指派代办任务
     *
     * @return
     */
    @RequestMapping(value = "/claim",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> claimWork(String taskId,String userId) throws URISyntaxException {
        log.info("claim method run taskId="+taskId+" userId="+userId);
        Task task=taskService.createTaskQuery().taskId(taskId).singleResult();
        if (StringUtils.isBlank(task.getAssignee())){
            taskService.claim(taskId,userId);//现在这个任务成为了任务领取者的个人任务了
        }

        return ResponseEntity.ok().build();
    }
    
    /**
     * 处理代办任务
     *
     * @return
     */
    @RequestMapping(value = "/complete",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> doneWork(String taskId,String[] keys,String[] values) throws URISyntaxException {
        Map map=new HashMap<>();
        if (keys!=null && keys.length>0){
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i],values[i]);
            }
        }
        log.info("doneWork method run taskId="+taskId+" keys="+keys+" values="+values);
        Task task=taskService.createTaskQuery().taskId(taskId).singleResult();
        if (StringUtils.isBlank(task.getAssignee())){
            taskService.claim(taskId,userService.getUserWithAuthorities().getId().toString());//现在这个任务成为了任务领取者的个人任务了
        }
        taskService.complete(taskId,map);

        return ResponseEntity.ok().build();
    }

    /**
     * 已完成任务
     *
     * @return
     */
    @RequestMapping(value = "/done",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> doneWork() throws URISyntaxException {
        List list=new ArrayList<>();


        List<HistoricProcessInstance> hpiList=historyService.createHistoricProcessInstanceQuery().finished().list();
        for (HistoricProcessInstance hpi : hpiList){
            Map map=new HashMap<>();

            map.put("id",hpi.getId());
            map.put("name",hpi.getName());
            map.put("definitionId",hpi.getProcessDefinitionId());
            map.put("startUserId",hpi.getStartUserId());
            map.put("startTime",hpi.getStartTime());
            map.put("endTime",hpi.getEndTime());

            list.add(map);
        }

        return ResponseEntity.ok(list);
    }

    /**
     * 跳转到指定节点
     *
     * @return
     */
    @RequestMapping(value = "/jump",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> jump(String processInstanceId,String activityId) throws URISyntaxException {

        managementService.executeCommand(new JumpActivityCmd(processInstanceId,activityId));

        Map map=new HashMap<>();
        map.put("success",true);

        return ResponseEntity.ok(map);
    }

    
    /**
     * 查询候选组中的代办任务
     *
     * @return
     */
    @RequestMapping(value = "/candidateGroup",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> candidateGroup() throws URISyntaxException {
    	List<String> candidateGroups = new ArrayList<String>();
    	Set<RoleUser> roles = userService.getUserWithAuthorities().getRoles();
    	List list=new ArrayList<>();
    	
    	for(RoleUser ru : roles){
    		candidateGroups.add(ru.getRole().getName());
    	}
		List<Task> task=taskService.createTaskQuery().taskCandidateGroupIn(candidateGroups).list();
        log.info("candidateUser method result="+task);
        for (Task t : task) {
            Map map=new HashMap<>();
            map.put("assignee",t.getAssignee());
            map.put("name",t.getName());
            map.put("refundOrderCode",t.getProcessVariables() != null?t.getProcessVariables().get("refundOrderCode"):"");
            
            list.add(map);
        }
        return ResponseEntity.ok(list);
    }
    
    /**
     * 查询候选人中的代办任务
     *
     * @return
     */
    @RequestMapping(value = "/candidateUser",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> candidateUser() throws URISyntaxException {
        List<Task> task=taskService.createTaskQuery().taskCandidateUser(userService.getUserWithAuthorities().getId().toString()).list();
        log.info("candidateUser method result="+task);
        return ResponseEntity.ok(task);
    }
}
