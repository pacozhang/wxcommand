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

public class PaySuccessCommand extends Command{

	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {
		ValueHolder vh =new ValueHolder();
		User user = this.helper.getOperator(event);
		JSONObject jo=null;
		
		try{
			jo=(JSONObject) event.getParameterValue("jsonObject");
			jo=jo.optJSONObject("params");
		}catch(Exception e){
			e.printStackTrace();
			vh.put("code", -1);
			vh.put("message", "֧��ʧ��1");
		}
		
		if(jo==null||!jo.has("orderid")||!jo.has("companyid")){
			vh.put("code", -1);
			vh.put("message", "û���ҵ���Ӧ����");
			return vh;
		}
		int orderid=jo.optInt("orderid",-1);
		int companyid=jo.optInt("companyid",-1);
		if(companyid<=0){
			logger.error("params error:companyid");
			vh.put("code", "-1");
			vh.put("message", "֧��ʧ��2");
			return vh;
		}
		WeUtils wu=WeUtilsManager.getByAdClientId(companyid);
		
		if(wu==null) {
			vh.put("code", "-1");
			vh.put("message", "֧��ʧ��3");
			return vh;
		}
		WxPublicControl wc=WxPublicControl.getInstance(wu.getAppId());

		if (orderid<=0) {
			logger.error("params error:orrderid:"+orderid);
			vh.put("code", "-1");
			vh.put("message", "֧��ʧ��4");
			return vh;
		}
		
		//---------------------------------------------���ݿⲿ��
		List all=null;
		try{
			all=QueryEngine.getInstance().doQueryList("select o.docno,o.tot_amt,v.wechatno from wx_order o join wx_vip v on o.wx_vip_id=v.id where o.id=?",new Object[] {orderid});
		}catch(Exception e){
			e.printStackTrace();
			vh.put("code", -1);
			vh.put("message", "֧��ʧ��5");
			return vh;
		}
		if(all==null||all.size()<=0){
			vh.put("code", -1);
			vh.put("message", "֧��ʧ��6");
		}
		all=(List)all.get(0);
		//docno--->te_num Ϊ������  tit_amt----->totalΪ���   wechatno------>openidΪ���͵�����
		String te_num=String.valueOf(all.get(0));
		String total=String.valueOf(all.get(1));
		String openid=String.valueOf(all.get(2));
		if(openid==null){
			vh.put("code", -1);
			vh.put("message", "����ʧ��");
			return vh;
		}
		Object ec=null;
		try{
		ec=QueryEngine.getInstance().doQueryOne("select to_char(wm_concat(bab.itemstatus))  from wx_orderitem oi join wx_brand_appendgoods bab on oi.wx_brand_appendgoods_id=bab.id where oi.wx_order_id=?",new Object[]{orderid});

		//ecΪ��������
		}catch(Exception e){
			e.printStackTrace();
			vh.put("code", -1);
			vh.put("message", "��ѯ�����쳣6");
		}
		if(ec==null){
			vh.put("code", -1);
			vh.put("message", "�޷�����Ϣ");
		}
		String devils_nam=String.valueOf(ec);
		//------------------------------------------------����΢�ŵ�json��ʽ������
		JSONObject first=new JSONObject();
		JSONObject orderMoneySum=new JSONObject();
		JSONObject orderProductName=new JSONObject();
		JSONObject Remark=new JSONObject();
		try{
			first.put("value", "����֧���Ѿ����,������Ϊ"+te_num+",�벻Ҫй¶���Ķ�����Ϣ���Է���ƭ");
			first.put("color", "#173177");
			orderMoneySum.put("value", total);
			first.put("color", "#173177");
			orderProductName.put("value", devils_nam);
			first.put("color", "#173177");
			Remark.put("value", "�ǳ���л�ϵ۶����ǵ�֧��");
			first.put("color", "#173177");
		}catch(Exception e){
			e.printStackTrace();
		}
		JSONObject data=new JSONObject();
		try{
			data.put("first", first);
			data.put("orderMoneySum", orderMoneySum);
			data.put("orderProductName", orderProductName);
			data.put("Remark", Remark);
		}catch(Exception e){
			
		}
		
		JSONObject paysuccess=new JSONObject();
		try {
			paysuccess.put("touser", openid);
											//ITQlax9ilxtCZjkTV4Qb3we87IUTAai836BN9N0dbE4
			paysuccess.put("template_id", "ITQlax9ilxtCZjkTV4Qb3we87IUTAai836BN9N0dbE4");
			paysuccess.put("url", "http://weixin.qq.com/download");
			paysuccess.put("data", data);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String py=paysuccess.toString();
		if(nds.util.Validator.isNull(py)) {
			vh.put("code", "-1");
			vh.put("message", "����ģ������Ϊ��");
			return vh;
		}
		
		WeTemplate wt=WeTemplate.getInstance(wu.getAppId());
		JSONObject wnjo = wt.sendTemplate(wc, py);
		
		if(wnjo!=null) {
			vh.put("code",wnjo.optInt("code"));
			vh.put("message",wnjo.optString("message"));
			vh.put("data", wnjo);
		}else {
			vh.put("code","-1");
			vh.put("message","ʧ�ܣ�");
		}
		
		
		return vh;
	}

}
