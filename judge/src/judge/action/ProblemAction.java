/**
 * 处理题目相关功能
 */

package judge.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import org.apache.struts2.ServletActionContext;

import judge.bean.DataTablesPage;
import judge.bean.Description;
import judge.bean.Problem;
import judge.bean.Submission;
import judge.bean.User;
import judge.spider.Spider;
import judge.submitter.Submitter;

import com.opensymphony.xwork2.ActionContext;

@SuppressWarnings({ "unchecked", "serial" })
public class ProblemAction extends BaseAction{

	private int id;	//problemId
	private int uid;
	private int isOpen;
	private int res;	//result
	private String OJId;
	private String probNum, probNum1, probNum2;
	private Problem problem;
	private Description description;
	private Submission submission;
	private List dataList;
	private String language;
	private String source;
	private String redir;
	private String un;
	private boolean inContest;
	private String _64Format;
	private DataTablesPage dataTablesPage;
	private Map<Object, String> languageList;

	public String toListProblem() {
		Map session = ActionContext.getContext().getSession();
		if (session.containsKey("error")){
			this.addActionError((String) session.get("error"));
		}
		session.remove("error");
		return SUCCESS;
	}

	public String listProblem() {
		Map session = ActionContext.getContext().getSession();
		StringBuffer hql = new StringBuffer("select problem.originOJ, problem.originProb, problem.title, problem.triggerTime, problem.source, problem.id, problem.url from Problem problem where 1=1 ");
		long cnt = baseService.count(hql.toString());
		dataTablesPage = new DataTablesPage();
		dataTablesPage.setITotalRecords(cnt);
		if (OJList.contains(OJId)){
			hql.append(" and problem.originOJ = '" + OJId + "' ");
		}
		Map paraMap = new HashMap();
		if (sSearch != null && !sSearch.trim().isEmpty()){
			sSearch = sSearch.toLowerCase().trim();
			paraMap.put("keyword", "%" + sSearch + "%");
			hql.append(" and (problem.title like :keyword or problem.originProb like :keyword or problem.source like :keyword) ");
		}
		dataTablesPage.setITotalDisplayRecords(baseService.count(hql.toString(), paraMap));
//		System.out.println("iSortCol_0 = " + iSortCol_0);
		if (iSortCol_0 != null){
			if (iSortCol_0 == 1){
				hql.append(" order by problem.originProb " + sSortDir_0);
			} else if (iSortCol_0 == 2){
				hql.append(" order by problem.title " + sSortDir_0);
			} else if (iSortCol_0 == 3){
				hql.append(" order by problem.triggerTime " + sSortDir_0 + " problem.originProb " + sSortDir_0);
			} else if (iSortCol_0 == 4){
				hql.append(" order by problem.source " + sSortDir_0);
			}
		}

		List<Object[]> aaData = baseService.list(hql.toString(), paraMap, iDisplayStart, iDisplayLength);
		for (Object[] o : aaData) {
			o[3] = ((Date)o[3]).getTime();
		}
		dataTablesPage.setAaData(aaData);

		if (session.containsKey("error")){
			this.addActionError((String) session.get("error"));
		}
		session.remove("error");

		return SUCCESS;
	}

	public String addProblem(){
		Map session = ActionContext.getContext().getSession();
		if (!OJList.contains(OJId)){
			session.put("error", "Please choose a legal OJ!");
			return ERROR;
		}
		User user = (User) session.get("visitor");
		if (user == null){
			session.put("error", "Please login first!");
			return ERROR;
		}

		if (probNum1 != null){
			probNum1 = probNum1.trim();
			if (probNum1.isEmpty()){
				probNum1 = null;
			}
		}
		if (probNum2 != null){
			probNum2 = probNum2.trim();
			if (probNum2.isEmpty()){
				probNum2 = null;
			}
		}
		if (probNum1 == null && probNum2 == null){
			session.put("error", "Please enter at least ONE problem number!");
			return ERROR;
		}
		List<String> probNumList = new ArrayList<String>();
		if (probNum2 == null){
			probNumList.add(probNum1);
		} else if (probNum1 == null){
			probNumList.add(probNum2);
		} else if (probNum1.equals(probNum2)){
			probNumList.add(probNum1);
		} else if (probNum1.matches("\\d+") && probNum2.matches("\\d+")){
			int l = Integer.parseInt(probNum1), r = Integer.parseInt(probNum2), tmp;
			if (l > r){
				tmp = l;
				l = r;
				r = tmp;
			}
			if (r - l > 9){
				session.put("error", "You can add 10 problems at most for each time!");
				return ERROR;
			}
			for (Integer a = l; a <= r; ++a) {
				probNumList.add(a.toString());
			}
		} else {
			session.put("error", "Invalid problem number ...");
			return ERROR;
		}

		for (String probNum : probNumList) {
			description = null;
			problem = judgeService.findProblem(OJId.trim(), probNum);
			if (problem == null){
				problem = new Problem();
				problem.setOriginOJ(OJId.trim());
				problem.setOriginProb(probNum);
			} else {
				for (Description desc : problem.getDescriptions()){
					if ("0".equals(desc.getAuthor())){
						description = desc;
						break;
					}
				}
			}
			if (description == null){
				description = new Description();
				description.setUpdateTime(new Date());
				description.setAuthor("0");
				description.setRemarks("Initialization.");
				description.setVote(0);
			}
			problem.setTitle("Crawling……");
			problem.setTimeLimit(1);
			problem.setTriggerTime(new Date());
			baseService.addOrModify(problem);
			Spider spider = (Spider) spiderMap.get(OJId).clone();
			spider.setProblem(problem);
			spider.setDescription(description);
			spider.start();
		}

		return SUCCESS;
	}

	public String viewProblem(){
		List list = baseService.query("select p from Problem p left join fetch p.descriptions where p.id = " + id);
		problem = (Problem) list.get(0);
		_64Format = lf.get(problem.getOriginOJ());
		return SUCCESS;
	}

	public String vote4Description(){
		Map session = ActionContext.getContext().getSession();
		Set votePids = (Set) session.get("votePids");
		if (votePids == null){
			votePids = new HashSet<Integer>();
			session.put("votePids", votePids);
		}
		Description desc = (Description) baseService.query(Description.class, id);
		desc.setVote(desc.getVote() + 1);
		baseService.addOrModify(desc);
		votePids.add(desc.getProblem().getId());
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
		problem = (Problem) baseService.query(Problem.class, id);
		if (problem == null) {
			return ERROR;
		}
		languageList = (Map<Object, String>) sc.getAttribute(problem.getOriginOJ());
		isOpen = user.getShare();
		return SUCCESS;
	}


	public String submit(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		if (user == null){
			return ERROR;
		}
		problem = (Problem) baseService.query(Problem.class, id);
		ServletContext sc = ServletActionContext.getServletContext();
		languageList = (Map<Object, String>) sc.getAttribute(problem.getOriginOJ());

		if (problem == null){
			this.addActionError("Please submit via normal approach!");
			return INPUT;
		}
		if (problem.getTimeLimit() == 1){
			this.addActionError("Crawling has not finished!");
			return INPUT;
		}

		if (!languageList.containsKey(language)){
			this.addActionError("No such a language!");
			return INPUT;
		}
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
		submission.setProblem(problem);
		submission.setUser(user);
		submission.setStatus("Pending……");
		submission.setLanguage(language);
		submission.setSource(source);
		submission.setIsOpen(isOpen);
		submission.setDispLanguage(((Map<String, String>)sc.getAttribute(problem.getOriginOJ())).get(language));
		submission.setUsername(user.getUsername());
		submission.setOriginOJ(problem.getOriginOJ());
		submission.setOriginProb(problem.getOriginProb());
		baseService.addOrModify(submission);
		if (user.getShare() != submission.getIsOpen()) {
			user.setShare(submission.getIsOpen());
			baseService.addOrModify(user);
		}
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

	public String status() {
		if (id != 0){
			problem = (Problem) baseService.query(Problem.class, id);
			OJId = problem.getOriginOJ();
			probNum = problem.getOriginProb();
		}

		Map session = ActionContext.getContext().getSession();
		if (session.containsKey("error")){
			this.addActionError((String) session.get("error"));
		}
		session.remove("error");

		return SUCCESS;
	}

	public String fetchStatus() {
		Map session = ActionContext.getContext().getSession();
		Map paraMap = new HashMap();
		User user = (User) session.get("visitor");
		int userId = user != null ? user.getId() : -1;
		int sup = user != null ? user.getSup() : 0;

		StringBuffer hql = new StringBuffer("select s.id, s.username, s.problem.id, s.status, s.memory, s.time, s.dispLanguage, length(s.source), s.subTime, s.user.id, s.isOpen, s.originOJ, s.originProb, s.contest.id from Submission s ");

		dataTablesPage = new DataTablesPage();

		dataTablesPage.setITotalRecords(9999999L);

		if (!inContest){
			hql.append(" where s.contest is null ");
		} else if (sup == 0){
			hql.append(" left join s.contest c where s.isPrivate = 0 and (c is null or c.endTime < :currentTime) ");
			paraMap.put("currentTime", new Date());
		} else {
			hql.append(" where 1 = 1 ");
		}

		if (un != null && !un.trim().isEmpty()){
			un = un.toLowerCase().trim();
			hql.append(" and s.username = '" + un + "' ");
		}

		if (id != 0){
			hql.append(" and s.problem.id = " + id);
		} else {
			if (!probNum.isEmpty()){
				hql.append(" and s.originProb = '" + probNum + "' ");
			}
			if (OJList.contains(OJId)){
				hql.append(" and s.originOJ = '" + OJId + "' ");
			}
		}

		if (res == 1){
			hql.append(" and s.status = 'Accepted' ");
		} else if (res == 2) {
			hql.append(" and s.status like 'wrong%' ");
		} else if (res == 3) {
			hql.append(" and s.status like 'time%' ");
		} else if (res == 4) {
			hql.append(" and (s.status like 'runtime%' or s.status like 'segment%' or s.status like 'crash%') ");
		} else if (res == 5) {
			hql.append(" and (s.status like 'presentation%' or s.status like 'format%') ");
		} else if (res == 6) {
			hql.append(" and s.status like 'compil%' ");
		} else if (res == 7) {
			hql.append(" and s.status like '%ing%' and s.status not like '%ting%' ");
		}

		hql.append(" order by s.id desc ");

		dataTablesPage.setITotalDisplayRecords(9999999L);

		List<Object[]> aaData = baseService.list(hql.toString(), paraMap, iDisplayStart, iDisplayLength);

		for (Object[] o : aaData) {
			o[8] = ((Date)o[8]).getTime();
			o[10] = (Integer)o[10] > 0 ? 2 : sup > 0 || (Integer)o[9] == userId ? 1 : 0;
		}

		dataTablesPage.setAaData(aaData);

		return SUCCESS;
	}

	public String toEditDescription(){
		Map session = ActionContext.getContext().getSession();
		List list = baseService.query("select d from Description d left join fetch d.problem where d.id = " + id);
		description = (Description) list.get(0);
		problem = description.getProblem();
		if (session.get("visitor") == null){
			return "login";
		}
		redir = ServletActionContext.getRequest().getHeader("Referer") + "&edit=1";
		return SUCCESS;
	}

	public String editDescription(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		if (user == null){
			session.put("error", "Please login first!");
			return ERROR;
		}
		if (id == 0){
			return ERROR;
		}
		description.setUpdateTime(new Date());
		description.setAuthor(user.getUsername());
		description.setVote(0);
		description.setProblem(new Problem(id));
		baseService.execute("delete from Description d where d.author = '" + user.getUsername() + "' and d.problem.id = " + id);
		baseService.addOrModify(description);
		return SUCCESS;
	}

	public String deleteDescription(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		if (user != null){
			description = (Description) baseService.query(Description.class, id);
			if (!description.getAuthor().equals("0") && (user.getSup() == 1 || user.getUsername().equals(description.getAuthor()))){
				baseService.delete(description);
			}
		}
		return SUCCESS;
	}

	public String viewSource(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		List list = baseService.query("select s from Submission s left join fetch s.contest left join fetch s.problem left join fetch s.user where s.id = " + id);
		if (list.isEmpty()){
			session.put("error", "No such submission!");
			return ERROR;
		}
		submission = (Submission) list.get(0);
		if (!(user != null && (user.getSup() != 0 || user.getId() == submission.getUser().getId()) || submission.getIsOpen() == 1 && (submission.getContest() == null || new Date().compareTo(submission.getContest().getEndTime()) > 0))){
			session.put("error", "No access to this code!");
			return ERROR;
		}
		problem = submission.getProblem();
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

		ServletContext sc = ServletActionContext.getServletContext();
		languageList = (Map<Object, String>) sc.getAttribute(problem.getOriginOJ());
		submission.setLanguage(languageList.get(submission.getLanguage()));
		uid = submission.getUser().getId();
		un = submission.getUser().getUsername();

		//这里language用作为shjs提供语言识别所需要的class名
		language = findClass4SHJS(submission.getLanguage());

		return SUCCESS;
	}

	public String toggleOpen(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		submission = (Submission) baseService.query(Submission.class, id);
		if (user == null || user.getSup() == 0 && user.getId() != submission.getUser().getId()){
			session.put("error", "No access to this code!");
			return ERROR;
		}
		submission.setIsOpen(1 - submission.getIsOpen());
		baseService.addOrModify(submission);
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
		} else {
			return "sh_c";
		}
	}
	
	public String rejudge(){
		Map session = ActionContext.getContext().getSession();
		User user = (User) session.get("visitor");
		submission = (Submission) baseService.query(Submission.class, id);
		if (submission == null || !submission.getStatus().equals("Judging Error 1") && (user == null || user.getSup() == 0)){
			return ERROR;
		}
		judgeService.rejudge(submission);
		return SUCCESS;
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

	public List getDataList() {
		return dataList;
	}
	public void setDataList(List dataList) {
		this.dataList = dataList;
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
	public DataTablesPage getDataTablesPage() {
		return dataTablesPage;
	}
	public void setDataTablesPage(DataTablesPage dataTablesPage) {
		this.dataTablesPage = dataTablesPage;
	}
	public int getRes() {
		return res;
	}
	public void setRes(int res) {
		this.res = res;
	}
	public String getUn() {
		return un;
	}
	public void setUn(String un) {
		this.un = un;
	}
	public Description getDescription() {
		return description;
	}
	public void setDescription(Description description) {
		this.description = description;
	}
	public String get_64Format() {
		return _64Format;
	}
	public void set_64Format(String _64Format) {
		this._64Format = _64Format;
	}
	public String getProbNum1() {
		return probNum1;
	}
	public void setProbNum1(String probNum1) {
		this.probNum1 = probNum1;
	}
	public String getProbNum2() {
		return probNum2;
	}
	public void setProbNum2(String probNum2) {
		this.probNum2 = probNum2;
	}
	public boolean isInContest() {
		return inContest;
	}
	public void setInContest(boolean inContest) {
		this.inContest = inContest;
	}

}
