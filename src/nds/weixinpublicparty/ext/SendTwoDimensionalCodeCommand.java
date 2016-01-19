package nds.weixinpublicparty.ext;

import java.io.File;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.liferay.util.Validator;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.publicplatform.api.WePopularizeSupport;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.query.QueryEngine;
import nds.util.Configurations;
import nds.util.NDSException;
import nds.util.Tools;
import nds.util.WebKeys;
import nds.weixin.ext.WeUtils;
import nds.weixin.ext.WeUtilsManager;

public class SendTwoDimensionalCodeCommand extends Command {
	
	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {

		ValueHolder vh =new ValueHolder();
		JSONObject wjo=new JSONObject();
		
		JSONObject jo = (JSONObject) event.getParameterValue("jsonObject");
		try {
			jo=new JSONObject(jo.optString("params"));
		} catch (JSONException e2) {
			e2.printStackTrace();
		}
		int vipid=jo.optInt("vipid",-1);
		if(vipid<=0){
			vh.put("code", "-1");
			vh.put("message", "失败");
			return vh;
		}

		try {
			//jo=new JSONObject(jo.optString("vip"+vip));
			//{"scene": {"scene_str": "123"}}
			wjo.put("action_info","{\"scene\":{\"scene_str\":\"vip"+vipid+"\"}}");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		logger.debug("jo->"+jo.toString());
		
		String viprisql="select v.rqcode,v.ad_client_id,v.photo from WX_VIP v where v.id = "+vipid;
		
		QueryEngine qe=null;
		try {
			qe = QueryEngine.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "失败");
			return vh;
		}
		
		List vips=null;
		int adClientId = 0;
		String rqcode = null;
	
		vips=qe.doQueryList(viprisql);
		
		if(vips==null||vips.size()<=0) {
			vh.put("code", -1);
			vh.put("message", "没有数据，请检查会员ID是否正确！");
			return vh;
		}
		
		vips=(List)vips.get(0);
		rqcode=String.valueOf(vips.get(0));
		adClientId=Tools.getInt(vips.get(1), 0);
		String vipphoto=String.valueOf(vips.get(2));
		
		if(Validator.isNotNull(rqcode)){
			vh.put("code", 0);
			vh.put("message", "已有二维码图片，无需重新生成！");
			return vh;
		}
		
		
		//获取与接口相关的信息对象
		WeUtils wu=WeUtilsManager.getByAdClientId(adClientId);
		if(wu==null) {
			logger.debug("publish menu error->not find WeUtils WeUtilsManager.getByAdClientId("+adClientId+")");
			vh.put("code","-1");
			vh.put("message","请到菜单【微信】的【微信接口配置】中设置APPID与APPSECRET并点击【刷新APP】按钮");
			return vh;
		}
		//判断APPID与APPSECRET是否为空
		if(nds.util.Validator.isNull(wu.getAppId())) {
			logger.debug("publish menu error->appid or appsecret is null[appid:"+wu.getAppId()+"][appsecret:"+wu.getAppSecret()+"]");
			vh.put("code","-1");
			vh.put("message","请到菜单【微信】的【微信接口配置】中设置APPID与APPSECRET并点击【刷新APP】按钮");
			return vh;
		}
		
		WxPublicControl wc=WxPublicControl.getInstance(wu.getAppId());
		if(wc==null) {
			logger.debug("publish menu error->not find WeControl WeControl.getInstance("+wu.getCustomId()+")");
			vh.put("code","-1");
			vh.put("message","请到菜单【微信】的【微信接口配置】中设置APPID与APPSECRET并点击【刷新APP】按钮");
			return vh;
		}
		
		
		
		JSONObject pp=null;
		WePopularizeSupport wpz=new WePopularizeSupport();
		
		pp=wpz.createPermanenceQuickmark(wc, wjo);
		
		if(pp==null) {
			vh.put("code", -1);
			vh.put("message", "系统异常，请重试。");
			return vh;
		}
		
		
		if(pp.has("url")&&nds.util.Validator.isNotNull(pp.optString("url"))) {
			Configurations conf=(Configurations)WebUtils.getServletContextManager().getActor(WebKeys.CONFIGURATIONS);
		    String m_storageDir = conf.getProperty("webclient.upload", "/act.net/webhome");
		    String svrPath = m_storageDir+ File.separator  + wu.getDoMain()+File.separator+"TwoDimensionalCode";
		    String fileName=new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())+".jpg";
    	    svrPath += File.separator + fileName;
		    
    	    JSONObject vipcardaddress=wpz.encoderQRCode(pp.optString("url"), svrPath, "jpg", 18, vipphoto);
    	    if(vipcardaddress.optInt("code",-1)==0){
    	    	String url=pp.optString("url");
    	    	String sqlupdate="update WX_VIP v set v.rqcode =? where v.id=?";
    	    	try {
					qe.executeUpdate(sqlupdate,new Object[]{"/servlets/userfolder/TwoDimensionalCode/"+fileName,vipid});
				} catch (Exception e) {
					e.printStackTrace();
				}
    	    }
			vh.put("code", 0);
			vh.put("message", "创建成功");
		}else {
			vh.put("code", pp.optInt("code"));
			vh.put("message", pp.optString("message"));
		}
		return vh;
	}
}
