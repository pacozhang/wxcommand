package nds.weixin.ext.requesterp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.query.QueryEngine;
import nds.query.QueryException;
import nds.rest.RestUtils;

public class RequestErp {

	private static Logger log = LoggerManager.getInstance().getLogger(RequestErp.class.getName());
	private static final String REQUEST_METHOD = "POST";

	private String serverUrl;
	private String ad_client_id;
	private String skey;
	private boolean iserp;

	/**
	 * @param ad_client_id
	 * @param serverUrl
	 * @param skey
	 * @param iserp	<br/>
	 * serverUrl,skey,iserp可能修改，每次查询生成RequestErp对象
	 */
	public RequestErp(String ad_client_id, String serverUrl,String skey,boolean iserp) {
		this.serverUrl = serverUrl;
		this.ad_client_id = ad_client_id;
		this.skey = skey;
		this.iserp = iserp;
	}
	
	

	public boolean isIserp() {
		return iserp;
	}


	
	/**
	 * @param method	 对应 erp 中RestWelife methodAnalysis(String command, HttpServletRequest  request)的command
	 * @param paramsjo	erp 中wticket$command 存储过程需要的参数
	 * @return
	 */
	public ValueHolder send(String method, JSONObject paramsjo) {
		
		ValueHolder vh = new ValueHolder();

		HashMap<String, String> params = new HashMap<String, String>();
		String ts = String.valueOf(System.currentTimeMillis());
		

		params.put("args[cardid]", ad_client_id);
		params.put("method", method);
		
		params.put("ts", ts);

		try {
			params.put("sig", nds.util.MD5Sum.toCheckSumStr(String.valueOf(ad_client_id) + ts + skey));
		} catch (IOException e) {
			e.printStackTrace();
			vh.put("code", -1); 
			vh.put("message", "RequestErp 生成签名错误");
			log.debug("RequestErp 生成签名错误");
			return vh; 
		}

		/*
		 * params.put("format", "JSON"); 
		 * params.put("client", "");
		 * params.put("ver", "1.0");
		 */



		params.put("args[params]", paramsjo.toString());
		
		log.debug("params->" + params);
		try {
			log.debug("request erp!!!");
			vh = RestUtils.sendRequest(serverUrl, params, REQUEST_METHOD);
			log.debug("vh->" + vh);
		} catch (Throwable tx) {
			log.debug("ERP网络通信障碍!" + tx.getLocalizedMessage());
			tx.printStackTrace();

			vh.put("code", -1);
			vh.put("message", "ERP网络通信障碍!");
		}

		return vh;
	}
	
	public static RequestErp getRequestErp(int client_id) throws QueryException{
		
		// 查询接口相关信息 url,skey
		List<ArrayList> all = QueryEngine.getInstance().doQueryList(
				"select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam,nvl(ifs.ismesauth,'N') from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id where wc.ad_client_id="
						+ client_id);
		if (all == null || all.size() <= 0) {
			throw new QueryException("未找到接口相关信息");
		}
		String skey = String.valueOf(all.get(0).get(3));
		String erpurl = String.valueOf(all.get(0).get(0));
		boolean iserp = "Y".equalsIgnoreCase(String.valueOf(all.get(0).get(2)));
		
		return new RequestErp(String.valueOf(client_id), erpurl, skey, iserp);
	}

}
