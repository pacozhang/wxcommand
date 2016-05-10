package nds.weixinpublicparty.ext;

import org.json.JSONArray;
import org.json.JSONObject;

import nds.control.event.DefaultWebEvent;
import nds.control.web.ClientControllerWebImpl;
import nds.control.web.WebUtils;
import nds.process.SvrProcess;
import nds.query.QueryEngine;

public class AutomaticRefundsvr extends SvrProcess{

	@Override
	protected void prepare() {
		
	}

	@Override
	protected String doIt() throws Exception {
		try{
			ClientControllerWebImpl controller=(ClientControllerWebImpl)WebUtils.getServletContextManager().getActor(nds.util.WebKeys.WEB_CONTROLLER);
			DefaultWebEvent event=new DefaultWebEvent("CommandEvent");
			JSONObject vjo=new JSONObject();
            JSONObject auditjo=new JSONObject();
            JSONArray orderids = QueryEngine.getInstance().doQueryJSONArray("select ro.id from wx_refundorders ro join wx_order o on ro.wx_order_id=o.id join wx_gmall wg on o.wx_gmall_id=wg.id where wg.isautorefund='Y' and o.tot_amt>0 and ro.refund_status=? ",new Object[]{2});
           // JSONArray orderids = QueryEngine.getInstance().doQueryJSONArray("select ro.id from wx_refundorders ro join wx_order o on ro.wx_order_id=o.id join wx_gmall wg on o.wx_gmall_id=wg.id where wg.isautorefund='Y' and o.tot_amt>0 and ro.refund_status<>? ",new Object[]{3});
            log.debug("AutomaticRefundsvr  orderids 本次处理退款单条数-->"+orderids.length()+"退款单ids-->"+orderids.toString());
            if (orderids.length()>0) {
				try {
					auditjo.put("orderids", orderids);
					vjo.put("params", auditjo);
				} catch (Exception e) {

				}
				event.put("jsonObject", vjo);
				event.put("nds.control.ejb.UserTransaction" , "N");
				event.setParameter("command", "nds.weixin.wxt.foreign.WeixinRefundOrderCommand");
				controller.handleEvent(event);
			}
		}catch(Exception e){
			log.debug("call nds.weixin.wxt.foreign.WeixinRefundOrderCommand error->"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}
	public boolean internalTransaction()
	{
		return false;
	}

}
