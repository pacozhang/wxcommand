package nds.weixinpublicparty.ext;

import java.util.List;

import nds.process.SvrProcess;
import nds.publicplatform.api.WeTemplate;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.query.QueryEngine;
import nds.weixin.ext.WeUtils;
import nds.weixin.ext.WeUtilsManager;

import org.json.JSONException;
import org.json.JSONObject;

import com.alibaba.fastjson.JSON;

public class ReservationRemindersvr extends SvrProcess{
	@Override
	protected void prepare() {
		
	}

	@Override
	protected String doIt() throws Exception {
		//-------------------------------------------sql
		List all=null;
		String itemname="";
		String openid="";
		String overdate="";
		JSONObject ev=new JSONObject();
		try{
			all=QueryEngine.getInstance().doQueryList("select to_char(wm_concat(ba.itemname)), v.wechatno, o.overduedate,o.ad_client_id from wx_order o join wx_vip v on o.wx_vip_id = v.id join wx_orderitem oi  on oi.wx_order_id = o.id join wx_brand_appendgoods ba on ba.id = oi.wx_brand_appendgoods_id where o.sale_status = 2 and o.ordertype = 5 and o.overduedate is not null and o.overduedate >= sysdate and o.overduedate <= sysdate + 1 group by v.wechatno, o.overduedate,o.ad_client_id");
		}catch(Exception e){
			e.printStackTrace();
		}
		if(all==null||all.size()<=0){
			return null;
		}

		for(int i=0;i<all.size();i++){
			itemname=String.valueOf(((List)all.get(i)).get(0));
			openid=String.valueOf(((List)all.get(i)).get(1));
			overdate=String.valueOf(((List)all.get(i)).get(2));
			String adclientid=String.valueOf(((List)all.get(i)).get(3));
		
	//		String itemname=String.valueOf(all.get(0));
	//		String openid=String.valueOf(all.get(1));
	//		String overdate=String.valueOf(all.get(2));
	//		if(openid==null){
	//			return null;
	//		}
			
			//-----------------------------------------打包数据（我是分割线）
	
	
			JSONObject first=new JSONObject();
			JSONObject keyword1=new JSONObject();
			JSONObject keyword2=new JSONObject();
			JSONObject remark=new JSONObject();	
			try{
			first.put("value", "尊敬的顾客，您好，您有预约的商品要到期了");
			first.put("color", "#173177");
			keyword1.put("value", "预约单号为"+itemname);
			keyword1.put("color", "#173177");
			keyword2.put("value", "截止至"+overdate);
			keyword2.put("color", "#173177");
			remark.put("value", "点击查看预约商品详情");
			remark.put("color", "#173177");
			}catch(Exception e){
				e.printStackTrace();
			}
			
			JSONObject data=new JSONObject();
			try{
			data.put("fiest", first);
			data.put("keyword1", keyword1);
			data.put("keyword2", keyword2);
			data.put("remark", remark);
			}catch(Exception e){
				e.printStackTrace();
			}
			
			JSONObject reservation=new JSONObject();
			try {
				reservation.put("touser",openid);
												//zdBFxFDJumaOUvK8p_4ZEmBucOhCgu7kqM7-EUOubl4
				reservation.put("template_id", "zdBFxFDJumaOUvK8p_4ZEmBucOhCgu7kqM7-EUOubl4");
				reservation.put("url", "http://weixin.qq.com/download");
				reservation.put("data", data);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			String rem=reservation.toString();
			if(rem==null){
				return null;
			}
			
			
			
			if(nds.util.Validator.isNull(adclientid)){
				return null;
			}
			int ad_client_id=Integer.parseInt(adclientid);
			WeUtils wu=WeUtilsManager.getByAdClientId(ad_client_id);
			if(wu==null){
				return null;
			}
			WxPublicControl wc=WxPublicControl.getInstance(wu.getAppId());
			WeTemplate wt=WeTemplate.getInstance(wu.getAppId());
			JSONObject wnjo = wt.sendTemplate(wc, rem);
	
	
		}
			return null;		
	}
}
