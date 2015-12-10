package nds.weixin.ext.dispose;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.query.QueryEngine;
import nds.query.QueryException;
import nds.weixin.ext.WeUtils;

import org.json.JSONObject;

public class MasssendjobfinishDispose implements  IMessageDispose{
	private static Logger logger= LoggerManager.getInstance().getLogger(MasssendjobfinishDispose.class.getName());
	@Override
	public void dispose(HttpServletRequest request,
			HttpServletResponse response, WxPublicControl wpc, JSONObject jo) {
		logger.debug("MasssendjobfinishDispose begin-> jo"+jo.toString());
		WeUtils wu=wpc.getWxPublic();
		int ad_client_id = wu.getAd_client_id();
		String MsgID = jo.optString("MsgID","");
		logger.debug("MasssendjobfinishDispose ad_client_id->"+ad_client_id+" MsgID->"+MsgID);
		String Status = jo.optString("Status","");
		String TotalCount = jo.optString("TotalCount","");
		String FilterCount = jo.optString("FilterCount","");
		String SentCount = jo.optString("SentCount","");
		String ErrorCount = jo.optString("ErrorCount","");
		String msg_status = "sendsuccess".equals(Status)?"2":"3";
		String update_msg_status = "update wx_mass_message t set t.msg_status=?,t.errmsg=?,t.totalcount=?,t.filtercount=?,t.sentcount=?,t.errorcount=? where t.ad_client_id=? and t.msg_id=?";
	
		try {
			QueryEngine.getInstance().executeUpdate(update_msg_status,new Object[]{msg_status,Status,TotalCount,FilterCount,SentCount,ErrorCount,ad_client_id,MsgID});
			PrintWriter pw = response.getWriter();
			pw.print("success");
			pw.flush();
			pw.close();
		} catch (QueryException e) {
			logger.debug("update wx_mass_message error msg_id-->"+MsgID);
			e.printStackTrace();
		} catch (IOException e) {
			logger.debug("response.getWriter() error msg_id-->"+MsgID);
			e.printStackTrace();
		}
		
	}

}
