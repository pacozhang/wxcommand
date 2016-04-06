package nds.weixinpublicparty.ext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.tools.RestUtils;
import nds.query.QueryEngine;
import nds.util.NDSException;

public class ShareCodeCommand extends Command {
	
	private static Logger logger= LoggerManager.getInstance().getLogger(ShareCodeCommand.class.getName());
	private String serverUrl;
	private String SKEY;
	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {
		ValueHolder vh=new ValueHolder();
		JSONObject jo=(JSONObject) event.getParameterValue("jsonObject");
		jo=jo.optJSONObject("params");
		
		int ad_client_id=jo.optInt("ad_client_id",-1);
		int wx_vip_id=jo.optInt("wx_vip_id",-1);
		int targetid=jo.optInt("targetid",-1);
		String fromid=jo.optString("fromid");//D表示订单，W表示文章，S表示商品
		int objid=jo.optInt("objid",-1);
		String url=jo.optString("url");
		
		String sql="select bonuspoints from wx_sharebonuslist"
			+" where sharetype='"+fromid
			+" ' and starttime<sysdate"
			+" and sysdate<endtime"
			+" and ad_client_id="+ad_client_id+" and rownum=1"
			+" and wx_gmall_id = (select wx_gmall_id from wx_vip where id= "+wx_vip_id+")";
		
		String isql="insert into wx_sharerecord(id,ad_client_id,ad_org_id,wx_vip_id,targetid,fromid,objid,url,bonuspoints,ownerid,modifierid,creationdate,modifieddate,isactive) select get_sequences('wx_sharerecord'),c.ad_client_id,c.ad_org_id,?,?,?,?,?,?,c.ownerid,c.modifierid,c.creationdate,c.modifieddate,c.isactive from web_client c where c.ad_client_id=?";
		
		String vipsql = "select v.VIPCARDNO from wx_vip v where v.id=?";
		JSONObject vipjson= QueryEngine.getInstance().doQueryObject(vipsql.toString(),new Object[]{wx_vip_id});
		String vipcardno = vipjson.optString("VIPCARDNO");
		int bonuspoints=0;
		Connection con=null;
		
		try {
			QueryEngine qe=null;
			
			try {
				qe = QueryEngine.getInstance();
				con=qe.getConnection();
			} catch (Exception e) {
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "失败");
				return vh;
			}
			
			bonuspoints=(Integer) qe.doQueryInt(sql, con);
			if(bonuspoints<0){
				vh.put("code", "-1");
				vh.put("message", "没有符合的数据");
				return vh;
			}
			int addsharerecord=qe.executeUpdate(isql,new Object[] {wx_vip_id,targetid,fromid,objid,url,bonuspoints,ad_client_id},con);
			if(addsharerecord<=0){
				vh.put("code", "-1");
				vh.put("message","数据已存在，无需插入");
				return vh;
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally{
			if(con!=null){
				try{
					con.close();
				}catch(Exception e){
					
				}
			}
		}
		
		boolean isErp=false;
		List all=QueryEngine.getInstance().doQueryList("select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id WHERE ifs.ad_client_id="+ad_client_id);
		if(all!=null&&all.size()>0) {
			logger.debug("WX_INTERFACESET size->"+all.size());
			serverUrl=(String)((List)all.get(0)).get(0);
			isErp="Y".equalsIgnoreCase((String)((List)all.get(0)).get(2));
			SKEY=(String)((List)all.get(0)).get(3);
			if(isErp&&(nds.util.Validator.isNull(serverUrl)||nds.util.Validator.isNull(SKEY))) {
				logger.debug("SERVERuRL OR SKEY IS NULL");
			}
		}else {
			System.out.println("not find WX_INTERFACESET");
		}
		
		if(isErp) {//接通线下
			HashMap<String, String> params =new HashMap<String, String>();
			String ts=String.valueOf(System.currentTimeMillis());
			logger.debug("ts->"+ts);
			params.put("args[cardid]",String.valueOf(ad_client_id));
			params.put("args[cardno]",vipcardno);
			params.put("args[docno]",ts);
			params.put("args[description]","成功分享链接送积分");
			params.put("args[integral]",String.valueOf(bonuspoints));				
			params.put("format","JSON");
			
			params.put("client","");
			params.put("ver","1.0");
			params.put("ts",ts);
			try {
				params.put("sig",nds.util.MD5Sum.toCheckSumStr(String.valueOf(ad_client_id) + ts+ SKEY));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			params.put("method","adjustIntegral");
			try{
				vh=RestUtils.sendRequest(serverUrl,params,"POST");
				logger.debug("vh->"+vh.get("message"));
			} catch (Throwable e) {
				logger.debug("ERP网络通信障碍!"+e.getLocalizedMessage());
				e.printStackTrace();
			}
			String result=(String) vh.get("message");
			logger.debug("是否成功调用"+result);
			try {
				jo = new JSONObject(result);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			if(jo.optInt("errCode",-1)==0) {//线下成功
				 vh.put("code", "0");
				 vh.put("message", "线下操作成功");
			}else{
				vh.put("code", "-1");
				vh.put("message", "线下操作失败");
			}
		}
		JSONObject consumejo=new JSONObject();
		JSONObject jo2=new JSONObject();
		try {
			consumejo.put("vipid", wx_vip_id);
			consumejo.put("getCredits", bonuspoints);
			consumejo.put("description", "成功分享链接送积分");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		   String resultStr=null;
		   ArrayList params2=new ArrayList();
		   params2.add(ad_client_id);
		   params2.add(consumejo.toString());
		   logger.debug("user.updateTrade call oracle params->"+consumejo.toString());
		   logger.debug("params2->"+params2);
		   ArrayList para=new ArrayList();
		   para.add( java.sql.Clob.class);
		   Collection list=QueryEngine.getInstance().executeFunction("wx_coupon_$r_adjust", params2, para);
		   resultStr=(String)list.iterator().next();
		   logger.debug("resultStr------>"+resultStr);
		   try {
			jo2=new JSONObject(resultStr);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if(jo2.optInt("errCode",-1)==0) {
			 vh.put("code", "0");
			 vh.put("message", "线上操作成功");
		}else{
			vh.put("code", "-1");
			vh.put("message", "线上操作失败");
			return vh;
		}
		
		return vh;
	}
}
