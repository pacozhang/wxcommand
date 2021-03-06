package nds.weixinpublicparty.ext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.liferay.util.Validator;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.control.web.ClientControllerWebImpl;
import nds.control.web.WebUtils;
import nds.query.QueryEngine;
import nds.query.QueryException;
import nds.rest.RestUtils;
import nds.util.NDSException;
import nds.util.Tools;
import nds.weixin.ext.tools.VipPhoneVerifyCode;

public class OpencardCommand extends Command{

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
			vh.put("message", "开卡异常请重试");
			return vh;
		}
		
		if (jo==null||!jo.has("companyid")||!jo.has("vipid")) {
			logger.error("params error:not put companyid or vipid");
			vh.put("code", "-1");
			vh.put("message", "开卡异常请重试");
			return vh;
		}
		
		int vipid=jo.optInt("vipid",-1);
		int companyid=jo.optInt("companyid",-1);
		
		//领卡导购门店到erp 参数begin--
		String nowstoreid = jo.optString("nowstore","");
		String nowguideid = jo.optString("nowguide","");
		String sex = jo.optString("SEX");
		String recommentCode = "";
		//领卡导购门店到erp 参数end--
		
		
		if (companyid<=0 || vipid<=0) {
			logger.error("params error:companyid:"+companyid+",vipid:"+vipid);
			vh.put("code", "-1");
			vh.put("message", "开卡异常请重试");
			return vh;
		}
		
		//判断接通线下参数
		List all=null;
		QueryEngine qe=null;
		try {
			qe = QueryEngine.getInstance();
			all=qe.doQueryList("select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam,nvl(ifs.ismesauth,'N') from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id WHERE ifs.ad_client_id="+companyid);
		} catch (Exception e) {
			logger.error("select set offline params error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("messae", "开卡异常请重试");
			return vh ;
		}
		if (all==null||all.size()<=0) {
			logger.error("select set offline params error:not find data");
			vh.put("code", "-1");
			vh.put("messae", "开卡异常请重试");
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
			vh.put("messae", "开卡异常请重试");
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
		
		String mallid="";
		//如果门店不为空，则查询出门店所属商城
		if(nds.util.Validator.isNotNull(nowstoreid)){
			try{
				Object omid=qe.doQueryOne("select s.wx_gmall_id from wx_store s where s.id=?",new Object[]{nowstoreid});
				mallid=omid==null?"":String.valueOf(omid);
			}catch(Exception e){
				logger.debug("sear gmallid error"+e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		
		//判断生日券
		String birthdaycouponcode=null;
		String birthdaycouponvalue=null;
		String birthdaycouponbegintime=null;
		String birthdaycouponendtime=null;
		
		//判断是否本月生日
		Date date=new Date();
		JSONObject birthdaycouponinfo=null;
		SimpleDateFormat df=new SimpleDateFormat("yyyyMMdd");
		String ymd=df.format(date);
		String ym=ymd.substring(4, 6);
		
		String birthday=jo.optString("BIRTHDAY");
		if(Validator.isNotNull(birthday)&&Validator.isNotNull(ym)&&ym.equals(birthday.substring(4, 6))){
			String qmsql="select cp.name,cp.num,nvl(cp.usetype1,2),nvl(cp.value,'0'),to_char(decode(nvl(cp.validay,0),0,nvl(cp.starttime,sysdate), sysdate), 'YYYYMMDD'),to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(cp.starttime, 1)),sysdate+cp.validay), 'YYYYMMDD')"
					+" from wx_birthdaycoupon bc join WX_COUPON cp on cp.id=bc.coupon"
					+" where nvl(cp.isactive, 'N') = 'Y'"
					+" and nvl(bc.status,'1') = '2'"
					+" and bc.ad_client_id="+companyid
					+" and to_number(to_char(to_date(bc.senddate,'yyyymmdd'),'yyyymm')) = to_number(to_char(sysdate,'yyyymm'))";
					
			
			List allcoupons=null;

			try {
				allcoupons = qe.doQueryList(qmsql);
			} catch (Exception e) {
				logger.debug("search opencard sendcoupon error:"+e.getLocalizedMessage());
				e.printStackTrace();
			}

			
			if(allcoupons!=null&&allcoupons.size()>0){
				List couponone=null;
				birthdaycouponinfo=new JSONObject();
				couponone=(List)allcoupons.get(0);
				try{
					birthdaycouponinfo.put("couponcode",String.valueOf(couponone.get(1)));
					birthdaycouponinfo.put("couponvalue",String.valueOf(couponone.get(3)));
					birthdaycouponinfo.put("begintime",String.valueOf(couponone.get(4)));
					birthdaycouponinfo.put("endtime",String.valueOf(couponone.get(5)));
				}catch(Exception e){
					
				}
			}
		}
		
		//未接通线下时，开卡送积分与券
		if(!isErp) {
			logger.debug("未接通ERP");
			
			//开卡送积分
			ArrayList returnparam=new ArrayList();
			ArrayList sendparam=new ArrayList();
			JSONObject sendintegral=new JSONObject();
			
			List vips=null;
			int senndintegral=0;
			try {
				vips=QueryEngine.getInstance().doQueryList("select decode(nvl(vbs.integral,'N'),'N',0,nvl(vbs.SENDINTEGRAL,0)),v.opencard_status from wx_vip v left join wx_vipbaseset vbs on v.viptype=vbs.id where v.id=?", new Object[] {vipid});
				if(vips==null||vips.size()<=0) {
					logger.debug("");
					vh.put("code", "-1");
					vh.put("message", "领卡失败");
					return vh;
				}
				vips=(List)vips.get(0);
				senndintegral=Tools.getInt(vips.get(0), 0);

				//判断是否已领卡
				String openstatus=String.valueOf(vips.get(1));
				if("2".equals(openstatus)){
					vh.put("code", "-1");
					vh.put("message", "不能重复领卡");
					return vh;
				}
				
				//线上发券
				ArrayList params=new ArrayList();
				params.add(vipid);
				ArrayList para=new ArrayList();
				para.add( java.sql.Clob.class);
				
				try {
					Collection list=QueryEngine.getInstance().executeFunction("wx_coupon_onlinecoupon",params,para);
					String res=(String)list.iterator().next();
					logger.debug("online send coupon result->"+res);
				}catch (QueryException e) {
					logger.debug("online send coupon erroe->"+e.getMessage());
					e.printStackTrace();
				}
				
				//领卡发生日券
				if(birthdaycouponinfo!=null){
					JSONObject consumejo=new JSONObject();
					
					ArrayList paramss=new ArrayList();
					paramss.add(companyid);

					ArrayList bpara=new ArrayList();
					bpara.add(java.sql.Clob.class);
					
					try {
						consumejo.put("vipid", vipid);
						consumejo.put("couponcode",birthdaycouponinfo.optString("couponcode"));
						consumejo.put("tickno","");
						paramss.add(consumejo.toString());
						
						Collection list=QueryEngine.getInstance().executeFunction("wx_coupon_$r_send",paramss,bpara);
						String res=(String)list.iterator().next();
						logger.debug("online brecommend send coupon result->"+res);
						//JSONObject tempjo=new JSONObject(res);
						//if(tempjo!=null&&"0".equals(tempjo.optString("code","-1"))){insertcount=1;}
					}catch (Exception e) {
						logger.debug("online brecommend send coupon erroe->"+e.getMessage());
						e.printStackTrace();
					}
				}
				
				if(senndintegral<=0){
					vh.put("code", "0");
					vh.put("message", "领卡成功");
					return vh;
				}
				
				sendintegral.put("vipid", vipid);
				sendintegral.put("integral", senndintegral);
				sendintegral.put("description", "领卡送积分");
				sendparam.add(sendintegral.toString());
				returnparam.add(java.sql.Clob.class);
				
				Collection list=QueryEngine.getInstance().executeFunction("wx_vip_adjustintegral",sendparam,returnparam);
				String res=(String)list.iterator().next();
				logger.debug("online opencard adjustintegral result->"+res);
				
				//修改领卡成功
				String sql="update wx_vip v set v.opencard_status=2,v.opendate=to_number(to_char(sysdate,'yyyyMMdd')),v.birthday=?,v.relname=?,v.sex=?,v.phonenum=?,v.store=?,v.nowstore=?, v.guide=?,v.nowguide=?,v.wx_gmall_id=? where v.id=?";
				try {
					QueryEngine.getInstance().executeUpdate(sql, new Object[] {jo.optString("BIRTHDAY"),jo.optString("RELNAME"),jo.optString("SEX"),phone,nowstoreid,nowstoreid,nowguideid,nowguideid,mallid,vipid});
				} catch (Exception e) {
					logger.error("opencard update vip error:"+e.getLocalizedMessage());
					e.printStackTrace();
					vh.put("code", "-1");
					vh.put("message", "领卡失败，请重试");
					return vh;
				}
				vh.put("code", "0");
				vh.put("message", "领卡成功");
			}catch(Exception e) {
				logger.debug("opencard adjustintegral error:"+e.getLocalizedMessage());
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "领卡失败，请重试");
			}
			return vh;
		}
		
		//调用线下开卡
		String ts=String.valueOf(System.currentTimeMillis());
		String sign=null;
		try {
			sign = nds.util.MD5Sum.toCheckSumStr(companyid + ts+ SKEY);
		} catch (IOException e) {
			logger.debug("opencard md5 error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "领卡失败，请重试");
			return vh;
		}
		HashMap<String, String> params =new HashMap<String, String>();
		//Connection conn = QueryEngine.getInstance().getConnection();
		boolean isSendCoupon=false;
		String couponUseType="0";
		String couponCode="";
		//conn.setAutoCommit(false);
		List al=null;
		JSONObject offparam=new JSONObject();
		JSONObject offvipinfo=new JSONObject();
		JSONObject offcouponinfo=new JSONObject();
		//组织参数
		try {
			al = QueryEngine.getInstance().doQueryList("select vp.wechatno,vp.vipcardno,vp.store,NVL(vt.ISSEND,'N'),cp.num,cp.usetype1,vt.code,nvl(cp.value,'0'),decode(nvl(vt.integral,'N'),'N',0,nvl(vt.sendintegral,0)),to_char(decode(nvl(cp.validay,0),0,nvl(cp.starttime,sysdate), sysdate), 'YYYYMMDD'),to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(cp.starttime, 1)),sysdate+cp.validay), 'YYYYMMDD'),wt.code,vp.relname,vp.sex,vp.birthday,vp.contactaddress,vp.opencard_status,vp.recomment_vip"+
					" from wx_vip vp LEFT JOIN wx_vipbaseset vt ON vp.viptype=vt.id LEFT JOIN WX_COUPON CP ON NVL(vt.LQTYPE,-1)=cp.id left join wx_store wt on vp.store=wt.id"+
					" WHERE vp.id=?",new Object[] {vipid});
		} catch (Exception e) {
			logger.error("opencard find vip error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "领卡失败，请重试");
			return vh;
		}
		
		if(all==null||al.size()<=0) {
			logger.error("opencard find vip error:not find data by vipid:"+vipid);
			vh.put("code", "-1");
			vh.put("message", "领卡失败");
			return vh;
		}
		
		al=(List)al.get(0);
		int integral=0;
		String coupontypecode=String.valueOf(al.get(4));
		String openstatus=String.valueOf(al.get(16));
		//判断是否重复领卡
		if("2".equals(openstatus)){
			logger.error("opencard find vip error:having opencard vipid:"+vipid);
			vh.put("code", "-1");
			vh.put("message", "不能重复领卡");
			return vh;
		}
		String storecode=null;
		String guideCode=null;
		try {
			integral=Tools.getInt(al.get(8), 0);
			offvipinfo.put("openid", String.valueOf (al.get(0)));
			offvipinfo.put("cardid",String.valueOf(companyid));
			offvipinfo.put("wshno",String.valueOf(al.get(1)));
			//offvipinfo.put("shopid",String.valueOf(al.get(2)));
			offvipinfo.put("shopid",String.valueOf(nowstoreid));
			offvipinfo.put("viptype",String.valueOf(al.get(6)));
			offvipinfo.put("credit",integral);
			//offvipinfo.put("storecode",String.valueOf(al.get(11)));
			offvipinfo.put("name",jo.optString("RELNAME"));
			offvipinfo.put("gender",jo.optString("SEX"));
			offvipinfo.put("birthday",jo.optString("BIRTHDAY"));
			offvipinfo.put("contactaddress",jo.optString("CONTACTADDRESS"));				
			offvipinfo.put("phonenum",phone);
			
			Object ostore=null;
			Object oguide=null;
			
			if(nds.util.Validator.isNotNull(nowstoreid)){
				try{
					ostore=QueryEngine.getInstance().doQueryOne("select t.code from wx_store t where t.id=?", new Object[]{nowstoreid});
					storecode=String.valueOf(ostore);
				}catch(Exception e){
					
				}
			}
			
			if(nds.util.Validator.isNotNull(nowguideid)){
				try{
					oguide=QueryEngine.getInstance().doQueryOne("select t.code from wx_guide t where t.id=?", new Object[]{nowguideid});
					guideCode=String.valueOf(oguide);
				}catch(Exception e){
					
				}
			}
			
			recommentCode = String.valueOf(al.get(17));
			offvipinfo.put("storecode",nds.util.Validator.isNotNull(storecode)?storecode:"" );//开卡门店编号
			offvipinfo.put("guideCode",nds.util.Validator.isNotNull(guideCode)?guideCode:"" );//开卡导购编号
			offvipinfo.put("recommentCode","null".equals(recommentCode)?"":recommentCode);//推荐人星云卡号

			isSendCoupon="Y".equalsIgnoreCase(String.valueOf(al.get(3)));
			couponUseType= String.valueOf(al.get(5));
			logger.debug("isSendCoupon->"+isSendCoupon+",couponCode->"+couponCode+",couponUseType->"+couponUseType);
			if(isSendCoupon||!"1".equalsIgnoreCase(couponUseType)&&nds.util.Validator.isNotNull(coupontypecode)) {
				offcouponinfo.put("coupon",coupontypecode);
				offcouponinfo.put("couponval",String.valueOf(al.get(7)));
				offcouponinfo.put("begintime",String.valueOf(al.get(9)));
				offcouponinfo.put("endtime",String.valueOf(al.get(10)));
				offparam.put("couponinfo", offcouponinfo);
			}
			offparam.put("vipinfo", offvipinfo);
			offparam.put("couponinfo", offcouponinfo);
			if(birthdaycouponinfo!=null&&birthdaycouponinfo.has("endtime")){offparam.put("birthdaycoupon", birthdaycouponinfo);}
			
			params.put("args[params]", offparam.toString());
			params.put("args[cardid]",String.valueOf(companyid));
			params.put("format", "JSON");
			params.put("client", "");
			params.put("ver","1.0");
			params.put("ts",ts);
			params.put("sig",sign);
			params.put("method","openCard");
		} catch (JSONException e) {
			logger.error("set offline params error:"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "领卡失败，请重试");
			return vh;
		}
		
		
		//调用线下开卡
		try{
			vh=RestUtils.sendRequest(serverUrl,params,"POST");
			//String url=serverUrl+"?"+RestUtils.delimit(params.entrySet(),true);
			//vh=RestUtils.sendRequest(url,null,"GET");
		} catch (Throwable e) {
			logger.debug("open card offline error->"+e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "领卡失败，请重试");
			return vh;
		}
		if(vh==null) {
			logger.error("open card offline error->return null");
			vh.put("code", "-1");
			vh.put("message", "领卡失败，请重试");
			return vh;
		}
		
		//处理线下开卡返回结果
		//{"result":{"data":{"code":"26f5lb99fr0-0","couponId":"6F5Lb99Fr"},"card":{"balance":0,"level":215,"no":"WX140515000000002","credit":0},"openid":"owAZBuEBBLn-LQ_5ebcbkSh_wFDk","cardid":"37"},"errMessage":"微生活会员开卡成功！","errCode":0}
		String result=(String) vh.get("message");
		logger.debug("open offline code result->"+result);
		JSONObject offjo=null;
		try {
			offjo= new JSONObject(result);
		}catch(Exception e) {
			vh.put("code", "-1");
			vh.put("message", "领卡失败，请重试");
			return vh;
		}
		
		//判断线下开卡是否成功
		if(offjo==null||offjo==JSONObject.NULL) {
			vh.put("code", "-1");
			vh.put("message", "线下领卡异常，请重试");
			return vh;
		}
		if(offjo.optInt("errCode",-1)!=0) {
			vh.put("code", "-1");
			vh.put("message", offjo.optString("errMessage"));
			return vh;
		}
		if(!offjo.has("result")) {
			vh.put("code", "-1");
			vh.put("message", "线下领卡异常，请重试");
			return vh;
		}
			
		//判断线下开卡是否返回会员信息
		JSONObject resjo=offjo.optJSONObject("result");
		if(resjo==null||resjo==JSONObject.NULL||!resjo.has("card")&&resjo.optJSONObject("card").has("no")) {
			vh.put("code", "-1");
			vh.put("message", "线下领卡异常，请重试");
			return vh;
		}
		
		//生成VIP二维码
		ClientControllerWebImpl controller=(ClientControllerWebImpl)WebUtils.getServletContextManager().getActor(nds.util.WebKeys.WEB_CONTROLLER);
		JSONObject param=new JSONObject();
		JSONObject jsparam=new JSONObject();
		try {
			param.put("vipid", vipid);
			jsparam.put("params", param);
			
			DefaultWebEvent qrcodeevent=new DefaultWebEvent("CommandEvent");
			qrcodeevent.put("jsonObject", jsparam);
			qrcodeevent.setParameter("command", "nds.weixinpublicparty.ext.SendTwoDimensionalCodeCommand");
			controller.handleEventBackground(qrcodeevent);
		} catch (JSONException e2) {
			e2.printStackTrace();
		}
		
		
		//领卡送积分
		if(integral>0) {
			ArrayList returnparam=new ArrayList();
			ArrayList sendparam=new ArrayList();
			JSONObject sendintegral=new JSONObject();
			try {
				sendintegral.put("vipid", vipid);
				sendintegral.put("integral", integral);
				sendintegral.put("description", "领卡送积分");
				sendparam.add(sendintegral.toString());
				returnparam.add(java.sql.Clob.class);
				
				Collection list=QueryEngine.getInstance().executeFunction("wx_vip_adjustintegral",sendparam,returnparam);
				String res=(String)list.iterator().next();
				logger.debug("online opencard adjustintegral result->"+res);
			}catch(Exception e) {
				logger.error("opencard send integral error:"+e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		
		//领卡送券
		JSONObject vipmessage=resjo.optJSONObject("data");
		if(isSendCoupon&&nds.util.Validator.isNotNull(coupontypecode)) {
			String couponcode=null;
			if(vipmessage!=null&&vipmessage!=JSONObject.NULL&&vipmessage.has("code")) {
				couponcode=vipmessage.optString("code");
			}
			JSONObject consumejo=new JSONObject();
			
			ArrayList paramss=new ArrayList();
			paramss.add(companyid);

			ArrayList para=new ArrayList();
			para.add(java.sql.Clob.class);
			
			try {
				consumejo.put("vipid", vipid);
				consumejo.put("couponcode",coupontypecode);
				consumejo.put("tickno",couponcode);
				paramss.add(consumejo.toString());
				
				Collection list=QueryEngine.getInstance().executeFunction("wx_coupon_$r_send",paramss,para);
				String res=(String)list.iterator().next();
				logger.debug("online brecommend send coupon result->"+res);
				//JSONObject tempjo=new JSONObject(res);
				//if(tempjo!=null&&"0".equals(tempjo.optString("code","-1"))){insertcount=1;}
			}catch (Exception e) {
				logger.debug("online brecommend send coupon erroe->"+e.getMessage());
				e.printStackTrace();
			}
		}
		
		//领卡送生日券
		JSONObject offbirthdaycoupon=resjo.optJSONObject("birthdaycoupon");
		if(birthdaycouponinfo!=null&&offbirthdaycoupon!=null&&offbirthdaycoupon.has("ticket")){
			JSONObject consumejo=new JSONObject();
			
			ArrayList paramss=new ArrayList();
			paramss.add(companyid);

			ArrayList bpara=new ArrayList();
			bpara.add(java.sql.Clob.class);
			
			try {
				consumejo.put("vipid", vipid);
				consumejo.put("couponcode",birthdaycouponinfo.optString("couponcode"));
				consumejo.put("tickno",offbirthdaycoupon.optString("ticket"));
				paramss.add(consumejo.toString());
				
				Collection list=QueryEngine.getInstance().executeFunction("wx_coupon_$r_send",paramss,bpara);
				String res=(String)list.iterator().next();
				logger.debug("online brecommend send coupon result->"+res);
				//JSONObject tempjo=new JSONObject(res);
				//if(tempjo!=null&&"0".equals(tempjo.optString("code","-1"))){insertcount=1;}
			}catch (Exception e) {
				logger.debug("online brecommend send coupon erroe->"+e.getMessage());
				e.printStackTrace();
			}
		}
		
		//修改会员资料
		JSONObject offvipjo=resjo.optJSONObject("card");
		
		String sql="update wx_vip v set (v.docno,v.vipcardno,v.viptype,v.store,v.isbd,v.opencard_status,v.opendate,v.integral,v.birthday,v.relname,v.sex,v.phonenum,v.contactaddress,v.province,v.city,v.area,v.nowstore,v.guide,v.nowguide,v.wx_gmall_id)="
				+" (select ?,?,nvl(vbs.id,ov.viptype),nvl(?,nvl(s.id,ov.store)),?,2,to_number(to_char(sysdate,'yyyyMMdd')),?,?,?,?,?,?,?,?,?,nvl(?,nvl(s.id,ov.store)),?,?,? from wx_vip ov left join wx_vipbaseset vbs on vbs.code=? and vbs.ad_client_id=? and vbs.code is not null left join wx_store s on s.code=? and s.ad_client_id=? and s.code is not null  where ov.id=? and rownum=1)"
				+" where v.id=?";
		
		try {
			QueryEngine.getInstance().executeUpdate(sql, new Object[] {offvipjo.optString("no"),offvipjo.optString("wshno"),nowstoreid,offvipjo.optString("isbd","N"),integral,jo.optString("BIRTHDAY"),jo.optString("RELNAME"),nds.util.Validator.isNotNull(sex)?sex:jo.optString("GENDER"),phone,jo.optString("CONTACTADDRESS"),jo.optString("PROVINCE"),jo.optString("CITY"),jo.optString("AREA"),
					nowstoreid,nowguideid,nowguideid,mallid,offvipjo.optString("level"),companyid,offvipjo.optString("shopcode"),companyid,vipid,vipid});
		} catch (Exception e) {
			logger.error("opencard update vip error:"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		vh.put("code", "0");
		vh.put("message", "领卡成功");
		return vh;
	}

}
