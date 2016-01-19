package nds.weixinpublicparty.ext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.query.QueryEngine;
import nds.rest.RestUtils;
import nds.util.NDSException;
import nds.util.Tools;
import nds.weixin.ext.tools.VipPhoneVerifyCode;

public class BindCardCommand extends Command{

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
			vh.put("message", "绑卡异常请重试");
			return vh;
		}
		
		if (jo==null||!jo.has("companyid")||!jo.has("vipid")) {
			logger.error("params error:not put companyid or vipid");
			vh.put("code", "-1");
			vh.put("message", "绑卡异常请重试");
			return vh;
		}
		
		int vipid=jo.optInt("vipid",-1);
		int companyid=jo.optInt("companyid",-1);
		
		if (companyid<=0 || vipid<=0) {
			logger.error("params error:companyid:"+companyid+",vipid:"+vipid);
			vh.put("code", "-1");
			vh.put("message", "绑卡异常请重试");
			return vh;
		}
		
		//判断接通线下参数
		List all=null;
		try {
			all=QueryEngine.getInstance().doQueryList("select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam,nvl(ifs.ismesauth,'N') from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id WHERE ifs.ad_client_id="+companyid);
		} catch (Exception e) {
			logger.error("select set offline params error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("messae", "绑卡异常请重试");
			return vh ;
		}
		if (all==null||all.size()<=0) {
			logger.error("select set offline params error:not find data");
			vh.put("code", "-1");
			vh.put("messae", "绑卡异常请重试");
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
			vh.put("messae", "绑卡异常请重试");
			return vh ;
		}
		
		//判断是否有接通ERP
		if(!isErp){
			logger.error("not connect erp");
			vh.put("code", "-1");
			vh.put("messae", "未接通ERP不能绑卡");
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
				logger.error("opencard verifyvipcode error:call VipPhoneVerifyCode.verifyphonecode error");
				vh.put("code", "-1");
				vh.put("message", "验证码信息异常，请重新输入");
				return vh;
			}
			if(!"0".equals(vh.get("code"))) {
				logger.error("opencard verifyvipcode error:"+vh.get("message"));
				return vh;
			}
		}
		
		int storeid=0;
		List al=null;
		
		try{
			al = QueryEngine.getInstance().doQueryList("select vp.wechatno,vp.vipcardno,vb.code,nvl(vp.store,0),vp.email,vp.idcard,vp.relname,vp.phonenum,vp.docno,vp.vippassword,nvl(c.isbsintegral,'N'),nvl(c.bsintegral,0),nvl(c.isbscoupon,'N'),nvl(c.bscoupon_id,0),nvl(vp.isbd,'N') from wx_vip vp join wx_vipbaseset vb on vp.viptype=vb.id join web_client c on vp.ad_client_id=c.ad_client_id WHERE vp.id=? ",new Object[] {vipid});
		}catch(Exception e){
			logger.error("bindcard find vip error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "绑卡失败，请重试");
			return vh;
		}
		
		if(al==null||al.size()<=0) {
			logger.error("bindcard find vip error:not find data by vipid:"+vipid);
			vh.put("code", "-1");
			vh.put("message", "绑卡失败");
			return vh;
		}
		al=(List)al.get(0);
		
		
		//判断是否已经绑卡
		String isbd=String.valueOf(al.get(14));
		if("Y".equalsIgnoreCase(isbd)){
			vh.put("code", "-1");
			vh.put("message", "不能重复绑卡");
			return vh;
		}
		
		boolean isbsi=false;
		int bsintegral=0;
		boolean isbsc=false;
		int bscouponid=0;
		String couponcode=null;
		String couponvalue=null;
		String coupontype=null;
		String begindate=null;
		String enddate=null;
		String ticket=null;
		
		JSONObject offparam=new JSONObject();
		JSONObject offvipinfo=new JSONObject();
		JSONObject offcouponinfo=new JSONObject();
		HashMap<String, String> params =new HashMap<String, String>();
		
		String ts=String.valueOf(System.currentTimeMillis());
		String sign=null;
		try {
			sign = nds.util.MD5Sum.toCheckSumStr(companyid + ts+ SKEY);
		} catch (IOException e) {
			logger.debug("bindcard md5 error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "绑卡失败，请重试");
			return vh;
		}
		
		try{
			offvipinfo.put("openid",String.valueOf(al.get(0)));
			offvipinfo.put("cardid",String.valueOf(companyid));
			offvipinfo.put("wshno",String.valueOf(al.get(1)));
			offvipinfo.put("name",String.valueOf(al.get(6)));
			offvipinfo.put("phone",phone);
			offvipinfo.put("email",String.valueOf(al.get(4)));
			offvipinfo.put("idno",String.valueOf(al.get(5)));
			offvipinfo.put("cardno",jo.optString("DOCNO"));
			offvipinfo.put("cardpwd",jo.optString("VIPPASSWORD",""));
			offvipinfo.put("viptype",String.valueOf(al.get(2)));
			offvipinfo.put("cardpin","");
			storeid=Tools.getInt(al.get(3),0);
			isbsi="Y".equalsIgnoreCase(String.valueOf(al.get(10)));
			if(isbsi){
				bsintegral=Tools.getInt(al.get(11), 0);
				offvipinfo.put("bsintegral",String.valueOf(bsintegral));
			}
			isbsc="Y".equalsIgnoreCase(String.valueOf(al.get(12)));
			if(isbsc){
				bscouponid=Tools.getInt(al.get(13), 0);
				if(bscouponid==0){
					logger.debug("bindcard send coupon error bscoupon_id:"+bscouponid);
					vh.put("code", "-1");
					vh.put("message", "绑卡失败");
					return vh;
				}
				List couponl=null;
				try{
					couponl=QueryEngine.getInstance().doQueryList("select cp.num,cp.value,cp.usetype1,to_char(decode(nvl(cp.validay,0),0,nvl(cp.starttime,sysdate), sysdate), 'YYYYMMDD'),to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(cp.starttime, 1)),sysdate+cp.validay), 'YYYYMMDD') from wx_coupon cp where cp.id=?",new Object[]{bscouponid});
				}catch(Exception e){
					logger.debug("bindcard send coupon error:"+e.getLocalizedMessage());
					e.printStackTrace();
					vh.put("code", "-1");
					vh.put("message", "绑卡失败");
					return vh;
				}
				
				if(couponl==null||couponl.size()<=0){
					logger.debug("bindcard send coupon error not find coupon by id:"+bscouponid);
					vh.put("code", "-1");
					vh.put("message", "绑卡失败");
					return vh;
				}
				couponl=(List)couponl.get(0);
				couponcode=String.valueOf(couponl.get(0));
				couponvalue=String.valueOf(couponl.get(1));
				coupontype=String.valueOf(couponl.get(2));
				begindate=String.valueOf(couponl.get(3));
				enddate=String.valueOf(couponl.get(4));
				
				//判断券使用类型
				if(!"1".equals(coupontype)){
					offcouponinfo.put("bsccode",couponcode);
					offcouponinfo.put("bscvalue",couponvalue);
					offcouponinfo.put("bscbdate",begindate);
					offcouponinfo.put("bscedate",enddate);
					offparam.put("couponinfo", offcouponinfo);
				}
			}
			offparam.put("vipinfo", offvipinfo);
		}catch(Exception e){
			
		}
		
		
		params.put("args[params]", offparam.toString());
		params.put("args[cardid]",String.valueOf(companyid));
		params.put("format","JSON");
		params.put("client","");
		params.put("ver","1.0");
		params.put("ts",ts);
		params.put("sig",sign);
		params.put("method","bindCard");
		logger.debug("bindcard offparam ------------------>"+offparam.toString());
		//调用线下开卡
		try{
			vh=RestUtils.sendRequest(serverUrl,params,"POST");
		} catch (Throwable e) {
			logger.debug("bindcard offline error->"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "绑卡失败，请重试");
			return vh;
		}
		if(vh==null) {
			logger.error("bindcard offline error->return null");
			vh.put("code", "-1");
			vh.put("message", "绑卡失败，请重试");
			return vh;
		}
		
		String result=(String) vh.get("message");
		logger.debug("bidcard offline code result->"+result);
		JSONObject offjo=null;
		try {
			offjo= new JSONObject(result);
		}catch(Exception e) {
			vh.put("code", "-1");
			vh.put("message", "绑卡失败，请重试");
			return vh;
		}
		
		//判断线下绑卡是否成功
		if(offjo==null||offjo==JSONObject.NULL) {
			vh.put("code", "-1");
			vh.put("message", "线下绑卡异常，请重试");
			return vh;
		}
		if(offjo.optInt("errCode",-1)!=0) {
			vh.put("code", "-1");
			vh.put("message", offjo.optString("errMessage"));
			return vh;
		}
		if(!offjo.has("result")) {
			vh.put("code", "-1");
			vh.put("message", "线下绑卡异常，请重试");
			return vh;
		}
		
		//判断线下绑卡是否返回会员信息
		JSONObject resjo=offjo.optJSONObject("result");
		if(resjo==null||resjo==JSONObject.NULL||!resjo.has("card")&&resjo.optJSONObject("card").has("no")) {
			vh.put("code", "-1");
			vh.put("message", "线下绑卡异常，请重试");
			return vh;
		}
		
		//绑卡送积分
		if(bsintegral>0) {
			ArrayList returnparam=new ArrayList();
			ArrayList sendparam=new ArrayList();
			JSONObject sendintegral=new JSONObject();
			try {
				sendintegral.put("vipid", vipid);
				sendintegral.put("integral", bsintegral);
				sendintegral.put("description", "绑卡送积分");
				sendparam.add(sendintegral.toString());
				returnparam.add(java.sql.Clob.class);
				
				Collection list=QueryEngine.getInstance().executeFunction("wx_vip_adjustintegral",sendparam,returnparam);
				String res=(String)list.iterator().next();
				logger.debug("online bindcard adjustintegral result->"+res);
			}catch(Exception e) {
				logger.error("bindcard send integral error:"+e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		
		//绑卡送券		
		JSONObject vipmessage=resjo.optJSONObject("data");
		if(isbsc&&nds.util.Validator.isNotNull(couponcode)) {
			if(vipmessage!=null&&vipmessage!=JSONObject.NULL&&vipmessage.has("code")) {
				ticket=vipmessage.optString("code");
			}
			JSONObject consumejo=new JSONObject();
			
			ArrayList paramss=new ArrayList();
			paramss.add(companyid);

			ArrayList para=new ArrayList();
			para.add(java.sql.Clob.class);
			
			try {
				consumejo.put("vipid", vipid);
				consumejo.put("couponcode",couponcode);
				consumejo.put("tickno",ticket);
				paramss.add(consumejo.toString());
				
				Collection list=QueryEngine.getInstance().executeFunction("wx_coupon_$r_send",paramss,para);
				String res=(String)list.iterator().next();
				logger.debug("online bindcard brecommend send coupon result->"+res);
			}catch (Exception e) {
				logger.debug("online bindcard brecommend send coupon erroe->"+e.getMessage());
				e.printStackTrace();
			}
		}
		
		JSONObject cardjo=resjo.optJSONObject("card");
		String sql="update wx_vip v"
				   +" set (v.integral,v.lastamt,v.viptype,v.docno,v.relname,v.email,v.phonenum,v.store,v.contactaddress,v.birthday,v.sex,v.province,v.city,v.area,v.isbd,v.bindtime)"
				   +" ="
				   +" (select ?,?,nvl(vbs.id,v.viptype),?,nvl(?,v.relname),?,nvl(?,v.phonenum),nvl(s.id,v.store),?,nvl(?,v.birthday),?,?,?,?,'Y',sysdate from wx_vip v left join wx_vipbaseset vbs on v.id=? and vbs.code=? and vbs.ad_client_id=? left join wx_store s on s.code=? and s.ad_client_id=? where v.id=? and rownum=1)"
				   +" where v.id=?";
		
		try{
			String birthday = cardjo.optString("birthday","");
			birthday = nds.util.Validator.isNotNull(birthday)?birthday:"";
			logger.debug("bindcard updatevip params : birthday-->"+birthday+"  name-->"+cardjo.optString("name","")+" phoneno-->"+cardjo.optString("phonenum",""));
			QueryEngine.getInstance().executeUpdate(sql,new Object[] {cardjo.optInt("credit"),cardjo.optDouble("balance"),cardjo.optString("no"),cardjo.optString("name",""),cardjo.optString("email"),cardjo.optString("phonenum",""),cardjo.optString("address"),birthday,cardjo.optString("gender"),cardjo.optString("province"),cardjo.optString("city"),cardjo.optString("depart"),vipid,cardjo.optString("level"),companyid,cardjo.optString("shopcode"),companyid,vipid,vipid});
		}catch(Exception e){
			logger.debug("online bindcard brecommend send coupon erroe->"+e.getMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "更新会员绑卡异常，请重试");
			return vh;
		}

		vh.put("code", "0");
		vh.put("message", "绑卡成功");
		return vh;
	}
}
