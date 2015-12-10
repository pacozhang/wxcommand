package nds.weixinpublicparty.ext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.query.QueryEngine;
import nds.rest.RestUtils;
import nds.util.NDSException;
import nds.weixin.ext.SipStatus;
import nds.weixin.ext.tools.VipPhoneVerifyCode;

public class UpdateErpVipCommand extends Command{

	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {
		JSONObject jo=null;
		ValueHolder vh =new ValueHolder();
		
		try {
			jo = (JSONObject) event.getParameterValue("jsonObject");
			jo=jo.optJSONObject("params");
		}catch(Exception e) {
			logger.error("params error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "完善资料异常，请重试");
			return vh;
		}
		
		if (jo==null||!jo.has("companyid")||!jo.has("vipid")) {
			logger.error("params error:not put companyid or vipid");
			vh.put("code", "-1");
			vh.put("message", "完善资料异常，请重试");
			return vh;
		}
		
		if (jo==null||!jo.has("companyid")||!jo.has("vipid")) {
			logger.error("params error:not put companyid or vipid");
			vh.put("code", "-1");
			vh.put("message", "完善资料异常，请重试");
			return vh;
		}
		
		int vipid=jo.optInt("vipid",-1);
		int companyid=jo.optInt("ad_client_id",-1);
		
		if (companyid<=0 || vipid<=0) {
			logger.error("params error:companyid:"+companyid+",vipid:"+vipid);
			vh.put("code", "-1");
			vh.put("message", "完善资料异常，请重试");
			return vh;
		}
		List all=null;
		
		try{
			all=QueryEngine.getInstance().doQueryList("select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam,nvl(ifs.ismesauth,'N') from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id WHERE ifs.ad_client_id="+companyid);
		}catch(Exception e){
			logger.error("select set offline params error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("messae", "完善资料异常，请重试");
			return vh ;
		}
		
		if (all==null||all.size()<=0) {
			logger.error("select set offline params error:not find data");
			vh.put("code", "-1");
			vh.put("messae", "完善资料异常，请重试");
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
			vh.put("messae", "完善资料异常，请重试");
			return vh ;
		}
		
		//判断是否需要短信验证
		String verifycode=jo.optString("verifycode");
		String phone=jo.optString("PHONENUM");
		if(isVerifyCode) {
			if(nds.util.Validator.isNull(verifycode)) {
				vh.put("code", "-1");
				vh.put("messae", "验证码为空，请输入");
				return vh ;
			}
			if(nds.util.Validator.isNull(phone)) {
				vh.put("code", "-1");
				vh.put("messae", "手机号为空，请输入");
				return vh ;
			}
			vh=VipPhoneVerifyCode.verifyphonecode(vipid, phone, verifycode);
			if(vh==null) {
				logger.error("update verifyvipcode error:call VipPhoneVerifyCode.verifyphonecode error");
				vh.put("code", "-1");
				vh.put("message", "验证码信息异常，请重新输入");
				return vh;
			}
			if(!"0".equals(vh.get("code"))) {
				logger.error("update verifyvipcode error:"+vh.get("message"));
				return vh;
			}
		}
		
		if(!isErp) {
			List al=null;
			String sign=null;
			JSONObject offparam=new JSONObject();
			String ts=String.valueOf(System.currentTimeMillis());
			HashMap<String, String> params =new HashMap<String, String>();
			
			try {
				sign = nds.util.MD5Sum.toCheckSumStr(companyid + ts+ SKEY);
			} catch (IOException e) {
				logger.debug("update md5 error:"+e.getLocalizedMessage());
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "完善资料异常，请重试");
				return vh;
			}
			try{
				al = QueryEngine.getInstance().doQueryList("select vp.wechatno,vs.code,vp.vipcardno from wx_vip vp,wx_vipbaseset vs WHERE vp.id=? AND vp.viptype=vs.id",new Object[] {vipid});
			}catch(Exception e){
				logger.error("update find vip error:"+e.getLocalizedMessage());
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "完善资料异常，请重试");
				return vh;
			}
			
			if(all==null||al.size()<=0) {
				logger.error("update find vip error:not find data by vipid:"+vipid);
				vh.put("code", "-1");
				vh.put("message", "完善资料异常，请重试");
				return vh;
			}
			
			al=(List)al.get(0);
			
			try{
				//"BIRTHDAY":"19340202","NAME":"jackrain","PHONENUM":"18005695669","CONTACTADDRESS":"天津市辖区和平区中国上海徐汇","GENDER":"1"
				offparam.put("args[openid]", String.valueOf(al.get(0)));
				offparam.put("args[cardid]",String.valueOf(companyid));
				offparam.put("args[wshno]","");
				offparam.put("args[name]",jo.optString("RELNAME"));
				offparam.put("args[gender]",jo.optString("SEX"));
				offparam.put("args[birthday]",jo.optString("BIRTHDAY"));
				offparam.put("args[contactaddress]",jo.optString("CONTACTADDRESS"));				
				offparam.put("args[phonenum]",jo.optString("PHONENUM"));
				offparam.put("args[viptype]", (String.valueOf(al.get(1))));
				offparam.put("args[email]","");
				offparam.put("args[idno]","");
			}catch(Exception e){
				
			}

			params.put("args[params]", offparam.toString());
			params.put("format","JSON");
			params.put("client","");
			params.put("ver","1.0");
			params.put("sig",sign);
			params.put("ts",ts);
			params.put("method","updateUserInfo");
			
			try{
				vh=RestUtils.sendRequest(serverUrl,params,"POST");
			} catch (Throwable tx) {
				logger.debug("update offline card error->"+tx.getLocalizedMessage());
				tx.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "完善资料异常，请重试");
				return vh;
			}
			String result=(String) vh.get("message");
			logger.debug("update offline result->"+result);
			
			JSONObject offjo=null;
			try {
				offjo= new JSONObject(result);
			}catch(Exception e) {
				logger.error("update find vip error:"+e.getLocalizedMessage());
				vh.put("code", "-1");
				vh.put("message", "完善资料异常，请重试");
				return vh;
			}
			if(offjo==null||offjo==JSONObject.NULL) {
				vh.put("code", "-1");
				vh.put("message", "完善资料异常，请重试");
				return vh;
			}
			if(offjo.optInt("errCode",-1)!=0) {
				vh.put("code", "-1");
				vh.put("message", offjo.optString("errMessage"));
				return vh;
			}
		}
		
		String sql="update wx_vip v set v.relname=?,v.sex=?,v.phonenum=?,v.contactaddress=? where v.id=?";
		try{
			QueryEngine.getInstance().executeUpdate(sql, new Object[]{jo.optString("RELNAME"),jo.optString("SEX"),jo.optString("PHONENUM"),jo.optString("CONTACTADDRESS"),vipid});
		}catch(Exception e){
			logger.debug("update vip error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "完善资料异常，请重试");
			return vh;
		}
		vh.put("code", "0");
		vh.put("message", "完善资料成功");
		
		return vh;
	}

}
