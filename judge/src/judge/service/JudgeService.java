package judge.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import judge.action.BaseAction;
import judge.bean.Contest;
import judge.bean.Problem;
import judge.bean.ReplayStatus;
import judge.bean.Submission;
import judge.service.imp.BaseService;
import judge.submitter.Submitter;
import judge.tool.ApplicationContainer;
import judge.tool.Tools;

@SuppressWarnings("unchecked")
public class JudgeService extends BaseService {

	private static final String cellOptions []= {
		"No submisson",																//0		0
		"Not solved, with one wrong submission",									//0		1

		"Solved at [0] minute with no wrong submisson",								//1		2
		"Solved at [0] minute with one wrong submission",							//1		3
		"Not solved, with [0] wrong submission(s)",									//1		4

		"Solved at [0] hour [1] minute with no wrong submisson",					//2		5
		"Solved at [0] hour [1] minute with one wrong submission",					//2		6
		"Solved at [0] minute with [1] submission(s)",								//2		7
		"Solved at [0] minute with [1] wrong submission(s)",						//2		8
		"Solved at [1] minute with [0] submission(s)",								//2		9
		"Solved at [1] minute with [0] wrong submission(s)",						//2		10
		
		"Solved at [0] hour [1] minute [2] second with no wrong submission",		//3		11
		"Solved at [0] hour [1] minute with [2] submission(s)",						//3		12
		"Solved at [0] hour [1] minute with [2] wrong submission(s)",				//3		13
		"Solved at [1] hour [2] minute with [0] submission(s)",						//3		14
		"Solved at [1] hour [2] minute with [0] wrong submission(s)",				//3		15
		
		"Solved at [0] hour [1] minute [2] second with [3] submission(s)",			//4		16
		"Solved at [0] hour [1] minute [2] second with [3] wrong submission(s)",	//4		17
		"Solved at [1] hour [2] minute [3] second with [0] submission(s)",			//4		18
		"Solved at [1] hour [2] minute [3] second with [0] wrong submission(s)",	//4		19

		"Not solved, with [0] wrong submissions, the last one at [1] minute",						//2		20
		"Not solved, with [0] wrong submissions, the last one at [1] hour [2] minute",				//3		21
		"Not solved, with [0] wrong submissions, the last one at [1] hour [2] minute [3] second",	//4		22

		"Not solved, with [1] wrong submissions, the last one at [0] minute",						//2		23
		"Not solved, with [2] wrong submissions, the last one at [0] hour [1] minute",				//3		24
		"Not solved, with [3] wrong submissions, the last one at [0] hour [1] minute [2] second",	//4		25

		"Not solved, with one wrong submission, at [0] minute",										//1		26
	};
	
	/**
	 * 根据提交ID查询结果
	 * @param id
	 * @return 0:ID 1:结果 2:内存 3:时间 4:额外信息
	 */
	public Object[] getResult(int id){
		Object[] ret = new Object[5];
		Submission s = (Submission) query(Submission.class, id);
		ret[0] = id;
		ret[1] = s.getStatus();
		ret[2] = s.getMemory();
		ret[3] = s.getTime();
		ret[4] = s.getAdditionalInfo() == null ? 0 : 1;
		return ret;
	}
	
	public Set fetchDescriptions(int id){
		List list = query("select p from Problem p left join fetch p.descriptions where p.id = " + id);
		return list.isEmpty() ? null : ((Problem)list.get(0)).getDescriptions();
	}
	
	public Problem findProblem(String OJ, String problemId){
		Map paraMap = new HashMap<String, String>();
		paraMap.put("OJ", OJ.trim());
		paraMap.put("pid", problemId.trim());
		List list = query("select p from Problem p left join fetch p.descriptions where p.originOJ = :OJ and p.originProb = :pid", paraMap);
		return list.isEmpty() ? null : (Problem)list.get(0);
	}

	public List findProblemSimple(String OJ, String problemId){
		Map paraMap = new HashMap<String, String>();
		paraMap.put("OJ", OJ.trim());
		paraMap.put("pid", problemId.trim());
		List<Object[]> list = query("select p.id, p.title, p.timeLimit from Problem p where p.originOJ = :OJ and p.originProb = :pid", paraMap);
		if (list.isEmpty() || (Integer)list.get(0)[2] == 1 || (Integer)list.get(0)[2] == 2){
			return null;
		}
		List res = new ArrayList();
		res.add(list.get(0)[0]);
		res.add(list.get(0)[1]);
		return res;
	}
	
	public void rejudge(Submission submission){
		if (submission.getId() == 0){
			return;
		}
		submission.setStatus("Pending Rejudge");
		submission.setAdditionalInfo(null);
		this.addOrModify(submission);
		try {
			Submitter submitter = (Submitter) BaseAction.submitterMap.get(submission.getOriginOJ()).clone();
			submitter.setSubmission(submission);
			submitter.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 将上次停止服务时正在判的提交置为Judging Error 1
	 */
	public void initJudge(){
		this.execute("update Submission s set s.status = 'Judging Error 1' where s.status like '%ing%' and s.status not like '%rror%'");
	}
	
	/**
	 * 将上次停止服务时正在抓取的题目置为“抓取失败”
	 */
	public void initProblemSpiding() {
		this.execute("update Problem p set p.timeLimit = 2 where p.timeLimit = 1");
	}
	
	/**
	 * 更新比赛排行数据
	 * @param cid 比赛id
	 * @param force 是否强制更新，为false则只有当文件不存在时才更新
	 * @return 0:比赛id		1:数据文件url
	 * @throws Exception
	 */
	public Object[] updateStandingData(Integer cid, boolean force) throws Exception{
		Contest contest = (Contest) this.query("select contest from Contest contest left join fetch contest.replayStatus where contest.id = " + cid).get(0);

		String relativePath = (String) ApplicationContainer.sc.getAttribute("StandingDataPath");
		String path = ApplicationContainer.sc.getRealPath(relativePath);
		File data = new File(path, cid.toString());
		Object[] res = new Object[]{cid, relativePath.substring(1) + "/" + cid.toString()};
		if (data.exists()){
			if (!force) {
				return res;
			}
		} else {
			data.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(data);
		Writer out = new OutputStreamWriter(fos, "UTF-8");
		
		if (contest.getReplayStatus() != null) {
			out.write(contest.getReplayStatus().getData());
		} else {
			List<Object[]> submissionList = this.query("select s.user.id, cp.num, s.status, s.subTime, s.username, s.user.nickname from Submission s, Cproblem cp where s.contest.id = " + cid + " and s.problem.id = cp.problem.id and s.contest.id = cp.contest.id order by s.id asc");
			long beginTime = contest.getBeginTime().getTime();

			StringBuffer submissionData = new StringBuffer("");
			StringBuffer nameData = new StringBuffer("");
			
			Map userMap = new HashMap();

			for (int i = 0; i < submissionList.size(); i++){
				Object[] info = submissionList.get(i);
				submissionData.append(",[").append(info[0]).append(",").append(((String)info[1]).charAt(0) - 'A').append(",").append("Accepted".equals(info[2]) ? 1 : 0).append(",").append((((Date)info[3]).getTime() - beginTime) / 1000L).append("]");
				userMap.put(info[0], new Object[]{info[4], info[5]});
			}
			
			Iterator it = userMap.entrySet().iterator();    
			while (it.hasNext())    
			{    
				Map.Entry entry = (Map.Entry) it.next();    
				Integer uid = (Integer) entry.getKey();
				Object[] name = (Object[]) entry.getValue();
				if (nameData.length() > 0) {
					nameData.append(",");
				}
				nameData.append("\"").append(uid).append("\":[\"").append(name[0]).append("\",\"").append(((String)name[1]).replaceAll("\"", "'")).append("\"]");
			}
			
			StringBuffer standingData = new StringBuffer("[{").append(nameData).append("}").append(submissionData).append("]");

			out.write(standingData.toString());
		}
		out.close();
		fos.close();

		return res;
	}
	
	public Object[] getStandingData(Integer cid) throws Exception{
		return updateStandingData(cid, false);
	}
	
	/**
	 * 获取比赛时间信息
	 * @param cid 比赛ID
	 * @return 0:比赛总时间(s)		1:比赛已进行时间(s)
	 */
	public Long[] getContestTimeInfo(Integer cid){
		List<Object[]> list = this.query("select c.beginTime, c.endTime from Contest c where c.id = " + cid);
		Long beginTime = ((Date) list.get(0)[0]).getTime();
		Long endTime = ((Date) list.get(0)[1]).getTime();
		Long totalTime = endTime - beginTime;
		Long elapsedTime = Math.max(new Date().getTime() - beginTime, 0L);
		return new Long[]{totalTime / 1000L, Math.min(totalTime, elapsedTime) / 1000L};
	}
	
	/**
	 * 切换源码公开属性
	 * @param sid source ID
	 */
	public void toggleOpen(Integer sid) {
		Submission submission = (Submission) this.query(Submission.class, sid);
		submission.setIsOpen(1 - submission.getIsOpen());
		this.addOrModify(submission);
	}


	/**
	 * 获取ranklist中单元格可能的选项
	 * @param ranklistCells
	 * @return
	 */
	public Map<String, Map<Integer, String> > getCellMeaningOptions(String[][] ranklistCells, long contestLength) {
		Map<String, Set<Integer>> temp = new HashMap<String, Set<Integer>>();
		Map<String, String> formatExample = new HashMap<String, String>();
		for (int j = ranklistCells.length - 1; j >= 0; --j) {
			String [] row = ranklistCells[j];
			for (int i = 1; i < row.length; ++i) {
				String symbolized = row[i].replaceAll("\\d+", "[d]");
				Integer[] numberSegments = getNumberSegments(row[i]);
				Set<Integer> curSet = temp.get(symbolized);
				if (curSet == null) {
					curSet = new HashSet<Integer>();
					if (numberSegments.length == 0) {
						curSet.addAll(Arrays.asList(new Integer[]{0, 1}));
					} else if (numberSegments.length == 1) {
						curSet.addAll(Arrays.asList(new Integer[]{2, 3, 4, 26}));
					} else if (numberSegments.length == 2) {
						curSet.addAll(Arrays.asList(new Integer[]{5, 6, 7, 8, 9, 10, 20, 23}));
					} else if (numberSegments.length == 3) {
						curSet.addAll(Arrays.asList(new Integer[]{11, 12, 13, 14, 15, 21, 24}));
					} else if (numberSegments.length == 4) {
						curSet.addAll(Arrays.asList(new Integer[]{16, 17, 18, 19, 22, 25}));
					}
					temp.put(symbolized, curSet);
				}
				String existingFormatExample = formatExample.get(symbolized);
				if (charSum(row[i]) > charSum(existingFormatExample)) {
					formatExample.put(symbolized, row[i]);
				}

				//时间错误
				if (numberSegments.length > 0 && numberSegments[0] * 60000 > contestLength) {
					curSet.remove(2);
					curSet.remove(3);
					curSet.remove(23);
					curSet.remove(26);
				}
				if (numberSegments.length > 1 && numberSegments[1] * 60000 > contestLength) {
					curSet.remove(9);
					curSet.remove(10);
					curSet.remove(20);
				}
				if (numberSegments.length > 1 && (numberSegments[1] > 59 || numberSegments[0] * 3600000 + numberSegments[1] * 60000 > contestLength)) {
					curSet.remove(5);
					curSet.remove(6);
					curSet.remove(12);
					curSet.remove(13);
					curSet.remove(24);
				}
				if (numberSegments.length > 2 && (numberSegments[2] > 59 || numberSegments[1] * 3600000 + numberSegments[2] * 60000 > contestLength)) {
					curSet.remove(14);
					curSet.remove(15);
					curSet.remove(21);
				}
				if (numberSegments.length > 2 && (numberSegments[1] > 59 || numberSegments[2] > 59 || numberSegments[0] * 3600000 + numberSegments[1] * 60000 + numberSegments[2] * 1000 > contestLength)) {
					curSet.remove(11);
					curSet.remove(16);
					curSet.remove(17);
					curSet.remove(25);
				}
				if (numberSegments.length > 3 && (numberSegments[2] > 59 || numberSegments[3] > 59 || numberSegments[1] * 3600000 + numberSegments[2] * 60000 + numberSegments[3] * 1000 > contestLength)) {
					curSet.remove(18);
					curSet.remove(19);
					curSet.remove(22);
				}
				//提交次数错误(0次总提交却solved)
				if (numberSegments.length > 0 && numberSegments[0] == 0) {
					curSet.remove(9);
					curSet.remove(14);
					curSet.remove(18);
				}
				if (numberSegments.length > 1 && numberSegments[1] == 0) {
					curSet.remove(7);
				}
				if (numberSegments.length > 2 && numberSegments[2] == 0) {
					curSet.remove(12);
				}
				if (numberSegments.length > 3 && numberSegments[3] == 0) {
					curSet.remove(16);
				}
			}
		}
		
		Map<String, Map<Integer, String> > ans = new TreeMap<String, Map<Integer,String>>();
		for (Iterator iterator = temp.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();    
			String symbolized = (String) entry.getKey();
			Integer[] numberSegments = getNumberSegments(formatExample.get(symbolized));
			Set<Integer> validOptionNumbers = (Set<Integer>) entry.getValue();
			Map<Integer, String> map = new TreeMap<Integer, String>();
			for (Iterator it = validOptionNumbers.iterator(); it.hasNext();) {
				Integer integer = (Integer) it.next();
				String template = cellOptions[integer];
				for (int i = 0; i < numberSegments.length; i++) {
					template = template.replaceAll("\\[" + i + "\\]", numberSegments[i].toString());
				}
				map.put(integer, template);
			}
			ans.put(formatExample.get(symbolized), map);
		}
		return ans;
	}

	/**
	 * 分割csv文件为String[][]
	 * @param file
	 * @param problemNumber
	 * @return
	 * @throws Exception
	 */
	public String[][] splitCells(File file, int problemNumber) throws Exception {
		if (file == null) {
			throw new Exception("Ranklist file is empty!");
		}
		String[][] result = null;
		
		try {
			result = Tools.splitCellsFromExcel(file);
		} catch (Exception e) {
			System.err.println("Not valid excel!");
			try {
				result = Tools.splitCellsFromCsv(file);
				return result;
			} catch (Exception e1) {
				System.err.println("Not valid csv!");
			}
		}
		
		if (result == null) {
			throw new Exception("The file is not valid Excel/CSV!");
		}

		if (result.length > 500) {
			throw new Exception("At most 500 teams!");
		}
		for (String[] contestantInfo : result) {
			if (contestantInfo.length - 1 > problemNumber) {
				throw new Exception("The number of problems do not match the contest's!");
			}
		}
		return result;
	}
	


	/**
	 * 生成replay的所有status
	 * @param ranklistCells 原csv二维数组
	 * @param cellMeaningOptions 所有可选cellMeaning
	 * @param selectedCellMeaning 已选的cellMeaning
	 * @param contestLength 比赛长度(ms)
	 * @throws Exception 
	 */
	public ReplayStatus getReplayStatus(String[][] ranklistCells, Map cellMeaningOptions, List<String> selectedCellMeaning, long contestLength) throws Exception {
		Map<String, Integer> meaningMap = new HashMap<String, Integer>();
		int i = 0;
		for (Iterator iterator = cellMeaningOptions.entrySet().iterator(); iterator.hasNext(); ++i) {
			Map.Entry entry = (Map.Entry) iterator.next();    
			String symbolized = ((String) entry.getKey()).replaceAll("\\d+", "[d]");
			Map validOptions = (Map) entry.getValue();
			Integer curMeaning = null;
			try {
				curMeaning = Integer.parseInt(selectedCellMeaning.get(i));
			} catch (Exception e) {
				throw new Exception("Invalid cell meaning selections!");
			}
			if (!validOptions.containsKey(curMeaning)) {
				throw new Exception("Invalid cell meaning selections!");
			}
			meaningMap.put(symbolized, curMeaning);
		}
		List<Submission> submissions = new ArrayList<Submission>();
		int totalSubmissionNumber = 0;
		for (String[] row : ranklistCells) {
			for (i = 1; i < row.length; ++i) {
				Integer idx = meaningMap.get(row[i].replaceAll("\\d+", "[d]"));
				long submissionInfo[] = getSubmissionInfo(getNumberSegments(row[i]), idx, contestLength);
				totalSubmissionNumber += submissionInfo[1];
				if (totalSubmissionNumber > 10000) {
					throw new Exception("At most 10000 submissions!");
				}

				for (int j = 0; j < submissionInfo[1]; j++) {
					Submission submission = new Submission();
					submission.setUsername(row[0]);
					submission.setId(i - 1);	//这里借用做存储题号
					if (j < submissionInfo[1] - 1) {
						submission.setSubTime(new Date(submissionInfo[0] - 1000L));
						submission.setStatus("0");
					} else {
						submission.setSubTime(new Date(submissionInfo[0]));
						submission.setStatus(submissionInfo[2] == 0 ? "0" : "1");
					}
					submissions.add(submission);
				}
			}
		}
		Collections.sort(submissions, new Comparator() {  
			public int compare(Object o1, Object o2) {
				Submission s1 = (Submission) o1;
				Submission s2 = (Submission) o2;
				return s1.getSubTime().compareTo(s2.getSubTime());
			}
		});
		
		StringBuffer sb = new StringBuffer("[{}");
		for (Submission submission : submissions) {
			sb.append(",[\"").append(submission.getUsername().trim().replaceAll("\"", "'")).append("\",").append(submission.getId()).append(",").append(submission.getStatus()).append(",").append(submission.getSubTime().getTime() / 1000L).append("]");
		}
		sb.append("]");
		
		ReplayStatus replayStatus = new ReplayStatus();
		replayStatus.setData(sb.toString());

		return replayStatus;
	}
	
	/**
	 * 获取一个team一道题的提交信息
	 * @param idx 
	 * @param contestLength 
	 * @param integers 
	 * @return [0]:最后一次提交时刻(ms)	[1]:总提交次数	[2]:0-(未AC) 1-(AC)	
	 * @throws Exception 
	 */
	private long[] getSubmissionInfo(Integer[] val, Integer idx, long contestLength) throws Exception {
		switch (idx) {
		case 0:
			return new long[]{contestLength, 0, 0};
		case 1:
			return new long[]{contestLength, 1, 0};
		case 2:
			return new long[]{val[0] * 60000, 1, 1};
		case 3:
			return new long[]{val[0] * 60000, 2, 1};
		case 4:
			return new long[]{contestLength, val[0], 0};
		case 5:
			return new long[]{val[0] * 3600000 + val[1] * 60000, 1, 1};
		case 6:
			return new long[]{val[0] * 3600000 + val[1] * 60000, 2, 1};
		case 7:
			return new long[]{val[0] * 60000, val[1], 1};
		case 8:
			return new long[]{val[0] * 60000, val[1] + 1, 1};
		case 9:
			return new long[]{val[1] * 60000, val[0], 1};
		case 10:
			return new long[]{val[1] * 60000, val[0] + 1, 1};
		case 11:
			return new long[]{val[0] * 3600000 + val[1] * 60000 + val[2] * 1000, 1, 1};
		case 12:
			return new long[]{val[0] * 3600000 + val[1] * 60000, val[2], 1};
		case 13:
			return new long[]{val[0] * 3600000 + val[1] * 60000, val[2] + 1, 1};
		case 14:
			return new long[]{val[1] * 3600000 + val[2] * 60000, val[0], 1};
		case 15:
			return new long[]{val[1] * 3600000 + val[2] * 60000, val[0] + 1, 1};
		case 16:
			return new long[]{val[0] * 3600000 + val[1] * 60000 + val[2] * 1000, val[3], 1};
		case 17:
			return new long[]{val[0] * 3600000 + val[1] * 60000 + val[2] * 1000, val[3] + 1, 1};
		case 18:
			return new long[]{val[1] * 3600000 + val[2] * 60000 + val[3] * 1000, val[0], 1};
		case 19:
			return new long[]{val[1] * 3600000 + val[2] * 60000 + val[3] * 1000, val[0] + 1, 1};
		case 20:
			return new long[]{val[1] * 60000, val[0], 0};
		case 21:
			return new long[]{val[1] * 3600000 + val[2] * 60000, val[0], 0};
		case 22:
			return new long[]{val[1] * 3600000 + val[2] * 60000 + val[3] * 1000, val[0], 0};
		case 23:
			return new long[]{val[0] * 60000, val[1], 0};
		case 24:
			return new long[]{val[0] * 3600000 + val[1] * 60000, val[2], 0};
		case 25:
			return new long[]{val[0] * 3600000 + val[1] * 60000 + val[2] * 1000, val[3], 0};
		case 26:
			return new long[]{val[0] * 60000, 1, 0};
		default:
			throw new Exception("Error occured!");
		}
	}
	
	/**
	 * 获取字符串中连续的数字段
	 * @param input
	 * @return
	 */
	private Integer[] getNumberSegments(String input) {
		Matcher m = Pattern.compile("\\d+").matcher(input);
		List<Integer> answer = new ArrayList<Integer>();
		while (m.find()) {
			answer.add(Integer.parseInt(m.group()));
		}
		return answer.toArray(new Integer[0]);
	}
	
	/**
	 * 字符串各个字符ascii值之和
	 * @param string
	 * @return
	 */
	private int charSum(String string) {
		if (string == null) {
			return 0;
		}
		int sum = 0;
		for (int i = 0; i < string.length(); ++i) {
			sum += string.charAt(i);
		}
		return sum;
	}


	
}
