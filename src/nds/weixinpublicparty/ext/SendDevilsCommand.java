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
				//��ȡ�û�
				User user = this.helper.getOperator(event);
				JSONObject jo=null;
				ValueHolder vh =new ValueHolder();

				//��ȡ��ӿ���ص���Ϣ����
			
				//jsonobject������ȡ��jsonobject��key---->paramsҲ���Ƕ�Ӧ��js��params��ֵ
				//ֵ���Լ�ֵ�Ե���ʽ(key��value)����ʽ�洢��JSONObject��
				try {
					jo = (JSONObject) event.getParameterValue("jsonObject");
				logger.error("jo--------?>"+jo);	
					jo=jo.optJSONObject("params");
				logger.error("jo1-------->"+jo);	
				}catch(Exception e) {
					logger.error("params error:"+e.getLocalizedMessage());
					e.printStackTrace();
					vh.put("code", "-1");
					vh.put("message", "������ѯ�쳣1");
					return vh;
				}
				
				//jsonobject�е�value��һ��jsonobject���󣬷�װ��value��ֵ
				if (jo==null||!jo.has("orderid")) {
					logger.error("params error:not put companyid or orderid");
					vh.put("code", "-1");
					vh.put("message", "������ѯ�쳣2");
					return vh;
				}
//				if (jo==null) {
//					logger.error("params error:not put companyid or orderid");
//					vh.put("code", "-1");
//					vh.put("message", "������ѯ�쳣2");
//					return vh;
//				}
				
				//ȡ������ֵ
				int orderid=jo.optInt("orderid",-1);
				logger.error("order------>"+orderid);
				int companyid=jo.optInt("companyid",-1);
				logger.error("companyid------>"+companyid);
				if(companyid<=0){
					logger.error("params error:companyid");
					vh.put("code", "-1");
					vh.put("message", "������ѯ�쳣3");
					return vh;
				}
				WeUtils wu=WeUtilsManager.getByAdClientId(companyid);
				
				if(wu==null) {
					vh.put("code", "-1");
					vh.put("message", "��ȡ�����û����ں�������Ϣ");
					return vh;
				}
				WxPublicControl wc=WxPublicControl.getInstance(wu.getAppId());

				if (orderid<=0) {
					logger.error("params error:orrderid:"+orderid);
					vh.put("code", "-1");
					vh.put("message", "������ѯ�쳣4");
					return vh;
				}
				//list��ʽ��ѯȡ�����ݿ���ֶ�ֵ�����Ҷ�ȡ�����ֶ�ֵ����Ϊ�յ��ж�
				List all=null;
				try{
				all=QueryEngine.getInstance().doQueryList("select o.docno,o.tot_amt,v.wechatno from wx_order o join wx_vip v on o.wx_vip_id=v.id where o.id=?",new Object[] {orderid});
				logger.error("all?------->"+all);
				}catch(Exception e){
					e.printStackTrace();
					vh.put("code", "-1");
					vh.put("message", "��ѯ�����쳣5");
				}
				
				if(all==null||all.size()<0){
					logger.error("all-------->"+all);
					vh.put("code", -1);
					vh.put("message", "��ʱû�з�����Ϣ");
					return vh;
				}
				//docno Ϊ������te_num  tit_amtΪ���total   wechatnoΪ���͵���openid
				all=(List)all.get(0);
				//ȡ��ֵ��ʱ������tostring��������ֹ�����ֶ�Ϊ�յ��������ʹ��tostring�Ҷ����жϣ���ô��Ҫ�жϵĵط��ܶ�
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
				//�����ݽ��д����ʱ����Ҫ��jsonobject�����룬����֮�����һ��json��ʽ�Ķ���
				//ʹ��put��ʽ��ֵ
				JSONObject first=new JSONObject();
				JSONObject keynote1=new JSONObject();
				JSONObject keynote2=new JSONObject();
				JSONObject keynote3=new JSONObject();
				JSONObject remark=new JSONObject();
				try {
					first.put("value", "���ķ����Ѿ����,����Ķ�����Ϊ:"+te_num);
					first.put("color", "#173177");
					keynote1.put("value", devils_nam);
					keynote1.put("color", "#173177");
					keynote2.put("value",  total);
					keynote2.put("color", "#173177");
					keynote3.put("value", "֧���ɹ�");
					keynote3.put("color", "#173177");
					remark.put("value", "���Ļ����Ѿ����ͣ���ӭ�����´ι���");
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
					vh.put("message", "����ģ������Ϊ��");
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
					vh.put("message","ʧ�ܣ�");
				}
				
				
		return vh;
	}

}
