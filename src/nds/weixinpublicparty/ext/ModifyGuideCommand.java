package nds.weixinpublicparty.ext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.query.QueryEngine;
import nds.query.SPResult;
import nds.rest.RestUtils;
import nds.util.NDSException;
import nds.util.Validator;

public class ModifyGuideCommand extends Command{

	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {
		JSONObject jo=null;
		ValueHolder vh =new ValueHolder();
		
		//判断公司ID 与VIP ID是否在参数中传入
		try {
			jo = (JSONObject) event.getParameterValue("jsonObject");
			jo=jo.optJSONObject("params");
		}catch(Exception e) {
			logger.error("params error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "更新导购异常请重试");
			return vh;
		}
		
		if (jo==null||!jo.has("storeid")||!jo.has("vipid")||!jo.has("guideid")) {
			logger.error("params error:not put storeid or vipid or guideid");
			vh.put("code", "-1");
			vh.put("message", "更新导购异常请重试");
			return vh;
		}
		
		int vipid=jo.optInt("vipid",-1);
		int storeid=jo.optInt("storeid",-1);
		int guideid	=jo.optInt("guideid",-1);
		if(vipid==-1||storeid==-1||guideid==-1){
			logger.error("params error:not put vipid or storeid or guideid");
			vh.put("code", "-1");
			vh.put("message", "更新导购异常请重试");
			return vh;
		}
		
		Object ostore=null;
		Object oguide=null;
		String storecode=null;
		String guideCode=null;
		String vipno=null;
		String companyid=null;
		List viplist=null;
		
		try{
			viplist=QueryEngine.getInstance().doQueryList("select v.ad_client_id,v.vipcardno from wx_vip v where v.id=?",new Object[]{vipid});
		}catch(Exception e){
			logger.debug("search vip byid:"+vipid+",error:"+e.getLocalizedMessage());
			vh.put("code", "-1");
			vh.put("message", "会员信息异常请重试");
			return vh;
		}
		if(viplist==null||viplist.size()<=0){
			logger.debug("search vip byid:"+vipid+",error: count is 0");
			vh.put("code", "-1");
			vh.put("message", "会员信息异常请重试");
			return vh;
		}
		
		viplist=(List)viplist.get(0);
		vipno=String.valueOf(viplist.get(1));
		companyid=String.valueOf(viplist.get(0));
		
		//判断接通线下参数
		List all=null;
		try {
			all=QueryEngine.getInstance().doQueryList("select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam,nvl(ifs.ismesauth,'N') from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id WHERE ifs.ad_client_id="+companyid);
		} catch (Exception e) {
			logger.error("select set offline params error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("messae", "更新导购异常请重试");
			return vh ;
		}
		if (all==null||all.size()<=0) {
			logger.error("select set offline params error:not find data");
			vh.put("code", "-1");
			vh.put("messae", "更新导购异常请重试");
			return vh ;
		}
		all=(List)all.get(0);
		
		String serverUrl=String.valueOf(all.get(0));
		boolean isErp="Y".equalsIgnoreCase(String.valueOf(all.get(2)));
		String SKEY=String.valueOf(all.get(3));
		boolean isVerifyCode="Y".equalsIgnoreCase(String.valueOf(all.get(4)));
		if(isErp&&(nds.util.Validator.isNull(serverUrl)||nds.util.Validator.isNull(SKEY))) {
			logger.error("SERVERuRL OR SKEY IS NULL");
			vh.put("code", "-1");
			vh.put("messae", "更新导购异常请重试");
			return vh ;
		}
		
		
		if(isErp){
			JSONObject offparam=new JSONObject();
			HashMap<String, String> params =new HashMap<String, String>();
			try{
				ostore=QueryEngine.getInstance().doQueryOne("select t.code from wx_store t where t.id=?", new Object[]{storeid});
				storecode=String.valueOf(ostore);
				
				oguide=QueryEngine.getInstance().doQueryOne("select t.code from wx_guide t where t.id=?", new Object[]{guideid});
				guideCode=String.valueOf(oguide);
				
			}catch(Exception e){
				logger.debug("search vip byid:"+vipid+",error:"+e.getLocalizedMessage());
				vh.put("code", "-1");
				vh.put("message", "更新导购异常请重试");
				return vh;
			}
			if(Validator.isNull(storecode)||Validator.isNull(guideCode)){
				logger.debug("storecode or guidecode is null");
				vh.put("code", "-1");
				vh.put("message", "导购信息异常请重试");
				return vh;
			}
			
			String ts=String.valueOf(System.currentTimeMillis());
			String sign=null;
			try {
				sign = nds.util.MD5Sum.toCheckSumStr(companyid + ts+ SKEY);
				offparam.put("vipno", vipno);
				offparam.put("storecode",storecode);
				offparam.put("guidecode", guideCode);
			} catch (Exception e) {
				logger.debug("update offine guide md5 error:"+e.getLocalizedMessage());
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "更新导购异常请重试");
				return vh;
			}
			
			params.put("args[params]", offparam.toString());
			params.put("args[cardid]",companyid);
			params.put("format", "JSON");
			params.put("client", "");
			params.put("ver","1.0");
			params.put("ts",ts);
			params.put("sig",sign);
			params.put("method","updateguide");
			
			//调用线下开卡
			try{
				vh=RestUtils.sendRequest(serverUrl,params,"POST");
			} catch (Throwable e) {
				logger.debug("update guide offline error->"+e.getLocalizedMessage());
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "更新导购异常请重试");
				return vh;
			}
			if(vh==null) {
				logger.error("update guide offline error->return null");
				vh.put("code", "-1");
				vh.put("message", "更新导购异常请重试");
				return vh;
			}
			
			String result=(String) vh.get("message");
			logger.debug("update offline guide result->"+result);
			JSONObject offjo=null;
			try {
				offjo= new JSONObject(result);
			}catch(Exception e) {
				vh.put("code", "-1");
				vh.put("message", "更新导购异常请重试");
				return vh;
			}
			
			//判断线更新导购信息是否成功
			if(offjo==null||offjo==JSONObject.NULL) {
				vh.put("code", "-1");
				vh.put("message", "更新导购异常请重试");
				return vh;
			}
			if(offjo.optInt("errCode",-1)!=0) {
				vh.put("code", "-1");
				vh.put("message", offjo.optString("errMessage"));
				return vh;
			}
			
		}
		
		List params =new ArrayList();
		params.add(companyid);
		params.add("<vipid>"+vipid+"</vipid><storeid>"+storeid+"</storeid><guideid>"+guideid+"</guideid>");
		
		SPResult spr=QueryEngine.getInstance().executeStoredProcedure("wx_vip_modifyguide", params, true);
		if(spr==null){
			vh.put("code", "-1");
			vh.put("message", "更新导购异常请重试");
			return vh;
		}
		if(spr.getCode()==0){
			vh.put("code", "0");
			vh.put("message", "更新导购成功");
		}else{
			vh.put("code", "-1");
			vh.put("message", spr.getMessage());
		}
		
		return vh;
	}

}
