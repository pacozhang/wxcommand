package nds.weixinpublicparty.ext;

import java.sql.Clob;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nds.process.SvrProcess;
import nds.query.QueryEngine;
import nds.query.QueryException;
import nds.util.Tools;
import nds.weixin.ext.requesterp.CommonSendCoupon;
import nds.weixin.ext.requesterp.HandleSendCouponResult;
import nds.weixin.ext.requesterp.HandleTimeCouponResult;

public class TimeCouponssvr extends SvrProcess {
	
	private static HandleSendCouponResult handleSendCouponResult = new HandleTimeCouponResult(); 
	
	@Override
	protected void prepare() {
		// TODO Auto-generated method stub

	}

	@Override
	protected String doIt() throws Exception {
		String nowdate = new SimpleDateFormat("yyyyMMdd").format(new Date());
		String couponsql = "select fc.ad_client_id,fc.id,cp.num,nvl(cp.usetype1,2),nvl(cp.value,'0'),to_char(nvl(cp.starttime,sysdate), 'YYYYMMDD'),to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(sysdate,1)),nvl(cp.starttime,sysdate)+nvl(cp.validay,0)), 'YYYYMMDD')"
				+ ",get_fitler_sql(fc.releasevips) from wx_timecoupons fc join WX_COUPON cp on cp.id=fc.issuingcoupons"
				+ " where nvl(cp.isactive, 'N') = 'Y'" + " and fc.senddate = " + nowdate
				+ " and nvl(fc.status,'1') = '2'" + " and nvl(fc.whethernot,'Y') = 'N'"
				+ " and to_char(sysdate,'YYYYMMDD')<to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(sysdate,1)),nvl(cp.starttime,sysdate)+nvl(cp.validay,0)), 'YYYYMMDD')";
		// 查出要发的会员
		String vipsql = "select v.id,v.wechatno,v.vipcardno" + " from   wx_vip v" + " where  v.ad_client_id=?"
				+ " and not exists (select 1 from wx_timecoupons_success wts where wts.wx_vip_id=v.id and wts.wx_timecoupon_id=?)"
				+ " and rownum<=5000"
				+ " and v.id ";
		// 更新是否发放
		String updatesql = "update wx_timecoupons fc set fc.whethernot='Y'  where fc.id=?";
		
		QueryEngine qe = null;
		try {

			qe = QueryEngine.getInstance();
			List<ArrayList> allcoupons = qe.doQueryList(couponsql);

			if (allcoupons == null || allcoupons.size() <= 0) {
				log.debug("TimeCouponssvr 没有符合的数据");
			} else {
				CommonSendCoupon commonSendCoupon;
				List<ArrayList> vips = null;


				int ad_client_id;
				int sendCouponid;
				String couponno;
				String coupontype;
				String couponvalue;
				String begintime;
				String endtime;
				String releasevips;

				for (ArrayList couponone : allcoupons) {

					ad_client_id = Tools.getInt(couponone.get(0), -1);
					sendCouponid = Tools.getInt(couponone.get(1), -1);
					couponno = String.valueOf(couponone.get(2));
					coupontype = String.valueOf(couponone.get(3));
					couponvalue = String.valueOf(couponone.get(4));
					begintime = String.valueOf(couponone.get(5));
					endtime = String.valueOf(couponone.get(6));
					Clob clob = (Clob) couponone.get(7);
					if (clob == null) {
						log.debug("TimeCouponssvr 没有符合的vip数据" + sendCouponid);
						continue;
					}
					try {
						releasevips = clob.getSubString((long) 1, (int) clob.length());
					} catch (SQLException e) {
						e.printStackTrace();
						log.debug("TimeCouponssvr 查询vip异常" + sendCouponid);
						continue;
					}

					vips = qe.doQueryList(vipsql + releasevips, new Object[] { ad_client_id, sendCouponid});

					if (vips == null || vips.size() <= 0) {
						log.debug("id:" + sendCouponid + " 没有符合的会员");
						// 没有会员未发放时，更新发券结果
						qe.executeUpdate(updatesql, new Object[] { sendCouponid });

						continue;
					}
					commonSendCoupon = new CommonSendCoupon(ad_client_id, sendCouponid, couponno, coupontype,
							couponvalue, begintime, endtime, vips, handleSendCouponResult);
					commonSendCoupon.sendCoupon();
				}

			}
		} catch (QueryException e) {
			e.printStackTrace();
			log.error("search wx_autosendcoupon error->" + e.getLocalizedMessage());
		}

		return null;
	}

}
