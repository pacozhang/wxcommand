package nds.weixinpublicparty.ext;

import java.rmi.RemoteException;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.publicplatform.api.WeTemplate;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.security.User;
import nds.util.NDSException;
import nds.weixin.ext.WeUtils;
import nds.weixin.ext.WeUtilsManager;

import org.json.JSONException;
import org.json.JSONObject;

public class TemplateCommand extends Command{


	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,RemoteException {
		//��ȡ�û�
		User user = this.helper.getOperator(event);
		ValueHolder vh =new ValueHolder();
		if(user==null) {
			logger.debug("TemplateCommand error->user logout");
			vh.put("code","-1");
			vh.put("message","�û������ڣ������µ�½->");
			return vh;
		}
		
		//��ȡ��ӿ���ص���Ϣ����
		WeUtils wu=WeUtilsManager.getByAdClientId(user.adClientId);
		if(wu==null) {
			vh.put("code", "-1");
			vh.put("message", "��ȡ�����û����ں�������Ϣ");
			return vh;
		}
		WxPublicControl wc=WxPublicControl.getInstance(wu.getAppId());
		JSONObject jo = (JSONObject) event.getParameterValue("jsonObject");
		try {
			jo=new JSONObject(jo.optString("params"));
		} catch (JSONException e) {
			logger.debug("parent is->"+jo.optString("params")+"not typeof JSONObject");
			vh.put("code", "-1");
			vh.put("message", "�����쳣");
			return vh;
		}
		String senddata=jo.optString("senddata");
		if(nds.util.Validator.isNull(senddata)) {
			vh.put("code", "-1");
			vh.put("message", "����ģ������Ϊ��");
			return vh;
		}

		WeTemplate wt=WeTemplate.getInstance(wu.getAppId());
		JSONObject wnjo = wt.sendTemplate(wc, senddata);
		
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
