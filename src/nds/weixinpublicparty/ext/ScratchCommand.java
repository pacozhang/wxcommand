package nds.weixinpublicparty.ext;

import java.rmi.RemoteException;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.util.NDSException;

import org.json.JSONObject;

public class ScratchCommand extends Command {

	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {
		ValueHolder vh = new ValueHolder();
		JSONObject jo = (JSONObject) event.getParameterValue("jsonObject");
		try {
			jo = jo.optJSONObject("params");
			logger.debug("WX_SCRATCHTICKET params adClientId->"+jo.optString("adClientId"));
			logger.debug("WX_SCRATCHTICKET params wxScratchticketId->"+jo.optString("wxScratchticketId"));
			logger.debug("WX_SCRATCHTICKET params wxVipId->"+jo.optString("wxVipId"));
		} catch (Exception e) {
			logger.debug("WX_SCRATCHTICKET error->params error");
			vh.put("code", "-1");
			vh.put("message", "params error->");
			return vh;
		}

		Scratch scratch = Scratch.getInstance(jo.optString("adClientId"));
		try {
			String result = null;
			result = scratch.scratch(jo.optString("adClientId"),
					jo.optString("wxScratchticketId"), jo.optString("wxVipId"));
			vh.put("code", "0");
			vh.put("message", result);
		} catch (Exception e) {
			logger.debug("WX_SCRATCHTICKET error->exception error");
			vh.put("code", "-1");
			vh.put("message", "exception error->" + e.getMessage());
			return vh;
		}
		return vh;
	}

}
