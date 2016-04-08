package nds.weixinpublicparty.ext.common;

import java.io.IOException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.log.Logger;
import nds.publicweixin.ext.tools.RestUtils;
import nds.query.QueryEngine;
import nds.query.QueryException;
import nds.util.Tools;

public class CommonSendCoupon {

	public static ValueHolder sendCoupon(String couponsql, String vipsql, String updatesql, Logger logger) {

		ValueHolder vh = new ValueHolder();
		Hashtable<Integer, List> compaoninf = new Hashtable<Integer, List>();
		Connection con = null;

		try {
			QueryEngine qe = null;

			try {
				qe = QueryEngine.getInstance();
				con = qe.getConnection();
			} catch (Exception e) {
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "ʧ��");
				return vh;
			}

			List allcoupons = null;
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
			}

			if (allcoupons == null || allcoupons.size() <= 0) {
				vh.put("code", "-1");
				vh.put("message", "û�з��ϵ�����");
				return vh;
			}

			List vips = null;

			List all = null;
			List cone = null;
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
			// ��ȯ��¼��
			int countlog = 0;

			HashMap<String, String> params = null;

			for (int i = 0; i < length; i++) {
				couponone = (List) allcoupons.get(i);
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
							// ��ѯ�ӿ������Ϣ url,skey
							all = QueryEngine.getInstance().doQueryList(
									"select ifs.erpurl,ifs.username,ifs.iserp,wc.wxparam,nvl(ifs.ismesauth,'N') from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id where wc.ad_client_id="
											+ ad_client_id);
							if (all == null || all.size() <= 0) {
								continue;
							}

							cone = (List) all.get(0);
							if (cone == null || cone.size() < 3) {
								continue;
							}
							compaoninf.put(ad_client_id, cone);
						} catch (Exception e) {
							logger.error("search company error->" + e.getLocalizedMessage());
							e.printStackTrace();
							continue;
						}
					}
					logger.debug("detailinfo:" + detailinfo);
					vips = qe.doQueryList(vipsql + detailinfo, new Object[] { ad_client_id }, con);
				} catch (QueryException e) {
					e.printStackTrace();
					try {
						if (con != null) {
							con.close();
						}
					} catch (Exception e1) {
						logger.error("search vip cloas connectiion error->" + e1.getLocalizedMessage());
						e1.printStackTrace();
					}
					continue;
				}

				if (vips == null || vips.size() <= 0) {
					vh.put("code", "-1");
					vh.put("message", "û�з��ϵĻ�Ա");
					return vh;
				}

				vipcount = vips.size();
				couponvalue = String.valueOf(couponone.get(4));
				coupontype = String.valueOf(couponone.get(3));
				coupontypecode = String.valueOf(couponone.get(2));
				begintime = String.valueOf(couponone.get(5));
				endtime = String.valueOf(couponone.get(6));

				skey = String.valueOf(cone.get(3));
				erpurl = String.valueOf(cone.get(0));
				iserp = "Y".equalsIgnoreCase(String.valueOf(cone.get(2)));
				for (int j = 0; j < vipcount; j++) {
					vipone = (List) vips.get(j);
					ticketno = "";
					ValueHolder vhr;
					// ���·�ȯ
					if (iserp && !"1".equals(coupontype)) {
						openid = String.valueOf(vipone.get(1));
						vipcardno = String.valueOf(vipone.get(2));
						params = new HashMap<String, String>();
						String ts = String.valueOf(System.currentTimeMillis());
						logger.error("ts->" + ts);
						params.put("args[openid]", openid);
						params.put("args[vipno]", vipcardno);
						params.put("args[couponno]", coupontypecode);
						params.put("args[couponvalue]", couponvalue);
						params.put("args[begintime]", begintime);
						params.put("args[endtime]", endtime);
						params.put("args[cardid]", String.valueOf(ad_client_id));

						params.put("format", "JSON");
						params.put("client", "");
						params.put("ver", "1.0");
						params.put("ts", ts);
						try {
							params.put("sig", nds.util.MD5Sum.toCheckSumStr(String.valueOf(ad_client_id) + ts + skey));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						params.put("method", "sendcoupon");

						logger.error("params->" + params);
						try {
							logger.error("welcome!!!");
							vhr = RestUtils.sendRequest(erpurl, params, "POST");
							logger.error("vhr->" + vhr);
						} catch (Throwable e) {
							logger.error("ERP����ͨ���ϰ�!" + e.getLocalizedMessage());
							e.printStackTrace();
							continue;
						}
						result = (String) vhr.get("message");
						logger.error("coupon offline code result->" + result);
						try {
							jo = new JSONObject(result);
							if (jo.optInt("errCode", -1) == 0) {
								vh.put("code", "0");
								vh.put("message", "�����ɹ�");
								vh.put("data", "�����ɹ�");

								ticketno = jo.optJSONObject("result").optJSONObject("data").optString("code");
							} else {
								vh.put("code", "-1");
								vh.put("message", "����ʧ��");
								continue;
							}
						} catch (Exception e) {
							continue;
						}
					}

					// ���Ϸ�ȯ
					vipid = Tools.getInt(vipone.get(0), -1);
					try {
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
						// ����ɹ�һ�Σ���¼��һ
						countlog = countlog + 1;
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				try {
					qe.executeUpdate(updatesql, new Object[] { fcid }, con);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			// ����жϼ�¼��������0�ͳɹ�
			if (countlog > 0) {
				vh.put("code", "0");
				vh.put("message", "��ȯ�ɹ�");
			} else {
				vh.put("code", "-1");
				vh.put("message", "��ȯʧ��");
			}
		} catch (Exception e) {
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

	}
}
