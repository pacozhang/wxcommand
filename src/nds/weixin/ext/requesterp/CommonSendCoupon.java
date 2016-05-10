package nds.weixin.ext.requesterp;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.query.QueryEngine;
import nds.query.QueryException;
import nds.util.Tools;

public class CommonSendCoupon {
	
	private static Logger logger = LoggerManager.getInstance().getLogger(CommonSendCoupon.class.getName());
	
	private static final String METHOD = "sendcouponold";

	private int ad_client_id;
	private int sendCouponid;
	private String couponno;
	private String coupontype;
	private String couponvalue;
	private String begintime;
	private String endtime;
	
	private HandleSendCouponResult handleSendCouponResult;
	
	private List<ArrayList> releasevipInfos;
	
	/**
	 * @param sendCouponid <br/>
	 * 		若是批量发券类型的表数据发券,传入发券表id<br/>
	 *		若是对单一会员发某张券,传入券id<br/>
	 * @param releasevipInfos List<ArrayList> <br/>
	 * 			需要发券的会员<br/>
	 * 		for(ArrayList vipone : releasevipInfos)<br/>
	 *			int vipid = Tools.getInt(vipone.get(0), -1);<br/>
	 *			String openid = String.valueOf(vipone.get(1));<br/>
	 *			String vipcardno = String.valueOf(vipone.get(2));<br/>
	 * @param handleSendCouponResult 为空时不执行<br/>
	 * 		对某个vip发券成功后执行handleSendCouponResult.doSuccess(vipid, sendCouponid, qe, con);<br/>
	 * 		失败后执行handleSendCouponResult.doFail(vipid, sendCouponid, qe, con);
	 * 
	 */
	public CommonSendCoupon(int ad_client_id, int sendCouponid, String couponno, String coupontype, String couponvalue,
			String begintime, String endtime,List<ArrayList> releasevipInfos,  HandleSendCouponResult handleSendCouponResult) {
		this.ad_client_id = ad_client_id;
		this.sendCouponid = sendCouponid;
		this.couponno = couponno;
		this.coupontype = coupontype;
		this.couponvalue = couponvalue;
		this.begintime = begintime;
		this.endtime = endtime;
		this.releasevipInfos = releasevipInfos;
		this.handleSendCouponResult = handleSendCouponResult;
	}
	
	public int getAd_client_id() {
		return ad_client_id;
	}
	public int getSendCouponid() {
		return sendCouponid;
	}
	public String getCouponno() {
		return couponno;
	}
	public String getCoupontype() {
		return coupontype;
	}
	public String getCouponvalue() {
		return couponvalue;
	}
	public String getBegintime() {
		return begintime;
	}
	public String getEndtime() {
		return endtime;
	}

	public List<ArrayList> getReleasevipInfos() {
		return releasevipInfos;
	}
	
	


	public ValueHolder sendCoupon(){
		ValueHolder vh = new ValueHolder();
		if(releasevipInfos==null||releasevipInfos.size()==0){
			vh.put("code", "-1");
			vh.put("message", "CommonSendCoupon发送会员为空");
			return vh;
		}


		Connection con = null;
		try {

			QueryEngine qe = null;

			qe = QueryEngine.getInstance();
			con = qe.getConnection();

			if(requestErp==null){
				requestErp = RequestErp.getRequestErp(ad_client_id);
			}
			
			JSONObject consumejo;
			int sendcount = 0;
			for(ArrayList vipone : releasevipInfos){
				
				int vipid = Tools.getInt(vipone.get(0), -1);
				String openid = String.valueOf(vipone.get(1));
				String vipcardno = String.valueOf(vipone.get(2));
				
				String ticketno = "";
				
				try {

					// 线下发券
					if (requestErp.isIserp() && !"1".equals(coupontype)) {

						JSONObject sendCouponjo = new JSONObject();

						sendCouponjo.put("openid", openid);
						sendCouponjo.put("vipon", vipcardno);
						sendCouponjo.put("couponno", couponno);
						sendCouponjo.put("couponvalue", couponvalue);
						sendCouponjo.put("begintime", begintime);
						sendCouponjo.put("endtime", endtime);

						ValueHolder vhr = requestErp.send(METHOD, sendCouponjo);

						if (!"0".equals(String.valueOf(vhr.get("code")))) {
							logger.debug("请求erp失败,失败记录1");
							handleFail(vipid, sendCouponid, qe, con);
							continue;
						}

						String result = (String) vhr.get("message");
						logger.debug("coupon offline code result->" + result);

						JSONObject jo = new JSONObject(result);
						if (jo.optInt("errCode", -1) == 0) {
							logger.debug("openid:" + openid + "--couponno" + couponno + "--线下发劵成功");
							ticketno = jo.optJSONObject("result").optJSONObject("data").optString("code");
						} else {
							
							logger.debug("openid:" + openid + "--couponno" + couponno + "线下发劵失败2");
							handleFail(vipid, sendCouponid, qe, con);
							continue;
						}

					}

					// 线上发券


					consumejo = new JSONObject();
					consumejo.put("vipid", vipid);
					consumejo.put("couponcode", couponno);
					consumejo.put("tickno", ticketno);

					String resultStr = null;
					ArrayList oparams = new ArrayList();
					oparams.add(ad_client_id);
					oparams.add(consumejo.toString());
					logger.error("CommonSendCoupon send coupon call oracle params->" + consumejo.toString());
					ArrayList returnpara = new ArrayList();
					returnpara.add(java.sql.Clob.class);
					Collection list = QueryEngine.getInstance().executeFunction("wx_coupon_$r_send", oparams,
							returnpara);
					resultStr = (String) list.iterator().next();

					logger.debug("resultStr->" + resultStr);
					JSONObject jo2 = new JSONObject(resultStr);

					if (jo2.optInt("code", -1) != 0) {
						logger.debug("线上发券失败,失败记录3");
						handleFail(vipid, sendCouponid, qe, con);

					}else{
						// 这个成功一次，记录加一,且插入成功数据
						sendcount++;
						handleSuccess(vipid, sendCouponid, qe, con);
					}

				} catch (JSONException e) {
					e.printStackTrace();
					logger.debug("解析数据错误，失败记录4");
					handleFail(vipid, sendCouponid, qe, con);
				} catch (QueryException e) {
					e.printStackTrace();
					logger.debug("执行sql异常，失败记录5");
					handleFail(vipid, sendCouponid, qe, con);
				}

			}
			
			vh.put("code", "0");
			vh.put("message", sendcount);
			logger.debug("本次发券请求总条数："+releasevipInfos.size()+"\t 成功条数："+sendcount);

		} catch (QueryException e) {
			logger.error("search company error->" + e.getLocalizedMessage());
			e.printStackTrace();
			vh.put("code", "-1");
			vh.put("message", "search company error->");
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (Exception e) {

				}
			}
		}

		return vh;

	
	}
	private RequestErp requestErp;
	

	private void handleSuccess(int vipid,int sendCouponid,QueryEngine qe,Connection con){
		if(handleSendCouponResult==null){
			return;
		}
		try {
			handleSendCouponResult.doSuccess(vipid, sendCouponid, qe, con);
		} catch (QueryException e) {
			e.printStackTrace();
			logger.error("处理发券失败时异常--vipid"+vipid+"--sendCouponid"+sendCouponid);
		}
	}
	
	private void handleFail(int vipid,int sendCouponid,QueryEngine qe,Connection con){
		if(handleSendCouponResult==null){
			return;
		}
		try {
			handleSendCouponResult.doFail(vipid, sendCouponid, qe, con);
		} catch (QueryException e) {
			e.printStackTrace();
			logger.error("处理发券成功时异常--vipid"+vipid+"--sendCouponid"+sendCouponid);
		}
	}
	
	
	
	public static ValueHolder sendCoupon(String couponsql, String vipsql, String updatesql, Logger logger) {
		return null;
		/*

		ValueHolder vh = new ValueHolder();
		Hashtable<Integer, RequestErp> compaoninf = new Hashtable<Integer, RequestErp>();
		Connection con = null;

		try {

			QueryEngine qe = null;

			qe = QueryEngine.getInstance();
			con = qe.getConnection();

			List<ArrayList> allcoupons = null;
			try {
				allcoupons = qe.doQueryList(couponsql, con);
			} catch (QueryException e) {
				logger.error("search wx_autosendcoupon error->" + e.getLocalizedMessage());
				e.printStackTrace();
				try {
					if (con != null) {
						con.close();
					}
				} catch (Exception e1) {
					logger.error("search wx_autosendcoupon cloas connectiion error->" + e1.getLocalizedMessage());
					e1.printStackTrace();
				}
				vh.put("code", "-1");
				vh.put("message", "失败");
				return vh;
			}

			if (allcoupons == null || allcoupons.size() <= 0) {
				vh.put("code", "-1");
				vh.put("message", "没有符合的数据");
				return vh;
			}

			List<ArrayList> vips = null;

			List<ArrayList> all = null;
			RequestErp cone = null;
			int ad_client_id = 0;
			List couponone = null;
			int length = allcoupons.size();

			String coupontypecode = null;
			String coupontype = null;
			String couponvalue = null;
			String begintime = null;
			String endtime = null;
			boolean iserp = false;
			String openid = null;
			int vipcount = 0;
			String vipcardno = null;
			List vipone = null;
			String skey = null;
			String erpurl = null;
			String result = null;
			JSONObject jo = null;
			String ticketno = null;
			JSONObject consumejo = null;
			int vipid = 0;
			int fcid = 0;

			for (int i = 0; i < length; i++) {
				couponone = allcoupons.get(i);
				ad_client_id = Tools.getInt(couponone.get(0), -1);
				fcid = Tools.getInt(couponone.get(1), -1);
				Clob clob = (Clob) couponone.get(7);
				String detailinfo = "";
				if (clob != null) {
					try {
						detailinfo = clob.getSubString((long) 1, (int) clob.length());
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (ad_client_id <= 0) {
					logger.error("ad_client_id error->" + ad_client_id);
					continue;
				}
				try {
					if (compaoninf.containsKey(ad_client_id)) {
						cone = compaoninf.get(ad_client_id);
					} else {
						try {
							// 查询接口相关信息 url,skey
							all = QueryEngine.getInstance().doQueryList(
									"select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam,nvl(ifs.ismesauth,'N') from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id where wc.ad_client_id="
											+ ad_client_id);
							if (all == null || all.size() <= 0) {
								continue;
							}
							skey = String.valueOf(all.get(0).get(3));
							erpurl = String.valueOf(all.get(0).get(0));
							iserp = "Y".equalsIgnoreCase(String.valueOf(all.get(0).get(2)));
							cone = new RequestErp(String.valueOf(ad_client_id), erpurl, skey,iserp);

							compaoninf.put(ad_client_id, cone);
						} catch (QueryException e) {
							logger.error("search company error->" + e.getLocalizedMessage());
							e.printStackTrace();
							continue;
						}
					}
					logger.debug("detailinfo:" + detailinfo);
					vips = qe.doQueryList(vipsql + detailinfo, new Object[] { ad_client_id }, con);
				} catch (QueryException e) {
					e.printStackTrace();
					logger.debug("id:" + String.valueOf(couponone.get(1)) + "  查询接口相关信息失败");
					continue;
				}

				if (vips == null || vips.size() <= 0) {
					logger.debug("id:" + String.valueOf(couponone.get(1)) + " 没有符合的会员");
					// 没有会员未发放时，更新发券结果
					qe.executeUpdate(updatesql, new Object[] {  fcid }, con);
					continue;
				}

				vipcount = vips.size();
				couponvalue = String.valueOf(couponone.get(4));
				coupontype = String.valueOf(couponone.get(3));
				coupontypecode = String.valueOf(couponone.get(2));
				begintime = String.valueOf(couponone.get(5));
				endtime = String.valueOf(couponone.get(6));

				int sendcount = 0;
				for (int j = 0; j < vipcount; j++) {

					vipone = vips.get(j);
					ticketno = "";
					ValueHolder vhr;

					try {

						// 线下发券
						if (iserp && !"1".equals(coupontype)) {
							openid = String.valueOf(vipone.get(1));
							vipcardno = String.valueOf(vipone.get(2));

							JSONObject sendCouponjo = new JSONObject();

							sendCouponjo.put("openid", openid);
							sendCouponjo.put("vipon", vipcardno);
							sendCouponjo.put("couponno", coupontypecode);
							sendCouponjo.put("couponvalue", couponvalue);
							sendCouponjo.put("begintime", begintime);
							sendCouponjo.put("endtime", endtime);

							vhr = cone.send(METHOD, sendCouponjo);

							if (!"0".equals(String.valueOf(vhr.get("code")))) {
								// 请求erp失败,失败记录1
								logger.debug("请求erp失败,失败记录1");
								continue;
							}

							result = (String) vhr.get("message");
							logger.debug("coupon offline code result->" + result);

							jo = new JSONObject(result);
							if (jo.optInt("errCode", -1) == 0) {
								logger.debug("openid:" + openid + "--couponno" + coupontypecode + "--线下发劵成功");
								ticketno = jo.optJSONObject("result").optJSONObject("data").optString("code");
							} else {
								// 线下发劵失败,失败记录2
								
								logger.debug("openid:" + openid + "--couponno" + coupontypecode + "线下发劵失败2");
								continue;
							}

						}

						// 线上发券
						vipid = Tools.getInt(vipone.get(0), -1);

						consumejo = new JSONObject();
						consumejo.put("vipid", vipid);
						consumejo.put("couponcode", coupontypecode);
						consumejo.put("tickno", ticketno);

						String resultStr = null;
						ArrayList oparams = new ArrayList();
						oparams.add(ad_client_id);
						oparams.add(consumejo.toString());
						logger.error("vipbirthday send coupon call oracle params->" + consumejo.toString());
						ArrayList returnpara = new ArrayList();
						returnpara.add(java.sql.Clob.class);
						Collection list = QueryEngine.getInstance().executeFunction("wx_coupon_$r_send", oparams,
								returnpara);
						resultStr = (String) list.iterator().next();

						logger.debug("resultStr->" + resultStr);
						JSONObject jo2 = new JSONObject(resultStr);

						if (jo2.optInt("code", -1) != 0) {
							//线上发券失败,失败记录3
							logger.debug("线上发券失败,失败记录3");

						}else{
							// 这个成功一次，记录加一
							sendcount++;
						}

					} catch (JSONException e) {
						e.printStackTrace();
						// 解析数据错误，失败记录4
						logger.debug("解析数据错误，失败记录4");
					}
				}


			}

		} catch (QueryException e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (Exception e) {

				}
			}
		}

		return vh;

	*/}

}
