package judge.tool;

import java.util.Map;
import com.opensymphony.xwork2.ActionContext;

public class OnlineTool {
	public static boolean isOnline() {
		Map session = ActionContext.getContext().getSession();
		return session.get("visitor") != null;
	}
}