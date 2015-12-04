package nds.weixin.ext.dispose;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.query.QueryEngine;
import nds.query.QueryException;
import nds.weixin.ext.WePublicparty;
import nds.weixin.ext.WePublicpartyManger;
import nds.weixin.ext.WeUtils;
import nds.weixin.ext.tools.WXBizMsgCrypt;
import nds.weixin.ext.tools.WeCreateOrSearchVip;

import org.json.JSONException;
import org.json.JSONObject;

public class SubscribeDispose implements IMessageDispose {
	private static Logger logger= LoggerManager.getInstance().getLogger(SubscribeDispose.class.getName());
	
	@Override
	public void dispose(HttpServletRequest request,HttpServletResponse response, WxPublicControl wpc, JSONObject jo) {
		int vipid=0;
		String code="";
		
		JSONObject vipjo=null;
		WeUtils wu=wpc.getWxPublic();
		
		WeCreateOrSearchVip wcos=new WeCreateOrSearchVip();
		
		//synchronized(this) {
			vipjo=wcos.vipDispose(wu, jo);
		//}
		
		
		if(vipjo==null||!vipjo.has("code")||!vipjo.has("vipid")){return;}
		code=vipjo.optString("code","-1");
		if(!"0".equals(code)) {return;}
		
		vipid=vipjo.optInt("vipid",0);
		
		//判断会员是否存在或创建成功
		if(vipid<=0) {return;}
		
		ArrayList params=new ArrayList();
		params.add(wu.getAd_client_id());
		JSONObject pa=new JSONObject();
		JSONObject pxml=new JSONObject();
		try {
			pxml.put("FromUserName",jo.optString("FromUserName"));
			pxml.put("ToUserName",jo.optString("ToUserName"));
			pxml.put("CreateTime", jo.optString("CreateTime"));
			pxml.put("MsgType",jo.optString("Event"));
			pxml.put("KeyWords","");
			pa.put("xml", pxml);

			/*WeMeunManager wm =WeMeunManager.getInstance("0");
			try {
				wm.createMenu(wc, "");
			}catch(Exception e) {e.printStackTrace();}*/
			
			
			params.add(org.json.XML.toString(pa));
			logger.debug("params->"+org.json.XML.toString(pa));
		} catch (JSONException e2) {
			e2.printStackTrace();
		}
		
		String resultStr=null;
		ArrayList para=new ArrayList();
		para.add(java.sql.Clob.class);
		try {
			Collection list=QueryEngine.getInstance().executeFunction("wx_message_$r_reply",params,para);
			resultStr=(String)list.iterator().next();
		} catch (QueryException e1) {
			logger.debug("SubscribeDispose execute function erroe->"+e1.getMessage());
			e1.printStackTrace();
		}
		
		WePublicparty wpp=WePublicpartyManger.getInstance().getWpc();//.getByAppid(pappid);
		String pappid=wpp.getAppid();
		
		if(wpp==null) {
			logger.error("not find wepublicpartyby appid:"+pappid);
		}
		try {
			WXBizMsgCrypt pc = new WXBizMsgCrypt(wpp.getToken(), wpp.getNewencodingaeskey(), wpp.getAppid());
			resultStr=pc.encryptMsg(resultStr, request.getParameter("timestamp"), request.getParameter("nonce"));
			logger.debug("resultStr:"+resultStr+",timestamp:"+request.getParameter("timestamp")+",nonce:"+request.getParameter("nonce"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		logger.debug("result->"+resultStr);
		
		try{
			PrintWriter pw=response.getWriter();
			pw.print(resultStr);
			pw.flush();
			pw.close();
		}catch(Exception e){
			logger.debug("SubscribeDispose error->"+e.getMessage());
			e.printStackTrace();
		}
	}

}
