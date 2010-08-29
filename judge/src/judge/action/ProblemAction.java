/**
 * 处理题目相关功能
 */

package judge.action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import org.apache.struts2.ServletActionContext;

import judge.bean.DataTablesPage;
import judge.bean.Problem;
import judge.bean.Submission;
import judge.bean.User;
import judge.service.IBaseService;
import judge.spider.HDUSpider;
import judge.spider.HUSTSpider;
import judge.spider.POJSpider;
import judge.spider.SGUSpider;
import judge.spider.SPOJSpider;
import judge.spider.Spider;
import judge.spider.URALSpider;
import judge.spider.UVALiveSpider;
import judge.spider.ZOJSpider;
import judge.submitter.HDUSubmitter;
import judge.submitter.HUSTSubmitter;
import judge.submitter.POJSubmitter;
import judge.submitter.SGUSubmitter;
import judge.submitter.SPOJSubmitter;
import judge.submitter.Submitter;
import judge.submitter.URALSubmitter;
import judge.submitter.UVALiveSubmitter;
import judge.submitter.ZOJSubmitter;

import com.opensymphony.xwork2.ActionContext;

@SuppressWarnings({ "unchecked", "serial" })
public class ProblemAction extends BaseAction{
	
	private IBaseService baseService;
	
	private int id;
	private int uid;
	private int isOpen;
	private String OJId;
	private String probNum;
	private Problem problem;
	private Submission submission;
	private List dataList;
	private String language;
	private String source;
	private String redir;
	private String un;
	private DataTablesPage dataTablesPage;
	private Map<Object, String> languageList;
	
	static private List<String> OJList = new ArrayList<String>();
	static {
		OJList.add("POJ");
		OJList.add("ZOJ");
		OJList.add("UVALive");
		OJList.add("SGU");
		OJList.add("URAL");
		OJList.add("HUST");
		OJList.add("SPOJ");
		OJList.add("HDU");
	}
	
	static public Map<String, Spider> spiderMap = new HashMap<String, Spider>();
	static {
		spiderMap.put("POJ", new POJSpider());
		spiderMap.put("ZOJ", new ZOJSpider());
		spiderMap.put("UVALive", new UVALiveSpider());
		spiderMap.put("SGU", new SGUSpider());
		spiderMap.put("URAL", new URALSpider());
		spiderMap.put("HUST", new HUSTSpider());
		spiderMap.put("SPOJ", new SPOJSpider());
		spiderMap.put("HDU", new HDUSpider());
	}
	
	static public Map<String, Submitter> submitterMap = new HashMap<String, Submitter>();
	static {
		submitterMap.put("POJ", new POJSubmitter());
		submitterMap.put("ZOJ", new ZOJSubmitter());
		submitterMap.put("UVALive", new UVALiveSubmitter());
		submitterMap.put("SGU", new SGUSubmitter());
		submitterMap.put("URAL", new URALSubmitter());
		submitterMap.put("HUST", new HUSTSubmitter());
		submitterMap.put("SPOJ", new SPOJSubmitter());
		submitterMap.put("HDU", new HDUSubmitter());
	}
	
	static public Map<String, String> lf = new HashMap<String, String>();
	static {
		lf.put("POJ", "%I64d & %I64u");
		lf.put("ZOJ", "%lld & %llu");
		lf.put("UVALive", "%lld & %llu");
		lf.put("SGU", "%I64d & %I64u");
		lf.put("URAL", "%I64d & %I64u");
		lf.put("HUST", "%lld & %llu");
		lf.put("SPOJ", "%lld & %llu");
		lf.put("HDU", "%I64d & %I64u");
	}

	public String toListProblem() {
		return SUCCESS;
	}
	
	public String listProblem() {
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		StringBuffer hql = new StringBuffer("select problem.id, problem.title, problem.addTime, problem.hidden, problem.creatorId, problem.originOJ, problem.originProb, problem.url from Problem problem where");
		if (user == null){
			hql.append(" problem.hidden = 0");
		} else if (user.getSup() == 0){
			hql.append(" problem.hidden = 0 or problem.creatorId = " + user.getId());
		} else {
			hql.append(" problem.id > 0");
		}
		long cnt = baseService.count(hql.toString());
		dataTablesPage = new DataTablesPage();
		dataTablesPage.setITotalDisplayRecords(cnt);
		dataTablesPage.setITotalRecords(cnt);
		if (sSearch != null && !sSearch.trim().isEmpty()){
			sSearch = sSearch.toLowerCase().trim();
			hql.append(" and (problem.title like '%" + sSearch + "%' or problem.originOJ like '%" + sSearch + "%' or problem.originProb like '%" + sSearch + "%'" + (sSearch.matches("\\d+") ? " or problem.id = " + sSearch : "") + ") ");
			dataTablesPage.setITotalDisplayRecords(baseService.count(hql.toString()));
		}
//		System.out.println("iSortCol_0 = " + iSortCol_0);
		if (iSortCol_0 != null){
			if (iSortCol_0 == 0){
				hql.append(" order by problem.id " + sSortDir_0);
			} else if (iSortCol_0 == 1){
				hql.append(" order by problem.title " + sSortDir_0);
			} else if (iSortCol_0 == 2){
				hql.append(" order by problem.originOJ " + sSortDir_0 + ", problem.originProb " + sSortDir_0);
			} else if (iSortCol_0 == 3){
				hql.append(" order by problem.addTime " + sSortDir_0);
			}
		}

		List<Object[]> tmp = baseService.list(hql.toString(), iDisplayStart, iDisplayLength);
		List aaData =  new ArrayList();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (Object[] o : tmp) {
			boolean userHasAccess = user != null && (user.getSup() != 0 || user.getId() == (Integer)o[4]);
			Object[] res = {
					o[0],
					"<a href='problem/viewProblem.action?id=" + o[0] + "'>" + o[1] + "</a>" + ((Integer)o[3] != 0 ? "<font color='red'>(Hidden)</font>" : ""),
					"<a href='" + o[7] + "'>" + o[5] + " " + o[6] + "</a>",
					sdf.format((Date)o[2]),
					userHasAccess ? "<a href='problem/toEditProblem.action?id=" + o[0] + "'>Edit</a>" : "",
					userHasAccess ? "<a href='javascript:void(0)' onclick='comfirmDeleteProblem(" + o[0] + ")'>Delete</a>" : "",
					userHasAccess ? "<a href='javascript:void(0)' onclick='toggleAccess(" + o[0] + ", $(this))'>" + ((Integer)o[3] != 0 ? "Reveal" : "Hide") + "</a>" : "",
			};
			aaData.add(res);
		}
		dataTablesPage.setAaData(aaData);
//		dataTablesPage.setSColumns("id,title,addTime,hidden,creatorId,originOJ,originProb,url");
		
//		System.out.println(iDisplayStart + " - " + iDisplayLength);
//		System.out.println("sSearch : " + sSearch);

//		System.out.println(dataTablesPage.getITotalDisplayRecords() + " ----- " + dataTablesPage.getITotalRecords() + " -- " + dataTablesPage.getAaData().size());
		
		this.addActionError((String) session.get("error"));
		session.remove("error");
		OJId = (String) session.get("OJId");

		return SUCCESS;
	}
	
	public String addProblem(){
		if (!OJList.contains(OJId)){
			this.addActionError("Please choose a legal OJ!");
			return ERROR;
		}
		if (probNum.isEmpty()){
			this.addActionError("Please enter the problem number!");
			return ERROR;
		}
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		if (user == null){
			this.addActionError("Please login first!");
			return ERROR;
		}
		session.put("OJId", OJId);
		Spider spider = (Spider) spiderMap.get(OJId).clone();
		problem = new Problem();
		problem.setCreatorId(user.getId());
		problem.setAddTime(new Date());
		problem.setOriginOJ(OJId.trim());
		problem.setOriginProb(probNum.replaceAll("\\s+", ""));
		problem.setTitle("Crawling……");
		problem.setHidden(1);
		problem.setTimeLimit(1);
		baseService.add(problem);
		spider.setProblem(problem);
		try {
			spider.start();
		} catch (Exception e) {
			e.printStackTrace();
			baseService.delete(problem);
			return ERROR;
		}
		return SUCCESS;
	}
	
	public String viewProblem(){
		problem = (Problem) baseService.query(Problem.class, id);
		if (problem.getDescription() != null && problem.getDescription().trim().isEmpty()){
			problem.setDescription(null);
		}
		if (problem.getInput() != null && problem.getInput().trim().isEmpty()){
			problem.setInput(null);
		}
		if (problem.getOutput() != null && problem.getOutput().trim().isEmpty()){
			problem.setOutput(null);
		}
		if (problem.getSampleInput() != null && problem.getSampleInput().trim().isEmpty()){
			problem.setSampleInput(null);
		}
		if (problem.getSampleOutput() != null && problem.getSampleOutput().trim().isEmpty()){
			problem.setSampleOutput(null);
		}
		if (problem.getHint() != null && problem.getHint().trim().isEmpty()){
			problem.setHint(null);
		}
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		if (problem.getHidden() > 0 && (user == null || user.getSup() == 0 && problem.getCreatorId() != user.getId())){
			session.put("error", "You don't have access to viewing this problem!");
			return ERROR;
		}
		session.put("oj", problem.getOriginOJ());
		problem.setOriginOJ(lf.get(problem.getOriginOJ()));
		session.put("problem", problem);
		return SUCCESS;
	}
	
	public String toSubmit(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		if (user == null){
//			session.put("redir", "../problem/toSubmit.action?id=" + id);
			return ERROR;
		}
		ServletContext sc = ServletActionContext.getServletContext();
		problem = (Problem) session.get("problem");
		problem.setOriginOJ((String) session.get("oj"));
		session.put("problem", problem);
		languageList = (Map<Object, String>) sc.getAttribute(problem.getOriginOJ());
		language = (String) session.get("L" + problem.getOriginOJ());
		isOpen = user.getShare();
		return SUCCESS;
	}
	
	
	public String submit(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		if (user == null){
			return ERROR;
		}
		problem = (Problem) session.get("problem");
		ServletContext sc = ServletActionContext.getServletContext();
		languageList = (Map<Object, String>) sc.getAttribute(problem.getOriginOJ());

		if (problem == null){
			this.addActionError("Please submit via usual approach!");
			return INPUT;
		}
		if (problem.getTimeLimit() == 1){
			this.addActionError("Crawling has not finished!");
			return INPUT;
		}

		if (problem.getHidden() > 0 && user.getSup() == 0 && problem.getCreatorId() != user.getId()){
			this.addActionError("This problem is temporarily hidden by the creator!");
			return INPUT;
		}
		

/*		
		dataList = baseService.query("select contest.beginTime, contest.endTime from Contest contest, Cproblem cproblem where cproblem.contestId = contest.id and cproblem.problemId = " + problem.getId());
		for (int i = 0; i < dataList.size(); i++){
			if (((Date)((Object[])dataList.get(i))[0]).compareTo(new Date()) < 0 && ((Date)((Object[])dataList.get(i))[1]).compareTo(new Date()) > 0){
				this.addActionError("This problem is now in use for contests!");
				return INPUT;
			}
		}
*/
		
		if (!languageList.containsKey(language)){
			this.addActionError("No such a language!");
			return INPUT;
		}
		session.put("L" + problem.getOriginOJ(), language);
		if (source.length() < 50){
			this.addActionError("Source code should be longer than 50 characters!");
			return INPUT;
		}
		if (source.length() > 30000){
			this.addActionError("Source code should be shorter than 30000 characters!");
			return INPUT;
		}
		submission = new Submission();
		submission.setSubTime(new Date());
		submission.setProblemId(problem.getId());
		submission.setUserId(user.getId());
		submission.setStatus("Pending……");
		submission.setLanguage(language);
		submission.setSource(source);
		submission.setIsOpen(isOpen);
		baseService.add(submission);
		try {
			Submitter submitter = (Submitter) submitterMap.get(problem.getOriginOJ()).clone();
			submitter.setSubmission(submission);
			submitter.start();
		} catch (Exception e) {
			e.printStackTrace();
			return ERROR;
		}
		return SUCCESS;
	}
	
	public String status(){
		Map session = ActionContext.getContext().getSession();
		session.put("pageIndex", 0);
		ServletContext sc = ServletActionContext.getServletContext();
		if (un != null){
			un = un.trim();
		}
		dataList = baseService.list("select submission.id, user.username, submission.problemId, submission.status, submission.memory, submission.time, submission.language, length(submission.source), submission.subTime, problem.originOJ, submission.status, user.id, submission.isOpen, problem.hidden from Submission submission, User user, Problem problem where submission.contestId = 0 and submission.userId = user.id and submission.problemId = problem.id " + (id > 0 ? "and problem.id = " + id : "") + (un != null && !un.isEmpty() ? " and user.username = '" + un + "'" : "") + " order by submission.subTime desc", 0, 20);
		for (int i = 0; i < dataList.size(); i++){
			((Object [])dataList.get(i))[6] = ((Map<String, String>)sc.getAttribute((String) ((Object [])dataList.get(i))[9])).get(((Object [])dataList.get(i))[6]);
			String st = ((String)((Object [])dataList.get(i))[3]);
			((Object [])dataList.get(i))[10] = st.equals("Accepted") ? "yes" : st.contains("ing") ? "pending" : "no";
		}
		this.addActionError((String) session.get("error"));
		session.remove("error");
		return SUCCESS;
	}
	
	public String statusPrev(){
		Map session = ActionContext.getContext().getSession();
		int pageIndex = (Integer)session.get("pageIndex") - 1;
		if (pageIndex < 0){
			pageIndex = 0;
		}
		session.put("pageIndex", pageIndex);
		ServletContext sc = ServletActionContext.getServletContext();
		dataList = baseService.list("select submission.id, user.username, submission.problemId, submission.status, submission.memory, submission.time, submission.language, length(submission.source), submission.subTime, problem.originOJ, submission.status, user.id, submission.isOpen, problem.hidden from Submission submission, User user, Problem problem where submission.contestId = 0 and submission.userId = user.id and submission.problemId = problem.id " + (id > 0 ? "and problem.id = " + id : "") + (un != null && !un.isEmpty() ? " and user.username = '" + un + "'" : "") + " order by submission.subTime desc", pageIndex * 20, 20);
		for (int i = 0; i < dataList.size(); i++){
			((Object [])dataList.get(i))[6] = ((Map<String, String>)sc.getAttribute((String) ((Object [])dataList.get(i))[9])).get(((Object [])dataList.get(i))[6]);
			String st = ((String)((Object [])dataList.get(i))[3]);
			((Object [])dataList.get(i))[10] = st.equals("Accepted") ? "yes" : st.contains("ing") ? "pending" : "no";
		}
		this.addActionError((String) session.get("error"));
		session.remove("error");
		return SUCCESS;
	}
	
	public String statusNext(){
		Map session = ActionContext.getContext().getSession();
		int pageIndex = (Integer)session.get("pageIndex") + 1;
		ServletContext sc = ServletActionContext.getServletContext();
		dataList = baseService.list("select submission.id, user.username, submission.problemId, submission.status, submission.memory, submission.time, submission.language, length(submission.source), submission.subTime, problem.originOJ, submission.status, user.id, submission.isOpen, problem.hidden from Submission submission, User user, Problem problem where submission.contestId = 0 and submission.userId = user.id and submission.problemId = problem.id " + (id > 0 ? "and problem.id = " + id : "") + (un != null && !un.isEmpty() ? " and user.username = '" + un + "'" : "") + " order by submission.subTime desc", pageIndex * 20, 20);
		if (dataList.size() == 0){
			pageIndex--;
			dataList = baseService.list("select submission.id, user.username, submission.problemId, submission.status, submission.memory, submission.time, submission.language, length(submission.source), submission.subTime, problem.originOJ, submission.status, user.id, submission.isOpen, problem.hidden from Submission submission, User user, Problem problem where submission.contestId = 0 and submission.userId = user.id and submission.problemId = problem.id " + (id > 0 ? "and problem.id = " + id : "") + (un != null && !un.isEmpty() ? " and user.username = '" + un + "'" : "") + " order by submission.subTime desc", pageIndex * 20, 20);
		}
		for (int i = 0; i < dataList.size(); i++){
			((Object [])dataList.get(i))[6] = ((Map<String, String>)sc.getAttribute((String) ((Object [])dataList.get(i))[9])).get(((Object [])dataList.get(i))[6]);
			String st = ((String)((Object [])dataList.get(i))[3]);
			((Object [])dataList.get(i))[10] = st.equals("Accepted") ? "yes" : st.contains("ing") ? "pending" : "no";
		}
		session.put("pageIndex", pageIndex);
		this.addActionError((String) session.get("error"));
		session.remove("error");
		return SUCCESS;
	}
	
	public String deleteProblem(){
		Map session = ActionContext.getContext().getSession();
		problem = (Problem) baseService.query(Problem.class, id);
		User user = (User) session.get("visitor");
		if (user == null || user.getSup() == 0 && problem.getCreatorId() != user.getId()){
			session.put("error", "You don't have access to operation on this problem!");
			return ERROR;
		}
		long c = baseService.count("from Submission submission where submission.problemId = " + id);
		long d = baseService.count("from Cproblem cproblem where cproblem.problemId = " + id);
		if (c > 0){
			session.put("error", "There are already submissions for this problem!");
			return ERROR;
		} else if (d > 0){
			session.put("error", "There are already contests using this problem!");
			return ERROR;
		} else {
			baseService.delete(problem);
		}
		return SUCCESS;
	}

	public String toggleAccess(){
		Map session = ActionContext.getContext().getSession();
		problem = (Problem) baseService.query(Problem.class, id);
		User user = (User) session.get("visitor");
		if (user == null || user.getSup() == 0 && problem.getCreatorId() != user.getId()){
			session.put("error", "You don't have access to operation on this problem!");
			return ERROR;
		}
		problem.setHidden(1 - problem.getHidden());
		baseService.modify(problem);
		return SUCCESS;
	}
	
	public String toEditProblem(){
		Map session = ActionContext.getContext().getSession();
		problem = (Problem) baseService.query(Problem.class, id);
		User user = (User) session.get("visitor");
		if (user == null || user.getSup() == 0 && problem.getCreatorId() != user.getId()){
			return ERROR;
		}
		return SUCCESS;
	}
	
	public String editProblem(){
		Map session = ActionContext.getContext().getSession();
		Problem curProblem = (Problem) baseService.query(Problem.class, id);
		User user = (User) session.get("visitor");
		if (user == null || user.getSup() == 0 && curProblem.getCreatorId() != user.getId()){
			session.put("error", "You don't have access to operation on this problem!");
			return ERROR;
		}
		curProblem.setTitle(problem.getTitle());
		curProblem.setDescription(problem.getDescription());
		curProblem.setInput(problem.getInput());
		curProblem.setOutput(problem.getOutput());
		curProblem.setSampleInput(problem.getSampleInput());
		curProblem.setSampleOutput(problem.getSampleOutput());
		curProblem.setHint(problem.getHint());
		baseService.modify(curProblem);
		return SUCCESS;
	}
	
	public String viewSource(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		submission = (Submission) baseService.query(Submission.class, id);
		if (user == null || user.getSup() == 0 && user.getId() != submission.getUserId()){
			if (submission.getIsOpen() == 0){
				session.put("error", "No access to this code!");
				return ERROR;
			}
			problem = (Problem) baseService.query(Problem.class, submission.getProblemId());
			if (problem.getHidden() == 1){
				session.put("error", "This source is currently not visible since the problem is hidden!");
				return ERROR;
			}
		}
		StringBuffer sb = new StringBuffer();
		String os = submission.getSource();
		for (int i = 0; i < os.length(); i++){
			char c = os.charAt(i);
			if (c == '&'){
				sb.append("&#38;");
			} else if (c == '"'){
				sb.append("&#34;");
			} else if (c == '<'){
				sb.append("&lt;");
			} else if (c == '>'){
				sb.append("&gt;");
			} else {
				sb.append(c);
			}
		}
		submission.setSource(sb.toString());
		problem = (Problem) baseService.query(Problem.class, submission.getProblemId());
		ServletContext sc = ServletActionContext.getServletContext();
		languageList = (Map<Object, String>) sc.getAttribute(problem.getOriginOJ());
		submission.setLanguage(languageList.get(submission.getLanguage()));
		user = (User) baseService.query(User.class, submission.getUserId());
		uid = user.getId();
		un = user.getUsername();

		//这里language用作为shjs提供语言识别所需要的class名
		language = findClass4SHJS(submission.getLanguage());

		return SUCCESS;
	}

	public String toggleOpen(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		submission = (Submission) baseService.query(Submission.class, id);
		if (user == null || user.getSup() == 0 && user.getId() != submission.getUserId()){
			session.put("error", "No access to this code!");
			return ERROR;
		}
		submission.setIsOpen(1 - submission.getIsOpen());
		baseService.modify(submission);
		return SUCCESS;
	}
	
	private String findClass4SHJS(String srcLang) {
		srcLang = " " + srcLang.toLowerCase() + " ";
		if (srcLang.contains("c++") || srcLang.contains("cpp") || srcLang.contains("g++")){
			return "sh_cpp";
		} else if (srcLang.contains(" c ") || srcLang.contains("gcc")){
			return "sh_c";
		} else if (srcLang.contains("c#")){
			return "sh_csharp";
		} else if (srcLang.contains("java ")){
			return "sh_java";
		} else if (srcLang.contains("pascal") || srcLang.contains("fpc")){
			return "sh_pascal";
		} else if (srcLang.contains("tcl")){
			return "sh_tcl";
		} else if (srcLang.contains("scala")){
			return "sh_scala";
		} else if (srcLang.contains("perl")){
			return "sh_perl";
		} else if (srcLang.contains("python")){
			return "sh_python";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("php")){
			return "sh_php";
		} else if (srcLang.contains("prolog")){
			return "sh_prolog";
		} else if (srcLang.contains("javascript")){
			return "sh_javascript";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else if (srcLang.contains("ruby")){
			return "sh_ruby";
		} else {
			return "sh_c";
		}
	}
	
	
	
	
	
	
	
	public int getUid() {
		return uid;
	}
	public void setUid(int uid) {
		this.uid = uid;
	}
	public Submission getSubmission() {
		return submission;
	}
	public void setSubmission(Submission submission) {
		this.submission = submission;
	}
	public int getIsOpen() {
		return isOpen;
	}
	public void setIsOpen(int isOpen) {
		this.isOpen = isOpen;
	}
	public String getUn() {
		return un;
	}
	public void setUn(String un) {
		this.un = un;
	}
	public List getDataList() {
		return dataList;
	}
	public void setDataList(List dataList) {
		this.dataList = dataList;
	}
	public List<String> getOJList() {
		return OJList;
	}
	public Map<Object, String> getLanguageList() {
		return languageList;
	}
	public void setLanguageList(Map<Object, String> languageList) {
		this.languageList = languageList;
	}
	public String getRedir() {
		return redir;
	}
	public void setRedir(String redir) {
		this.redir = redir;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Problem getProblem() {
		return problem;
	}
	public void setProblem(Problem problem) {
		this.problem = problem;
	}
	public String getOJId() {
		return OJId;
	}
	public void setOJId(String id) {
		OJId = id;
	}
	public String getProbNum() {
		return probNum;
	}
	public void setProbNum(String probNum) {
		this.probNum = probNum;
	}
	public IBaseService getBaseService() {
		return baseService;
	}
	public void setBaseService(IBaseService baseService) {
		this.baseService = baseService;
	}
	public DataTablesPage getDataTablesPage() {
		return dataTablesPage;
	}
	public void setDataTablesPage(DataTablesPage dataTablesPage) {
		this.dataTablesPage = dataTablesPage;
	}

}
