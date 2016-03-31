package nds.weixinpublicparty.ext;

import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.control.web.ClientControllerWebImpl;
import nds.control.web.WebUtils;
import nds.process.SvrProcess;

public class TimeCouponssvr extends SvrProcess {

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub

	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		ValueHolder vh=null;
		try{
			ClientControllerWebImpl controller=(ClientControllerWebImpl)WebUtils.getServletContextManager().getActor(nds.util.WebKeys.WEB_CONTROLLER);
			DefaultWebEvent event=new DefaultWebEvent("CommandEvent");

			event.setParameter("command", "nds.weixinpublicparty.ext.TimeCouponsCommand");
			vh= controller.handleEvent(event);
		}catch(Exception e){
			log.debug("call nds.weixinpublicparty.ext.TimeCouponsCommand error->"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}

}
