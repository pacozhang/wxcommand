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
			vh.put("message", "���������쳣��������");
			return vh;
		}
		
		if (jo==null||!jo.has("companyid")||!jo.has("vipid")) {
			logger.error("params error:not put companyid or vipid");
			vh.put("code", "-1");
			vh.put("message", "���������쳣��������");
			return vh;
		}
		
		if (jo==null||!jo.has("companyid")||!jo.has("vipid")) {
			logger.error("params error:not put companyid or vipid");
			vh.put("code", "-1");
			vh.put("message", "���������쳣��������");
			return vh;
		}
		
		int vipid=jo.optInt("vipid",-1);
		int companyid=jo.optInt("companyid",-1);
		
		//�����ŵ굽erp ����begin--
		int nowstoreid = jo.optInt("nowstore",-1);
		int nowguideid = jo.optInt("nowguide",-1);
		//�����ŵ굽erp ����end--
		
		if (companyid<=0 || vipid<=0||nowstoreid<=0||nowguideid<=0) {
			logger.error("params error:companyid:"+companyid+",vipid:"+vipid);
			vh.put("code", "-1");
			vh.put("message", "���������쳣��������");
			return vh;
		}
		List all=null;
		
		try{
			all=QueryEngine.getInstance().doQueryList("select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam,nvl(ifs.ismesauth,'N') from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id WHERE ifs.ad_client_id="+companyid);
		}catch(Exception e){
			logger.error("select set offline params error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("messae", "���������쳣��������");
			return vh ;
		}
		
		if (all==null||all.size()<=0) {
			logger.error("select set offline params error:not find data");
			vh.put("code", "-1");
			vh.put("messae", "���������쳣��������");
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
			vh.put("messae", "���������쳣��������");
			return vh ;
		}
		
		//�ж��Ƿ���Ҫ������֤
		String verifycode=jo.optString("verifycode");
		String phone=jo.optString("PHONENUM");
		if(isVerifyCode) {
			if(nds.util.Validator.isNull(verifycode)) {
				vh.put("code", "-1");
				vh.put("messae", "��֤��Ϊ�գ�������");
				return vh ;
			}
			if(nds.util.Validator.isNull(phone)) {
				vh.put("code", "-1");
				vh.put("messae", "�ֻ���Ϊ�գ�������");
				return vh ;
			}
			vh=VipPhoneVerifyCode.verifyphonecode(vipid, phone, verifycode);
			if(vh==null) {
				logger.error("update verifyvipcode error:call VipPhoneVerifyCode.verifyphonecode error");
				vh.put("code", "-1");
				vh.put("message", "��֤����Ϣ�쳣������������");
				return vh;
			}
			if(!"0".equals(vh.get("code"))) {
				logger.error("update verifyvipcode error:"+vh.get("message"));
				return vh;
			}
		}
		
		if(isErp) {
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
				vh.put("message", "���������쳣��������");
				return vh;
			}
			try{
				al = QueryEngine.getInstance().doQueryList("select vp.wechatno,vs.code,vp.vipcardno from wx_vip vp,wx_vipbaseset vs WHERE vp.id=? AND vp.viptype=vs.id",new Object[] {vipid});
			}catch(Exception e){
				logger.error("update find vip error:"+e.getLocalizedMessage());
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "���������쳣��������");
				return vh;
			}
			
			if(all==null||al.size()<=0) {
				logger.error("update find vip error:not find data by vipid:"+vipid);
				vh.put("code", "-1");
				vh.put("message", "���������쳣��������");
				return vh;
			}
			
			al=(List)al.get(0);
			String storecode=null;
			String guideCode=null;

			try{
				//"BIRTHDAY":"19340202","NAME":"jackrain","PHONENUM":"18005695669","CONTACTADDRESS":"�����Ͻ����ƽ���й��Ϻ����","GENDER":"1"
				offparam.put("openid", String.valueOf(al.get(0)));
				offparam.put("cardid",String.valueOf(companyid));
				//offparam.put("args[wshno]","");
				offparam.put("name",jo.optString("RELNAME"));
				offparam.put("gender",jo.optString("SEX"));
				//offparam.put("args[birthday]",jo.optString("BIRTHDAY"));
				//offparam.put("args[contactaddress]",jo.optString("CONTACTADDRESS"));				
				offparam.put("phone",jo.optString("PHONENUM"));
				//offparam.put("args[viptype]", (String.valueOf(al.get(1))));
				//offparam.put("args[email]","");
				//offparam.put("args[idno]","");
				
				storecode = String.valueOf(QueryEngine.getInstance().doQueryOne("select t.code from wx_store t where t.id=?", new Object[]{nowstoreid}));
				guideCode = String.valueOf(QueryEngine.getInstance().doQueryOne("select t.code from wx_guide t where t.id=?", new Object[]{nowguideid}));
			
				offparam.put("storecode",storecode);//�ŵ���
				offparam.put("guideCode",guideCode);//�������

			}catch(Exception e){
				
			}
			params.put("args[gender]", jo.optString("SEX"));
			params.put("args[cardid]", String.valueOf(companyid));
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
				vh.put("message", "���������쳣��������");
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
				vh.put("message", "���������쳣��������");
				return vh;
			}
			if(offjo==null||offjo==JSONObject.NULL) {
				vh.put("code", "-1");
				vh.put("message", "���������쳣��������");
				return vh;
			}
			if(offjo.optInt("errCode",-1)!=0) {
				vh.put("code", "-1");
				vh.put("message", offjo.optString("errMessage"));
				return vh;
			}
		}
		
		/*
		 String sql="update wx_vip v set v.relname=?,v.sex=?,v.phonenum=?,v.nowstore=?,v.nowguide=?,v.ischangecount=nvl(v.ischangecount,0)+1 where v.id=?";
		 try{
			QueryEngine.getInstance().executeUpdate(sql, new Object[]{jo.optString("RELNAME"),jo.optString("SEX"),jo.optString("PHONENUM"),nowstoreid,nowguideid,vipid});
		
		 */
		
		String sql="update wx_vip v set v.relname=?,v.sex=?,v.store=nvl(v.store,?),v.guide=nvl(v.guide,?),v.phonenum=?,v.nowstore=?,v.nowguide=?,v.ischangecount=decode(nvl(v.nowguide,?),?,nvl(v.ischangecount,0),nvl(v.ischangecount,0)+1) where v.id=?";
		try{
			QueryEngine.getInstance().executeUpdate(sql, new Object[]{jo.optString("RELNAME"),jo.optString("SEX"),nowstoreid,nowguideid,jo.optString("PHONENUM"),nowstoreid,nowguideid,nowguideid,nowguideid,vipid});
		}catch(Exception e){
			logger.debug("update vip error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "���������쳣��������");
			return vh;
		}
		vh.put("code", "0");
		vh.put("message", "�������ϳɹ�");
		
		return vh;
	}

}
