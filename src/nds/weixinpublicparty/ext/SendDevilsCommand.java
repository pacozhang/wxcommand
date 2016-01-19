package nds.weixinpublicparty.ext;

import java.rmi.RemoteException;

import java.util.List;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.publicplatform.api.WeTemplate;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.query.QueryEngine;
import nds.security.User;
import nds.util.NDSException;
import nds.weixin.ext.WeUtils;
import nds.weixin.ext.WeUtilsManager;

import org.json.JSONException;
import org.json.JSONObject;

public class SendDevilsCommand extends Command{

	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {
				//获取用户
				User user = this.helper.getOperator(event);
				JSONObject jo=null;
				ValueHolder vh =new ValueHolder();

				//获取与接口相关的信息对象
			
				//jsonobject对象中取出jsonobject的key---->params也就是对应的js中params的值
				//值是以键值对的形式(key，value)的形式存储在JSONObject中
				try {
					jo = (JSONObject) event.getParameterValue("jsonObject");
				logger.error("jo--------?>"+jo);	
					jo=jo.optJSONObject("params");
				logger.error("jo1-------->"+jo);	
				}catch(Exception e) {
					logger.error("params error:"+e.getLocalizedMessage());
					e.printStackTrace();
					vh.put("code", "-1");
					vh.put("message", "发货查询异常1");
					return vh;
				}
				
				//jsonobject中的value是一个jsonobject对象，封装了value的值
				if (jo==null||!jo.has("orderid")) {
					logger.error("params error:not put companyid or orderid");
					vh.put("code", "-1");
					vh.put("message", "发货查询异常2");
					return vh;
				}
//				if (jo==null) {
//					logger.error("params error:not put companyid or orderid");
//					vh.put("code", "-1");
//					vh.put("message", "发货查询异常2");
//					return vh;
//				}
				
				//取出参数值
				int orderid=jo.optInt("orderid",-1);
				logger.error("order------>"+orderid);
				int companyid=jo.optInt("companyid",-1);
				logger.error("companyid------>"+companyid);
				if(companyid<=0){
					logger.error("params error:companyid");
					vh.put("code", "-1");
					vh.put("message", "发货查询异常3");
					return vh;
				}
				WeUtils wu=WeUtilsManager.getByAdClientId(companyid);
				
				if(wu==null) {
					vh.put("code", "-1");
					vh.put("message", "获取不到用户公众号配置信息");
					return vh;
				}
				WxPublicControl wc=WxPublicControl.getInstance(wu.getAppId());

				if (orderid<=0) {
					logger.error("params error:orrderid:"+orderid);
					vh.put("code", "-1");
					vh.put("message", "发货查询异常4");
					return vh;
				}
				//list形式查询取出数据库的字段值，并且对取出的字段值进行为空的判断
				List all=null;
				try{
				all=QueryEngine.getInstance().doQueryList("select o.docno,o.tot_amt,v.wechatno from wx_order o join wx_vip v on o.wx_vip_id=v.id where o.id=?",new Object[] {orderid});
				logger.error("all?------->"+all);
				}catch(Exception e){
					e.printStackTrace();
					vh.put("code", "-1");
					vh.put("message", "查询发货异常5");
				}
				
				if(all==null||all.size()<0){
					logger.error("all-------->"+all);
					vh.put("code", -1);
					vh.put("message", "暂时没有发货信息");
					return vh;
				}
				//docno 为订单号te_num  tit_amt为金额total   wechatno为发送的人openid
				all=(List)all.get(0);
				//取出值得时候不能用tostring方法，防止出现字段为空的情况。若使用tostring且对其判断，那么需要判断的地方很多
				String te_num=String.valueOf(all.get(0));
				String total=String.valueOf(all.get(1));
				String openid=String.valueOf(all.get(2));
				if(openid==null){
					vh.put("code", -1);
					vh.put("message", "发货失败");
					return vh;
				}
				Object ec=null;
				try{
				ec=QueryEngine.getInstance().doQueryOne("select to_char(wm_concat(bab.itemstatus))  from wx_orderitem oi join wx_brand_appendgoods bab on oi.wx_brand_appendgoods_id=bab.id where oi.wx_order_id=?",new Object[]{orderid});


				}catch(Exception e){
					e.printStackTrace();
					vh.put("code", -1);
					vh.put("message", "查询发货异常6");
				}
				if(ec==null){
					vh.put("code", -1);
					vh.put("message", "无发货信息");
				}
				String devils_nam=String.valueOf(ec);
				//对数据进行打包的时候主要用jsonobject对象传入，传入之后就是一个json形式的对象
				//使用put方式传值
				JSONObject first=new JSONObject();
				JSONObject keynote1=new JSONObject();
				JSONObject keynote2=new JSONObject();
				JSONObject keynote3=new JSONObject();
				JSONObject remark=new JSONObject();
				try {
					first.put("value", "您的发货已经完成,货物的订单号为:"+te_num);
					first.put("color", "#173177");
					keynote1.put("value", devils_nam);
					keynote1.put("color", "#173177");
					keynote2.put("value",  total);
					keynote2.put("color", "#173177");
					keynote3.put("value", "支付成功");
					keynote3.put("color", "#173177");
					remark.put("value", "您的货物已经发送，欢迎您的下次光临");
					remark.put("color", "173177");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				JSONObject data=new JSONObject();
				try {
					data.put("first", first);
					data.put("keynote1", keynote1);
					data.put("keynote2", keynote2);
					data.put("keynote3", keynote3);
					data.put("remark", remark);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				

				
				JSONObject senddevils=new JSONObject();
				try {
					senddevils.put("touser", openid);
													//RY1699VHy1kTvQoOMq94C3PcXI6c8WEJ55sFF4dPS0U
					senddevils.put("template_id", "RY1699VHy1kTvQoOMq94C3PcXI6c8WEJ55sFF4dPS0U");
					senddevils.put("url", "http://weixin.qq.com/download");
					senddevils.put("data", data);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				String sd=senddevils.toString();
				if(nds.util.Validator.isNull(sd)) {
					vh.put("code", "-1");
					vh.put("message", "发送模板内容为空");
					return vh;
				}

				WeTemplate wt=WeTemplate.getInstance(wu.getAppId());
				JSONObject wnjo = wt.sendTemplate(wc, sd);
				
				if(wnjo!=null) {
					vh.put("code",wnjo.optInt("code"));
					vh.put("message",wnjo.optString("message"));
					vh.put("data", wnjo);
				}else {
					vh.put("code","-1");
					vh.put("message","失败！");
				}
				
				
		return vh;
	}

}
