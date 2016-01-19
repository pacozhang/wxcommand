package nds.weixinpublicparty.ext;

import org.json.JSONException;
import org.json.JSONObject;

import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.control.web.ClientControllerWebImpl;
import nds.control.web.WebUtils;
import nds.process.SvrProcess;

public class GetAddresssvr  extends SvrProcess{

	@Override
	protected void prepare() {
		
	}

	@Override
	protected String doIt() throws Exception {
		ValueHolder vh=null;
		try{
			ClientControllerWebImpl controller=(ClientControllerWebImpl)WebUtils.getServletContextManager().getActor(nds.util.WebKeys.WEB_CONTROLLER);
			DefaultWebEvent event=new DefaultWebEvent("CommandEvent");


			event.setParameter("command", "nds.weixinpublicparty.ext.GetAddressCommand");
			vh= controller.handleEvent(event);
		}catch(Exception e){
			log.debug("call nds.weixinpublicparty.ext.GetAddressCommand error->"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}

}
